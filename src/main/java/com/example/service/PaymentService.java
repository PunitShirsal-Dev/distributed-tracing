package com.example.service;

import brave.Span;
import brave.Tracer;
import com.example.dto.OrderRequest;
import io.micrometer.tracing.annotation.NewSpan;
import io.micrometer.tracing.annotation.SpanTag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PaymentService {
    private final Tracer tracer;

    public PaymentService(Tracer tracer) {
        this.tracer = tracer;
    }

    @NewSpan("payment.process")
    public String processPayment(@SpanTag("order.request") OrderRequest request) {
        log.info("Processing payment for order, user: {}", request.getUserId());

        // Get current span and add baggage
        Span currentSpan = tracer.currentSpan();
        if (currentSpan != null) {
            currentSpan.tag("payment.method", "CREDIT_CARD");
            currentSpan.tag("user.id", request.getUserId());
        }

        try {
            // Simulate payment processing
            Thread.sleep(150);

            // Simulate occasional failure
            if (Math.random() < 0.15) {
                throw new RuntimeException("Payment gateway timeout");
            }

            String paymentId = "PAY-" + System.currentTimeMillis();
            log.info("Payment processed successfully: {}", paymentId);

            return paymentId;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Payment processing interrupted", e);
        }
    }
}