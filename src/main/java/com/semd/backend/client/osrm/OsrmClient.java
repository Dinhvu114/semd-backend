package com.semd.backend.client.osrm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class OsrmClient {

    private static final Logger log = LoggerFactory.getLogger(OsrmClient.class);

    @Value("${app.osrm.base-url:http://localhost:5000}")
    private String baseUrl;

    @Value("${app.osrm.profile:driving}")
    private String profile;

    @Value("${app.osrm.max-retries:2}")
    private int maxRetries;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(2000))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Gọi OSRM Route API
     * @param fromLon kinh độ điểm đầu
     * @param fromLat vĩ độ điểm đầu
     * @param toLon   kinh độ điểm cuối
     * @param toLat   vĩ độ điểm cuối
     */
    public OsrmRouteResponse getRoute(double fromLon, double fromLat,
                                      double toLon,   double toLat) {
        // OSRM dùng thứ tự longitude,latitude
        String url = String.format(
                "%s/route/v1/%s/%f,%f;%f,%f?overview=full&geometries=geojson&steps=true&annotations=duration,distance",
                baseUrl, profile, fromLon, fromLat, toLon, toLat
        );

        for (int attempt = 1; attempt <= maxRetries + 1; attempt++) {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofMillis(5000))
                        .GET()
                        .build();

                HttpResponse<String> res = httpClient.send(req,
                        HttpResponse.BodyHandlers.ofString());

                if (res.statusCode() != 200) {
                    throw new RuntimeException("OSRM trả HTTP " + res.statusCode());
                }

                OsrmRouteResponse response = objectMapper.readValue(
                        res.body(), OsrmRouteResponse.class);

                if (!"Ok".equals(response.getCode())) {
                    throw new RuntimeException("OSRM NoRoute: " + response.getCode());
                }

                return response;

            } catch (Exception e) {
                log.warn("OSRM attempt {}/{} failed: {}", attempt, maxRetries + 1, e.getMessage());
                if (attempt > maxRetries) {
                    throw new RuntimeException("OSRM_UNAVAILABLE: " + e.getMessage());
                }
            }
        }
        throw new RuntimeException("OSRM_UNAVAILABLE");
    }
}