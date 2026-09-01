package com.semd.backend.dto.response;

import java.math.BigDecimal;

public class DriverEarningSummaryResponse {
    private BigDecimal pendingEarnings;
    private BigDecimal paidEarnings;
    private Integer missionCount;
    private BigDecimal averagePerMission;

    public BigDecimal getPendingEarnings() { return pendingEarnings; }
    public void setPendingEarnings(BigDecimal pendingEarnings) { this.pendingEarnings = pendingEarnings; }
    public BigDecimal getPaidEarnings() { return paidEarnings; }
    public void setPaidEarnings(BigDecimal paidEarnings) { this.paidEarnings = paidEarnings; }
    public Integer getMissionCount() { return missionCount; }
    public void setMissionCount(Integer missionCount) { this.missionCount = missionCount; }
    public BigDecimal getAveragePerMission() { return averagePerMission; }
    public void setAveragePerMission(BigDecimal averagePerMission) { this.averagePerMission = averagePerMission; }
}