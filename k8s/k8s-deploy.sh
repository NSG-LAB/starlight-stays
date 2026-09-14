#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
echo "🌟 Deploying Starlight Stays Service Mesh to Kubernetes..."

kubectl apply -f "$DIR/namespace.yaml"
kubectl apply -f "$DIR/configmaps-secrets.yaml"
kubectl apply -f "$DIR/infrastructure/"

echo "⏳ Waiting for databases and message broker..."
kubectl rollout status statefulset/postgres -n starlight-mesh --timeout=120s || true
kubectl rollout status statefulset/rabbitmq -n starlight-mesh --timeout=120s || true

echo "🚀 Deploying microservices..."
kubectl apply -f "$DIR/microservices/"
kubectl apply -f "$DIR/ingress.yaml"
kubectl apply -f "$DIR/hpa.yaml"

echo "✓ Deployment submitted. Verifying pods in starlight-mesh namespace:"
kubectl get pods,svc,hpa -n starlight-mesh
