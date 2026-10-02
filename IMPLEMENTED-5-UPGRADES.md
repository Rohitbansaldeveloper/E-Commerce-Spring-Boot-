> **Current deployment note:** This repository is Kubernetes-only. Docker Compose and Eureka are not used. Kubernetes Service DNS is the internal discovery mechanism.

# Five upgrades implemented

1. Product Service now uses `SelfProductService` + PostgreSQL as the primary implementation; FakeStore remains as an optional legacy adapter.
2. JWT now carries roles, API Gateway enforces ADMIN for product/category writes, and logout uses a Redis deny-list until token expiry.
3. Added Order Service with PostgreSQL persistence, product-price validation through the Kubernetes Product Service DNS endpoint, order ownership, and `OrderCreated` Kafka events.
4. Added `PaymentInitiated` Kafka publication from Payment Service and Kafka consumers in Email Service for order/payment events.
5. Redis product cache now uses a 10-minute TTL and cache eviction on update/delete.

## Admin demo
Set `ADMIN_EMAIL` in `.env`, then sign up using that email. The resulting JWT contains `USER` and `ADMIN`. The API Gateway allows product/category write operations only for the ADMIN role.

## Order API
`POST /orders` with `Authorization: Bearer <JWT>` and body:
```json
{"items":[{"productId":1,"quantity":2}]}
```
The Gateway injects the authenticated user id as `X-User-Id`; clients should not set that header themselves.

## Main flow
Client -> API Gateway -> User/Product/Order/Payment services.
Order creation -> Product Service -> PostgreSQL; then `OrderCreated` -> Kafka.
Payment initiation -> Stripe -> `PaymentInitiated` -> Kafka.
User logout -> Redis JWT blacklist.


## Inventory Service upgrade
- Added inventory-service with its own PostgreSQL database (database-per-service).
- Atomic stock reservation uses a conditional SQL UPDATE (`available_quantity >= quantity`) to prevent overselling under concurrency.
- Added `@Version` optimistic locking metadata for inventory rows.
- Redis caches stock values with a 5-minute TTL and invalidates cache after stock changes.
- Order Service reserves inventory before committing an order and compensates reservations if a later step fails.
- Added Product/Order/OrderItem indexes and non-null/FK constraints.
- Gateway exposes `/inventory/**`; non-GET inventory operations require `ROLE_ADMIN`.
