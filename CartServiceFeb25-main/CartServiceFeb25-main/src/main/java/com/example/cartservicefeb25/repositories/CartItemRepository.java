package com.example.cartservicefeb25.repositories;
import com.example.cartservicefeb25.models.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface CartItemRepository extends JpaRepository<CartItem,Long> { Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId); }
