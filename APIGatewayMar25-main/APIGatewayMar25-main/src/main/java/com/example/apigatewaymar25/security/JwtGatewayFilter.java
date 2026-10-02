package com.example.apigatewaymar25.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtGatewayFilter implements GlobalFilter, Ordered {
    private final SecretKey key;
    private final String issuer;
    private final String internalServiceToken;
    private final StringRedisTemplate redisTemplate;

    public JwtGatewayFilter(@Value("${jwt.secret}") String secret,
                            @Value("${jwt.issuer:UserServiceFeb25}") String issuer,
                            @Value("${internal.service.token}") String internalServiceToken,
                            StringRedisTemplate redisTemplate) {
        if (secret == null || secret.length() < 32) throw new IllegalArgumentException("jwt.secret must contain at least 32 characters");
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        if (internalServiceToken == null || internalServiceToken.length() < 32) throw new IllegalArgumentException("internal.service.token must contain at least 32 characters");
        this.internalServiceToken = internalServiceToken;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (path.equals("/payments/webhook") || path.equals("/users/login") || path.equals("/users/signup")
                || path.equals("/users/verify-otp") || path.equals("/users/resend-otp")
                || path.equals("/users/forgot-password") || path.equals("/users/verify-reset-otp")
                || path.equals("/users/reset-password")) {
            return chain.filter(exchange.mutate().request(exchange.getRequest().mutate()
                    .headers(headers -> headers.set("X-Service-Token", internalServiceToken)).build()).build());
        }

        String auth = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.startsWith("Bearer ")) return unauthorized(exchange);
        try {
            Claims claims = Jwts.parser().verifyWith(key).requireIssuer(issuer).build()
                    .parseSignedClaims(auth.substring(7).trim()).getPayload();
            if (claims.getId() != null && Boolean.TRUE.equals(redisTemplate.hasKey("jwt:blacklist:" + claims.getId()))) return unauthorized(exchange);

            exchange = exchange.mutate().request(exchange.getRequest().mutate()
                    .headers(headers -> headers.set("X-User-Id", claims.getSubject())).build()).build();

            if (requiresAdmin(exchange.getRequest().getMethod(), path)) {
                Object roles = claims.get("roles");
                if (!(roles instanceof List<?> list) || list.stream().noneMatch(r -> "ADMIN".equals(String.valueOf(r)))) {
                    return forbidden(exchange);
                }
            }
            exchange = exchange.mutate().request(exchange.getRequest().mutate()
                    .headers(headers -> headers.set("X-Service-Token", internalServiceToken)).build()).build();
            return chain.filter(exchange);
        } catch (Exception ex) {
            return unauthorized(exchange);
        }
    }

    private boolean requiresAdmin(HttpMethod method, String path) {
        if (path.startsWith("/products") || path.startsWith("/categories") || (path.startsWith("/inventory") && method != HttpMethod.GET)) {
            return method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH || method == HttpMethod.DELETE;
        }
        return false;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) { exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED); return exchange.getResponse().setComplete(); }
    private Mono<Void> forbidden(ServerWebExchange exchange) { exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN); return exchange.getResponse().setComplete(); }
    @Override public int getOrder() { return -100; }
}
