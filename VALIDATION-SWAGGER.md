# Validation and Swagger Upgrade

## Validation

Bean Validation is enabled in User, Product, Cart, Order, Inventory and Payment services.

Examples:
- User email: `@Email`, required fields, password length
- OTP: exactly 6 digits
- Product: non-blank title, non-negative price, required category
- Cart: positive product ID and quantity
- Order: at least one item, positive product IDs and quantities
- Inventory: positive IDs, quantity >= 1, positive reservation TTL
- Payment: positive order ID and required idempotency key

Controllers use `@Valid` for request bodies and `@Validated` for request parameters.

## Consistent validation responses

Services include a `@RestControllerAdvice` handler that returns structured 400 responses, for example:

```json
{
  "timestamp": "2026-09-06T10:30:00Z",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "errors": {
    "quantity": "Quantity must be greater than 0"
  }
}
```

## Swagger / OpenAPI

Springdoc OpenAPI UI has been added to the REST business services.

Swagger UI:
- Product: `http://localhost:8081/swagger-ui.html`
- User: `http://localhost:7070/swagger-ui.html`
- Order: `http://localhost:8083/swagger-ui.html`
- Inventory: `http://localhost:8084/swagger-ui.html`
- Payment: `http://localhost:8082/swagger-ui.html`
- Cart: `http://localhost:8085/swagger-ui.html`

OpenAPI JSON is available at `/v3/api-docs` on each service.

The API Gateway is intentionally not given a separate Swagger UI because it is a routing/security layer rather than the owner of the downstream API contracts. The service-level documentation is the source of truth for each API.

## Verification

- All Maven POM files were XML-parsed successfully.
- Java brace-balance scan passed.
- A complete Maven build was not run because the environment does not have the project's external Maven dependencies available for download.
