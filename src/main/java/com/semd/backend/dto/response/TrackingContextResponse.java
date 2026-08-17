package com.semd.backend.dto.response;

public class TrackingContextResponse {
    private Integer callId;
    private Integer dispatchRequestId;
    private String requestStatus;
    private Integer missionId;
    private String missionStatus;
    private Long simulationId;

    public Integer getCallId() { return callId; }
    public void setCallId(Integer callId) { this.callId = callId; }
    public Integer getDispatchRequestId() { return dispatchRequestId; }
    public void setDispatchRequestId(Integer dispatchRequestId) { this.dispatchRequestId = dispatchRequestId; }
    public String getRequestStatus() { return requestStatus; }
    public void setRequestStatus(String requestStatus) { this.requestStatus = requestStatus; }
    public Integer getMissionId() { return missionId; }
    public void setMissionId(Integer missionId) { this.missionId = missionId; }
    public String getMissionStatus() { return missionStatus; }
    public void setMissionStatus(String missionStatus) { this.missionStatus = missionStatus; }
    public Long getSimulationId() { return simulationId; }
    public void setSimulationId(Long simulationId) { this.simulationId = simulationId; }
}