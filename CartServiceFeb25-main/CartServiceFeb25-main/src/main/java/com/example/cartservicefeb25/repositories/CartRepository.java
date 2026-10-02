package com.example.cartservicefeb25.repositories;
import com.example.cartservicefeb25.models.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface CartRepository extends JpaRepository<Cart,Long> { Optional<Cart> findByUserId(Long userId); }
