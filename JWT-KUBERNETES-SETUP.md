# JWT + Kubernetes setup

## JWT flow
- `POST /users/signup` creates a user and hashes the password with BCrypt.
- `POST /users/login` returns a signed HMAC JWT in `tokenValue`.
- API Gateway validates the JWT for protected routes and forwards valid requests.
- `/users/login`, `/users/signup`, and `/users/validate/**` are public.
- `/products/**`, `/payments/**`, and other gateway routes require `Authorization: Bearer <JWT>`.
- JWT lifetime defaults to 1 hour and is controlled by `JWT_EXPIRATION_SECONDS`.
- `JWT_SECRET` must be at least 32 characters and must be identical in User Service and API Gateway.

## Run on Kubernetes
1. Start a Kubernetes cluster (Minikube is sufficient for local development).
2. Enable the NGINX Ingress controller and Metrics Server.
3. Create the `ecommerce` namespace.
4. Create the Kubernetes secrets from `k8s/secret-template.yaml`.
5. Apply the infrastructure manifests and application manifests described in `k8s/README.md`.
6. Access the application through the Kubernetes Ingress. Do not expose individual business services directly.

The supported runtime for this project is Kubernetes; the repository contains only Kubernetes deployment manifests.

## Example
Once the Ingress host resolves to the cluster, use the gateway URL:

```bash
curl -X POST http://ecommerce.local/users/signup \
  -H 'Content-Type: application/json' \
  -d '{"name":"Rohit","email":"rohit@example.com","password":"Password123"}'

curl -X POST http://ecommerce.local/users/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"rohit@example.com","password":"Password123"}'
```

Use the returned `tokenValue`:

```bash
curl http://ecommerce.local/products/1 \
  -H "Authorization: Bearer <tokenValue>"
```

## JWT responsibility after architecture cleanup
- User Service authenticates login credentials and issues/signs the JWT.
- API Gateway is the centralized JWT authentication/authorization boundary for incoming client requests.
- User Service does not install or run a JWT authentication filter for normal incoming requests.
- User Service still parses the token during logout only to obtain its JTI and place that JTI on the Redis blacklist; this is token revocation logic, not request authentication.
