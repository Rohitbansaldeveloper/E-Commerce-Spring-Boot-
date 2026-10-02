package com.scaler.productservicefeb25.models;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(name = "products")
@Table(name = "products",
       indexes = {
           @Index(name = "idx_products_title", columnList = "title"),
           @Index(name = "idx_products_category_id", columnList = "category_id"),
           @Index(name = "idx_products_price", columnList = "price")
       })
public class Product extends BaseModel {
    @Column(nullable = false, length = 200)
    @NotBlank(message = "Product title is required")
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(length = 1000)
    private String imageUrl;

    @Column(nullable = false)
    @PositiveOrZero(message = "Product price cannot be negative")
    private double price;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_product_category"))
    @NotNull(message = "Product category is required")
    @Valid
    private Category category;
}
