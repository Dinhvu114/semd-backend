package com.semd.backend.controller;

import com.semd.backend.service.FileStorageService;
import com.semd.backend.service.EmergencyCallService;
import com.semd.backend.dto.common.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/files")
@Tag(name = "File Storage", description = "Quản lý lưu trữ file trên MinIO")
public class FileController {

    private final FileStorageService fileStorageService;
    private final EmergencyCallService callService;

    public FileController(FileStorageService fileStorageService, EmergencyCallService callService) {
        this.fileStorageService = fileStorageService;
        this.callService = callService;
    }

    @PostMapping(value = "/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Upload file", description = "Upload file âm thanh hoặc ảnh lên MinIO")
    public ResponseEntity<BaseResponse<com.semd.backend.dto.FileUploadResponse>> upload(
            @RequestPart("file") MultipartFile file) {

        com.semd.backend.dto.FileUploadResponse response = fileStorageService.uploadFile(file);
        return ResponseEntity.ok(BaseResponse.success("Upload file thành công", response));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Liệt kê tệp tin trên MinIO", description = "Liệt kê toàn bộ các đối tượng trong bucket khớp với prefix (Yêu cầu quyền ADMIN)")
    public ResponseEntity<BaseResponse<java.util.List<com.semd.backend.dto.MinioObjectDto>>> listFiles(
            @io.swagger.v3.oas.annotations.Parameter(
                description = "Prefix thư mục lọc file",
                schema = @io.swagger.v3.oas.annotations.media.Schema(allowableValues = {"emergency-calls/", ""})
            )
            @RequestParam(value = "prefix", required = false, defaultValue = "") String prefix
    ) {
        java.util.List<com.semd.backend.dto.MinioObjectDto> files = fileStorageService.listObjects(prefix);
        return ResponseEntity.ok(BaseResponse.success(files));
    }

    @GetMapping("/metadata/{*objectKey}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy siêu dữ liệu của file", description = "Lấy metadata (kích thước, loại, thời gian sửa đổi) của một object trên MinIO (Yêu cầu quyền ADMIN)")
    public ResponseEntity<BaseResponse<com.semd.backend.dto.MinioObjectDto>> getMetadata(
            @PathVariable("objectKey") String objectKey
    ) {
        com.semd.backend.dto.MinioObjectDto metadata = fileStorageService.getObjectMetadata(objectKey);
        return ResponseEntity.ok(BaseResponse.success(metadata));
    }

    @GetMapping("/download/{*objectKey}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Tải file từ MinIO", description = "Tải xuống tệp tin từ MinIO (Yêu cầu sở hữu cuộc gọi hoặc quyền admin/điều phối)")
    public ResponseEntity<?> downloadFile(
            @PathVariable("objectKey") String objectKey,
            @AuthenticationPrincipal com.semd.backend.security.UserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }

        // 1. Phân quyền: Kiểm tra xem tệp tin có liên kết với cuộc gọi cấp cứu nào không
        com.semd.backend.entity.EmergencyCall call = callService.getCallByAudioObjectKey(objectKey).orElse(null);
        if (call != null) {
            boolean isAdminOrDispatcher = principal.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_DISPATCHER"));
            if (!isAdminOrDispatcher && !call.getReporterPhone().equals(principal.getPhoneNumber())) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                        .body(BaseResponse.fail("Bạn không có quyền tải xuống tệp tin ghi âm của cuộc gọi này", 403));
            }
        } else {
            // Nếu file tự do (chưa được gán vào cuộc gọi nào): Chỉ ADMIN mới được tải
            boolean isAdmin = principal.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (!isAdmin) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                        .body(BaseResponse.fail("Chỉ tài khoản ADMIN mới được quyền tải tệp tin chưa được gán", 403));
            }
        }

        // 2. Download file
        try {
            io.minio.GetObjectResponse stream = fileStorageService.downloadFile(objectKey);
            String filename = objectKey.contains("/") ? objectKey.substring(objectKey.lastIndexOf("/") + 1) : objectKey;
            String contentType = stream.headers().get("Content-Type");
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .body(new org.springframework.core.io.InputStreamResource(stream));

        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("Lỗi khi tải file: " + e.getMessage(), 500));
        }
    }

    @DeleteMapping("/admin/{*objectKey}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa file trên MinIO", description = "Xóa vĩnh viễn một tệp tin trên MinIO (Yêu cầu quyền ADMIN)")
    public ResponseEntity<BaseResponse<Void>> deleteFile(
            @PathVariable("objectKey") String objectKey
    ) {
        fileStorageService.deleteFile(objectKey);
        return ResponseEntity.ok(BaseResponse.success(null));
    }
}
