#!/bin/bash

set -euo pipefail

NAMESPACE="${NAMESPACE:-java-security}"
HELM_RELEASE="${HELM_RELEASE:-java-security}"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

# Confirm deletion
read -p "Are you sure you want to delete the deployment? (y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    log_warn "Deletion cancelled"
    exit 0
fi

log_warn "Deleting Helm release: $HELM_RELEASE"
helm uninstall "$HELM_RELEASE" --namespace "$NAMESPACE" || true

log_warn "Deleting namespace: $NAMESPACE"
kubectl delete namespace "$NAMESPACE" || true

log_info "Deployment deleted successfully"
