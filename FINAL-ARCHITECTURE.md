# Final Architecture

## Deployment
- Kubernetes is the only orchestrator. Docker Compose is removed.
- Kubernetes Services + DNS provide internal service discovery. Eureka is removed.
- Ingress is the external HTTP entry point; API Gateway performs JWT validation, RBAC, routing, rate limiting and CORS.

## Purchase flows
- Cart: add items -> cart checkout -> `POST /orders`.
- Buy Now: client sends the selected items directly to `POST /orders`; a persistent Cart entry is not required.
- Payment is explicit: after order creation, `POST /orders/{id}/payment` initiates the provider flow. Stripe webhook is verified by Payment Service using `Stripe-Signature`.

## Internal security
- Gateway and service-to-service HTTP clients send `X-Service-Token`.
- Services reject HTTP requests without the shared internal credential.
- Kubernetes NetworkPolicies restrict which pods may reach each business service.
- Health probes bypass the application service-token filter so Kubernetes liveness/readiness checks continue to work.

## Reliability
- Inventory reservation uses transactional locking.
- Order/payment state transitions use Saga compensation.
- Outbox events are persisted with business state before Kafka publication.
- Idempotency keys and processed-event records protect order/payment flows from duplicate requests/events.

## Kubernetes prerequisites
- A Kubernetes Metrics Server is required for HPA CPU/memory metrics.
- Jenkins must provide `INTERNAL_SERVICE_TOKEN` in the `ecommerce-k8s-env` credential.
