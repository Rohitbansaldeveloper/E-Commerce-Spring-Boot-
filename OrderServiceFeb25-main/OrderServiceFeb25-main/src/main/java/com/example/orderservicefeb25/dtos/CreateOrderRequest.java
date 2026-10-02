package com.example.orderservicefeb25.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter @Setter
public class CreateOrderRequest {
    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<Item> items;

    @Getter @Setter
    public static class Item {
        @NotNull(message = "Product id is required")
        @Positive(message = "Product id must be positive")
        private Long productId;

        @Positive(message = "Quantity must be greater than 0")
        private int quantity;
    }
}
