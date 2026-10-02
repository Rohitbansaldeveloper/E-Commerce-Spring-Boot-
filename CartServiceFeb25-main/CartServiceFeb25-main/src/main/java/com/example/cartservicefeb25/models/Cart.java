package com.example.cartservicefeb25.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="carts", indexes={@Index(name="idx_carts_user_id", columnList="user_id")})
@Getter @Setter
public class Cart {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(name="user_id", nullable=false, unique=true)
    private Long userId;
    @Column(nullable=false, length=20)
    private String status = "ACTIVE";
    @Column(nullable=false) private Instant createdAt;
    @Column(nullable=false) private Instant updatedAt;
    @OneToMany(mappedBy="cart", cascade=CascadeType.ALL, orphanRemoval=true)
    private List<CartItem> items = new ArrayList<>();
    public void addItem(CartItem item){ items.add(item); item.setCart(this); }
}
