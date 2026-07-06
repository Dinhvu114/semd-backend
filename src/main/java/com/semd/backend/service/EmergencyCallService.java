package com.semd.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.semd.backend.entity.AuditLog;
import com.semd.backend.entity.EmergencyCall;
import com.semd.backend.repository.AuditLogRepository;
import com.semd.backend.repository.EmergencyCallRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.semd.backend.entity.DispatchRequest;
import com.semd.backend.entity.ServiceType;
import com.semd.backend.entity.EdgeNode;
import com.semd.backend.repository.DispatchRequestRepository;
import com.semd.backend.repository.ServiceTypeRepository;
import com.semd.backend.repository.EdgeNodeRepository;
import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class EmergencyCallService {

    private final EmergencyCallRepository callRepository;
    private final FileStorageService fileStorageService;
    private final AuditLogRepository auditLogRepository;
    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final DispatchRequestRepository dispatchRequestRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final EdgeNodeRepository edgeNodeRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public EmergencyCallService(EmergencyCallRepository callRepository,
                                FileStorageService fileStorageService,
                                AuditLogRepository auditLogRepository,
                                StringRedisTemplate redisTemplate,
                                SimpMessagingTemplate messagingTemplate,
                                DispatchRequestRepository dispatchRequestRepository,
                                ServiceTypeRepository serviceTypeRepository,
                                EdgeNodeRepository edgeNodeRepository) {
        this.callRepository = callRepository;
        this.fileStorageService = fileStorageService;
        this.auditLogRepository = auditLogRepository;
        this.redisTemplate = redisTemplate;
        this.messagingTemplate = messagingTemplate;
        this.dispatchRequestRepository = dispatchRequestRepository;
        this.serviceTypeRepository = serviceTypeRepository;
        this.edgeNodeRepository = edgeNodeRepository;
    }

    public EmergencyCall createEmergencySosCall(String reporterPhone, String reporterName, Double latitude, Double longitude) {
        EmergencyCall call = new EmergencyCall();
        call.setReporterPhone(reporterPhone);
        call.setReporterName(reporterName);
        call.setCallStartTime(LocalDateTime.now());
        call.setStatus("SOS");
        
        if (longitude != null && latitude != null) {
            call.setLocation(geometryFactory.createPoint(new Coordinate(longitude, latitude)));
        }
        
        EmergencyCall savedCall = callRepository.save(call);

        saveAuditLog(savedCall, longitude, latitude);

        // Tự động tìm Vùng biên chứa tọa độ cuộc gọi
        EdgeNode edgeNode = null;
        if (longitude != null && latitude != null) {
            edgeNode = edgeNodeRepository.findContainingNode(longitude, latitude).orElse(null);
        }
        // Fallback về vùng hoạt động đầu tiên hoạt động nếu không khớp tọa độ cụ thể
        if (edgeNode == null) {
            edgeNode = edgeNodeRepository.findAll().stream().filter(EdgeNode::getIsActive).findFirst().orElse(null);
        }

        // Lấy loại hình dịch vụ cơ bản mặc định (BLS)
        ServiceType serviceType = serviceTypeRepository.findByTypeCode("BLS").orElse(null);

        // Tự động tạo yêu cầu điều phối (DispatchRequest) cho cuộc gọi SOS khẩn cấp nhanh
        DispatchRequest request = new DispatchRequest();
        request.setCall(savedCall);
        request.setServiceType(serviceType);
        request.setEdgeNode(edgeNode);
        request.setUrgencyLevel("CRITICAL"); // Đặt mặc định mức khẩn cấp CRITICAL cho SOS nhanh
        request.setTargetLocation(savedCall.getLocation());
        request.setStatus("PENDING");
        dispatchRequestRepository.save(request);

        // Phát tán WebSocket tới điều phối viên
        messagingTemplate.convertAndSend("/topic/calls", savedCall);
        messagingTemplate.convertAndSend("/topic/dispatches", request); // Gửi thêm kênh dispatches cho Dashboard

        return savedCall;
    }


    /**
     * Nhận yêu cầu cuộc gọi thoại khẩn cấp kèm định vị từ người dân.
     */
    public EmergencyCall createEmergencyVoiceCall(String reporterPhone, String reporterName, Double latitude, Double longitude, String audioObjectKey) {
        // 1. Lấy URL công khai của file từ object key
        String audioUrl = fileStorageService.getPublicUrl(audioObjectKey);

        // 2. Lưu thông tin cuộc gọi mới vào PostgreSQL
        EmergencyCall call = new EmergencyCall();
        call.setReporterPhone(reporterPhone);
        call.setReporterName(reporterName);
        call.setCallStartTime(LocalDateTime.now());
        call.setAudioUrl(audioUrl);
        call.setStatus("RECEIVED");
        
        if (longitude != null && latitude != null) {
            call.setLocation(geometryFactory.createPoint(new Coordinate(longitude, latitude)));
        }
        
        EmergencyCall savedCall = callRepository.save(call);

        // Ghi Audit Log cho hành động của người báo cáo
        saveAuditLog(savedCall, longitude, latitude);

        // 3. Đẩy thông tin job phân tích AI vào Redis Queue (LPUSH)
        enqueueCallForAI(savedCall.getId(), audioUrl);

        // Phát tán WebSocket tới điều phối viên
        messagingTemplate.convertAndSend("/topic/calls", savedCall);

        return savedCall;
    }


    private void saveAuditLog(EmergencyCall call, Double longitude, Double latitude) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setTableName("emergency_calls");
            auditLog.setRecordId(call.getId().longValue());
            auditLog.setOperation("INSERT");
            auditLog.setChangedBy(null); // Người báo cáo/người dùng bên ngoài không có account hệ thống
            auditLog.setOldData(null);
            auditLog.setNewData(buildAuditData(call, longitude, latitude));
            auditLog.setChangedAt(LocalDateTime.now());
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            System.err.println("Failed to write audit log for emergency call: " + e.getMessage());
        }
    }

    private Map<String, Object> buildAuditData(EmergencyCall call, Double longitude, Double latitude) {
        Map<String, Object> data = new HashMap<>();
        data.put("reporterPhone", call.getReporterPhone());
        data.put("reporterName", call.getReporterName());
        data.put("status", call.getStatus());
        data.put("audioUrl", call.getAudioUrl());
        data.put("longitude", longitude);
        data.put("latitude", latitude);
        data.put("createdAt", call.getCreatedAt() != null ? call.getCreatedAt().toString() : null);
        return data;
    }

    /**
     * Đẩy job vào Redis Queue để FastAPI Worker lấy ra xử lý
     */
    private void enqueueCallForAI(Integer callId, String audioUrl) {
        try {
            Map<String, Object> jobData = Map.of(
                "call_id", callId,
                "audio_url", audioUrl
            );
            String jsonMessage = objectMapper.writeValueAsString(jobData);
            
            // LPUSH để xếp hàng đợi
            redisTemplate.opsForList().leftPush("emergency:ai:queue", jsonMessage);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to enqueue AI processing job to Redis", e);
        }
    }

    /**
     * Nhận kết quả callback từ FastAPI và phát tán WebSocket cho điều phối viên.
     */
    public EmergencyCall handleAICallback(Integer callId, String transcript, String urgency, Double confidence, List<String> symptoms) {
        // 1. Tìm cuộc gọi trong cơ sở dữ liệu
        EmergencyCall call = callRepository.findById(callId)
                .orElseThrow(() -> new IllegalArgumentException("Emergency call not found with ID: " + callId));

        // 2. Cập nhật thông tin phân tích từ AI
        call.setAiTranscript(transcript);
        call.setAiUrgencyPrediction(urgency);
        call.setAiConfidenceScore(BigDecimal.valueOf(confidence));
        call.setStatus("ANALYZED");

        EmergencyCall updatedCall = callRepository.save(call);

        // 3. Tự động ánh xạ độ khẩn cấp sang mã loại dịch vụ xe cấp cứu (ALS / BLS)
        String serviceTypeCode = "BLS";
        if ("CRITICAL".equalsIgnoreCase(urgency) || "HIGH".equalsIgnoreCase(urgency)) {
            serviceTypeCode = "ALS";
        }
        ServiceType serviceType = serviceTypeRepository.findByTypeCode(serviceTypeCode).orElse(null);

        // 4. Tìm Vùng biên quản lý (EdgeNode) của cuộc gọi cấp cứu
        Double longitude = call.getLongitude();
        Double latitude = call.getLatitude();
        EdgeNode edgeNode = null;
        if (longitude != null && latitude != null) {
            edgeNode = edgeNodeRepository.findContainingNode(longitude, latitude).orElse(null);
        }
        // Fallback về vùng hoạt động đầu tiên hoạt động nếu không khớp
        if (edgeNode == null) {
            edgeNode = edgeNodeRepository.findAll().stream().filter(EdgeNode::getIsActive).findFirst().orElse(null);
        }

        // 5. Tự động tạo yêu cầu điều phối (DispatchRequest) liên kết với cuộc gọi cấp cứu
        DispatchRequest request = new DispatchRequest();
        request.setCall(updatedCall);
        request.setServiceType(serviceType);
        request.setEdgeNode(edgeNode);
        request.setUrgencyLevel(urgency);
        request.setTargetLocation(updatedCall.getLocation());
        request.setStatus("PENDING");
        
        // Lưu danh sách triệu chứng trích xuất từ AI vào extendedRequirements
        if (symptoms != null && !symptoms.isEmpty()) {
            request.setExtendedRequirements(Map.of("symptoms", symptoms));
        }
        
        dispatchRequestRepository.save(request);

        // 6. Phát tán WebSocket tới điều phối viên qua các topic tương ứng
        messagingTemplate.convertAndSend("/topic/calls", updatedCall);
        messagingTemplate.convertAndSend("/topic/dispatches", request);

        return updatedCall;
    }

    public List<EmergencyCall> getMyCalls(String reporterPhone) {
        return callRepository.findByReporterPhoneOrderByCallStartTimeDesc(reporterPhone);
    }

    public java.util.Optional<EmergencyCall> getCallDetails(Integer id) {
        return callRepository.findById(id);
    }

    public java.util.Optional<EmergencyCall> getCallByAudioObjectKey(String objectKey) {
        return callRepository.findFirstByAudioUrlContaining(objectKey);
    }
}

