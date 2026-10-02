package com.example.paymentservicefeb25.configuration;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {
    @Bean public NewTopic paymentEventsTopic() { return new NewTopic("payment-events", 3, (short) 1); }
    @Bean public NewTopic paymentEventsDlt() { return new NewTopic("payment-events.DLT", 3, (short) 1); }
}
