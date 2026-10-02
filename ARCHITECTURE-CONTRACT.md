# Architecture Contract

## Deployment
- Kubernetes is the only orchestration/deployment platform.
- Dockerfiles remain because Kubernetes deploys container images.
- Docker Compose is intentionally not part of this project.
- Eureka is intentionally not part of this project.

## Service discovery
- Kubernetes Service DNS is the only internal service-discovery mechanism.
- Examples: `http://product-service:8081`, `http://order-service:8083`.
- API Gateway routes use direct Kubernetes Service URLs; no `lb://` routes.

## External traffic
Internet -> Kubernetes Ingress -> API Gateway -> business services.

## Order entry
- `POST /orders` is the canonical Order Service create endpoint.
- It supports both direct Buy Now requests and requests forwarded from Cart Checkout.
- A persistent Cart entry is not required by Order Service.
- `POST /cart/checkout` converts the current cart into the canonical `/orders` request.

## Payment webhook
- `POST /payments/webhook` is the only unauthenticated Gateway route.
- Payment Service verifies Stripe-Signature.
- Application JWT is not used for Stripe server-to-server webhooks.

## Payment redirect
- Kubernetes local default: `http://ecommerce.local/payment/success`.
- Production must override `PAYMENT_SUCCESS_URL` with the public frontend URL.

## Reliability
- Inventory reservation uses database locking/transactions.
- Order idempotency uses `Idempotency-Key`.
- Order state changes publish through the transactional outbox.
- Kafka consumers must be idempotent and support retry/DLT where configured.
- Saga compensation releases inventory when payment/order processing fails.
