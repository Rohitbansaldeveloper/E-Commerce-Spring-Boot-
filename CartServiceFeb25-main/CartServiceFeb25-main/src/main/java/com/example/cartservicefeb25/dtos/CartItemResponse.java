package com.example.cartservicefeb25.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CartItemResponse {
    private Long productId;
    private int quantity;
}
