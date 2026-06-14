package com.semd.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Health Check", description = "Kiểm tra hệ thống")
public class HealthController {

    @GetMapping("/hello")
    @Operation(summary = "Hello World", description = "API kiểm tra server đang chạy")
    public ResponseEntity<Map<String, Object>> hello() {
        return ResponseEntity.ok(Map.of(
                "message", "SEMD Backend is running!",
                "system", "Smart Emergency Medical Dispatch",
                "timestamp", LocalDateTime.now().toString(),
                "status", "OK"
        ));
    }
}