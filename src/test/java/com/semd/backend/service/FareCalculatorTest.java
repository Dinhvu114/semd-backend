package com.semd.backend.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FareCalculatorTest {

    private final FareCalculator calculator = new FareCalculator();

    @Test
    void blsFare_10km_shouldCalculateCorrectly() {
        // 200,000 + 400,000 * 10 = 4,200,000
        FareCalculator.FareBreakdown result = calculator.calculate("BLS", BigDecimal.valueOf(10));

        assertEquals(0, result.baseFare().compareTo(BigDecimal.valueOf(200000)));
        assertEquals(0, result.distanceFare().compareTo(BigDecimal.valueOf(4000000)));
        assertEquals(0, result.totalFare().compareTo(BigDecimal.valueOf(4200000)));
    }

    @Test
    void alsFare_10km_shouldCalculateCorrectly() {
        // 300,000 + 450,000 * 10 = 4,800,000
        FareCalculator.FareBreakdown result = calculator.calculate("ALS", BigDecimal.valueOf(10));

        assertEquals(0, result.baseFare().compareTo(BigDecimal.valueOf(300000)));
        assertEquals(0, result.distanceFare().compareTo(BigDecimal.valueOf(4500000)));
        assertEquals(0, result.totalFare().compareTo(BigDecimal.valueOf(4800000)));
    }

    @Test
    void fareSplit_platformDriverProvider_shouldSumToTotal() {
        FareCalculator.FareBreakdown fare = calculator.calculate("ALS", BigDecimal.valueOf(12));

        BigDecimal platform = fare.totalFare().multiply(BigDecimal.valueOf(0.10));
        BigDecimal afterCommission = fare.totalFare().subtract(platform);
        BigDecimal driver = afterCommission.multiply(BigDecimal.valueOf(0.20));
        BigDecimal provider = afterCommission.subtract(driver);

        BigDecimal sum = platform.add(driver).add(provider);

        assertEquals(0, sum.setScale(0, java.math.RoundingMode.HALF_UP)
                .compareTo(fare.totalFare().setScale(0, java.math.RoundingMode.HALF_UP)));
    }
}