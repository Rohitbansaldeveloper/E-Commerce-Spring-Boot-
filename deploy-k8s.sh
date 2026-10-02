#!/usr/bin/env bash
set -euo pipefail
: "${DOCKERHUB_NAMESPACE:?Set DOCKERHUB_NAMESPACE first}"
IMAGE_TAG="${IMAGE_TAG:-latest}"

kubectl apply -f k8s/namespace.yaml

# For local/demo deployments only. In Jenkins, the Secret is generated from
# the protected ecommerce-k8s-env credential instead of this template.
if [[ "${APPLY_DEMO_SECRET:-false}" == "true" ]]; then
  kubectl apply -f k8s/secret-template.yaml
fi

kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/stateful-infra-persistence.yaml
for f in k8s/postgresql-*.yaml k8s/redis.yaml k8s/kafka.yaml; do
  kubectl apply -f "$f"
done

for f in k8s/api-gateway.yaml k8s/user-service.yaml k8s/product-service.yaml k8s/cart-service.yaml k8s/order-service.yaml k8s/inventory-service.yaml k8s/payment-service.yaml k8s/email-service.yaml; do
  sed -e "s#__DOCKERHUB_NAMESPACE__#${DOCKERHUB_NAMESPACE}#g" \
      -e "s#__IMAGE_TAG__#${IMAGE_TAG}#g" "$f" | kubectl apply -f -
done

kubectl apply -f k8s/ingress.yaml
kubectl apply -f k8s/hpa.yaml
kubectl apply -f k8s/pdb.yaml

for svc in api-gateway user-service product-service cart-service order-service inventory-service payment-service email-service; do
  kubectl rollout status "deployment/${svc}" -n ecommerce --timeout=180s
done

kubectl get pods -n ecommerce
kubectl get ingress -n ecommerce
kubectl get hpa -n ecommerce
