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
import java.util.Locale;
import java.util.Set;

@Service
public class FileStorageService {

    private static final long MAX_UPLOAD_SIZE = 25L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".aac", ".m4a", ".mp3", ".mp4", ".ogg", ".wav", ".webm",
            ".jpg", ".jpeg", ".png", ".webp"
    );

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
        validateUpload(file);
        try {
            // Định dạng đường dẫn dạng: emergency-calls/yyyy/MM/dd/uuid.ext
            String datePath = DateTimeFormatter.ofPattern("yyyy/MM/dd").format(LocalDate.now());
            String originalExtension = getFileExtension(file.getOriginalFilename()).toLowerCase(Locale.ROOT);
            String uniqueFileName = UUID.randomUUID() + originalExtension;
            String objectKey = "emergency-calls/" + datePath + "/" + uniqueFileName;

            // Đẩy file lên MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(resolveContentType(file))
                            .build()
            );

            return new FileUploadResponse(objectKey, resolveContentType(file), file.getSize());

        } catch (Exception e) {
            throw new RuntimeException("Upload file thất bại: " + e.getMessage());
        }
    }

    private void validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File upload không được để trống");
        }
        if (file.getSize() > MAX_UPLOAD_SIZE) {
            throw new IllegalArgumentException("File upload không được vượt quá 25 MB");
        }

        String extension = getFileExtension(file.getOriginalFilename()).toLowerCase(Locale.ROOT);
        String contentType = resolveContentType(file);
        boolean supportedMimeType = contentType.startsWith("audio/") || contentType.startsWith("image/");
        boolean genericMimeWithKnownExtension = "application/octet-stream".equals(contentType)
                && ALLOWED_EXTENSIONS.contains(extension);
        if (!ALLOWED_EXTENSIONS.contains(extension) || (!supportedMimeType && !genericMimeWithKnownExtension)) {
            throw new IllegalArgumentException("Chỉ hỗ trợ file âm thanh hoặc hình ảnh hợp lệ");
        }
    }

    private String resolveContentType(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType;
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

    /**
     * Liệt kê danh sách các đối tượng trên MinIO khớp với prefix
     */
    public java.util.List<com.semd.backend.dto.MinioObjectDto> listObjects(String prefix) {
        java.util.List<com.semd.backend.dto.MinioObjectDto> objectList = new java.util.ArrayList<>();
        try {
            Iterable<io.minio.Result<io.minio.messages.Item>> results = minioClient.listObjects(
                    io.minio.ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(prefix)
                            .recursive(true)
                            .build()
            );
            for (io.minio.Result<io.minio.messages.Item> result : results) {
                io.minio.messages.Item item = result.get();
                if (!item.isDir()) {
                    String contentType = java.net.URLConnection.guessContentTypeFromName(item.objectName());
                    if (contentType == null) {
                        contentType = "application/octet-stream";
                    }
                    objectList.add(new com.semd.backend.dto.MinioObjectDto(
                            item.objectName(),
                            item.size(),
                            contentType,
                            item.lastModified()
                    ));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Không thể liệt kê danh sách tệp tin từ MinIO: " + e.getMessage());
        }
        return objectList;
    }

    /**
     * Lấy thông tin siêu dữ liệu (metadata) của đối tượng
     */
    public com.semd.backend.dto.MinioObjectDto getObjectMetadata(String objectKey) {
        try {
            io.minio.StatObjectResponse stat = minioClient.statObject(
                    io.minio.StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );
            return new com.semd.backend.dto.MinioObjectDto(
                    stat.object(),
                    stat.size(),
                    stat.contentType(),
                    stat.lastModified()
            );
        } catch (Exception e) {
            throw new RuntimeException("Không thể lấy thông tin metadata của tệp tin: " + e.getMessage());
        }
    }

    /**
     * Download đối tượng nhị phân từ MinIO
     */
    public io.minio.GetObjectResponse downloadFile(String objectKey) {
        try {
            return minioClient.getObject(
                    io.minio.GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Không thể tải tệp tin từ MinIO: " + e.getMessage());
        }
    }

    /**
     * Xoá đối tượng khỏi MinIO
     */
    public void deleteFile(String objectKey) {
        try {
            minioClient.removeObject(
                    io.minio.RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Không thể xóa tệp tin khỏi MinIO: " + e.getMessage());
        }
    }
}

