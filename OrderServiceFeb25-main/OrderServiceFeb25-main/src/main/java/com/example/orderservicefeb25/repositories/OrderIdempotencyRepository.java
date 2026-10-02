package com.example.orderservicefeb25.repositories;

import com.example.orderservicefeb25.models.OrderIdempotency;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface OrderIdempotencyRepository extends JpaRepository<OrderIdempotency, Long> {
    Optional<OrderIdempotency> findByIdempotencyKey(String key);

    @Modifying
    @Query(value="INSERT IGNORE INTO order_idempotency (idempotency_key, order_id, created_at) VALUES (:key, NULL, CURRENT_TIMESTAMP)", nativeQuery=true)
    int claimKey(@Param("key") String key);
}
