package com.semd.backend.dto.response;

import java.util.List;

public class RouteGeometryResponse {
    private Long simulationId;
    private List<LegGeometry> legs;

    public Long getSimulationId() { return simulationId; }
    public void setSimulationId(Long simulationId) { this.simulationId = simulationId; }
    public List<LegGeometry> getLegs() { return legs; }
    public void setLegs(List<LegGeometry> legs) { this.legs = legs; }

    public static class LegGeometry {
        private String type;        // TO_SCENE hoặc TO_HOSPITAL
        private Double distanceMeters;
        private Double durationSeconds;
        private Object geometry;    // GeoJSON object nguyên gốc từ OSRM

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Double getDistanceMeters() { return distanceMeters; }
        public void setDistanceMeters(Double distanceMeters) { this.distanceMeters = distanceMeters; }
        public Double getDurationSeconds() { return durationSeconds; }
        public void setDurationSeconds(Double durationSeconds) { this.durationSeconds = durationSeconds; }
        public Object getGeometry() { return geometry; }
        public void setGeometry(Object geometry) { this.geometry = geometry; }
    }
}