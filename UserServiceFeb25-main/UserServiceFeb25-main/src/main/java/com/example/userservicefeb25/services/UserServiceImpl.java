package com.example.userservicefeb25.services;

import com.example.userservicefeb25.dtos.SendEmailDto;
import com.example.userservicefeb25.models.Token;
import com.example.userservicefeb25.models.User;
import com.example.userservicefeb25.repositories.TokenRepository;
import com.example.userservicefeb25.repositories.UserRepository;
import com.example.userservicefeb25.repositories.RoleRepository;
import io.jsonwebtoken.Claims;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.example.userservicefeb25.security.jwt.JwtService;
import com.example.userservicefeb25.outbox.OutboxEvent;
import com.example.userservicefeb25.outbox.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository; // kept for backward-compatible database schema
    private final RoleRepository roleRepository;
    private final StringRedisTemplate redisTemplate;
    @Value("${security.admin-email:}")
    private String adminEmail;
    private final ObjectMapper objectMapper;
    private final OutboxEventRepository outboxEventRepository;
    private final JwtService jwtService;
    private final OtpService otpService;

    public UserServiceImpl(BCryptPasswordEncoder bCryptPasswordEncoder,
                           UserRepository userRepository,
                           TokenRepository tokenRepository,
                           ObjectMapper objectMapper,
                           JwtService jwtService,
                           RoleRepository roleRepository,
                           OutboxEventRepository outboxEventRepository,
                           StringRedisTemplate redisTemplate,
                           OtpService otpService) {
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.objectMapper = objectMapper;
        this.jwtService = jwtService;
        this.roleRepository = roleRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.redisTemplate = redisTemplate;
        this.otpService = otpService;
    }

    @Override
    @Transactional(readOnly = true)
    public Token login(String email, String password) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isEmpty()) return null;

        User user = optionalUser.get();
        if (!bCryptPasswordEncoder.matches(password, user.getPassword())) return null;
        if (Boolean.FALSE.equals(user.getEmailVerified())) {
            throw new IllegalStateException("Email is not verified. Please verify the OTP first.");
        }

        // JWT is now the access token. The legacy Token entity is retained only so
        // existing database tables/data do not break during migration.
        Token token = new Token();
        token.setValue(jwtService.generateToken(user));
        token.setUser(user);
        return token;
    }

    @Override
    @Transactional
    public User signUp(String name, String email, String password) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email is already registered");
        }

        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setPassword(bCryptPasswordEncoder.encode(password));
        user.setEmailVerified(false);
        com.example.userservicefeb25.models.Role userRole = roleRepository.findByValue("USER").orElseGet(() -> {
            com.example.userservicefeb25.models.Role role = new com.example.userservicefeb25.models.Role();
            role.setValue("USER");
            return roleRepository.save(role);
        });
        java.util.List<com.example.userservicefeb25.models.Role> roles = new java.util.ArrayList<>();
        roles.add(userRole);
        if (adminEmail != null && !adminEmail.isBlank() && adminEmail.equalsIgnoreCase(email)) {
            roles.add(roleRepository.findByValue("ADMIN").orElseGet(() -> {
                com.example.userservicefeb25.models.Role role = new com.example.userservicefeb25.models.Role();
                role.setValue("ADMIN");
                return roleRepository.save(role);
            }));
        }
        user.setRoles(roles);

        User savedUser = userRepository.save(user);

        otpService.sendRegistrationOtp(email);
        return savedUser;
    }

    @Override
    public void logout(String tokenValue) {
        if (tokenValue == null || tokenValue.isBlank()) return;
        try {
            Claims claims = jwtService.parse(tokenValue);
            long ttlMillis = claims.getExpiration().getTime() - System.currentTimeMillis();
            if (ttlMillis > 0 && claims.getId() != null) {
                redisTemplate.opsForValue().set("jwt:blacklist:" + claims.getId(), "revoked", java.time.Duration.ofMillis(ttlMillis));
            }
        } catch (Exception ignored) {
            // Invalid/expired tokens are already unusable.
        }
    }
    @Override
    @Transactional
    public void verifyRegistrationOtp(String email, String otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or OTP"));
        if (Boolean.TRUE.equals(user.getEmailVerified())) return;
        if (!otpService.verifyRegistrationOtp(email, otp)) {
            throw new IllegalArgumentException("Invalid or expired OTP");
        }
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    @Override
    public void resendRegistrationOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email is not registered"));
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new IllegalStateException("Email is already verified");
        }
        otpService.resendRegistrationOtp(email);
    }

    @Override
    public void forgotPassword(String email) {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email is required");
        userRepository.findByEmail(email).ifPresent(user -> otpService.sendPasswordResetOtp(email));
    }

    @Override
    public String verifyPasswordResetOtp(String email, String otp) {
        if (userRepository.findByEmail(email).isEmpty()) return null;
        String token = otpService.verifyPasswordResetOtp(email, otp);
        if (token == null) throw new IllegalArgumentException("Invalid or expired OTP");
        return token;
    }

    @Override
    @Transactional
    public void resetPassword(String resetToken, String newPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("Password must contain at least 8 characters");
        }
        String email = otpService.consumeResetToken(resetToken);
        if (email == null) throw new IllegalArgumentException("Invalid or expired reset token");
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token"));
        user.setPassword(bCryptPasswordEncoder.encode(newPassword));
        userRepository.save(user);
    }

}
