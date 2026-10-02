# CI/CD + Kubernetes Implementation

This project now includes the first ten requested DevOps/Kubernetes capabilities:

1. Jenkins CI pipeline
2. Docker image build and push
3. Kubernetes Deployment + Service
4. ConfigMap + Kubernetes Secret
5. Readiness + liveness probes
6. GitHub → Jenkins webhook trigger
7. Rolling deployment strategy
8. Horizontal Pod Autoscaling
9. NGINX Ingress
10. Persistent storage for PostgreSQL, Redis, Kafka and ZooKeeper

## Pipeline

```text
GitHub push
   ↓ webhook
Jenkins
   ↓
Checkout
   ↓
Maven clean test (all services)
   ↓
Docker build
   ↓
Docker Hub push
   ↓
Kubernetes apply
   ↓
RollingUpdate
   ↓
Readiness/Liveness
   ↓
Ingress + HPA
   ↓
Rollout verification
```

## Jenkins credentials

Create these credentials in Jenkins:

- `dockerhub-creds`: Docker Hub username/password credential
- `kubeconfig-ecommerce`: Secret file containing the kubeconfig used by Jenkins
- `ecommerce-k8s-env`: Secret text containing newline-separated Kubernetes secret values

The `ecommerce-k8s-env` value is converted into a Kubernetes Secret during deployment. Real secrets are never committed to the repository.

## GitHub webhook

In the GitHub repository settings, add a webhook pointing to your Jenkins endpoint:

```text
https://<jenkins-host>/github-webhook/
```

Set content type to `application/json` and enable the push event. The Jenkinsfile contains `githubPush()` so a push can trigger the pipeline when the Jenkins GitHub integration/plugin is configured.

For local Jenkins, expose Jenkins through a reachable HTTPS endpoint (for example, a controlled tunnel during development). Do not expose Jenkins directly to the public internet without authentication and appropriate security controls.

## HPA prerequisite

HPA requires the Kubernetes Metrics API. On Minikube:

```bash
minikube addons enable metrics-server
kubectl top pods -n ecommerce
```

If `kubectl top` does not return metrics, HPA cannot make CPU/memory scaling decisions yet.

## Ingress prerequisite

On Minikube:

```bash
minikube addons enable ingress
```

The Ingress uses:

```text
Host: ecommerce.local
Path: /
Backend: api-gateway:8080
```

Only API Gateway is exposed externally; internal services remain ClusterIP.

## Persistent storage

Each PostgreSQL service already has its own PVC. Redis now uses a PVC with AOF enabled. Kafka and ZooKeeper also have PVCs. This means deleting/restarting a pod does not intentionally discard its persisted data, subject to the Kubernetes storage implementation.

## Important local note

The Kubernetes manifests use Docker Hub image placeholders. Jenkins replaces them with the configured Docker Hub namespace and Jenkins build tag before applying the application deployments.
