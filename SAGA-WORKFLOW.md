# Saga Workflow

The project now uses an Order Service Saga Orchestrator. Each microservice keeps its own database, so the workflow is coordinated with local transactions, REST calls, Kafka events, and compensation rather than one distributed transaction.

## Flow

1. `POST /orders` validates products and reserves inventory. Inventory uses a database `PESSIMISTIC_WRITE` row lock. The order is saved as `PAYMENT_PENDING`.
2. `POST /orders/{id}/payment` sends the Order total to Payment Service, which creates the payment link.
3. Payment Service publishes `PaymentInitiated` to `payment-events`.
4. A verified payment-provider webhook should publish either `PaymentSucceeded` or `PaymentFailed`. The included `/payments/saga/success/{id}` and `/payments/saga/failure/{id}` endpoints are demo/test callbacks.
5. Order Service consumes the event. Success changes the order to `PAID`. Failure triggers compensation: every reserved item is released and the order becomes `CANCELLED`.

## Failure example

```text
Create Order
    |
    v
Reserve Inventory -- PESSIMISTIC_WRITE
    |
    v
PAYMENT_PENDING
    |
    v
Payment Service
    |
    +---- success ----> PaymentSucceeded -> Order PAID
    |
    +---- failure ----> PaymentFailed -> Release Inventory -> Order CANCELLED
```

This is a Saga because each service commits independently and a later failure is handled by a compensating action instead of a distributed ACID transaction.

## Important production note

Kafka delivery is at-least-once. Compensation should therefore be made idempotent and backed by a compensation outbox/retry mechanism in a production implementation. The current implementation demonstrates the Saga flow and keeps late success events from resurrecting a cancelled order.
