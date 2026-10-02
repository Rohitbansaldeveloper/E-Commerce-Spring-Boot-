package com.example.cartservicefeb25.dtos;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCartItemRequest {
    @Min(1)
    private int quantity;
}
