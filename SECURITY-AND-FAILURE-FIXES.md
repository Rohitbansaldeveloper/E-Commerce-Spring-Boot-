> **Current deployment note:** This repository is Kubernetes-only. Docker Compose and Eureka are not used. Kubernetes Service DNS is the internal discovery mechanism.

# Failure fixes implemented

1. Kafka Docker networking: internal clients use `kafka:29092`; host clients use `kafka:29092`. Kubernetes uses `kafka:29092`.
2. Startup: Kubernetes readiness/liveness probes and bounded client retries handle PostgreSQL/Redis/Kafka and Kubernetes Service DNS/startup races.
3. JWT: Gateway and User Service read the same `JWT_SECRET`/issuer from environment. Secret must be >= 32 characters. The Gateway overwrites `X-User-Id` from the verified JWT subject; clients cannot choose another user id.
4. Kubernetes Service DNS: Order Service calls stable Kubernetes Service names directly; bounded retries handle transient readiness gaps.
5. Inventory: pessimistic row locking protects reservations.
6. Outbox: Order and Payment persist events in their local DB transaction before Kafka publication.
7. Idempotency: Order uses an atomic PostgreSQL `INSERT IGNORE` claim; payment/webhook and processed-event tables have unique keys.
8. Product cache: explicit eviction remains on create/update/delete; cached values are DTOs serialized as JSON, not JPA entities/proxies.
9. Reservation expiry: 15-minute expiry job releases abandoned reservations; payment success now confirms reservations before marking the order PAID.
10. Kafka poison messages: retry + same-partition DLT configuration is retained.
11. Stripe: raw request body + `Stripe-Signature` are verified before processing; provider event IDs are deduplicated.
12. User ownership: Gateway derives `X-User-Id` from JWT and Order repository queries use `(id,userId)` for ownership. Business services are not exposed through the Ingress; only the API Gateway is externally reachable.

## Operational caveat

Kafka/Outbox is at-least-once. Consumers remain idempotent. A successful Stripe payment for an already expired reservation is intentionally not allowed to silently turn the order into PAID; it must be reconciled/refunded according to business policy.
