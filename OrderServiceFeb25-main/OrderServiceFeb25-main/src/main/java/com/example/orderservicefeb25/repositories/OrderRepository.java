package com.example.orderservicefeb25.repositories;
import com.example.orderservicefeb25.models.Order; import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface OrderRepository extends JpaRepository<Order,Long> { List<Order> findByUserIdOrderByCreatedAtDesc(Long userId); Optional<Order> findByIdAndUserId(Long id, Long userId); }
