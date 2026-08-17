package com.semd.backend.service;

import com.semd.backend.dto.response.TrackingContextResponse;
import com.semd.backend.entity.DispatchRequest;
import com.semd.backend.entity.EmergencyCall;
import com.semd.backend.repository.AmbulanceSimulationRepository;
import com.semd.backend.repository.DispatchMissionRepository;
import com.semd.backend.repository.DispatchRequestRepository;
import com.semd.backend.repository.EmergencyCallRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class EmergencyCallTrackingService {

    private final EmergencyCallRepository callRepository;
    private final DispatchRequestRepository dispatchRequestRepository;
    private final DispatchMissionRepository missionRepository;
    private final AmbulanceSimulationRepository simulationRepository;

    public EmergencyCallTrackingService(
            EmergencyCallRepository callRepository,
            DispatchRequestRepository dispatchRequestRepository,
            DispatchMissionRepository missionRepository,
            AmbulanceSimulationRepository simulationRepository) {

        this.callRepository = callRepository;
        this.dispatchRequestRepository = dispatchRequestRepository;
        this.missionRepository = missionRepository;
        this.simulationRepository = simulationRepository;
    }

    public TrackingContextResponse getTrackingContext(Integer callId) {

        EmergencyCall call = callRepository.findById(callId)
                .orElseThrow(() ->
                        new RuntimeException("CALL_NOT_FOUND: " + callId));

        TrackingContextResponse res = new TrackingContextResponse();
        res.setCallId(call.getId());

        // Lấy DispatchRequest mới nhất của EmergencyCall
        Optional<DispatchRequest> requestOpt =
                dispatchRequestRepository
                        .findFirstByCallIdOrderByCreatedAtDesc(call.getId());

        // Call chưa có DispatchRequest
        if (requestOpt.isEmpty()) {
            return res;
        }

        DispatchRequest request = requestOpt.get();

        res.setDispatchRequestId(request.getId());
        res.setRequestStatus(request.getStatus().name());

        // Lấy mission đang active
        missionRepository.findActiveMissionByRequestId(request.getId())
                .ifPresent(mission -> {

                    res.setMissionId(mission.getId());
                    res.setMissionStatus(mission.getStatus().name());

                    // Lấy simulation nếu có
                    simulationRepository.findByMissionId(mission.getId())
                            .ifPresent(simulation ->
                                    res.setSimulationId(simulation.getId()));
                });

        return res;
    }
}