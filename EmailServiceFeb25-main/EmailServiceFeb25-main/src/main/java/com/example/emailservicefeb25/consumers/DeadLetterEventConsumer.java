package com.example.emailservicefeb25.consumers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class DeadLetterEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(DeadLetterEventConsumer.class);

    @KafkaListener(topics = {"sendEmail.DLT", "order-events.DLT", "payment-events.DLT"}, groupId = "kafka-dlt-monitor")
    public void onDeadLetter(String event) {
        // DLT messages are retained for manual inspection/replay. We intentionally do not
        // rethrow here, otherwise the DLT itself would keep retrying forever.
        log.error("Kafka message moved to DLT: {}", event);
    }
}
