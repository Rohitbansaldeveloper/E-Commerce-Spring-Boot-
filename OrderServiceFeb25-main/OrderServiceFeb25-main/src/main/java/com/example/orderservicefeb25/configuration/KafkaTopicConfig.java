package com.example.orderservicefeb25.configuration;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
    private static final int PARTITIONS = 3;
    private static final short REPLICATION_FACTOR = 1; // local single-broker setup
    private static final String RETENTION_MS = "604800000"; // 7 days
    private static final String DLT_RETENTION_MS = "2592000000"; // 30 days

    @Bean public NewTopic orderEventsTopic() {
        return TopicBuilder.name("order-events").partitions(PARTITIONS).replicas(REPLICATION_FACTOR)
                .config("retention.ms", RETENTION_MS).build();
    }
    @Bean public NewTopic orderEventsDlt() {
        return TopicBuilder.name("order-events.DLT").partitions(PARTITIONS).replicas(REPLICATION_FACTOR)
                .config("retention.ms", DLT_RETENTION_MS).build();
    }
    @Bean public NewTopic paymentEventsTopic() {
        return TopicBuilder.name("payment-events").partitions(PARTITIONS).replicas(REPLICATION_FACTOR)
                .config("retention.ms", RETENTION_MS).build();
    }
    @Bean public NewTopic paymentEventsDlt() {
        return TopicBuilder.name("payment-events.DLT").partitions(PARTITIONS).replicas(REPLICATION_FACTOR)
                .config("retention.ms", DLT_RETENTION_MS).build();
    }
}
