#!/bin/bash

set -euo pipefail

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Configuration
DOCKER_REGISTRY="${DOCKER_REGISTRY:-docker.io}"
DOCKER_REPO="${DOCKER_REPO:-java-security}"
IMAGE_TAG="${IMAGE_TAG:-latest}"
NAMESPACE="${NAMESPACE:-java-security}"
KUBE_CONTEXT="${KUBE_CONTEXT:-}"
HELM_RELEASE="${HELM_RELEASE:-java-security}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."

    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed"
        exit 1
    fi

    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl is not installed"
        exit 1
    fi

    if ! command -v helm &> /dev/null; then
        log_error "Helm is not installed"
        exit 1
    fi

    log_info "All prerequisites met"
}

# Build Docker image
build_image() {
    log_info "Building Docker image..."
    docker build -t "${DOCKER_REGISTRY}/${DOCKER_REPO}:${IMAGE_TAG}" \
                 -t "${DOCKER_REGISTRY}/${DOCKER_REPO}:latest" \
                 -f "$PROJECT_ROOT/Dockerfile" \
                 "$PROJECT_ROOT"
    log_info "Image built successfully: ${DOCKER_REGISTRY}/${DOCKER_REPO}:${IMAGE_TAG}"
}

# Push Docker image
push_image() {
    log_info "Pushing Docker image to registry..."
    docker push "${DOCKER_REGISTRY}/${DOCKER_REPO}:${IMAGE_TAG}"
    docker push "${DOCKER_REGISTRY}/${DOCKER_REPO}:latest"
    log_info "Image pushed successfully"
}

# Setup kubectl context
setup_context() {
    if [ -n "$KUBE_CONTEXT" ]; then
        log_info "Switching to context: $KUBE_CONTEXT"
        kubectl config use-context "$KUBE_CONTEXT"
    fi

    log_info "Current context: $(kubectl config current-context)"
    log_info "Current user: $(kubectl config view -o jsonpath='{.current-context}')"
}

# Deploy using Kustomize
deploy_kustomize() {
    log_info "Deploying using Kustomize..."
    kubectl apply -k "$PROJECT_ROOT/k8s"
    log_info "Kustomize deployment completed"
}

# Deploy using Helm
deploy_helm() {
    log_info "Deploying using Helm..."

    helm repo update

    helm upgrade --install "$HELM_RELEASE" "$PROJECT_ROOT/helm" \
        --namespace "$NAMESPACE" \
        --create-namespace \
        --set "image.registry=$DOCKER_REGISTRY" \
        --set "image.repository=$DOCKER_REPO" \
        --set "image.tag=$IMAGE_TAG" \
        --wait \
        --timeout 5m

    log_info "Helm deployment completed"
}

# Wait for rollout
wait_rollout() {
    log_info "Waiting for deployment rollout..."
    kubectl rollout status deployment/java-security \
        -n "$NAMESPACE" \
        --timeout=5m
    log_info "Deployment rolled out successfully"
}

# Verify deployment
verify_deployment() {
    log_info "Verifying deployment..."

    log_info "Pods:"
    kubectl get pods -n "$NAMESPACE" -l app=java-security

    log_info "Services:"
    kubectl get svc -n "$NAMESPACE"

    log_info "Ingress:"
    kubectl get ingress -n "$NAMESPACE"

    log_info "Deployment status:"
    kubectl describe deployment java-security -n "$NAMESPACE" | tail -20
}

# Show logs
show_logs() {
    log_info "Recent deployment logs:"
    kubectl logs -n "$NAMESPACE" -l app=java-security --tail=50 -f &
}

# Main deployment flow
main() {
    log_info "Starting deployment process..."
    log_info "Configuration:"
    echo "  Docker Registry: $DOCKER_REGISTRY"
    echo "  Docker Repo: $DOCKER_REPO"
    echo "  Image Tag: $IMAGE_TAG"
    echo "  Namespace: $NAMESPACE"
    echo "  Helm Release: $HELM_RELEASE"

    read -p "Continue with deployment? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        log_warn "Deployment cancelled"
        exit 0
    fi

    check_prerequisites
    setup_context
    build_image
    push_image
    deploy_helm
    wait_rollout
    verify_deployment

    log_info "Deployment completed successfully!"
}

# Parse command line arguments
case "${1:-}" in
    build)
        check_prerequisites
        build_image
        ;;
    push)
        check_prerequisites
        push_image
        ;;
    deploy)
        setup_context
        deploy_helm
        wait_rollout
        verify_deployment
        ;;
    logs)
        show_logs
        ;;
    *)
        main
        ;;
esac
