package com.example.config;

import brave.baggage.BaggageField;
import brave.baggage.BaggagePropagation;
import brave.baggage.BaggagePropagationConfig;
import brave.baggage.CorrelationScopeConfig;
import brave.context.slf4j.MDCScopeDecorator;
import brave.propagation.B3Propagation;
import brave.propagation.Propagation;
import brave.propagation.ThreadLocalCurrentTraceContext;
import io.micrometer.tracing.brave.bridge.BraveBaggageManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@Slf4j
public class TracingConfig {

    /**
     * Configure B3 propagation with baggage support
     */
    @Bean
    public Propagation.Factory propagationFactory() {
        // Define baggage fields to propagate
        BaggageField userId = BaggageField.create("user-id");
        BaggageField tenantId = BaggageField.create("tenant-id");
        BaggageField correlationId = BaggageField.create("correlation-id");

        // Configure baggage propagation
        return BaggagePropagation.newFactoryBuilder(B3Propagation.FACTORY)
                .add(BaggagePropagationConfig.SingleBaggageField.newBuilder(userId)
                        .addKeyName("baggage-user-id")
                        .build())
                .add(BaggagePropagationConfig.SingleBaggageField.newBuilder(tenantId)
                        .addKeyName("baggage-tenant-id")
                        .build())
                .add(BaggagePropagationConfig.SingleBaggageField.newBuilder(correlationId)
                        .addKeyName("baggage-correlation-id")
                        .build())
                .build();
    }

    /**
     * Configure trace context with MDC support
     */
    @Bean
    public ThreadLocalCurrentTraceContext currentTraceContext() {
        return ThreadLocalCurrentTraceContext.newBuilder()
                .addScopeDecorator(MDCScopeDecorator.newBuilder()
                        .add(CorrelationScopeConfig.SingleCorrelationField.newBuilder(userId())
                                .flushOnUpdate()
                                .build())
                        .add(CorrelationScopeConfig.SingleCorrelationField.newBuilder(tenantId())
                                .flushOnUpdate()
                                .build())
                        .add(CorrelationScopeConfig.SingleCorrelationField.newBuilder(correlationId())
                                .flushOnUpdate()
                                .build())
                        .build())
                .build();
    }

    /**
     * Configure baggage manager
     */
    @Bean
    public BraveBaggageManager braveBaggageManager() {
        return new BraveBaggageManager();
    }

    /**
     * Auto-instrumented RestTemplate for tracing
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public BaggageField userId() {
        return BaggageField.create("user-id");
    }

    @Bean
    public BaggageField tenantId() {
        return BaggageField.create("tenant-id");
    }

    @Bean
    public BaggageField correlationId() {
        return BaggageField.create("correlation-id");
    }
}