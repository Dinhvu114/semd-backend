package com.semd.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.semd.backend.entity.*;
import com.semd.backend.repository.AuditLogRepository;
import com.semd.backend.repository.EmergencyCallRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.semd.backend.repository.DispatchRequestRepository;
import com.semd.backend.repository.ServiceTypeRepository;
import com.semd.backend.repository.DispatchMissionRepository;
import com.semd.backend.repository.AmbulanceSimulationRepository;
import com.semd.backend.dto.emergencyCall.CallStatusResponse;
import com.semd.backend.dto.emergencyCall.CallTrackingResponse;
import com.semd.backend.dto.response.TrackingResponse;
import com.semd.backend.exception.ResourceNotFoundException;
import com.semd.backend.dto.emergencyCall.EmergencyCallResponse;
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
    private final DispatchMissionRepository missionRepository;
    private final AmbulanceSimulationRepository simulationRepository;
    private final AmbulanceJourneyService ambulanceJourneyService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public EmergencyCallService(EmergencyCallRepository callRepository,
                                FileStorageService fileStorageService,
                                AuditLogRepository auditLogRepository,
                                StringRedisTemplate redisTemplate,
                                SimpMessagingTemplate messagingTemplate,
                                DispatchRequestRepository dispatchRequestRepository,
                                ServiceTypeRepository serviceTypeRepository,
                                DispatchMissionRepository missionRepository,
                                AmbulanceSimulationRepository simulationRepository,
                                AmbulanceJourneyService ambulanceJourneyService) {
        this.callRepository = callRepository;
        this.fileStorageService = fileStorageService;
        this.auditLogRepository = auditLogRepository;
        this.redisTemplate = redisTemplate;
        this.messagingTemplate = messagingTemplate;
        this.dispatchRequestRepository = dispatchRequestRepository;
        this.serviceTypeRepository = serviceTypeRepository;
        this.missionRepository = missionRepository;
        this.simulationRepository = simulationRepository;
        this.ambulanceJourneyService = ambulanceJourneyService;
    }

    @Transactional
    public EmergencyCallResponse createEmergencySosCall(String reporterPhone, String reporterName, Double latitude,
                                                        Double longitude, String description) {
        EmergencyCall call = new EmergencyCall();
        call.setReporterPhone(reporterPhone);
        call.setReporterName(reporterName);
        call.setCallStartTime(LocalDateTime.now());
        call.setCallType(EmergencyCallType.SOS);
        call.setStatus(EmergencyCallStatus.RECEIVED);
        call.setDescription(normalizeDescription(description));

        if (longitude != null && latitude != null) {
            call.setLocation(geometryFactory.createPoint(new Coordinate(longitude, latitude)));
        }

        EmergencyCall savedCall = callRepository.save(call);

        saveAuditLog(savedCall, longitude, latitude);

        // Lấy loại hình dịch vụ cơ bản mặc định (BLS)
        ServiceType serviceType = serviceTypeRepository.findByTypeCode("BLS").orElse(null);

        // Tự động tạo yêu cầu điều phối (DispatchRequest) cho cuộc gọi SOS khẩn cấp nhanh
        // Không còn phân vùng theo OperationZone — request mang trực tiếp vị trí sự cố
        DispatchRequest request = new DispatchRequest();
        request.setCall(savedCall);
        request.setServiceType(serviceType);
        request.setUrgencyLevel("CRITICAL");
        request.setTargetLocation(savedCall.getLocation());
        request.setStatus(DispatchRequestStatus.PENDING);
        DispatchRequest savedRequest = dispatchRequestRepository.save(request);

        messagingTemplate.convertAndSend("/topic/calls", savedCall);
        messagingTemplate.convertAndSend("/topic/dispatches", request);

        return toResponse(savedCall, savedRequest);
    }

    @Transactional
    public EmergencyCallResponse createEmergencyVoiceCall(String reporterPhone, String reporterName, Double latitude,
                                                          Double longitude, String audioObjectKey, String description) {
        String audioUrl = fileStorageService.getPublicUrl(audioObjectKey);

        EmergencyCall call = new EmergencyCall();
        call.setReporterPhone(reporterPhone);
        call.setReporterName(reporterName);
        call.setCallStartTime(LocalDateTime.now());
        call.setAudioUrl(audioUrl);
        call.setCallType(EmergencyCallType.CALL);
        call.setStatus(EmergencyCallStatus.RECEIVED);
        call.setDescription(normalizeDescription(description));

        if (longitude != null && latitude != null) {
            call.setLocation(geometryFactory.createPoint(new Coordinate(longitude, latitude)));
        }

        EmergencyCall savedCall = callRepository.save(call);

        saveAuditLog(savedCall, longitude, latitude);

        enqueueCallForAI(savedCall.getId(), audioUrl);

        messagingTemplate.convertAndSend("/topic/calls", savedCall);

        return toResponse(savedCall, null);
    }

    private void saveAuditLog(EmergencyCall call, Double longitude, Double latitude) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setTableName("emergency_calls");
            auditLog.setRecordId(call.getId().longValue());
            auditLog.setOperation("INSERT");
            auditLog.setChangedBy(null);
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
        data.put("callType", call.getCallType().name());
        data.put("status", call.getStatus().name());
        data.put("audioUrl", call.getAudioUrl());
        data.put("description", call.getDescription());
        data.put("longitude", longitude);
        data.put("latitude", latitude);
        data.put("createdAt", call.getCreatedAt() != null ? call.getCreatedAt().toString() : null);
        return data;
    }

    private void enqueueCallForAI(Integer callId, String audioUrl) {
        try {
            Map<String, Object> jobData = Map.of(
                    "call_id", callId,
                    "audio_url", audioUrl
            );
            String jsonMessage = objectMapper.writeValueAsString(jobData);
            redisTemplate.opsForList().leftPush("emergency:ai:queue", jsonMessage);
        } catch (Exception e) {
            throw new RuntimeException("Failed to enqueue AI processing job to Redis", e);
        }
    }

    public EmergencyCall handleAICallback(Integer callId, String transcript, String urgency, Double confidence, List<String> symptoms) {
        EmergencyCall call = callRepository.findById(callId)
                .orElseThrow(() -> new IllegalArgumentException("Emergency call not found with ID: " + callId));

        call.setAiTranscript(transcript);
        call.setAiUrgencyPrediction(urgency);
        call.setAiConfidenceScore(BigDecimal.valueOf(confidence));
        call.setStatus(EmergencyCallStatus.AI_ANALYZED);

        EmergencyCall updatedCall = callRepository.save(call);

        // Tự động ánh xạ độ khẩn cấp sang mã loại dịch vụ xe cấp cứu (ALS / BLS)
        String serviceTypeCode = "BLS";
        if ("CRITICAL".equalsIgnoreCase(urgency) || "HIGH".equalsIgnoreCase(urgency)) {
            serviceTypeCode = "ALS";
        }
        ServiceType serviceType = serviceTypeRepository.findByTypeCode(serviceTypeCode).orElse(null);

        // Không còn tìm OperationZone — request mang trực tiếp vị trí sự cố
        DispatchRequest request = new DispatchRequest();
        request.setCall(updatedCall);
        request.setServiceType(serviceType);
        request.setUrgencyLevel(urgency);
        request.setTargetLocation(updatedCall.getLocation());
        request.setStatus(DispatchRequestStatus.PENDING);

        if (symptoms != null && !symptoms.isEmpty()) {
            request.setExtendedRequirements(Map.of("symptoms", symptoms));
        }

        dispatchRequestRepository.save(request);

        messagingTemplate.convertAndSend("/topic/calls", updatedCall);
        messagingTemplate.convertAndSend("/topic/dispatches", request);

        return updatedCall;
    }

    public List<EmergencyCallResponse> getMyCalls(String reporterPhone) {
        return callRepository.findByReporterPhoneOrderByCallStartTimeDesc(reporterPhone).stream()
                .map(this::toResponse)
                .toList();
    }

    public java.util.Optional<EmergencyCallResponse> getCallDetails(Integer id) {
        return callRepository.findById(id).map(this::toResponse);
    }

    public EmergencyCallResponse getOwnedCallDetails(Integer id, String reporterPhone) {
        return toResponse(findOwnedCall(id, reporterPhone));
    }

    public CallStatusResponse getOwnedCallStatus(Integer id, String reporterPhone) {
        EmergencyCall call = findOwnedCall(id, reporterPhone);
        DispatchRequest request = latestRequest(call.getId());
        DispatchMission mission = latestMission(request);

        return new CallStatusResponse(
                call.getId(),
                call.getStatus().name(),
                request != null ? request.getId() : null,
                request != null ? request.getStatus().name() : null,
                mission != null ? mission.getId() : null,
                mission != null ? mission.getStatus().name() : null,
                resolveUpdatedAt(call, request, mission)
        );
    }

    public CallTrackingResponse getOwnedCallTracking(Integer id, String reporterPhone) {
        EmergencyCall call = findOwnedCall(id, reporterPhone);
        DispatchRequest request = latestRequest(call.getId());
        DispatchMission mission = latestMission(request);
        DispatchResource resource = mission != null ? mission.getResource() : null;
        TrackingResponse tracking = null;

        if (mission != null && simulationRepository.findByMissionId(mission.getId()).isPresent()) {
            tracking = ambulanceJourneyService.getTrackingByMission(mission.getId());
        }

        return new CallTrackingResponse(
                call.getId(),
                call.getStatus().name(),
                request != null ? request.getId() : null,
                request != null ? request.getStatus().name() : null,
                mission != null ? mission.getId() : null,
                mission != null ? mission.getStatus().name() : null,
                resource != null ? resource.getId() : null,
                resource != null ? resource.getResourceCode() : null,
                resource != null ? resource.getStatus().name() : null,
                resource != null && resource.getCurrentLocation() != null ? resource.getCurrentLocation().getX() : null,
                resource != null && resource.getCurrentLocation() != null ? resource.getCurrentLocation().getY() : null,
                tracking
        );
    }

    private EmergencyCall findOwnedCall(Integer id, String reporterPhone) {
        return callRepository.findByIdAndReporterPhone(id, reporterPhone)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cuộc gọi ID: " + id));
    }

    private DispatchRequest latestRequest(Integer callId) {
        return dispatchRequestRepository.findFirstByCallIdOrderByCreatedAtDesc(callId).orElse(null);
    }

    private DispatchMission latestMission(DispatchRequest request) {
        if (request == null) return null;
        return missionRepository.findAllByRequestId(request.getId()).stream()
                .max(java.util.Comparator.comparing(
                        DispatchMission::getDispatchedAt,
                        java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder())))
                .orElse(null);
    }

    private LocalDateTime resolveUpdatedAt(EmergencyCall call, DispatchRequest request, DispatchMission mission) {
        if (mission != null) {
            LocalDateTime[] timestamps = {
                    mission.getCompletedAt(), mission.getArrivedHospitalAt(), mission.getStartTransportAt(),
                    mission.getArrivedSceneAt(), mission.getEnRouteAt(), mission.getAcceptedAt(),
                    mission.getCancelledAt(), mission.getDispatchedAt()
            };
            for (LocalDateTime timestamp : timestamps) {
                if (timestamp != null) return timestamp;
            }
        }
        if (request != null && request.getCreatedAt() != null) return request.getCreatedAt();
        return call.getCreatedAt();
    }

    public java.util.Optional<EmergencyCall> getCallByAudioObjectKey(String objectKey) {
        return callRepository.findFirstByAudioUrlContaining(objectKey);
    }

    private EmergencyCallResponse toResponse(EmergencyCall call) {
        DispatchRequest request = dispatchRequestRepository
                .findFirstByCallIdOrderByCreatedAtDesc(call.getId())
                .orElse(null);
        return toResponse(call, request);
    }

    private EmergencyCallResponse toResponse(EmergencyCall call, DispatchRequest request) {
        return new EmergencyCallResponse(
                call.getId(),
                request != null ? request.getId() : null,
                call.getCallType().name(),
                call.getStatus().name(),
                request != null ? request.getStatus().name() : null,
                call.getDescription(),
                call.getReporterName(),
                call.getReporterPhone(),
                call.getAudioUrl(),
                call.getAiTranscript(),
                call.getAiUrgencyPrediction(),
                call.getAiConfidenceScore(),
                call.getLatitude(),
                call.getLongitude(),
                call.getCreatedAt()
        );
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
    }
}