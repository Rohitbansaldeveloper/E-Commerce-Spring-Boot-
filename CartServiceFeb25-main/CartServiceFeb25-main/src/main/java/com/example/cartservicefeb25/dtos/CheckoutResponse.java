package com.example.cartservicefeb25.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CheckoutResponse {
    private String message;
    private Object order;
}
