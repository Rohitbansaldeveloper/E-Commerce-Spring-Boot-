# Failure scenarios and fixes

## 1 Kafka host vs container
Kubernetes uses the Kafka Service DNS name `kafka:29092`; clients inside the cluster do not depend on host-published Kafka ports.

## 2 Infrastructure startup
Kubernetes uses readiness/liveness probes for PostgreSQL/Redis/Kafka and application pods. Application-to-application calls also have bounded retries because readiness is not the same as process start.

## 3 JWT mismatch
Gateway and User Service receive the same `JWT_SECRET` and `JWT_ISSUER`. The secret is required and must be at least 32 characters.

## 4 Kubernetes service-DNS startup race
Order Service uses normal `RestTemplate` with Kubernetes Service DNS URLs, and bounded retries handle transient service-readiness failures.

## 5 Overselling
Inventory reservation uses `PESSIMISTIC_WRITE` (`SELECT ... FOR UPDATE`) and a unique `(order_id, product_id)` reservation constraint.

## 6 Lost Kafka event
Order and Payment persist an Outbox row in the same local DB transaction. Publishers retry pending rows.

## 7 Duplicate events
Processed-event tables have unique event IDs. Order transitions are idempotent: PAID is a no-op and CANCELLED cannot be resurrected by a late success event.

## 8 Duplicate checkout/payment requests
Order and Payment use atomic PostgreSQL `INSERT IGNORE` idempotency-key claims, then persist the resulting order/payment ID. Reservation rows also have a unique `(order_id, product_id)`.

## 9 Stale product cache
Product writes explicitly evict `product:<id>`. TTL remains a backstop. Cache values are JSON DTOs rather than JPA entities.

## 10 Abandoned checkout
Inventory reservations expire after 15 minutes and a scheduled job releases them. Payment success confirms the reservation before the order is marked PAID.

## 11 Poison Kafka message
Spring Kafka retries failed records and sends exhausted records to `<topic>.DLT` on the same partition.

## 12 Stripe webhook spoofing
The raw body is verified with `Stripe-Signature` and the configured endpoint secret before the event is trusted. Provider event IDs are deduplicated.

## 13 User impersonation
The Gateway derives `X-User-Id` from the verified JWT subject, overwriting any client-supplied header. Order reads use `findByIdAndUserId`. Internal business services are not exposed through the Ingress; only the API Gateway is externally reachable.
