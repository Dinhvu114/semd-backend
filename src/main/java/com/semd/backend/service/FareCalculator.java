package com.semd.backend.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class FareCalculator {

    // ── Bảng giá cố định (giai đoạn demo) ──────────────────
    private static final BigDecimal BLS_BASE_FARE     = BigDecimal.valueOf(200000);
    private static final BigDecimal BLS_PRICE_PER_KM  = BigDecimal.valueOf(40000);

    private static final BigDecimal ALS_BASE_FARE     = BigDecimal.valueOf(300000);
    private static final BigDecimal ALS_PRICE_PER_KM  = BigDecimal.valueOf(45000);

    public record FareBreakdown(
            String serviceTypeCode,
            BigDecimal distanceKm,
            BigDecimal baseFare,
            BigDecimal pricePerKm,
            BigDecimal distanceFare,
            BigDecimal totalFare
    ) {}

    public FareBreakdown calculate(String serviceTypeCode, BigDecimal distanceKm) {
        if (distanceKm == null) {
            distanceKm = BigDecimal.ZERO;
        }

        BigDecimal baseFare;
        BigDecimal pricePerKm;

        // Mặc định coi service khác BLS đều tính theo ALS
        if ("BLS".equalsIgnoreCase(serviceTypeCode)) {
            baseFare = BLS_BASE_FARE;
            pricePerKm = BLS_PRICE_PER_KM;
        } else {
            baseFare = ALS_BASE_FARE;
            pricePerKm = ALS_PRICE_PER_KM;
        }

        BigDecimal distanceFare = distanceKm
                .multiply(pricePerKm)
                .setScale(0, RoundingMode.HALF_UP);

        BigDecimal totalFare = baseFare.add(distanceFare);

        return new FareBreakdown(
                serviceTypeCode,
                distanceKm,
                baseFare,
                pricePerKm,
                distanceFare,
                totalFare
        );
    }
}