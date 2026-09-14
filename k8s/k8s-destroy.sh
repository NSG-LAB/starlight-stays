#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
echo "🧹 Tearing down Starlight Stays Kubernetes deployment..."

kubectl delete -f "$DIR/hpa.yaml" --ignore-not-found=true
kubectl delete -f "$DIR/ingress.yaml" --ignore-not-found=true
kubectl delete -f "$DIR/microservices/" --ignore-not-found=true
kubectl delete -f "$DIR/infrastructure/" --ignore-not-found=true
kubectl delete -f "$DIR/configmaps-secrets.yaml" --ignore-not-found=true
kubectl delete -f "$DIR/namespace.yaml" --ignore-not-found=true

echo "✓ Starlight Stays namespace and resources removed."
