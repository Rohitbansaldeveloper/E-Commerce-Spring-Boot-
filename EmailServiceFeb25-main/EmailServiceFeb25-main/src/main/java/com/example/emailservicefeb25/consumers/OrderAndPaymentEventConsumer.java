package com.example.emailservicefeb25.consumers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderAndPaymentEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(OrderAndPaymentEventConsumer.class);
    private final ObjectMapper objectMapper;

    public OrderAndPaymentEventConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "order-events", groupId = "email-service-orders")
    public void onOrderEvent(String event) throws Exception {
        JsonNode node = parseAndValidate(event, "OrderServiceFeb25");
        log.info("Consumed order event type={} orderId={} eventId={}",
                node.get("eventType").asText(), node.get("orderId").asText(), node.get("eventId").asText());
    }

    @KafkaListener(topics = "payment-events", groupId = "email-service-payments")
    public void onPaymentEvent(String event) throws Exception {
        JsonNode node = parseAndValidate(event, "PaymentServiceFeb25");
        log.info("Consumed payment event type={} orderId={} eventId={}",
                node.get("eventType").asText(), node.get("orderId").asText(), node.get("eventId").asText());
    }

    private JsonNode parseAndValidate(String event, String expectedSource) throws Exception {
        JsonNode node = objectMapper.readTree(event);
        if (node.get("eventId") == null || node.get("eventType") == null ||
                node.get("eventVersion") == null || node.get("source") == null) {
            throw new IllegalArgumentException("Invalid Kafka event envelope");
        }
        if (!expectedSource.equals(node.get("source").asText())) {
            throw new IllegalArgumentException("Unexpected event source: " + node.get("source").asText());
        }
        return node;
    }
}
