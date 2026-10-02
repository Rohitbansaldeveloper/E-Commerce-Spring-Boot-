package com.example.cartservicefeb25.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name="cart_items", uniqueConstraints=@UniqueConstraint(name="uk_cart_product", columnNames={"cart_id","product_id"}), indexes={@Index(name="idx_cart_items_cart_id", columnList="cart_id"),@Index(name="idx_cart_items_product_id", columnList="product_id")})
@Getter @Setter
public class CartItem {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="cart_id", nullable=false, foreignKey=@ForeignKey(name="fk_cart_item_cart"))
    private Cart cart;
    @Column(name="product_id", nullable=false) private Long productId;
    @Column(nullable=false) private int quantity;
}
