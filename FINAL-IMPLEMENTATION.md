# Final implementation status

This archive is the consolidated implementation of the planned e-commerce microservices architecture.

## Client-facing services through API Gateway
- User Service `/users/**`
- Product Service `/products/**`
- Cart Service `/cart/**`
- Order Service `/orders/**`

Inventory, Payment and Email remain internal services. Kubernetes Service DNS is service discovery.

## Implemented
- JWT issuance in User Service and JWT validation/role authorization at API Gateway.
- Redis JWT deny-list for logout.
- OTP registration verification and password-reset flow with hashed OTPs, expiry, attempt limit and resend cooldown.
- Product filtering with title/category/price/pagination/sorting using JPA Specifications.
- Bean Validation + consistent validation error handling + service-level Swagger/OpenAPI.
- Kafka explicit topics, reliable producer settings, structured events, transactional outbox in User/Order, consumer retries and DLT monitoring.
- Cart Service with PostgreSQL as source of truth (`carts`, `cart_items`) and Redis retained for cache/infrastructure.
- Direct Buy Now and Cart Checkout both create orders through Order Service.
- Order Service with PostgreSQL (`orders`, `order_items`, idempotency, processed events, outbox).
- Inventory Service with PostgreSQL, reservation records, pessimistic row locking, expiry and Redis stock cache.
- Saga: order -> inventory reservation -> payment initiation -> PaymentSucceeded => PAID; PaymentFailed => release reservations => CANCELLED.
- Payment Service persistence and idempotency; Stripe payment link now uses the actual Order total instead of a fixed global amount.
- Kubernetes deployment with separate PostgreSQL instances for User/Product/Cart/Order/Inventory/Payment plus Redis, Kafka, Zookeeper, Kubernetes Service DNS and all services.

## Important design rule
No service directly queries another service's database. Cross-service relationships use IDs and REST/Kafka.

## Kafka reliability upgrade

Added consumer-side retry/DLT handling to Order Service, Kafka consumer reliability settings, business-keyed events, correlation IDs, event-version validation, topic retention policies, and documentation of at-least-once/idempotent processing.
