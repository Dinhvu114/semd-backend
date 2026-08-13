package com.semd.backend.service;

import com.semd.backend.dto.DispatchRequestDto;
import com.semd.backend.dto.request.ConfirmDispatchRequest;
import com.semd.backend.dto.request.RejectDispatchRequest;
import com.semd.backend.dto.request.SeverityUpdateRequest;
import com.semd.backend.dto.request.VerifyDispatchRequest;
import com.semd.backend.dto.response.*;
import com.semd.backend.entity.*;
import com.semd.backend.exception.ResourceNotFoundException;
import com.semd.backend.exception.BusinessConflictException;
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
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DispatchRequestService {

    private static final double ETA_WEIGHT = 0.35;
    private static final double DISTANCE_WEIGHT = 0.25;
    private static final double CAPABILITY_WEIGHT = 0.25;
    private static final double FRESHNESS_WEIGHT = 0.10;
    private static final double RISK_WEIGHT = 0.05;
    private static final double AVERAGE_SPEED_KMH = 40.0;
    private static final long LOCATION_STALE_SECONDS = 1800;
    private static final int RECOMMENDATION_LIMIT = 3;

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
    // 1. GET /dispatch-requests (filter by status, zoneId)
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
    // 2. GET /dispatch-requests/{id} (detail)
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
                req.getVerifiedBy() != null ? req.getVerifiedBy().getFullName() : null,
                req.getVerifiedAt(),
                req.getVerificationNote(),
                req.getRejectedBy() != null ? req.getRejectedBy().getFullName() : null,
                req.getRejectedAt(),
                req.getRejectionReason(),
                req.getConfirmedAddress(),
                req.getConfirmedLatitude(),
                req.getConfirmedLongitude(),
                req.getConfirmedUrgencyLevel(),
                missionCount,
                req.getExtendedRequirements(),
                req.getCreatedAt());
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
                "audio_url", call.getAudioUrl());
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, httpEntity, Map.class);
            Map<String, Object> result = response.getBody();
            if (result == null)
                throw new RuntimeException("AI Service trả về phản hồi trống.");

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
                    "confidence", confidence);
        } catch (Exception e) {
            throw new RuntimeException("Không thể kết nối tới AI Service: " + e.getMessage());
        }
    }

    // ──────────────────────────────────────────────
    // 4. POST /dispatch-requests/{id}/confirm
    // ──────────────────────────────────────────────
    @Transactional
    public Map<String, String> confirm(Integer id, ConfirmDispatchRequest req, Integer currentUserId) {
        DispatchRequest request = findById(id);

        if (request.getStatus() != DispatchRequestStatus.PENDING) {
            throw new BusinessConflictException(
                    "Chỉ yêu cầu ở trạng thái PENDING mới được xác minh");
        }

        User verifier = requireUser(currentUserId);
        LocalDateTime now = LocalDateTime.now();
        request.setConfirmedBy(verifier);
        request.setConfirmedAt(now);
        request.setReviewNote(req.note());
        request.setVerifiedBy(verifier);
        request.setVerifiedAt(now);
        request.setVerificationNote(req.note());
        request.setStatus(DispatchRequestStatus.CONFIRMED);
        requestRepository.save(request);

        broadcastUpdate(request);
        return Map.of("status", "CONFIRMED");
    }

    @Transactional
    public DispatchRequestDetailDto verify(
            Integer id,
            VerifyDispatchRequest req,
            Integer currentUserId) {
        DispatchRequest request = findById(id);
        if (request.getStatus() != DispatchRequestStatus.PENDING) {
            throw new BusinessConflictException(
                    "Chỉ yêu cầu ở trạng thái PENDING mới được xác minh");
        }

        validateCoordinates(req.confirmedLatitude(), req.confirmedLongitude());
        User verifier = requireUser(currentUserId);
        LocalDateTime now = LocalDateTime.now();

        request.setStatus(DispatchRequestStatus.CONFIRMED);
        request.setVerifiedAt(now);
        request.setVerifiedBy(verifier);
        request.setVerificationNote(trimToNull(req.verificationNote()));
        request.setConfirmedAddress(trimToNull(req.confirmedAddress()));
        request.setConfirmedLatitude(req.confirmedLatitude());
        request.setConfirmedLongitude(req.confirmedLongitude());
        request.setConfirmedUrgencyLevel(normalizeUrgency(req.confirmedUrgencyLevel()));
        request.setConfirmedAt(now);
        request.setConfirmedBy(verifier);
        request.setReviewNote(request.getVerificationNote());
        if (request.getConfirmedUrgencyLevel() != null) {
            request.setUrgencyLevel(request.getConfirmedUrgencyLevel());
        }

        requestRepository.save(request);
        broadcastUpdate(request);
        return getDetail(id);
    }

    // ──────────────────────────────────────────────
    // 5. POST /dispatch-requests/{id}/reject
    // ──────────────────────────────────────────────
    @Transactional
    public Map<String, String> reject(Integer id, RejectDispatchRequest req, Integer currentUserId) {
        DispatchRequest request = findById(id);

        if (request.getStatus() != DispatchRequestStatus.PENDING) {
            throw new BusinessConflictException(
                    "Chỉ yêu cầu ở trạng thái PENDING mới được từ chối");
        }

        String reason = trimToNull(req.reason());
        if (reason == null) {
            throw new IllegalArgumentException("Lý do từ chối không được để trống");
        }
        request.setReviewNote(reason);
        request.setRejectionReason(reason);
        request.setRejectedAt(LocalDateTime.now());
        request.setRejectedBy(requireUser(currentUserId));
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
    @Transactional
    public List<RecommendationItemDto> recommend(Integer id) {
        DispatchRequest request = findById(id);

        if (request.getStatus() != DispatchRequestStatus.CONFIRMED
                && request.getStatus() != DispatchRequestStatus.RECOMMENDING) {
            throw new BusinessConflictException(
                    "Chỉ request CONFIRMED hoặc RECOMMENDING mới được đề xuất xe");
        }
        if (request.getTargetLocation() == null) {
            throw new IllegalStateException("Yêu cầu #" + id + " chưa có tọa độ để đề xuất xe.");
        }

        // Lần đầu gọi recommendation:
        // CONFIRMED -> RECOMMENDING
        if (request.getStatus() == DispatchRequestStatus.CONFIRMED) {
            request.setStatus(DispatchRequestStatus.RECOMMENDING);
            requestRepository.save(request);
            broadcastUpdate(request);
        }

        LocalDateTime calculatedAt = LocalDateTime.now();
        List<CandidateScore> candidates = resourceRepository
                .findAllByStatus(DispatchResourceStatus.AVAILABLE)
                .stream()
                .filter(resource -> isEligible(resource, request, calculatedAt))
                .map(resource -> buildCandidate(resource, request, calculatedAt))
                .toList();

        if (candidates.isEmpty()) {
            return List.of();
        }

        double minEta = candidates.stream().mapToDouble(CandidateScore::etaSeconds).min().orElse(0);
        double maxEta = candidates.stream().mapToDouble(CandidateScore::etaSeconds).max().orElse(0);
        double minDistance = candidates.stream().mapToDouble(CandidateScore::distanceKm).min().orElse(0);
        double maxDistance = candidates.stream().mapToDouble(CandidateScore::distanceKm).max().orElse(0);

        List<ScoredCandidate> ranked = candidates.stream()
                .map(candidate -> scoreCandidate(candidate, minEta, maxEta, minDistance, maxDistance))
                .sorted(Comparator.comparingDouble(ScoredCandidate::score).reversed()
                        .thenComparingDouble(candidate -> candidate.candidate().etaSeconds())
                        .thenComparing(candidate -> candidate.candidate().resource().getId()))
                .limit(RECOMMENDATION_LIMIT)
                .toList();

        List<RecommendationItemDto> result = new ArrayList<>();
        for (int index = 0; index < ranked.size(); index++) {
            result.add(toRecommendation(ranked.get(index), index + 1));
        }
        return result;
    }

    private boolean isEligible(
            DispatchResource resource,
            DispatchRequest request,
            LocalDateTime calculatedAt) {
        System.out.println(
                "=== CHECK RESOURCE "
                        + resource.getId()
                        + " / "
                        + resource.getResourceCode()
                        + " ===");

        System.out.println(
                "driver = "
                        + (resource.getCurrentDriver() != null));

        System.out.println(
                "location = "
                        + resource.getCurrentLocation());

        System.out.println(
                "updatedAt = "
                        + resource.getUpdatedAt());

        System.out.println(
                "calculatedAt = "
                        + calculatedAt);

        if (resource.getCurrentLocation() == null
                || resource.getCurrentDriver() == null) {

            System.out.println("REJECT: driver/location");
            return false;
        }

        if (resource.getUpdatedAt() == null
                || resource.getUpdatedAt()
                        .isBefore(
                                calculatedAt.minusSeconds(
                                        LOCATION_STALE_SECONDS))) {

            System.out.println("REJECT: stale location");
            return false;
        }

        boolean capability = hasRequiredCapabilities(
                resource,
                request);

        System.out.println(
                "request serviceType = "
                        + (request.getServiceType() == null
                                ? null
                                : request.getServiceType().getId()));

        System.out.println(
                "resource type = "
                        + (resource.getResourceType() == null
                                ? null
                                : resource.getResourceType().getId()));

        System.out.println(
                "capability eligible = "
                        + capability);

        return capability;
    }

    private CandidateScore buildCandidate(
            DispatchResource resource, DispatchRequest request, LocalDateTime calculatedAt) {
        double distanceKm = haversine(
                request.getTargetLocation().getY(), request.getTargetLocation().getX(),
                resource.getCurrentLocation().getY(), resource.getCurrentLocation().getX());
        double etaSeconds = distanceKm / AVERAGE_SPEED_KMH * 3600;
        long locationAgeSeconds = Math.max(
                0, Duration.between(resource.getUpdatedAt(), calculatedAt).getSeconds());
        return new CandidateScore(
                resource, etaSeconds, distanceKm,
                calculateCapability(resource, request), locationAgeSeconds, calculateRisk(resource));
    }

    private ScoredCandidate scoreCandidate(
            CandidateScore candidate,
            double minEta, double maxEta,
            double minDistance, double maxDistance) {
        double nEta = normalizeCost(candidate.etaSeconds(), minEta, maxEta);
        double nDistance = normalizeCost(candidate.distanceKm(), minDistance, maxDistance);
        double nCapability = clamp01(candidate.capability());
        double nFreshness = clamp01(
                1.0 - (double) candidate.locationAgeSeconds() / LOCATION_STALE_SECONDS);
        double nRisk = clamp01(candidate.risk());
        double score = ETA_WEIGHT * nEta
                + DISTANCE_WEIGHT * nDistance
                + CAPABILITY_WEIGHT * nCapability
                + FRESHNESS_WEIGHT * nFreshness
                - RISK_WEIGHT * nRisk;
        return new ScoredCandidate(
                candidate, score, nEta, nDistance, nCapability, nFreshness, nRisk);
    }

    private RecommendationItemDto toRecommendation(ScoredCandidate scored, int rank) {
        CandidateScore candidate = scored.candidate();
        List<String> reasons = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        if (scored.nEta() >= 0.8)
            reasons.add("Thời gian dự kiến đến thấp");
        if (scored.nDistance() >= 0.8)
            reasons.add("Khoảng cách gần");
        if (scored.nCapability() >= 1.0)
            reasons.add("Đáp ứng đầy đủ năng lực yêu cầu");
        if (scored.nFreshness() >= 0.8)
            reasons.add("Vị trí được cập nhật gần đây");
        if (scored.nRisk() > 0)
            warnings.add("Xe có dữ liệu cảnh báo vận hành");

        return new RecommendationItemDto(
                candidate.resource().getId(),
                candidate.resource().getResourceCode(),
                rank,
                round(scored.score() * 100, 2),
                Math.round(candidate.etaSeconds()),
                round(candidate.distanceKm(), 2),
                candidate.resource().getUpdatedAt(),
                "HAVERSINE_ESTIMATE",
                new RecommendationItemDto.ScoreBreakdown(
                        round(scored.nEta(), 4),
                        round(scored.nDistance(), 4),
                        round(scored.nCapability(), 4),
                        round(scored.nFreshness(), 4),
                        round(scored.nRisk(), 4)),
                reasons,
                warnings);
    }

    private boolean hasRequiredCapabilities(DispatchResource resource, DispatchRequest request) {
        Set<String> required = stringSet(request.getExtendedRequirements(), "requiredCapabilities");
        if (required.isEmpty()) {
            return request.getServiceType() == null
                    || resource.getResourceType() == null
                    || Objects.equals(request.getServiceType().getId(), resource.getResourceType().getId());
        }
        return stringSet(resource.getExtendedAttributes(), "capabilities").containsAll(required);
    }

    private double calculateCapability(DispatchResource resource, DispatchRequest request) {
        Set<String> required = stringSet(request.getExtendedRequirements(), "requiredCapabilities");
        Set<String> preferred = stringSet(request.getExtendedRequirements(), "preferredCapabilities");
        Set<String> actual = stringSet(resource.getExtendedAttributes(), "capabilities");

        if (!preferred.isEmpty()) {
            long matched = preferred.stream().filter(actual::contains).count();
            return 0.75 + 0.25 * matched / preferred.size();
        }
        if (!required.isEmpty()) {
            return 1.0;
        }
        return request.getServiceType() != null
                && resource.getResourceType() != null
                && Objects.equals(request.getServiceType().getId(), resource.getResourceType().getId())
                        ? 1.0
                        : 0.5;
    }

    private double calculateRisk(DispatchResource resource) {
        Map<String, Object> attributes = resource.getExtendedAttributes();
        if (attributes == null) {
            return 0;
        }
        double risk = Boolean.TRUE.equals(attributes.get("maintenanceDue")) ? 0.6 : 0;
        Object warningCount = attributes.get("activeWarningCount");
        if (warningCount instanceof Number number) {
            risk += Math.min(number.doubleValue() * 0.1, 0.4);
        }
        return clamp01(risk);
    }

    private Set<String> stringSet(Map<String, Object> source, String key) {
        if (source == null || !(source.get(key) instanceof Collection<?> values)) {
            return Set.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(value -> value.toString().trim().toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
    }

    private double normalizeCost(double value, double min, double max) {
        if (Double.compare(min, max) == 0) {
            return 1.0;
        }
        return clamp01(1.0 - (value - min) / (max - min));
    }

    private double clamp01(double value) {
        return Math.max(0, Math.min(1, value));
    }

    private double round(double value, int scale) {
        double factor = Math.pow(10, scale);
        return Math.round(value * factor) / factor;
    }

    private record CandidateScore(
            DispatchResource resource,
            double etaSeconds,
            double distanceKm,
            double capability,
            long locationAgeSeconds,
            double risk) {
    }

    private record ScoredCandidate(
            CandidateScore candidate,
            double score,
            double nEta,
            double nDistance,
            double nCapability,
            double nFreshness,
            double nRisk) {
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
        newMission.setStatus(DispatchMissionStatus.DISPATCHED);
        DispatchMission saved = missionRepository.save(newMission);

        request.setStatus(DispatchRequestStatus.DISPATCHED);
        requestRepository.save(request);

        MissionStatusLog log = new MissionStatusLog();
        log.setMission(saved);
        log.setOldStatus(null);
        log.setNewStatus(DispatchMissionStatus.DISPATCHED.name());
        log.setNote("Điều phối lại với xe: " + newResource.getResourceCode());
        log.setCreatedAt(LocalDateTime.now());
        statusLogRepository.save(log);

        broadcastUpdate(request);
        return Map.of("missionId", saved.getId(), "resourceId", newResourceId);
    }

    // ──────────────────────────────────────────────
    // 9. POST /dispatch-requests/{id}/cancel
    // ──────────────────────────────────────────────
    // @Transactional
    // public Map<String, String> cancel(Integer id) {
    // DispatchRequest request = findById(id);

    // if (request.getStatus() == DispatchRequestStatus.COMPLETED) {
    // throw new IllegalStateException("Không thể hủy yêu cầu đã hoàn thành.");
    // }

    // // Giải phóng xe đang gắn (nếu có)
    // missionRepository.findActiveMissionByRequestId(id).ifPresent(mission -> {
    // mission.setStatus(DispatchMissionStatus.CANCELLED);
    // DispatchResource resource = mission.getResource();
    // if (resource != null) {
    // resource.setStatus(DispatchResourceStatus.AVAILABLE);
    // resourceRepository.save(resource);
    // }

    // MissionStatusLog log = new MissionStatusLog();
    // log.setMission(mission);
    // log.setOldStatus(mission.getStatus().name());
    // log.setNewStatus(DispatchMissionStatus.CANCELLED.name());
    // log.setNote("Hủy yêu cầu - giải phóng xe");
    // log.setCreatedAt(LocalDateTime.now());
    // statusLogRepository.save(log);
    // missionRepository.save(mission);
    // });

    // request.setStatus(DispatchRequestStatus.CANCELLED);
    // requestRepository.save(request);

    // broadcastUpdate(request);
    // return Map.of("status", "CANCELLED");
    // }

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
                        log.getNote()));
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
                requestRepository.countByStatus(DispatchRequestStatus.REJECTED)
        // requestRepository.countByStatus(DispatchRequestStatus.CANCELLED)
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
            specification = specification.and((root, query, cb) -> cb.equal(cb.upper(root.get("urgencyLevel")),
                    urgencyLevel.trim().toUpperCase()));
        }
        if (serviceTypeId != null) {
            specification = specification
                    .and((root, query, cb) -> cb.equal(root.get("serviceType").get("id"), serviceTypeId));
        }
        if (operationZoneId != null) {
            specification = specification
                    .and((root, query, cb) -> cb.equal(root.get("operationZone").get("id"), operationZoneId));
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

    private User requireUser(Integer userId) {
        if (userId == null) {
            throw new IllegalArgumentException("Không xác định được người dùng hiện tại");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy người dùng id: " + userId));
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank())
            return null;
        return value.trim();
    }

    private String normalizeUrgency(String urgency) {
        String normalized = trimToNull(urgency);
        if (normalized == null)
            return null;
        normalized = normalized.toUpperCase(Locale.ROOT);
        if (!Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL").contains(normalized)) {
            throw new IllegalArgumentException(
                    "confirmedUrgencyLevel chỉ chấp nhận LOW, MEDIUM, HIGH hoặc CRITICAL");
        }
        return normalized;
    }

    private void validateCoordinates(
            java.math.BigDecimal latitude,
            java.math.BigDecimal longitude) {
        if (latitude != null
                && (latitude.compareTo(java.math.BigDecimal.valueOf(-90)) < 0
                        || latitude.compareTo(java.math.BigDecimal.valueOf(90)) > 0)) {
            throw new IllegalArgumentException(
                    "confirmedLatitude phải nằm trong khoảng -90 đến 90");
        }
        if (longitude != null
                && (longitude.compareTo(java.math.BigDecimal.valueOf(-180)) < 0
                        || longitude.compareTo(java.math.BigDecimal.valueOf(180)) > 0)) {
            throw new IllegalArgumentException(
                    "confirmedLongitude phải nằm trong khoảng -180 đến 180");
        }
    }

    private void broadcastUpdate(DispatchRequest request) {
        messagingTemplate.convertAndSend("/topic/dispatcher/requests",
                (Object) Map.of(
                        "id", request.getId(),
                        "status", request.getStatus().name()));
    }

    private DispatchRequestDto mapToDto(DispatchRequest req) {
        Integer callId = req.getCall() != null ? req.getCall().getId() : null;
        Integer serviceTypeId = req.getServiceType() != null ? req.getServiceType().getId() : null;
        String serviceTypeName = req.getServiceType() != null ? req.getServiceType().getDisplayName() : null;
        Integer zoneId = req.getOperationZone() != null ? req.getOperationZone().getId() : null;
        String zoneName = req.getOperationZone() != null ? req.getOperationZone().getZoneName() : null;
        Integer dispatcherId = req.getCreatedByDispatcher() != null ? req.getCreatedByDispatcher().getId() : null;
        String dispatcherName = req.getCreatedByDispatcher() != null ? req.getCreatedByDispatcher().getFullName()
                : null;

        return new DispatchRequestDto(
                req.getId(), callId, serviceTypeId, serviceTypeName,
                zoneId, zoneName, dispatcherId, dispatcherName,
                req.getUrgencyLevel(), req.getLongitude(), req.getLatitude(),
                req.getStatus() != null ? req.getStatus().name() : null,
                req.getExtendedRequirements(), req.getCreatedAt());
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
