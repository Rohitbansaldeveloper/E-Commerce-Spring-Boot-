> **Current deployment note:** This repository is Kubernetes-only. Docker Compose and Eureka are not used. Kubernetes Service DNS is the internal discovery mechanism.

# Spring Microservices - Fixed Version

## Services and default ports
- Kubernetes Service DNS Server: 8761
- API Gateway: 8080
- Product Service: 8081
- User Service: 7070
- Payment Service: 8082
- Email Service: 8181

## Infrastructure defaults
- PostgreSQL: localhost:3306
- Redis: localhost:6379
- Kafka: kafka:29092

## Environment variables
Set these before running the services where applicable:

```text
POSTGRES_HOST=localhost
POSTGRES_USER=...
POSTGRES_PASSWORD=...
KAFKA_BOOTSTRAP_SERVERS=kafka:29092
REDIS_HOST=localhost
REDIS_PORT=6379
MAIL_USERNAME=your-gmail-address
MAIL_PASSWORD=your-gmail-app-password
MAIL_FROM=your-gmail-address
STRIPE_KEY=your-stripe-secret-key
PAYMENT_CURRENCY=INR
PAYMENT_SUCCESS_URL=http://ecommerce.local/payment/success
```

Do not commit real passwords, SMTP app passwords, Stripe keys, or database credentials.

## Recommended startup order
1. Kubernetes Service DNS Server
2. Kafka / Redis / PostgreSQL
3. Product Service, User Service, Payment Service, Email Service
4. API Gateway

## Main Gateway routes
- `/products/**` -> Product Service
- `/users/**` -> User Service
- `/payments/**` -> Payment Service

## Direct Buy Now / Order Without Cart

The Order Service is independent of the Cart Service. A customer can now purchase a product directly without adding it to the cart.

### Buy Now API

`POST /orders/buy-now`

Headers:
- `Authorization: Bearer <JWT>`
- `Idempotency-Key: <unique-key>`

Body example:
```json
{
  "items": [
    {
      "productId": 10,
      "quantity": 1
    }
  ]
}
```

Flow:
`Client -> API Gateway -> Order Service -> Product Service -> Inventory Service -> Order DB -> Kafka`

The Cart Service is not involved in this flow.

`POST /orders` remains available as the generic direct order-creation API, while `/orders/buy-now` makes the direct-purchase intent explicit. Cart checkout continues to use the same Order Service creation logic, so there is no duplicated business logic.
