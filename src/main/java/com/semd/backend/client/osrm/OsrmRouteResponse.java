package com.semd.backend.client.osrm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OsrmRouteResponse {

    @JsonProperty("code")
    private String code;

    @JsonProperty("routes")
    private List<OsrmRoute> routes;

    public String getCode() { return code; }
    public List<OsrmRoute> getRoutes() { return routes; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OsrmRoute {
        @JsonProperty("distance")
        private Double distance;

        @JsonProperty("duration")
        private Double duration;

        @JsonProperty("geometry")
        private OsrmGeometry geometry;

        @JsonProperty("legs")
        private List<OsrmLeg> legs;

        public Double getDistance() { return distance; }
        public Double getDuration() { return duration; }
        public OsrmGeometry getGeometry() { return geometry; }
        public List<OsrmLeg> getLegs() { return legs; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OsrmGeometry {
        @JsonProperty("type")
        private String type;

        @JsonProperty("coordinates")
        private List<List<Double>> coordinates; // [lon, lat]

        public String getType() { return type; }
        public List<List<Double>> getCoordinates() { return coordinates; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OsrmLeg {
        @JsonProperty("distance")
        private Double distance;

        @JsonProperty("duration")
        private Double duration;

        @JsonProperty("annotation")
        private OsrmAnnotation annotation;

        public Double getDistance() { return distance; }
        public Double getDuration() { return duration; }
        public OsrmAnnotation getAnnotation() { return annotation; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OsrmAnnotation {
        @JsonProperty("duration")
        private List<Double> duration;

        @JsonProperty("distance")
        private List<Double> distance;

        public List<Double> getDuration() { return duration; }
        public List<Double> getDistance() { return distance; }
    }
}