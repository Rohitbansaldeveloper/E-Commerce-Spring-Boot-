# Fixes after runtime-oriented code review

## Fixed
1. Inventory reservation confirmation now locks the reservation row with `PESSIMISTIC_WRITE`.
2. Reservation release now also locks the reservation row.
3. Reservation expiry re-locks and re-checks the reservation before releasing stock, preventing expiry/confirmation races from double-releasing stock.
4. Order creation compensation now tracks actual `reservationId` values rather than reconstructing releases from product/quantity.
5. Compensation retries the exact reservation-release operation and no longer silently swallows failures. Reservation expiry remains the final safety net.
6. API Gateway now explicitly replaces `X-User-Id` using `headers.set(...)`, so a client-supplied identity cannot survive alongside the JWT-derived identity.

## Important runtime verification still required
- Run concurrent reservation/confirmation/expiry tests.
- Run Maven tests for every service.
- Run the Kubernetes deployment end-to-end (Minikube or another Kubernetes cluster).
- Verify Kafka retry/DLT behavior with a poison message.
- Verify Stripe webhook against Stripe's test environment.
