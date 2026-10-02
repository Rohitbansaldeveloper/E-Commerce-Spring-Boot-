package com.example.emailservicefeb25.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class InternalServiceTokenFilter extends OncePerRequestFilter {
    private final String expectedToken;

    public InternalServiceTokenFilter(@Value("${internal.service.token}") String expectedToken) {
        if (expectedToken == null || expectedToken.length() < 32) {
            throw new IllegalArgumentException("internal.service.token must contain at least 32 characters");
        }
        this.expectedToken = expectedToken;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/actuator/health") || path.startsWith("/actuator/health/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String actual = request.getHeader("X-Service-Token");
        if (actual == null || !MessageDigest.isEqual(
                expectedToken.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            response.getWriter().write("{\"error\":\"Missing or invalid internal service credential\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
