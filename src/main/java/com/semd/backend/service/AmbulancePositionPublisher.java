package com.semd.backend.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class AmbulancePositionPublisher {

    private final SimpMessagingTemplate messaging;
    private final AtomicLong sequence = new AtomicLong();

    public AmbulancePositionPublisher(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    public void publish(
            Integer resourceId,
            Integer missionId,
            String sourceType,
            double longitude,
            double latitude
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put("eventId", UUID.randomUUID().toString());
        payload.put("eventType", "AMBULANCE_POSITION_UPDATED");
        payload.put("occurredAt", OffsetDateTime.now().toString());

        payload.put("resourceId", resourceId);
        payload.put("missionId", missionId);
        payload.put("sourceType", sourceType);

        payload.put(
                "position",
                Map.of(
                        "longitude", longitude,
                        "latitude", latitude
                )
        );

        payload.put("sequence", sequence.incrementAndGet());

        messaging.convertAndSend(
                "/topic/dispatcher/ambulances",
                (Object) payload
        );
    }
}