package com.semd.backend.scheduler;

import com.semd.backend.repository.EmergencyCallRepository;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.messages.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class FileCleanupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(FileCleanupScheduler.class);

    private final MinioClient minioClient;
    private final EmergencyCallRepository callRepository;

    @Value("${minio.bucket-name}")
    private String bucketName;

    public FileCleanupScheduler(MinioClient minioClient, EmergencyCallRepository callRepository) {
        this.minioClient = minioClient;
        this.callRepository = callRepository;
    }

    /**
     * Tác vụ lập lịch chạy lúc 2:00 sáng mỗi ngày để dọn dẹp các tệp tin rác trên MinIO.
     * Tệp rác là các tệp trong thư mục "emergency-calls/" nhưng không được bất kỳ bản ghi nào trong
     * bảng emergency_calls của PostgreSQL tham chiếu tới.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOrphanFiles() {
        logger.info("Bắt đầu tác vụ dọn dẹp file rác trên MinIO...");
        try {
            // 1. Lấy tất cả URL ghi âm đang hoạt động từ database
            List<String> activeUrls = callRepository.findAllAudioUrls();
            Set<String> activeObjectKeys = new HashSet<>();

            String bucketPrefix = "/" + bucketName + "/";
            for (String url : activeUrls) {
                if (url == null) continue;
                int index = url.indexOf(bucketPrefix);
                if (index != -1) {
                    String objectKey = url.substring(index + bucketPrefix.length());
                    activeObjectKeys.add(objectKey);
                } else {
                    // Nếu là đường dẫn tương đối hoặc không chứa tên bucket, lưu trực tiếp
                    activeObjectKeys.add(url);
                }
            }

            logger.info("Tìm thấy {} file ghi âm đang hoạt động trong database.", activeObjectKeys.size());

            // 2. Duyệt tất cả các file trong thư mục "emergency-calls/" của MinIO
            Iterable<Result<Item>> objects = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix("emergency-calls/")
                            .recursive(true)
                            .build()
            );

            int deletedCount = 0;
            for (Result<Item> result : objects) {
                Item item = result.get();
                String objectKey = item.objectName();

                // Bỏ qua nếu là thư mục ảo
                if (item.isDir()) continue;

                // 3. Nếu file trong MinIO không có trong danh sách database -> Xoá file rác
                if (!activeObjectKeys.contains(objectKey)) {
                    logger.info("Phát hiện file rác không được tham chiếu: {}. Đang xoá...", objectKey);
                    minioClient.removeObject(
                            RemoveObjectArgs.builder()
                                    .bucket(bucketName)
                                    .object(objectKey)
                                    .build()
                    );
                    deletedCount++;
                }
            }

            logger.info("Hoàn thành tác vụ dọn dẹp! Đã xoá tổng cộng {} file rác.", deletedCount);

        } catch (Exception e) {
            logger.error("Lỗi xảy ra trong quá trình dọn dẹp file rác MinIO: {}", e.getMessage(), e);
        }
    }
}
