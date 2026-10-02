package com.example.emailservicefeb25.configuration;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {
    @Bean public NewTopic sendEmailTopic() { return new NewTopic("sendEmail", 3, (short) 1); }
    @Bean public NewTopic sendEmailDlt() { return new NewTopic("sendEmail.DLT", 3, (short) 1); }
    @Bean public NewTopic orderEventsTopic() { return new NewTopic("order-events", 3, (short) 1); }
    @Bean public NewTopic orderEventsDlt() { return new NewTopic("order-events.DLT", 3, (short) 1); }
    @Bean public NewTopic paymentEventsTopic() { return new NewTopic("payment-events", 3, (short) 1); }
    @Bean public NewTopic paymentEventsDlt() { return new NewTopic("payment-events.DLT", 3, (short) 1); }
}
