package com.semd.backend.controller;

import com.semd.backend.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/upload")
    @Operation(summary = "Upload file", description = "Upload file âm thanh hoặc ảnh lên MinIO")
    public ResponseEntity<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file) {

        String fileUrl = fileStorageService.uploadFile(file);

        return ResponseEntity.ok(Map.of(
                "message", "Upload thành công!",
                "url", fileUrl
        ));
    }
}