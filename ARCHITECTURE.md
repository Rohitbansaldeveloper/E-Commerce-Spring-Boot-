> **Current deployment note:** This repository is Kubernetes-only. Docker Compose and Eureka are not used. Kubernetes Service DNS is the internal discovery mechanism.

# E-Commerce Microservices Architecture

This project follows the requested architecture: the client enters through the API Gateway, and the Gateway directly exposes **User, Product, Cart, and Order services**. Inventory, Payment, and Email are internal services.

## 1. Client-facing architecture

```text
                                  CLIENT
                                    |
                                    v
                             +-------------+
                             | API GATEWAY |
                             |    :8080    |
                             +-------------+
                                    |
                  +-----------------+-----------------+-----------------+
                  |                 |                 |                 |
                  v                 v                 v                 v
            USER SERVICE     PRODUCT SERVICE     CART SERVICE     ORDER SERVICE
               :7070              :8081              :8085              :8083
```

### Gateway routes

| Client path | Gateway destination | Kubernetes Service DNS service |
|---|---|---|
| `/users/**` | User Service | `UserServiceFeb25` |
| `/products/**` | Product Service | `product-service` |
| `/cart/**` | Cart Service | `CartServiceFeb25` |
| `/orders/**` | Order Service | `order-service` |

Inventory, Payment, and Email do not have Gateway routes.

## 2. Internal service connections

```text
CART SERVICE
     |
     | checkout
     v
ORDER SERVICE
     |
     +----------> PRODUCT SERVICE
     |               |
     |               +--> Product DB / Redis
     |
     +----------> INVENTORY SERVICE
     |               |
     |               +--> Inventory DB / Redis
     |
     +----------> PAYMENT SERVICE
                     |
                     +--> Stripe
```

These internal calls use Kubernetes Service DNS service names and load-balanced `RestTemplate` where implemented.

## 3. Checkout flow

```text
Client
  |
  | POST /cart/checkout
  v
API Gateway
  |
  v
Cart Service
  |
  | POST http://order-service/orders
  v
Order Service
  |
  +--> Product Service: GET /products/{productId}
  |        |
  |        +--> Product DB / Redis
  |
  +--> Inventory Service: reserve stock
  |        |
  |        +--> Inventory DB / Redis
  |
  +--> Payment Service: payment initiation
           |
           +--> Stripe
```

The Cart Service clears the cart only after the Order Service successfully creates the order.

## 4. Buy Now flow

Buy Now does not require an existing cart item.

```text
Client
  |
  | POST /orders/buy-now
  v
API Gateway
  |
  v
Order Service
  |
  +--> Product Service: validate product + obtain current price
  |
  +--> Inventory Service: reserve stock
  |
  +--> Payment Service: start payment
```

Order Service remains responsible for creating and owning the order. The Buy Now endpoint uses the same order creation/Saga logic as normal order creation.

## 5. Why Kubernetes Service DNS is used

The services do not hard-code container IP addresses. Internal calls use logical service names such as:

```text
http://order-service/orders
http://product-service/products/{id}
http://inventory-service/inventory/...
http://PaymentServiceFeb25/payments/...
```

Kubernetes Service DNS resolves these service names to registered service instances.

## 6. Async communication

Kafka is used for event-driven work such as:

```text
User / Order / Payment
        |
        v
      Kafka
        |
        v
  Email Service
        |
        v
      SMTP
```

Payment events are also consumed by Order Service to complete the payment part of the Saga.

## 7. Data ownership

- User Service -> User DB
- Product Service -> Product DB + Redis cache
- Cart Service -> Redis cart data
- Order Service -> Order DB
- Inventory Service -> Inventory DB + Redis cache
- Payment Service -> Payment DB + Stripe
- Email Service -> no business database required

Each business service owns its own data; services communicate through REST for synchronous operations and Kafka for asynchronous events.
