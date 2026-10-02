# Kafka Reliability Improvements

This project uses Kafka as an event backbone with explicit reliability controls.

## Implemented

1. **Explicit topics** with 3 partitions and replication factor 1 for the local single-broker setup.
   - `sendEmail`
   - `order-events`
   - `payment-events`
   - matching `.DLT` topics
2. **Reliable producers**: `acks=all`, producer idempotence, bounded in-flight requests, retries, delivery/request timeouts and compression.
3. **Partition keys** use the business identity where available (for example `orderId`) so related events can remain ordered within a partition.
4. **Structured event envelope**: `eventId`, `eventType`, `eventVersion`, `source`, `occurredAt`, `correlationId` and event data.
5. **Event-version validation** on the Order payment-event consumer so unsupported schemas fail safely instead of being interpreted incorrectly.
6. **Transactional Outbox** in User Service and Order Service. Database changes and their outbox record commit together, and scheduled publishers retry unsent records until Kafka accepts them.
7. **Consumer retry + Dead Letter Topic**. Failed records are retried before being published to `<topic>.DLT`.
8. **DLT monitoring consumer** logs messages moved to a DLT for manual inspection/replay.
9. **Kafka readiness/health checks** in Kubernetes; dependent services use Kubernetes readiness and bounded application retries.
10. **Consumer tuning**: auto-commit disabled, record acknowledgement, `read_committed`, bounded poll size, heartbeat/session tuning and listener concurrency where configured.
11. **Topic retention**: normal event topics retain 7 days; DLT topics retain 30 days in the local setup to support investigation/replay.
12. **Consumer idempotency**: Order and Payment persist processed event IDs with unique constraints so Kafka redelivery does not repeat business effects.

## Failure flow

```text
Kafka consumer
   |
   | processing fails
   v
retry #1 -> retry #2 -> retry #3
   |
   | still failing
   v
<topic>.DLT
   |
   v
DLT monitor / investigation / controlled replay
```

## Important limitation

The local Kubernetes deployment runs one Kafka broker, so replication factor is intentionally `1`. For production/high availability, use at least 3 brokers and replication factor 3 with an appropriate minimum in-sync replica configuration.

Kafka delivery is normally at-least-once from the application's point of view. Therefore Outbox + idempotent consumers are both required: Outbox protects DB-to-Kafka publication, while idempotency protects business effects from redelivery.
