# Cart Service

Cart Service runs on port 8085 and uses Redis as the cart store. It uses Kubernetes Service DNS for internal communication and is exposed to clients through API Gateway under `/cart/**`.

## APIs

- `POST /cart/items` — add/increment an item
- `GET /cart` — get current user's cart
- `PUT /cart/items/{productId}` — set exact quantity
- `DELETE /cart/items/{productId}` — remove one item
- `DELETE /cart` — clear cart
- `POST /cart/checkout` — create an Order from the cart

All cart operations require the gateway-provided `X-User-Id` header. Checkout also requires `Idempotency-Key`.

## Design

- Redis hash key: `cart:<userId>`
- Hash field: `productId`
- Hash value: quantity
- Product price is never trusted from the cart.
- Add/update validates the product through Product Service.
- Checkout sends only product IDs and quantities to Order Service.
- Order Service fetches current prices and performs inventory reservation/Saga processing.
- Cart is cleared only after Order Service returns success.

## Checkout flow

```text
Client
  -> API Gateway
  -> Cart Service
       -> Redis (read cart)
       -> Order Service (product IDs + quantities)
            -> Product Service (current prices)
            -> Inventory Service (reservation)
            -> PAYMENT_PENDING
  <- Order created
  -> Cart Service clears Redis cart
```
