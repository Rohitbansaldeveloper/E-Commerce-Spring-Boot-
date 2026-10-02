package com.example.orderservicefeb25.services;

import com.example.orderservicefeb25.dtos.CreateOrderRequest;
import com.example.orderservicefeb25.dtos.ProductResponse;
import com.example.orderservicefeb25.models.Order;
import com.example.orderservicefeb25.models.OrderItem;
import com.example.orderservicefeb25.models.OrderStatus;
import com.example.orderservicefeb25.outbox.OutboxEvent;
import com.example.orderservicefeb25.outbox.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;

@Service
public class OrderService {
    private final com.example.orderservicefeb25.repositories.OrderRepository repo;
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;
    private final OutboxEventRepository outboxRepository;
    private final com.example.orderservicefeb25.repositories.OrderIdempotencyRepository idempotencyRepository;
    private final String productServiceUrl;
    private final String inventoryServiceUrl;
    private final String paymentServiceUrl;

    public OrderService(com.example.orderservicefeb25.repositories.OrderRepository repo,
                        RestTemplate restTemplate,
                        ObjectMapper mapper,
                        OutboxEventRepository outboxRepository, com.example.orderservicefeb25.repositories.OrderIdempotencyRepository idempotencyRepository,
                        @Value("${product.service.url}") String productServiceUrl,
                        @Value("${inventory.service.url}") String inventoryServiceUrl,
                        @Value("${payment.service.url}") String paymentServiceUrl) {
        this.repo = repo;
        this.restTemplate = restTemplate;
        this.mapper = mapper;
        this.outboxRepository = outboxRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.productServiceUrl = productServiceUrl;
        this.inventoryServiceUrl = inventoryServiceUrl;
        this.paymentServiceUrl = paymentServiceUrl;
    }

    /**
     * Saga step 1: validate products and reserve inventory.
     * The order is persisted as PAYMENT_PENDING because inventory is already reserved
     * and payment is the next saga step.
     */
    @Transactional
    public Order create(Long userId, CreateOrderRequest req, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) throw new IllegalArgumentException("Idempotency-Key header is required");
        // Atomic DB claim prevents two concurrent requests with the same key from both reserving stock.
        if (idempotencyRepository.claimKey(idempotencyKey) == 0) {
            var existing = idempotencyRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> new IllegalStateException("Idempotency key is currently being processed"));
            if (existing.getOrderId() == null) throw new IllegalStateException("Idempotency key is currently being processed");
            return repo.findById(existing.getOrderId()).orElseThrow();
        }
        if (userId == null || req == null || req.getItems() == null || req.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }

        List<String> reservationIds = new ArrayList<>();
        try {
            Order order = new Order();
            order.setUserId(userId);
            order.setStatus(OrderStatus.PAYMENT_PENDING);
            order.setCreatedAt(Instant.now());
            order.setUpdatedAt(Instant.now());
            order = repo.save(order);

            java.math.BigDecimal total = java.math.BigDecimal.ZERO;
            for (CreateOrderRequest.Item item : req.getItems()) {
                if (item.getProductId() == null || item.getQuantity() <= 0) {
                    throw new IllegalArgumentException("Each item needs a valid productId and positive quantity");
                }

                ProductResponse product = withRetry(() -> restTemplate.getForObject(
                        productServiceUrl + "/products/" + item.getProductId(),
                        ProductResponse.class));

                if (product == null) {
                    throw new IllegalArgumentException("Product not found: " + item.getProductId());
                }

                String reservationId = withRetry(() -> restTemplate.postForObject(
                        inventoryServiceUrl + "/inventory/orders/" + order.getId() + "/reserve/" + item.getProductId()
                                + "?quantity=" + item.getQuantity(), null, String.class));
                reservationIds.add(reservationId);

                OrderItem orderItem = new OrderItem();
                orderItem.setProductId(product.getId());
                orderItem.setProductName(product.getTitle());
                orderItem.setQuantity(item.getQuantity());
                orderItem.setUnitPrice(java.math.BigDecimal.valueOf(product.getPrice()));
                orderItem.setSubtotal(java.math.BigDecimal.valueOf(product.getPrice()).multiply(java.math.BigDecimal.valueOf(item.getQuantity())));
                orderItem.setReservationId(reservationId);
                order.addItem(orderItem);
                total = total.add(java.math.BigDecimal.valueOf(product.getPrice()).multiply(java.math.BigDecimal.valueOf(item.getQuantity())));
            }

            order.setTotalAmount(total);
            Order saved = repo.save(order);
            var idem = idempotencyRepository.findByIdempotencyKey(idempotencyKey).orElseThrow();
            idem.setOrderId(saved.getId());
            idempotencyRepository.save(idem);
            saveOutboxEvent("OrderCreated", saved);
            saveOutboxEvent("OrderPaymentPending", saved);
            return saved;
        } catch (RuntimeException ex) {
            // Compensation for any reservation made before a later step failed.
            releaseReservations(reservationIds);
            throw ex;
        }
    }

    /** Saga step 2: start payment through Payment Service. */
    public String startPayment(Long orderId, Long userId, String idempotencyKey) {
        Order order = repo.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cancelled order cannot be paid");
        }
        if (order.getStatus() == OrderStatus.PAID) {
            throw new IllegalStateException("Order is already paid");
        }
        if (order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            throw new IllegalStateException("Payment can only be initiated for an order in PAYMENT_PENDING state");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required");
        }

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("Idempotency-Key", idempotencyKey);
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return withRetry(() -> restTemplate.postForObject(
                paymentServiceUrl + "/payments/initiate/" + orderId,
                new org.springframework.http.HttpEntity<>(java.util.Map.of("amount", order.getTotalAmount()), headers), String.class));
    }

    public Optional<Order> get(Long id, Long userId) {
        return repo.findByIdAndUserId(id, userId);
    }

    public List<Order> getForUser(Long userId) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /** Saga success transition: PaymentService published PaymentSucceeded. */
    @Transactional
    public void markPaid(Long id) {
        Order order = repo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));
        if (order.getStatus() == OrderStatus.PAID) return;
        if (order.getStatus() == OrderStatus.CANCELLED) {
            // A late success event must not resurrect a cancelled order.
            return;
        }
        // Confirm reservations before marking the order PAID. If a reservation expired,
        // this throws and Kafka retries the event instead of creating a paid-but-unstocked order.
        for (OrderItem item : order.getItems()) {
            if (item.getReservationId() != null) {
                withRetry(() -> { restTemplate.postForEntity(
                        inventoryServiceUrl + "/inventory/reservations/" + item.getReservationId() + "/confirm",
                        null, Void.class); return null; });
            }
        }
        order.setStatus(OrderStatus.PAID);
        order.setPaymentStatus("SUCCESS");
        order.setUpdatedAt(Instant.now());
        Order saved = repo.save(order);
        saveOutboxEvent("OrderPaid", saved);
    }

    /** Saga failure transition + compensation: release every reserved item. */
    @Transactional
    public void cancelAndCompensate(Long id) {
        Order order = repo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));

        if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.CANCELLED) {
            return;
        }

        // Compensation is executed before the final CANCELLED transition.
        for (OrderItem item : order.getItems()) {
            if (item.getReservationId() != null) {
                restTemplate.postForEntity(inventoryServiceUrl + "/inventory/reservations/" + item.getReservationId() + "/release", null, Void.class);
            } else {
                restTemplate.postForEntity(inventoryServiceUrl + "/inventory/" + item.getProductId() + "/release?quantity=" + item.getQuantity(), null, Void.class);
            }
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus("FAILED");
        order.setUpdatedAt(Instant.now());
        Order saved = repo.save(order);
        saveOutboxEvent("OrderCancelled", saved);
    }

    private void releaseReservations(List<String> reservationIds) {
        RuntimeException firstFailure = null;
        for (String reservationId : reservationIds) {
            try {
                withRetry(() -> {
                    restTemplate.postForEntity(
                            inventoryServiceUrl + "/inventory/reservations/" + reservationId + "/release",
                            null, Void.class);
                    return null;
                });
            } catch (RuntimeException ex) {
                if (firstFailure == null) firstFailure = ex;
            }
        }
        if (firstFailure != null) {
            throw new IllegalStateException(
                    "Order creation failed and inventory compensation could not be completed. " +
                    "Reservation expiry is the final safety net.", firstFailure);
        }
    }

    private <T> T withRetry(java.util.function.Supplier<T> action) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try { return action.get(); }
            catch (RuntimeException ex) {
                last = ex;
                if (attempt < 3) {
                    try { Thread.sleep(500L * attempt); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); throw ex; }
                }
            }
        }
        throw last;
    }

    private void saveOutboxEvent(String type, Order order) {
        try {
            OutboxEvent outbox = new OutboxEvent();
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("eventId", outbox.getEventId());
            event.put("eventType", type);
            event.put("eventVersion", 1);
            event.put("source", "OrderServiceFeb25");
            event.put("correlationId", "order-" + order.getId());
            event.put("occurredAt", Instant.now().toString());
            event.put("orderId", order.getId());
            event.put("userId", order.getUserId());
            event.put("totalAmount", order.getTotalAmount());
            event.put("status", order.getStatus().name());

            outbox.setTopic("order-events");
            outbox.setEventKey(String.valueOf(order.getId()));
            outbox.setPayload(mapper.writeValueAsString(event));
            outboxRepository.save(outbox);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to create order Kafka event", ex);
        }
    }
}
