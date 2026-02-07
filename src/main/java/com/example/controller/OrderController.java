package com.example.controller;

import com.example.dto.OrderRequest;
import com.example.service.OrderService;
import io.micrometer.observation.annotation.Observed;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/orders")
    public ResponseEntity<Map<String, Object>> createOrder(
            @Valid @RequestBody OrderRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        // Propagate headers to request
        request.setUserId(userId);
        request.setTenantId(tenantId);
        request.setCorrelationId(correlationId);

        log.info("Received order creation request from user: {}, tenant: {}, correlation: {}",
                userId, tenantId, correlationId);

        try {
            Map<String, Object> result = orderService.createOrder(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Order creation failed", e);
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", e.getMessage(),
                            "success", false,
                            "traceId", getCurrentTraceId()
                    ));
        }
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<Map<String, Object>> getOrder(@PathVariable String id) {
        log.info("Fetching order: {}", id);

        // Simulate processing
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return ResponseEntity.ok(Map.of(
                "orderId", id,
                "status", "PROCESSING",
                "traceId", getCurrentTraceId()
        ));
    }

    @PostMapping("/audit")
    public ResponseEntity<Map<String, Object>> audit(@RequestBody Map<String, Object> auditData) {
        log.info("Audit endpoint called with data: {}", auditData);

        // This simulates an external service being called
        return ResponseEntity.ok(Map.of(
                "audited", true,
                "timestamp", System.currentTimeMillis(),
                "traceId", getCurrentTraceId()
        ));
    }

    private String getCurrentTraceId() {
        try {
            // In real implementation, get from tracing context
            return "trace-" + System.currentTimeMillis();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
