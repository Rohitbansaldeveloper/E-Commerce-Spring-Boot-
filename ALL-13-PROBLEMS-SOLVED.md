> **Current deployment note:** This repository is Kubernetes-only. Docker Compose and Eureka are not used. Kubernetes Service DNS is the internal discovery mechanism.

# All 13 Production/Interview Problems Solved

This version addresses the 13 failure scenarios from the project review.

1. **Kafka host/container networking** — dual listeners: `kafka:29092` for Docker/Kubernetes and `kafka:29092` for host clients.
2. **Startup readiness** — PostgreSQL/Redis/Kafka readiness probes, restart policy, and bounded application retries for transient service discovery/readiness races.
3. **JWT mismatch** — Gateway and User Service use the same environment-provided secret/issuer; secret is required and validated as at least 32 characters.
4. **Service discovery conflict** — removed Eureka/load-balanced discovery and switched Order Service to configurable Kubernetes Service DNS URLs with bounded retries.
5. **Inventory overselling** — pessimistic row locking plus unique order/product reservation constraint.
6. **DB/Kafka dual-write** — transactional Outbox in Order and Payment services.
7. **Duplicate Kafka events** — processed-event tables with unique event IDs and idempotent Order state transitions.
8. **Duplicate checkout/payment requests** — atomic PostgreSQL `INSERT IGNORE` idempotency-key claims and unique reservation constraints.
9. **Stale product cache** — explicit eviction on writes, TTL as a backstop, and JSON DTO caching instead of JPA entities.
10. **Abandoned reservations** — 15-minute reservation expiry/release job; successful payment confirms reservations before the order becomes PAID.
11. **Poison Kafka messages** — bounded retry with `DefaultErrorHandler` and same-partition `<topic>.DLT` recovery.
12. **Stripe webhook spoofing** — raw-body signature verification with `Stripe-Signature` and unique provider event-id deduplication.
13. **User impersonation** — Gateway derives `X-User-Id` from the verified JWT subject; Order queries enforce ownership with `(id,userId)` and internal business services are not host-published.

## Important distributed-systems behavior

- Kafka and Outbox are **at-least-once**; consumers remain idempotent.
- A payment that arrives after an inventory reservation expired is not silently converted into a paid order. It requires reconciliation/refund according to business policy.
- Docker/Kubernetes startup ordering is not treated as a correctness guarantee; application calls still use bounded retry where transient readiness races are possible.
