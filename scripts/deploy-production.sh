#!/bin/bash
# ========== scripts/deploy-production.sh ==========
set -e

echo "================================================"
echo "   LoadTest Platform - Production Deployment"
echo "================================================"
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Configuration
REGISTRY="your-registry.com"
VERSION=$(cat VERSION)
NAMESPACE="loadtest"

echo -e "${YELLOW}Deploying version: ${VERSION}${NC}"
echo ""

# Step 1: Build images
echo "Step 1: Building Docker images..."
docker build -t ${REGISTRY}/loadtest-controller:${VERSION} controller/
docker build -t ${REGISTRY}/loadtest-worker:${VERSION} worker/
docker build -t ${REGISTRY}/loadtest-frontend:${VERSION} frontend/

echo -e "${GREEN}✓ Images built${NC}"

# Step 2: Push to registry
echo ""
echo "Step 2: Pushing images to registry..."
docker push ${REGISTRY}/loadtest-controller:${VERSION}
docker push ${REGISTRY}/loadtest-worker:${VERSION}
docker push ${REGISTRY}/loadtest-frontend:${VERSION}

echo -e "${GREEN}✓ Images pushed${NC}"

# Step 3: Update Kubernetes manifests
echo ""
echo "Step 3: Updating Kubernetes manifests..."
sed -i "s|image:.*loadtest-controller.*|image: ${REGISTRY}/loadtest-controller:${VERSION}|g" k8s/controller-deployment.yaml
sed -i "s|image:.*loadtest-worker.*|image: ${REGISTRY}/loadtest-worker:${VERSION}|g" k8s/worker-deployment.yaml
sed -i "s|image:.*loadtest-frontend.*|image: ${REGISTRY}/loadtest-frontend:${VERSION}|g" k8s/frontend-deployment.yaml

echo -e "${GREEN}✓ Manifests updated${NC}"

# Step 4: Apply to Kubernetes
echo ""
echo "Step 4: Deploying to Kubernetes..."
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmaps.yaml
kubectl apply -f k8s/secrets.yaml
kubectl apply -f k8s/mongodb-statefulset.yaml
kubectl apply -f k8s/redis-deployment.yaml
kubectl apply -f k8s/controller-deployment.yaml
kubectl apply -f k8s/worker-deployment.yaml
kubectl apply -f k8s/frontend-deployment.yaml
kubectl apply -f k8s/ingress.yaml
kubectl apply -f k8s/monitoring.yaml

echo -e "${GREEN}✓ Deployed to Kubernetes${NC}"

# Step 5: Wait for rollout
echo ""
echo "Step 5: Waiting for rollout to complete..."
kubectl rollout status deployment/controller -n ${NAMESPACE} --timeout=5m
kubectl rollout status deployment/worker -n ${NAMESPACE} --timeout=5m
kubectl rollout status deployment/frontend -n ${NAMESPACE} --timeout=5m

echo -e "${GREEN}✓ Rollout completed${NC}"

# Step 6: Health check
echo ""
echo "Step 6: Running health checks..."
sleep 10

CONTROLLER_POD=$(kubectl get pods -n ${NAMESPACE} -l app=controller -o jsonpath='{.items[0].metadata.name}')
kubectl exec -n ${NAMESPACE} ${CONTROLLER_POD} -- curl -f http://localhost:8080/actuator/health

echo -e "${GREEN}✓ Health checks passed${NC}"

# Step 7: Summary
echo ""
echo "================================================"
echo -e "${GREEN}   Deployment Successful! 🎉${NC}"
echo "================================================"
echo ""
echo "Version: ${VERSION}"
echo "Namespace: ${NAMESPACE}"
echo ""
echo "Access points:"
kubectl get ingress -n ${NAMESPACE}
echo ""
echo "Worker count:"
kubectl get pods -n ${NAMESPACE} -l app=worker --no-headers | wc -l
echo ""