package com.semd.backend.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class FileStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.endpoint}")
    private String endpoint;

    public FileStorageService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    /**
     * Upload file lên MinIO
     * @param file  File từ người dùng gửi lên
     * @return URL public của file sau khi upload
     */
    public String uploadFile(MultipartFile file) {
        try {
            // Tự sinh tên file ngẫu nhiên để không bị trùng
            String originalExtension = getFileExtension(file.getOriginalFilename());
            String uniqueFileName = UUID.randomUUID() + originalExtension;

            // Đẩy file lên MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(uniqueFileName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            // Trả về URL để lưu vào PostgreSQL
            return endpoint + "/" + bucketName + "/" + uniqueFileName;

        } catch (Exception e) {
            throw new RuntimeException("Upload file thất bại: " + e.getMessage());
        }
    }

    // Lấy đuôi file (.mp3, .wav, .jpg...)
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf("."));
    }
}
