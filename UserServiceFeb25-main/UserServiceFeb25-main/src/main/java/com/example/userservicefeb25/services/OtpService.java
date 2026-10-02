package com.example.userservicefeb25.services;

import com.example.userservicefeb25.outbox.OutboxEvent;
import com.example.userservicefeb25.outbox.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class OtpService {
    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    private static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(10);
    private static final int MAX_ATTEMPTS = 5;

    private final StringRedisTemplate redisTemplate;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpService(StringRedisTemplate redisTemplate,
                      OutboxEventRepository outboxEventRepository,
                      ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    public void sendRegistrationOtp(String email) {
        sendOtp(email, "registration");
    }

    public void resendRegistrationOtp(String email) {
        sendOtp(email, "registration");
    }

    public void sendPasswordResetOtp(String email) {
        sendOtp(email, "password-reset");
    }

    public boolean verifyRegistrationOtp(String email, String otp) {
        return verifyOtp(email, otp, "registration");
    }

    public String verifyPasswordResetOtp(String email, String otp) {
        if (!verifyOtp(email, otp, "password-reset")) return null;
        String resetToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(resetTokenKey(resetToken), email, RESET_TOKEN_TTL);
        return resetToken;
    }

    public String consumeResetToken(String resetToken) {
        String key = resetTokenKey(resetToken);
        String email = redisTemplate.opsForValue().get(key);
        if (email != null) redisTemplate.delete(key);
        return email;
    }

    private void sendOtp(String email, String purpose) {
        String normalizedEmail = normalizeEmail(email);
        String cooldownKey = cooldownKey(normalizedEmail, purpose);
        Boolean created = redisTemplate.opsForValue().setIfAbsent(cooldownKey, "1", RESEND_COOLDOWN);
        if (!Boolean.TRUE.equals(created)) {
            throw new IllegalStateException("Please wait before requesting another OTP");
        }

        String otp = String.format("%06d", secureRandom.nextInt(1_000_000));
        String otpKey = otpKey(normalizedEmail, purpose);
        String attemptsKey = attemptsKey(normalizedEmail, purpose);
        redisTemplate.opsForValue().set(otpKey, hash(otp), OTP_TTL);
        redisTemplate.opsForValue().set(attemptsKey, "0", OTP_TTL);

        String subject = "Your verification OTP";
        String body = "Your OTP is " + otp + ". It expires in 5 minutes. Do not share this OTP with anyone.";
        if ("password-reset".equals(purpose)) {
            subject = "Your password reset OTP";
            body = "Your password reset OTP is " + otp + ". It expires in 5 minutes. Do not share this OTP with anyone.";
        }
        publishEmail(normalizedEmail, subject, body);
    }

    private boolean verifyOtp(String email, String otp, String purpose) {
        String normalizedEmail = normalizeEmail(email);
        if (otp == null || !otp.matches("\\d{6}")) return false;

        String attemptsKey = attemptsKey(normalizedEmail, purpose);
        String attemptsValue = redisTemplate.opsForValue().get(attemptsKey);
        int attempts = attemptsValue == null ? 0 : Integer.parseInt(attemptsValue);
        if (attempts >= MAX_ATTEMPTS) return false;

        String storedHash = redisTemplate.opsForValue().get(otpKey(normalizedEmail, purpose));
        if (storedHash == null) return false;

        if (!MessageDigest.isEqual(storedHash.getBytes(StandardCharsets.UTF_8),
                hash(otp).getBytes(StandardCharsets.UTF_8))) {
            long next = redisTemplate.opsForValue().increment(attemptsKey);
            if (next == 1) redisTemplate.expire(attemptsKey, OTP_TTL);
            return false;
        }

        redisTemplate.delete(otpKey(normalizedEmail, purpose));
        redisTemplate.delete(attemptsKey);
        redisTemplate.delete(cooldownKey(normalizedEmail, purpose));
        return true;
    }

    private void publishEmail(String email, String subject, String body) {
        try {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("eventId", UUID.randomUUID().toString());
            event.put("eventType", "SendOtpEmail");
            event.put("eventVersion", 1);
            event.put("source", "UserServiceFeb25");
            event.put("occurredAt", Instant.now().toString());
            event.put("correlationId", "email-" + UUID.randomUUID());
            event.put("email", email);
            event.put("subject", subject);
            event.put("body", body);

            OutboxEvent outbox = new OutboxEvent();
            outbox.setTopic("sendEmail");
            outbox.setEventKey(email);
            outbox.setPayload(objectMapper.writeValueAsString(event));
            outboxEventRepository.save(outbox);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to create OTP email event", e);
        }
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email is required");
        return email.trim().toLowerCase();
    }

    private String hash(String otp) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(otp.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private String otpKey(String email, String purpose) { return "otp:" + purpose + ":" + email; }
    private String attemptsKey(String email, String purpose) { return "otp:attempts:" + purpose + ":" + email; }
    private String cooldownKey(String email, String purpose) { return "otp:cooldown:" + purpose + ":" + email; }
    private String resetTokenKey(String token) { return "password-reset-token:" + token; }
}
