package com.ncr.storage.service;

import com.ncr.storage.dto.UrlDTO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface StorageService {

    /**
     *
     * @param institutionId
     * @param folder    If null, then use a default folder
     * @param fileName  If null, then use filename from multipart file item
     * @param request
     * @return
     * @throws Exception
     */
    UrlDTO handleUpload(String institutionId, String folder, String fileName, HttpServletRequest request) throws Exception;

    List<String> getFilesByInstitutionAndFolder (String institutionId, String folder);
}