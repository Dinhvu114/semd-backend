package com.semd.backend.controller;

import com.semd.backend.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/files")
@Tag(name = "File Storage", description = "Upload file lên MinIO")
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping(value = "/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Upload file", description = "Upload file âm thanh hoặc ảnh lên MinIO")
    public ResponseEntity<com.semd.backend.dto.FileUploadResponse> upload(
            @RequestPart("file") MultipartFile file) {


        com.semd.backend.dto.FileUploadResponse response = fileStorageService.uploadFile(file);
        return ResponseEntity.ok(response);
    }

}