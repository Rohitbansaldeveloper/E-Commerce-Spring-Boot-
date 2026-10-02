package com.example.orderservicefeb25.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "order_items", indexes = {
        @Index(name = "idx_order_items_order_id", columnList = "order_id"),
        @Index(name = "idx_order_items_product_id", columnList = "product_id")
})
@Getter @Setter
public class OrderItem {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(name="product_id", nullable=false)
    private Long productId;

    @Column(name="product_name", nullable=false, length=255)
    private String productName;

    @Column(nullable=false)
    private int quantity;

    @Column(name="reservation_id", length=80)
    private String reservationId;

    @Column(nullable=false, precision=19, scale=2)
    private java.math.BigDecimal unitPrice;

    @Column(nullable=false, precision=19, scale=2)
    private java.math.BigDecimal subtotal;

    @JsonIgnore
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="order_id", nullable=false,
            foreignKey=@ForeignKey(name="fk_order_item_order"))
    private Order order;
}
