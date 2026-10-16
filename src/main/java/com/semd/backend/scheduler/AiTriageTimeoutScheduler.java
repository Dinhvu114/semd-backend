package com.semd.backend.scheduler;

import com.semd.backend.entity.EmergencyCall;
import com.semd.backend.entity.EmergencyCallStatus;
import com.semd.backend.entity.EmergencyCallType;
import com.semd.backend.repository.EmergencyCallRepository;
import com.semd.backend.service.EmergencyCallService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class AiTriageTimeoutScheduler {

    private static final Logger log = LoggerFactory.getLogger(AiTriageTimeoutScheduler.class);

    private final EmergencyCallRepository callRepository;
    private final EmergencyCallService callService;

    @Value("${app.ai.timeout-seconds:15}")
    private int timeoutSeconds;

    public AiTriageTimeoutScheduler(EmergencyCallRepository callRepository, EmergencyCallService callService) {
        this.callRepository = callRepository;
        this.callService = callService;
    }

    /**
     * Chạy định kỳ mỗi 5 giây quét các cuộc gọi ghi âm đang chờ AI (status = RECEIVED)
     * nhưng đã quá timeoutSeconds (mặc định 15s).
     * Khi phát hiện, kích hoạt Fallback tạo DispatchRequest để ca cấp cứu hiện lên Dashboard điều phối.
     */
    @Scheduled(fixedDelay = 5000)
    public void scanAndHandleTimedOutCalls() {
        try {
            LocalDateTime threshold = LocalDateTime.now().minusSeconds(timeoutSeconds);
            List<EmergencyCall> timedOutCalls = callRepository.findByCallTypeAndStatusAndCallStartTimeBefore(
                    EmergencyCallType.CALL,
                    EmergencyCallStatus.RECEIVED,
                    threshold
            );

            if (!timedOutCalls.isEmpty()) {
                log.info("Phát hiện {} cuộc gọi voice quá hạn phản hồi AI ({}s)", timedOutCalls.size(), timeoutSeconds);
                for (EmergencyCall call : timedOutCalls) {
                    try {
                        callService.handleAiTimeout(call.getId());
                    } catch (Exception e) {
                        log.error("Lỗi khi xử lý timeout cho cuộc gọi #{}: {}", call.getId(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Lỗi trong quá trình quét AI Timeout: {}", e.getMessage());
        }
    }
}
