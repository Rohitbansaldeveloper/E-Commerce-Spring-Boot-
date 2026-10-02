package com.example.cartservicefeb25.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddCartItemRequest {
    @NotNull
    @Positive(message = "productId must be positive")
    private Long productId;

    @Min(1)
    private int quantity = 1;
}
