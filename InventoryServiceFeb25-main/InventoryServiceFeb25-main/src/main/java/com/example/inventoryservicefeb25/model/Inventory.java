package com.example.inventoryservicefeb25.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "inventory",
       uniqueConstraints = @UniqueConstraint(name = "uk_inventory_product", columnNames = "product_id"),
       indexes = @Index(name = "idx_inventory_product_id", columnList = "product_id"))
@Getter @Setter
public class Inventory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private int availableQuantity;

    @Version
    private long version;
}
