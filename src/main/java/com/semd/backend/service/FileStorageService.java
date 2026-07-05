package com.semd.backend.service;

import com.semd.backend.dto.FileUploadResponse;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
     * Upload file lên MinIO và trả về thông tin chi tiết bao gồm objectKey
     * @param file  File từ người dùng gửi lên
     * @return FileUploadResponse chứa objectKey, contentType và size
     */
    public FileUploadResponse uploadFile(MultipartFile file) {
        try {
            // Định dạng đường dẫn dạng: emergency-calls/yyyy/MM/dd/uuid.ext
            String datePath = DateTimeFormatter.ofPattern("yyyy/MM/dd").format(LocalDate.now());
            String originalExtension = getFileExtension(file.getOriginalFilename());
            String uniqueFileName = UUID.randomUUID() + originalExtension;
            String objectKey = "emergency-calls/" + datePath + "/" + uniqueFileName;

            // Đẩy file lên MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            return new FileUploadResponse(objectKey, file.getContentType(), file.getSize());

        } catch (Exception e) {
            throw new RuntimeException("Upload file thất bại: " + e.getMessage());
        }
    }

    /**
     * Lấy URL công khai của file từ object key
     */
    public String getPublicUrl(String objectKey) {
        if (objectKey == null) return null;
        // Đảm bảo không bị lặp endpoint
        if (objectKey.startsWith("http://") || objectKey.startsWith("https://")) {
            return objectKey;
        }
        return endpoint + "/" + bucketName + "/" + objectKey;
    }

    // Lấy đuôi file (.mp3, .wav, .jpg...)
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf("."));
    }
}
