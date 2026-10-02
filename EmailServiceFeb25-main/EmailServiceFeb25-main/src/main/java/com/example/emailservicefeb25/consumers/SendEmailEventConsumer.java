package com.example.emailservicefeb25.consumers;

import com.example.emailservicefeb25.dtos.SendEmailDto;
import com.example.emailservicefeb25.utils.EmailUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class SendEmailEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(SendEmailEventConsumer.class);
    private final ObjectMapper objectMapper;
    private final EmailUtil emailUtil;

    public SendEmailEventConsumer(ObjectMapper objectMapper, EmailUtil emailUtil) {
        this.objectMapper = objectMapper;
        this.emailUtil = emailUtil;
    }

    @KafkaListener(topics = "sendEmail", groupId = "emailService")
    public void handleSendEmailEvent(String message) throws Exception {
        JsonNode root = objectMapper.readTree(message);
        if (root.get("eventId") == null || root.get("eventType") == null || root.get("email") == null) {
            throw new IllegalArgumentException("Invalid sendEmail event");
        }

        SendEmailDto emailDto = new SendEmailDto();
        emailDto.setEmail(root.get("email").asText());
        emailDto.setSubject(root.hasNonNull("subject") ? root.get("subject").asText() : "Notification");
        emailDto.setBody(root.hasNonNull("body") ? root.get("body").asText() : "");

        emailUtil.sendEmail(emailDto.getEmail(), emailDto.getSubject(), emailDto.getBody());
        log.info("Email event processed successfully eventId={} email={}", root.get("eventId").asText(), emailDto.getEmail());
    }
}
