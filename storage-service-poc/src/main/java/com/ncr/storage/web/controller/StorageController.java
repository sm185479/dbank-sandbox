package com.ncr.storage.web.controller;

import com.intuit.ifs.afeLibrary.util.dto.RequestContext;
import com.ncr.storage.dto.UrlDTO;
import com.ncr.storage.service.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@Slf4j
public class StorageController {

    @Autowired
    private StorageService storageService;

    @GetMapping(value = "/v1/storage/files", produces = {MediaType.APPLICATION_JSON_VALUE})
    public DeferredResult<ResponseEntity> getFiles(@RequestParam(required = false) String folder) {
        DeferredResult<ResponseEntity> response = new DeferredResult<>();

        try{
            List<String> files = storageService.getFilesByInstitutionAndFolder(RequestContext.get(RequestContext.BC_ID),
                    folder);

            response.setResult(new ResponseEntity<>(files, HttpStatus.OK));

        }catch (Exception e){
            response.setResult(new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR));
        }

        return response;
    }

    @PostMapping(value = "/v1/storage", produces = {MediaType.APPLICATION_JSON_VALUE})
    public DeferredResult<ResponseEntity> uploadFile(@RequestParam(required = false) String folder, HttpServletRequest request){

        DeferredResult<ResponseEntity> response = new DeferredResult<>();

        log.debug("uploading file to folder: " + folder);

        try{
            UrlDTO dto = storageService.handleUpload(RequestContext.get(RequestContext.BC_ID),
                    folder, null, request);

            response.setResult(new ResponseEntity<>(dto, HttpStatus.OK));

        }catch (Exception e){
            response.setResult(new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR));
        }

        return response;
    }

}
