# Phase 1 — upgrades 1–7 implemented

1. **Payment DB** — PaymentService now persists payment state in its own PostgreSQL database.
2. **Verified Stripe webhook** — `/payments/webhook` verifies `Stripe-Signature` before processing.
3. **Payment idempotency** — `Idempotency-Key` is required for payment initiation and persisted with a unique DB constraint.
4. **Order idempotency** — `Idempotency-Key` is required on `POST /orders` and maps to the created order.
5. **Inventory Reservation ID** — reservations are persisted with a unique reservation ID and linked to order/product.
6. **Reservation expiration** — a scheduled job releases RESERVED stock after the configured TTL (default 15 minutes).
7. **Kafka consumer idempotency** — OrderService persists consumed payment event IDs and ignores duplicate deliveries.

Also retained: pessimistic row locking for inventory reservation/release, Saga compensation, Kafka DLT/retry infrastructure, Redis caching, and transactional outbox for orders.

Full Maven compilation/runtime verification was not possible in this environment because external dependency downloads are unavailable.
