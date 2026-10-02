# New upgrades: OTP + Product Filtering

## Product filtering
GET /products supports optional query parameters:
- title
- category
- minPrice
- maxPrice
- pageNumber
- pageSize
- sortBy=title|price|id
- direction=asc|desc

Examples:
- GET /products?title=laptop
- GET /products?category=Electronics
- GET /products?minPrice=10000&maxPrice=50000
- GET /products?category=Electronics&title=laptop&minPrice=10000&maxPrice=50000&sortBy=price&direction=desc

Implemented with Spring Data JPA Specifications, so filters can be combined without creating a repository method for every combination.

## OTP
OTP is implemented in User Service and uses Redis for short-lived state.
- 6 digit SecureRandom OTP
- SHA-256 hash stored in Redis
- 5 minute expiry
- maximum 5 verification attempts
- 60 second resend cooldown
- registration email verification
- forgot-password OTP
- one-time reset token after successful reset OTP verification
- OTP emails are sent through the existing transactional outbox -> Kafka -> Email Service -> SMTP flow

Public endpoints:
POST /users/verify-otp
POST /users/resend-otp
POST /users/forgot-password
POST /users/verify-reset-otp
POST /users/reset-password
