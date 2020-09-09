package com.ncr.storage.service;

import com.google.api.client.util.ByteStreams;
import com.google.api.gax.paging.Page;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.*;
import com.ncr.storage.dto.UrlDTO;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@Slf4j
public class StorageServiceImpl implements StorageService {

    public static final String DEFAULT_FOLDER = "default";

    @Value("${gcp.storage.bucket.name}")
    private String bucketName;

    @Value("${gcp.storage.public.access.base.uri}")
    private String baseURI;

    @Autowired
    private Storage storage;

    @Override
    public UrlDTO handleUpload(String institutionId, String folder, String filename, HttpServletRequest request) throws Exception {

        UploadAttributesDTO attributesDTO = null;
        UrlDTO urlDTO = new UrlDTO();

        try {
            attributesDTO = upload(institutionId, folder, filename, request);

            urlDTO.setImageUrl(attributesDTO.getBasePath() + attributesDTO.getFilePath());
            return urlDTO;
        } catch (Exception e) {
            log.error("Error in file upload for filename: " + filename, e);
            throw e;
        }
    }

    private UploadAttributesDTO upload(String institutionId, String folder, String filename, HttpServletRequest request) throws Exception {
        DiskFileItemFactory factory = new DiskFileItemFactory();
        factory.setRepository(
                new File(System.getProperty("java.io.tmpdir")));
        factory.setSizeThreshold(
                DiskFileItemFactory.DEFAULT_SIZE_THRESHOLD);
        factory.setFileCleaningTracker(null);

        ServletFileUpload upload = new ServletFileUpload(factory);

        List items = upload.parseRequest(request);

        UploadAttributesDTO dto = new UploadAttributesDTO();
        dto.setBasePath(getBasePath());

        Iterator iter = items.iterator();
        while (iter.hasNext()) {
            FileItem item = (FileItem)iter.next();

            if (!item.isFormField()) {

                if(StringUtils.isBlank(filename)){
                    filename = item.getName();

                    if(StringUtils.isNotBlank(filename)){
                        filename = filename.replaceAll(" ","_");
                    }
                    else{
                        log.error(" no filename found ");
                        throw new Exception("NOFILENAME");
                    }
                    log.info("filename using item.getname {} " , filename);
                }
                filename  = filename.toLowerCase();

                dto.setFilePath(buildFilePath(institutionId, folder, filename));

                BlobInfo blobInfo = null;

                try (InputStream uploadedStream = item.getInputStream()){
                   blobInfo = BlobInfo.newBuilder(bucketName, dto.getFilePath())
                           .setContentType(item.getContentType())
                           .setContentDisposition(String.format("inline; filename=\"%s\"", filename))
                           .build();
                    try (WriteChannel writer = storage.writer(blobInfo)) {
                        ByteStreams.copy(uploadedStream, Channels.newOutputStream(writer));
                    }
                } catch (StorageException ex) {
                    if (!(400 == ex.getCode() && "invalid".equals(ex.getReason()))) {
                        throw ex;
                    }
                }
            }
        }

        return dto;
    }

    /**
     * This is very simplified.  Any real service should deal with the paging.
     * @param institutionId
     * @param folder
     * @return
     */
    @Override
    public List<String> getFilesByInstitutionAndFolder (String institutionId, String folder) {
        List<String> files = new ArrayList<>();

        String prefix = institutionId + "/" + StringUtils.defaultIfBlank(folder, DEFAULT_FOLDER);

        Page<Blob> fileBlobs3 = storage.list(bucketName, Storage.BlobListOption.prefix(prefix), Storage.BlobListOption.fields(Storage.BlobField.ID, Storage.BlobField.NAME));

        files = StreamSupport.stream(fileBlobs3.getValues().spliterator(), false)
                .map(blob -> (getBasePath() + blob.getName()))
                .collect(Collectors.toList());

        return files;
    }

    private String buildFilePath(String institutionId, String folder, String filename) {
        String outputFolder = StringUtils.defaultIfBlank(folder, DEFAULT_FOLDER);

        // NOTE:  in normal file systems, we often split out directories that could contain many files for file performance
        // reasons.  However, when using GCP storage, that's not a problem and it's not even a real directory structure.
        // The file name, itself, is the path you choose.  For instance, if you upload a file with a name like:
        // 56/123456/images/boat.jpg, that is the actual name of the object.  No directories.

        StringBuilder stringBuilder = new StringBuilder()
                .append(institutionId)
                .append("/")
                .append(outputFolder)
                .append("/")
                .append(filename);

        return stringBuilder.toString();
    }

    private String getBasePath () {
        StringBuilder base = new StringBuilder()
                .append(baseURI.endsWith("/") ? baseURI : baseURI + "/")
                .append(bucketName)
                .append("/");

        return base.toString();
    }

    @Data
    class UploadAttributesDTO {
        private String basePath;
        private String filePath;
    }
}