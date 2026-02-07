package com.example.service;

import brave.Span;
import brave.Tracer;
import com.example.dto.OrderRequest;
import io.micrometer.tracing.annotation.NewSpan;
import io.micrometer.tracing.annotation.SpanTag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {

    private final Tracer tracer;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;

    /**
     * Create order with distributed tracing
     */
    public Map<String, Object> createOrder(OrderRequest request) {
        log.info("Creating order for product: {}, quantity: {}, user: {}",
                request.getProductId(), request.getQuantity(), request.getUserId());

        // Start a custom span
        Span orderSpan = tracer.nextSpan().name("order.creation").start();
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(orderSpan)) {
            orderSpan.tag("product.id", request.getProductId());
            orderSpan.tag("user.id", request.getUserId() != null ? request.getUserId() : "anonymous");

            // Step 1: Check inventory (nested span)
            boolean inStock = inventoryService.checkStock(request.getProductId(), request.getQuantity());
            if (!inStock) {
                throw new RuntimeException("Insufficient stock for product: " + request.getProductId());
            }

            // Step 2: Process payment (async with tracing)
            String paymentId = processPaymentAsync(request);

            // Step 3: Update inventory
            updateInventory(request.getProductId(), request.getQuantity());

            // Step 4: Call external service (shows trace propagation)
            if (request.getCorrelationId() != null) {
                Map<String, Object> auditResult = callAuditService(request);
                log.debug("Audit result: {}", auditResult);
            }

            String orderId = generateOrderId();
            orderSpan.tag("order.id", orderId);

            return Map.of(
                    "orderId", orderId,
                    "status", "CREATED",
                    "paymentId", paymentId,
                    "message", "Order created successfully",
                    "traceId", tracer.currentSpan().context().traceIdString()
            );

        } catch (Exception e) {
            log.error("Order creation failed", e);
            orderSpan.error(e);
            throw e;
        } finally {
            orderSpan.finish();
        }
    }

    /**
     * Method with @NewSpan annotation
     */
    @NewSpan("inventory.update")
    private void updateInventory(@SpanTag("product.id") String productId,
                                 @SpanTag("quantity") Integer quantity) {
        log.info("Updating inventory for product: {}, quantity: {}", productId, quantity);

        // Simulate database update
        try {
            Thread.sleep(75);
            log.debug("Inventory updated successfully");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Inventory update interrupted", e);
        }
    }

    /**
     * Async processing with trace context propagation
     */
    private String processPaymentAsync(OrderRequest request) {
        log.info("Starting async payment processing");

        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            // Trace context is automatically propagated
            return paymentService.processPayment(request);
        });

        return future.join();
    }

    /**
     * External service call with trace propagation
     */
    @NewSpan("external.audit.call")
    private Map<String, Object> callAuditService(OrderRequest request) {
        String auditUrl = "http://localhost:8080/api/audit"; // Mock URL
        log.info("Calling audit service: {}", auditUrl);

        try {
            // RestTemplate is auto-instrumented for tracing
            RestTemplate restTemplate = new RestTemplate();
            return restTemplate.postForObject(auditUrl, request, Map.class);
        } catch (Exception e) {
            log.warn("Audit service call failed: {}", e.getMessage());
            return Map.of("status", "AUDIT_FAILED", "error", e.getMessage());
        }
    }

    private String generateOrderId() {
        return "ORD-" + System.currentTimeMillis() + "-" + new Random().nextInt(1000);
    }
}