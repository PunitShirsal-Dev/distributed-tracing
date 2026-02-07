package com.example.service;

import brave.Span;
import brave.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class InventoryService {
    private final Tracer tracer;

    public InventoryService(Tracer tracer) {
        this.tracer = tracer;
    }

    /**
     * Check stock with custom span
     */
    public boolean checkStock(String productId, Integer quantity) {
        Span stockSpan = tracer.nextSpan().name("inventory.check").start();
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(stockSpan)) {
            stockSpan.tag("product.id", productId);
            stockSpan.tag("requested.quantity", quantity.toString());

            log.info("Checking stock for product: {}", productId);

            // Simulate database call
            Thread.sleep(100);

            // Mock business logic
            boolean inStock = quantity <= 100; // Assume we have 100 in stock

            stockSpan.tag("in.stock", String.valueOf(inStock));
            stockSpan.annotate("Stock check completed");

            return inStock;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            stockSpan.error(e);
            throw new RuntimeException("Stock check interrupted", e);
        } finally {
            stockSpan.finish();
        }
    }
}