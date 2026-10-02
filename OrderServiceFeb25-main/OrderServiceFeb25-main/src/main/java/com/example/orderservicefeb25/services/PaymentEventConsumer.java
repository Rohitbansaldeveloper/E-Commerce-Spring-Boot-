package com.example.orderservicefeb25.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.example.orderservicefeb25.models.ProcessedEvent;
import com.example.orderservicefeb25.repositories.ProcessedEventRepository;
import java.time.Instant;

@Component
public class PaymentEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final ObjectMapper mapper;
    private final OrderService orderService;
    private final ProcessedEventRepository processedEvents;

    public PaymentEventConsumer(ObjectMapper mapper, OrderService orderService, ProcessedEventRepository processedEvents) {
        this.mapper = mapper;
        this.orderService = orderService;
        this.processedEvents = processedEvents;
    }

    @KafkaListener(topics = "payment-events", groupId = "order-service-saga")
    @Transactional
    public void consume(String payload) throws Exception {
        JsonNode event = mapper.readTree(payload);
        String eventId = event.path("eventId").asText();
        if (eventId.isBlank()) throw new IllegalArgumentException("Payment event does not contain eventId");
        if (processedEvents.existsByEventId(eventId)) { log.info("Ignoring duplicate payment event {}", eventId); return; }
        int version = event.path("eventVersion").asInt(0);
        if (version != 1) throw new IllegalArgumentException("Unsupported payment event version: " + version);
        String type = event.path("eventType").asText();
        String correlationId = event.path("correlationId").asText("");
        long orderId = event.path("orderId").asLong(0);

        if (orderId == 0) {
            throw new IllegalArgumentException("Payment event does not contain orderId");
        }

        switch (type) {
            case "PaymentSucceeded" -> {
                log.info("Saga payment success: orderId={} correlationId={}", orderId, correlationId);
                orderService.markPaid(orderId);
            }
            case "PaymentFailed" -> {
                log.warn("Saga payment failure: compensating inventory for orderId={} correlationId={}", orderId, correlationId);
                orderService.cancelAndCompensate(orderId);
            }
            default -> log.debug("Ignoring payment event type={} orderId={}", type, orderId);
        }
        ProcessedEvent processed = new ProcessedEvent(); processed.setEventId(eventId); processed.setProcessedAt(Instant.now()); processedEvents.save(processed);
    }
}
