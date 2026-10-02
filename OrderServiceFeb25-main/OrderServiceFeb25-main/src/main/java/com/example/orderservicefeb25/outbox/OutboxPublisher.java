package com.example.orderservicefeb25.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(OutboxEventRepository repository, KafkaTemplate<String, String> kafkaTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${kafka.outbox.publish-interval-ms:5000}")
    @Transactional
    public void publishPendingEvents() {
        for (OutboxEvent event : repository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc()) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getEventKey(), event.getPayload()).get();
                event.setPublishedAt(java.time.Instant.now());
                repository.save(event);
                log.info("Published outbox eventId={} topic={} key={}", event.getEventId(), event.getTopic(), event.getEventKey());
            } catch (Exception ex) {
                log.error("Kafka publish failed for outbox eventId={} topic={}; it will be retried", event.getEventId(), event.getTopic(), ex);
            }
        }
    }
}
