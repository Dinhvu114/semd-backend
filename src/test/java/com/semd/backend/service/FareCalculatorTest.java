package com.semd.backend.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FareCalculatorTest {

    private final FareCalculator calculator = new FareCalculator();

    @Test
    void blsFare_10km_shouldCalculateCorrectly() {
        // 200,000 + 40,000 * 10 = 600,000
        FareCalculator.FareBreakdown result = calculator.calculate("BLS", BigDecimal.valueOf(10));

        assertEquals(0, result.baseFare().compareTo(BigDecimal.valueOf(200000)));
        assertEquals(0, result.distanceFare().compareTo(BigDecimal.valueOf(400000)));
        assertEquals(0, result.totalFare().compareTo(BigDecimal.valueOf(600000)));
    }

    @Test
    void alsFare_10km_shouldCalculateCorrectly() {
        // 300,000 + 45,000 * 10 = 750,000
        FareCalculator.FareBreakdown result = calculator.calculate("ALS", BigDecimal.valueOf(10));

        assertEquals(0, result.baseFare().compareTo(BigDecimal.valueOf(300000)));
        assertEquals(0, result.distanceFare().compareTo(BigDecimal.valueOf(450000)));
        assertEquals(0, result.totalFare().compareTo(BigDecimal.valueOf(750000)));
    }

    @Test
    void fareSplit_platformDriverProvider_shouldSumToTotal() {
        FareCalculator.FareBreakdown fare = calculator.calculate("ALS", BigDecimal.valueOf(12));

        BigDecimal platform = fare.totalFare().multiply(BigDecimal.valueOf(0.10));
        BigDecimal afterCommission = fare.totalFare().subtract(platform);
        BigDecimal driver = afterCommission.multiply(BigDecimal.valueOf(0.20));
        BigDecimal provider = afterCommission.subtract(driver);

        BigDecimal sum = platform.add(driver).add(provider);

        assertEquals(0, sum.setScale(0, RoundingMode.HALF_UP)
                .compareTo(fare.totalFare().setScale(0, RoundingMode.HALF_UP)));
    }
}