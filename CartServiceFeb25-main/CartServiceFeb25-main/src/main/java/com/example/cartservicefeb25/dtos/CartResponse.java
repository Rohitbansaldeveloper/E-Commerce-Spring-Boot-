package com.example.cartservicefeb25.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CartResponse {
    private Long userId;
    private List<CartItemResponse> items;
}
