package com.example.paymentservicefeb25.models;
import jakarta.persistence.*; import lombok.Getter; import lombok.Setter; import java.time.Instant;
@Entity @Table(name="payments", uniqueConstraints=@UniqueConstraint(name="uk_payment_order",columnNames="order_id")) @Getter @Setter
public class Payment { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="order_id",nullable=false) private Long orderId; @Column(nullable=false, precision=19, scale=2) private java.math.BigDecimal amount; @Column(name="provider_payment_id") private String providerPaymentId; @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private PaymentStatus status; @Column(nullable=false) private Instant createdAt; @Column(nullable=false) private Instant updatedAt; }
