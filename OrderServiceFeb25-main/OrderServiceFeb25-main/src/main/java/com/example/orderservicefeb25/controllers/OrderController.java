package com.example.orderservicefeb25.controllers;

import com.example.orderservicefeb25.dtos.CreateOrderRequest;
import com.example.orderservicefeb25.models.Order;
import com.example.orderservicefeb25.services.OrderService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;

import java.util.*;

@RestController
@Validated
@RequestMapping("/orders")
public class OrderController {
    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    /**
     * Creates an order directly from the supplied product list.
     * The request does not require a persistent Cart entry, so this endpoint
     * supports both Cart Checkout (Cart Service forwards its items here) and
     * direct Buy Now purchases.
     */
    @PostMapping
    public ResponseEntity<Order> create(@RequestHeader("X-User-Id") Long userId,
                                        @RequestHeader("Idempotency-Key") @NotBlank(message = "Idempotency-Key is required") String idempotencyKey,
                                        @Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(userId, request, idempotencyKey));
    }

    @PostMapping("/{id}/payment")
    public ResponseEntity<String> startPayment(@PathVariable @Positive(message = "Order id must be positive") Long id,
                                               @RequestHeader("X-User-Id") Long userId,
                                               @RequestHeader("Idempotency-Key") @NotBlank(message = "Idempotency-Key is required") String idempotencyKey) {
        return ResponseEntity.ok(service.startPayment(id, userId, idempotencyKey));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> get(@PathVariable Long id,
                                     @RequestHeader("X-User-Id") Long userId) {
        return service.get(id, userId).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/my")
    public List<Order> my(@RequestHeader("X-User-Id") Long userId) {
        return service.getForUser(userId);
    }

}
