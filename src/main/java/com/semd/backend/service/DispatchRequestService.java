package com.semd.backend.service;

import com.semd.backend.dto.DispatchRequestDto;
import com.semd.backend.dto.request.ConfirmDispatchRequest;
import com.semd.backend.dto.request.RejectDispatchRequest;
import com.semd.backend.dto.request.SeverityUpdateRequest;
import com.semd.backend.dto.response.*;
import com.semd.backend.entity.*;
import com.semd.backend.exception.ResourceNotFoundException;
import com.semd.backend.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
@Service
public class DispatchRequestService {

    private final DispatchRequestRepository requestRepository;
    private final DispatchMissionRepository missionRepository;
    private final DispatchResourceRepository resourceRepository;
    private final MissionStatusLogRepository statusLogRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RestTemplate restTemplate;

    @Value("${ai.service.url:http://localhost:8000}")
    private String aiServiceUrl;

    public DispatchRequestService(
            DispatchRequestRepository requestRepository,
            DispatchMissionRepository missionRepository,
            DispatchResourceRepository resourceRepository,
            MissionStatusLogRepository statusLogRepository,
            UserRepository userRepository,
            SimpMessagingTemplate messagingTemplate,
            RestTemplate restTemplate) {
        this.requestRepository = requestRepository;
        this.missionRepository = missionRepository;
        this.resourceRepository = resourceRepository;
        this.statusLogRepository = statusLogRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
        this.restTemplate = restTemplate;
    }

    // ──────────────────────────────────────────────
    // 1. GET /dispatch-requests  (filter by status, zoneId)
    // ──────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<DispatchRequestDto> getAllRequests(DispatchRequestStatus status, Integer zoneId) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        List<DispatchRequest> list;

        if (status != null && zoneId != null) {
            list = requestRepository.findByStatusAndOperationZoneId(status, zoneId, sort);
        } else if (status != null) {
            list = requestRepository.findByStatus(status, sort);
        } else if (zoneId != null) {
            list = requestRepository.findByOperationZoneId(zoneId, sort);
        } else {
            list = requestRepository.findAll(sort);
        }

        return list.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────
    // 2. GET /dispatch-requests/{id}  (detail)
    // ──────────────────────────────────────────────
    @Transactional(readOnly = true)
    public DispatchRequestDetailDto getDetail(Integer id) {
        DispatchRequest req = findById(id);
        EmergencyCall call = req.getCall();
        long missionCount = missionRepository.countByRequestId(id);

        return new DispatchRequestDetailDto(
                req.getId(),
                call != null ? call.getId() : null,
                call != null ? call.getReporterPhone() : null,
                call != null ? call.getReporterName() : null,
                call != null ? call.getAudioUrl() : null,
                call != null ? call.getAiTranscript() : null,
                call != null ? call.getAiUrgencyPrediction() : null,
                call != null ? call.getAiConfidenceScore() : null,
                req.getUrgencyLevel(),
                req.getTriageLevel(),
                req.getReviewNote(),
                req.getStatus() != null ? req.getStatus().name() : null,
                req.getLongitude(),
                req.getLatitude(),
                req.getOperationZone() != null ? req.getOperationZone().getZoneName() : null,
                req.getServiceType() != null ? req.getServiceType().getDisplayName() : null,
                req.getConfirmedBy() != null ? req.getConfirmedBy().getFullName() : null,
                req.getConfirmedAt(),
                missionCount,
                req.getExtendedRequirements(),
                req.getCreatedAt()
        );
    }

    // ──────────────────────────────────────────────
    // 3. POST /dispatch-requests/{id}/analyze
    // ──────────────────────────────────────────────
    @Transactional
    public Map<String, Object> analyze(Integer id) {
        DispatchRequest req = findById(id);
        EmergencyCall call = req.getCall();

        if (call == null || call.getAudioUrl() == null) {
            throw new IllegalStateException("Yêu cầu #" + id + " không có file ghi âm để phân tích.");
        }

        // Gọi đồng bộ FastAPI AI Service
        String url = aiServiceUrl + "/api/v1/ai/analyze-call";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of(
                "call_id", call.getId(),
                "audio_url", call.getAudioUrl()
        );
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, httpEntity, Map.class);
            Map<String, Object> result = response.getBody();
            if (result == null) throw new RuntimeException("AI Service trả về phản hồi trống.");

            // Cập nhật kết quả AI vào DB
            String urgency = (String) result.getOrDefault("urgency", "MEDIUM");
            Number confidence = (Number) result.getOrDefault("confidence", 0);

            call.setAiTranscript((String) result.getOrDefault("transcript", ""));
            call.setAiUrgencyPrediction(urgency);
            call.setAiConfidenceScore(new java.math.BigDecimal(confidence.toString()));
            call.setStatus(EmergencyCallStatus.AI_ANALYZED);

            req.setUrgencyLevel(urgency);
            req.setStatus(DispatchRequestStatus.PENDING);
            requestRepository.save(req);

            return Map.of(
                    "symptoms", result.getOrDefault("symptoms", List.of()),
                    "severity", urgency,
                    "confidence", confidence
            );
        } catch (Exception e) {
            throw new RuntimeException("Không thể kết nối tới AI Service: " + e.getMessage());
        }
    }

    // ──────────────────────────────────────────────
    // 4. POST /dispatch-requests/{id}/confirm
    // ──────────────────────────────────────────────
    @Transactional
    public Map<String, String> confirm(Integer id, ConfirmDispatchRequest req) {
        DispatchRequest request = findById(id);

        if (request.getStatus() == DispatchRequestStatus.REJECTED ||
            request.getStatus() == DispatchRequestStatus.CANCELLED ||
            request.getStatus() == DispatchRequestStatus.COMPLETED) {
            throw new IllegalStateException("Không thể xác nhận yêu cầu ở trạng thái: " + request.getStatus());
        }

        if (req.dispatcherId() != null) {
            userRepository.findById(req.dispatcherId())
                    .ifPresent(request::setConfirmedBy);
        }
        request.setConfirmedAt(LocalDateTime.now());
        request.setReviewNote(req.note());
        request.setStatus(DispatchRequestStatus.CONFIRMED);
        requestRepository.save(request);

        broadcastUpdate(request);
        return Map.of("status", "CONFIRMED");
    }

    // ──────────────────────────────────────────────
    // 5. POST /dispatch-requests/{id}/reject
    // ──────────────────────────────────────────────
    @Transactional
    public Map<String, String> reject(Integer id, RejectDispatchRequest req) {
        DispatchRequest request = findById(id);

        if (request.getStatus() == DispatchRequestStatus.DISPATCHED ||
            request.getStatus() == DispatchRequestStatus.COMPLETED) {
            throw new IllegalStateException("Không thể từ chối yêu cầu ở trạng thái: " + request.getStatus());
        }

        request.setReviewNote(req.reason());
        request.setStatus(DispatchRequestStatus.REJECTED);
        requestRepository.save(request);

        broadcastUpdate(request);
        return Map.of("status", "REJECTED");
    }

    // ──────────────────────────────────────────────
    // 6. PATCH /dispatch-requests/{id}/severity
    // ──────────────────────────────────────────────
    @Transactional
    public Map<String, String> updateSeverity(Integer id, SeverityUpdateRequest req) {
        DispatchRequest request = findById(id);
        request.setTriageLevel(req.severity());
        request.setUrgencyLevel(req.severity());
        requestRepository.save(request);
        return Map.of("triageLevel", req.severity());
    }

    // ──────────────────────────────────────────────
    // 7. POST /dispatch-requests/{id}/recommend
    // ──────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<RecommendationItemDto> recommend(Integer id) {
        DispatchRequest request = findById(id);

        if (request.getTargetLocation() == null) {
            throw new IllegalStateException("Yêu cầu #" + id + " chưa có tọa độ để đề xuất xe.");
        }

        double lat = request.getTargetLocation().getY();
        double lon = request.getTargetLocation().getX();

        List<DispatchResource> available = resourceRepository.findAll()
                .stream()
                .filter(r -> r.getStatus() == DispatchResourceStatus.AVAILABLE)
                .filter(r -> r.getCurrentLocation() != null)
                .collect(Collectors.toList());

        return available.stream()
                .map(r -> {
                    double rLat = r.getCurrentLocation().getY();
                    double rLon = r.getCurrentLocation().getX();
                    double distKm = haversine(lat, lon, rLat, rLon);
                    double score = Math.max(0, 100 - distKm * 10);
                    long eta = (long) (distKm / 40.0 * 3600); // 40km/h trung bình
                    List<String> reason = new ArrayList<>();
                    if (distKm < 2) reason.add("Gần nhất");
                    if (r.getResourceType() != null) reason.add(r.getResourceType().getDisplayName());
                    return new RecommendationItemDto(
                            r.getId(), r.getResourceCode(), Math.round(score * 10.0) / 10.0,
                            eta, Math.round(distKm * 100.0) / 100.0, reason
                    );
                })
                .sorted(Comparator.comparingDouble(RecommendationItemDto::distanceKm))
                .limit(5)
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────
    // 8. POST /dispatch-requests/{id}/redispatch
    // ──────────────────────────────────────────────
    @Transactional
    public Map<String, Object> redispatch(Integer requestId, Integer newResourceId) {
        DispatchRequest request = findById(requestId);

        // Hủy mission đang chạy và giải phóng xe cũ
        missionRepository.findActiveMissionByRequestId(requestId).ifPresent(mission -> {
            mission.setStatus(DispatchMissionStatus.CANCELLED);
            DispatchResource oldResource = mission.getResource();
            if (oldResource != null) {
                oldResource.setStatus(DispatchResourceStatus.AVAILABLE);
                resourceRepository.save(oldResource);
            }

            MissionStatusLog log = new MissionStatusLog();
            log.setMission(mission);
            log.setOldStatus(mission.getStatus().name());
            log.setNewStatus(DispatchMissionStatus.CANCELLED.name());
            log.setNote("Điều phối lại - hủy mission cũ");
            log.setCreatedAt(LocalDateTime.now());
            statusLogRepository.save(log);
            missionRepository.save(mission);
        });

        // Tạo mission mới với xe mới
        DispatchResource newResource = resourceRepository.findById(newResourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy xe id: " + newResourceId));

        if (newResource.getStatus() != DispatchResourceStatus.AVAILABLE) {
            throw new IllegalStateException("Xe #" + newResourceId + " không sẵn sàng: " + newResource.getStatus());
        }

        newResource.setStatus(DispatchResourceStatus.DISPATCHED);
        resourceRepository.save(newResource);

        DispatchMission newMission = new DispatchMission();
        newMission.setRequest(request);
        newMission.setResource(newResource);
        newMission.setStatus(DispatchMissionStatus.CREATED);
        DispatchMission saved = missionRepository.save(newMission);

        request.setStatus(DispatchRequestStatus.DISPATCHED);
        requestRepository.save(request);

        MissionStatusLog log = new MissionStatusLog();
        log.setMission(saved);
        log.setOldStatus(null);
        log.setNewStatus(DispatchMissionStatus.CREATED.name());
        log.setNote("Điều phối lại với xe: " + newResource.getResourceCode());
        log.setCreatedAt(LocalDateTime.now());
        statusLogRepository.save(log);

        broadcastUpdate(request);
        return Map.of("missionId", saved.getId(), "resourceId", newResourceId);
    }

    // ──────────────────────────────────────────────
    // 9. POST /dispatch-requests/{id}/cancel
    // ──────────────────────────────────────────────
    @Transactional
    public Map<String, String> cancel(Integer id) {
        DispatchRequest request = findById(id);

        if (request.getStatus() == DispatchRequestStatus.COMPLETED) {
            throw new IllegalStateException("Không thể hủy yêu cầu đã hoàn thành.");
        }

        // Giải phóng xe đang gắn (nếu có)
        missionRepository.findActiveMissionByRequestId(id).ifPresent(mission -> {
            mission.setStatus(DispatchMissionStatus.CANCELLED);
            DispatchResource resource = mission.getResource();
            if (resource != null) {
                resource.setStatus(DispatchResourceStatus.AVAILABLE);
                resourceRepository.save(resource);
            }

            MissionStatusLog log = new MissionStatusLog();
            log.setMission(mission);
            log.setOldStatus(mission.getStatus().name());
            log.setNewStatus(DispatchMissionStatus.CANCELLED.name());
            log.setNote("Hủy yêu cầu - giải phóng xe");
            log.setCreatedAt(LocalDateTime.now());
            statusLogRepository.save(log);
            missionRepository.save(mission);
        });

        request.setStatus(DispatchRequestStatus.CANCELLED);
        requestRepository.save(request);

        broadcastUpdate(request);
        return Map.of("status", "CANCELLED");
    }

    // ──────────────────────────────────────────────
    // 10. GET /dispatch-requests/{id}/timeline
    // ──────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<TimelineEventDto> getTimeline(Integer id) {
        DispatchRequest request = findById(id);
        List<TimelineEventDto> events = new ArrayList<>();

        // Sự kiện 1: Cuộc gọi được tiếp nhận
        EmergencyCall call = request.getCall();
        if (call != null) {
            events.add(new TimelineEventDto("CALL_RECEIVED", call.getCallStartTime(),
                    "Người báo cáo: " + call.getReporterPhone()));

            // Sự kiện 2: AI đã phân tích
            if (call.getAiTranscript() != null) {
                events.add(new TimelineEventDto("AI_ANALYZED", call.getCreatedAt(),
                        "Mức độ: " + call.getAiUrgencyPrediction() +
                        " | Độ tin cậy: " + call.getAiConfidenceScore() + "%"));
            }
        }

        // Sự kiện 3: Dispatcher xác nhận/từ chối
        if (request.getConfirmedAt() != null) {
            String event = request.getStatus() == DispatchRequestStatus.REJECTED ? "REJECTED" : "CONFIRMED";
            events.add(new TimelineEventDto(event, request.getConfirmedAt(), request.getReviewNote()));
        }

        // Sự kiện 4+: Các lần tạo/thay đổi trạng thái mission
        List<DispatchMission> missions = missionRepository.findAllByRequestId(id);
        for (DispatchMission mission : missions) {
            List<MissionStatusLog> logs = statusLogRepository
                    .findAllByMissionIdOrderByCreatedAtAsc(mission.getId());
            for (MissionStatusLog log : logs) {
                events.add(new TimelineEventDto(
                        "MISSION_" + log.getNewStatus(),
                        log.getCreatedAt(),
                        log.getNote()
                ));
            }
        }

        // Sắp xếp theo thời gian
        events.sort(Comparator.comparing(TimelineEventDto::time, Comparator.nullsLast(Comparator.naturalOrder())));
        return events;
    }

    // ──────────────────────────────────────────────
    // 11. GET /dispatch-requests/statistics
    // ──────────────────────────────────────────────
    @Transactional(readOnly = true)
    public StatisticsResponse getStatistics() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        return new StatisticsResponse(
                requestRepository.countByStatus(DispatchRequestStatus.PENDING),
                requestRepository.countByStatus(DispatchRequestStatus.CONFIRMED),
                requestRepository.countByStatus(DispatchRequestStatus.CONFIRMED),
                requestRepository.countByStatus(DispatchRequestStatus.DISPATCHED),
                requestRepository.countByStatusAndCreatedAtAfter(DispatchRequestStatus.COMPLETED, startOfToday),
                requestRepository.countByStatus(DispatchRequestStatus.REJECTED),
                requestRepository.countByStatus(DispatchRequestStatus.CANCELLED)
        );
    }

    // ──────────────────────────────────────────────
    // Existing (giữ nguyên)
    // ──────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<DispatchRequestDto> search(
            DispatchRequestStatus status,
            String urgencyLevel,
            Integer serviceTypeId,
            Integer operationZoneId,
            Pageable pageable) {
        Specification<DispatchRequest> specification = (root, query, cb) -> cb.conjunction();
        if (status != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (urgencyLevel != null && !urgencyLevel.isBlank()) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(cb.upper(root.get("urgencyLevel")), urgencyLevel.trim().toUpperCase()));
        }
        if (serviceTypeId != null) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("serviceType").get("id"), serviceTypeId));
        }
        if (operationZoneId != null) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("operationZone").get("id"), operationZoneId));
        }
        return requestRepository.findAll(specification, pageable).map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public DispatchRequestDto getRequestById(Integer id) {
        return mapToDto(findById(id));
    }

    // ──────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────
    private DispatchRequest findById(Integer id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dispatch_request id: " + id));
    }

    private void broadcastUpdate(DispatchRequest request) {
        messagingTemplate.convertAndSend("/topic/dispatcher/requests",
                (Object) Map.of(
                        "id", request.getId(),
                        "status", request.getStatus().name()
                ));
    }

    private DispatchRequestDto mapToDto(DispatchRequest req) {
        Integer callId = req.getCall() != null ? req.getCall().getId() : null;
        Integer serviceTypeId = req.getServiceType() != null ? req.getServiceType().getId() : null;
        String serviceTypeName = req.getServiceType() != null ? req.getServiceType().getDisplayName() : null;
        Integer zoneId = req.getOperationZone() != null ? req.getOperationZone().getId() : null;
        String zoneName = req.getOperationZone() != null ? req.getOperationZone().getZoneName() : null;
        Integer dispatcherId = req.getCreatedByDispatcher() != null ? req.getCreatedByDispatcher().getId() : null;
        String dispatcherName = req.getCreatedByDispatcher() != null ? req.getCreatedByDispatcher().getFullName() : null;

        return new DispatchRequestDto(
                req.getId(), callId, serviceTypeId, serviceTypeName,
                zoneId, zoneName, dispatcherId, dispatcherName,
                req.getUrgencyLevel(), req.getLongitude(), req.getLatitude(),
                req.getStatus() != null ? req.getStatus().name() : null,
                req.getExtendedRequirements(), req.getCreatedAt()
        );
    }

    /**
     * Công thức Haversine - tính khoảng cách km giữa 2 tọa độ GPS
     */
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
