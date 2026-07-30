package com.semd.backend.dto.response;

import java.time.LocalDateTime;

public record TimelineEventDto(
        String event,
        LocalDateTime time,
        String note
) {}
