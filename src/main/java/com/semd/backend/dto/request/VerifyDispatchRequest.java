package com.semd.backend.dto.request;

import java.math.BigDecimal;

public record VerifyDispatchRequest(
        String verificationNote,
        String confirmedUrgencyLevel,
        String confirmedAddress,
        BigDecimal confirmedLatitude,
        BigDecimal confirmedLongitude
) {}
