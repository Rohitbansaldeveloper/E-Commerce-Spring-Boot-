# Kubernetes deployment

## Required components
- Kubernetes cluster (Minikube is enough for local development)
- Ingress controller (NGINX)
- Metrics Server (required by HPA)
- StorageClass that can provision `ReadWriteOnce` PVCs (Minikube's default storage class works)

## Local Minikube setup

```bash
minikube start --cpus=6 --memory=8192
minikube addons enable ingress
minikube addons enable metrics-server
kubectl create namespace ecommerce
```

Create the demo secret only for local learning:

```bash
kubectl apply -f k8s/secret-template.yaml
```

Then deploy infrastructure and applications:

```bash
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/stateful-infra-persistence.yaml
kubectl apply -f k8s/postgres-user.yaml
kubectl apply -f k8s/postgres-product.yaml
kubectl apply -f k8s/postgres-cart.yaml
kubectl apply -f k8s/postgres-order.yaml
kubectl apply -f k8s/postgres-inventory.yaml
kubectl apply -f k8s/postgres-payment.yaml
kubectl apply -f k8s/redis.yaml
kubectl apply -f k8s/kafka.yaml
```

Application images are expected to use the placeholders `__DOCKERHUB_NAMESPACE__` and `__IMAGE_TAG__`. Jenkins replaces those placeholders before applying each deployment.

```bash
kubectl apply -f k8s/ingress.yaml
kubectl apply -f k8s/hpa.yaml
```

For Minikube, add the host entry returned by your environment. A simple local option is:

```text
<MINIKUBE_IP> ecommerce.local
```

Then browse through the Ingress host rather than exposing every service externally.

## Jenkins secrets

Create one Jenkins **Secret text** credential with ID:

`ecommerce-k8s-env`

Its content should be newline-separated `KEY=value` pairs matching the keys in `secret-template.yaml`, for example:

```text
JWT_SECRET=<long-random-secret>
JWT_ISSUER=UserServiceFeb25
JWT_EXPIRATION_SECONDS=3600
STRIPE_KEY=<stripe-secret>
STRIPE_WEBHOOK_SECRET=<stripe-webhook-secret>
MAIL_USERNAME=<mail-user>
MAIL_PASSWORD=<mail-password>
MAIL_FROM=<from-address>
ADMIN_EMAIL=<admin-email>
USERSERVICE_DB_USER=userservicefeb25user
USERSERVICE_DB_PASSWORD=<password>
USERSERVICE_DB_ROOT_PASSWORD=<root-password>
...
```

Do not commit real credentials to Git.

The Jenkins pipeline creates/updates the Kubernetes Secret from this credential at deploy time.

## CI/CD behavior

1. GitHub push triggers Jenkins through the `githubPush()` trigger.
2. Jenkins checks out the repository.
3. Each Spring Boot service runs `./mvnw clean test`.
4. Jenkins builds versioned Docker images.
5. Images are pushed to Docker Hub.
6. Kubernetes manifests are applied.
7. Deployments use `RollingUpdate` with `maxUnavailable: 0`.
8. Readiness/liveness probes control pod health.
9. Ingress routes external HTTP traffic to API Gateway.
10. HPA can scale API Gateway, Product, Order, Inventory and Payment based on CPU/memory.
11. PostgreSQL, Redis, Kafka and ZooKeeper have persistent storage where appropriate.
12. Jenkins waits for rollout completion and fails the build if a deployment does not become ready.


## Required deployment prerequisites

- Jenkins credential `ecommerce-k8s-env` must include `INTERNAL_SERVICE_TOKEN` (at least 32 random characters).
- Kubernetes Metrics Server must be installed if the HPA in `hpa.yaml` is enabled.
- Ingress-NGINX must be installed and `ecommerce.local` must resolve to the Ingress controller for local testing.
