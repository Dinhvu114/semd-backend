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

    // Đọc từ config thay vì hard-code
    @Value("${app.osrm.connect-timeout-ms:2000}")
    private int connectTimeoutMs;

    @Value("${app.osrm.read-timeout-ms:5000}")
    private int readTimeoutMs;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public OsrmRouteResponse getRoute(double fromLon, double fromLat,
                                      double toLon,   double toLat) {
        String url = String.format(
                "%s/route/v1/%s/%f,%f;%f,%f" +
                        "?overview=full&geometries=geojson&steps=true&annotations=duration,distance",
                baseUrl, profile, fromLon, fromLat, toLon, toLat
        );

        Exception lastException = null;
        for (int attempt = 1; attempt <= maxRetries + 1; attempt++) {
            try {
                HttpClient httpClient = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                        .build();

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofMillis(readTimeoutMs))
                        .GET()
                        .build();

                HttpResponse<String> res = httpClient.send(req,
                        HttpResponse.BodyHandlers.ofString());

                if (res.statusCode() != 200) {
                    throw new RuntimeException("OSRM HTTP " + res.statusCode());
                }

                OsrmRouteResponse response = objectMapper.readValue(
                        res.body(), OsrmRouteResponse.class);

                if (!"Ok".equals(response.getCode())) {
                    throw new RuntimeException("OSRM NoRoute: " + response.getCode());
                }

                return response;

            } catch (Exception e) {
                lastException = e;
                log.warn("OSRM attempt {}/{}: {}", attempt, maxRetries + 1, e.getMessage());
            }
        }
        throw new RuntimeException("OSRM_UNAVAILABLE: " + lastException.getMessage());
    }
}