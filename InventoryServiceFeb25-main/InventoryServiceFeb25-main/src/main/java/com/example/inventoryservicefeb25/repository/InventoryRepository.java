package com.example.inventoryservicefeb25.repository;

import com.example.inventoryservicefeb25.model.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByProductId(Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.productId = :productId")
    Optional<Inventory> findByProductIdForUpdate(@Param("productId") Long productId);

    @Modifying
    @Query("""
        update Inventory i
           set i.availableQuantity = i.availableQuantity + :quantity
         where i.productId = :productId
    """)
    int release(@Param("productId") Long productId, @Param("quantity") int quantity);
}
