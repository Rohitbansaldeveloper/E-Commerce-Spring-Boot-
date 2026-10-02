package com.example.inventoryservicefeb25.repository;

import com.example.inventoryservicefeb25.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation,Long> {
    Optional<InventoryReservation> findByReservationId(String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from InventoryReservation r where r.reservationId = :id")
    Optional<InventoryReservation> findByReservationIdForUpdate(@Param("id") String id);
    Optional<InventoryReservation> findByOrderIdAndProductId(Long orderId,Long productId);
    List<InventoryReservation> findByStatusAndExpiresAtBefore(ReservationStatus status,Instant now);
    List<InventoryReservation> findByOrderId(Long orderId);
}
