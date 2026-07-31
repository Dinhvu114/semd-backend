package com.semd.backend.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class SimulationEventPublisher {

    private final SimpMessagingTemplate messaging;
    private final AtomicLong sequence = new AtomicLong(0);

    public SimulationEventPublisher(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    public void publishPosition(Long simulationId, Integer missionId, Integer resourceId,
                                String status, String phase,
                                double lon, double lat,
                                double progressPercent, double remainingDistanceM,
                                double etaSeconds) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventId",                UUID.randomUUID().toString());
        payload.put("eventType",              "AMBULANCE_POSITION_UPDATED");
        payload.put("occurredAt",             OffsetDateTime.now().toString());
        payload.put("simulationId",           simulationId);
        payload.put("missionId",              missionId);
        payload.put("resourceId",             resourceId);
        payload.put("sourceType",             "SIMULATION");
        payload.put("status",                 status);
        payload.put("phase",                  phase);
        payload.put("position",               Map.of("longitude", lon, "latitude", lat));
        payload.put("progressPercent",        progressPercent);
        payload.put("remainingDistanceMeters",remainingDistanceM);
        payload.put("etaSeconds",             etaSeconds);
        payload.put("sequence",               sequence.incrementAndGet());

        messaging.convertAndSend("/topic/simulations/" + simulationId, (Object) payload);
        messaging.convertAndSend("/topic/dispatcher/ambulances",       (Object) payload);
    }

    public void publishEvent(Long simulationId, Integer missionId,
                             Integer resourceId, String eventType) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventId",      UUID.randomUUID().toString());
        payload.put("eventType",    eventType);
        payload.put("occurredAt",   OffsetDateTime.now().toString());
        payload.put("simulationId", simulationId);
        payload.put("missionId",    missionId);
        payload.put("resourceId",   resourceId);
        payload.put("sourceType",   "SIMULATION");
        payload.put("sequence",     sequence.incrementAndGet());

        messaging.convertAndSend("/topic/simulations/" + simulationId, (Object) payload);
    }
}