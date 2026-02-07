package com.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Slf4j
class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        log.debug("Health check called");
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "order-service",
                "timestamp", String.valueOf(System.currentTimeMillis())
        ));
    }

    @GetMapping("/ready")
    public ResponseEntity<Map<String, String>> ready() {
        return ResponseEntity.ok(Map.of(
                "status", "READY",
                "timestamp", String.valueOf(System.currentTimeMillis())
        ));
    }
}