package com.example.orderservicefeb25.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name="order_idempotency", uniqueConstraints=@UniqueConstraint(name="uk_order_idempotency", columnNames="idempotency_key"))
@Getter @Setter
public class OrderIdempotency {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(name="idempotency_key", nullable=false, length=100)
    private String idempotencyKey;
    @Column(nullable=true)
    private Long orderId;
    @Column(nullable=false)
    private Instant createdAt;
}
