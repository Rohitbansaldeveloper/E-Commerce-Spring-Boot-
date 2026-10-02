package com.example.cartservicefeb25.controllers;

import com.example.cartservicefeb25.dtos.AddCartItemRequest;
import com.example.cartservicefeb25.dtos.CartResponse;
import com.example.cartservicefeb25.dtos.CheckoutResponse;
import com.example.cartservicefeb25.dtos.UpdateCartItemRequest;
import com.example.cartservicefeb25.services.CartService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/cart")
public class CartController {
    private final CartService service;

    public CartController(CartService service) {
        this.service = service;
    }

    @PostMapping("/items")
    public ResponseEntity<Void> addItem(@RequestHeader("X-User-Id") Long userId,
                                        @Valid @RequestBody AddCartItemRequest request) {
        service.addItem(userId, request.getProductId(), request.getQuantity());
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public CartResponse getCart(@RequestHeader("X-User-Id") Long userId) {
        return service.getCart(userId);
    }

    @PutMapping("/items/{productId}")
    public ResponseEntity<Void> updateItem(@RequestHeader("X-User-Id") Long userId,
                                           @PathVariable @Positive(message="productId must be positive") Long productId,
                                           @Valid @RequestBody UpdateCartItemRequest request) {
        service.updateItem(userId, productId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<Void> removeItem(@RequestHeader("X-User-Id") Long userId,
                                           @PathVariable @Positive(message="productId must be positive") Long productId) {
        service.removeItem(userId, productId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(@RequestHeader("X-User-Id") Long userId) {
        service.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/checkout")
    public CheckoutResponse checkout(@RequestHeader("X-User-Id") Long userId,
                                     @RequestHeader("Idempotency-Key") @NotBlank(message="Idempotency-Key is required") String idempotencyKey) {
        return service.checkout(userId, idempotencyKey);
    }
}
