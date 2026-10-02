# Kubernetes-Native Service Discovery

## Decision

The project no longer uses Netflix Eureka for service discovery. Kubernetes Services and cluster DNS are the service registry and load-balancing mechanism.

## Request flow

Internet -> Kubernetes Ingress -> API Gateway -> Kubernetes Service -> Pods

Internal calls use stable Kubernetes DNS names such as:
- `http://user-service:7070`
- `http://product-service:8081`
- `http://cart-service:8085`
- `http://order-service:8083`
- `http://inventory-service:8084`
- `http://payment-service:8082`

Kubernetes resolves each Service name and routes traffic to ready Pods. The application therefore does not need Eureka, Eureka Client, or Spring Cloud LoadBalancer.

## Conflicts removed

1. Eureka Server deployment and Eureka Client dependencies removed.
2. `lb://...` Gateway routes replaced with direct Kubernetes Service URLs.
3. `@LoadBalanced RestTemplate` replaced with normal `RestTemplate`.
4. Order and Cart internal calls now use configurable service URLs.
5. Kubernetes manifests no longer reference `service-discovery` or `EUREKA_SERVER_URL`.
6. Jenkins no longer builds, pushes, deploys, or waits for a Eureka image.
7. The project uses Kubernetes Service DNS as the only supported runtime service-discovery mechanism.
8. A ConfigMap now owns internal service URLs so deployment configuration is centralized.

## Why this is cleaner

Ingress is only the external HTTP entry point. Kubernetes Services provide internal discovery and load balancing. Keeping Eureka as a second registry would duplicate service-discovery responsibilities and add another component that can fail.
