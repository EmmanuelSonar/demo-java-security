# Infrastructure as Code (IaC) Overview

This document provides a comprehensive overview of the Infrastructure as Code setup for the Java Security demo application.

## Table of Contents

1. [File Structure](#file-structure)
2. [Containerization](#containerization)
3. [Kubernetes Deployment](#kubernetes-deployment)
4. [Helm Charts](#helm-charts)
5. [CI/CD Pipelines](#cicd-pipelines)
6. [Local Development](#local-development)
7. [Quick Start Guide](#quick-start-guide)
8. [Production Considerations](#production-considerations)

---

## File Structure

```
.
├── Dockerfile                          # Multi-stage Docker build
├── docker-compose.yml                  # Local development environment
├── k8s/                               # Kubernetes manifests
│   ├── namespace.yaml                 # Namespace isolation
│   ├── deployment.yaml                # Pod deployment config
│   ├── service.yaml                   # LoadBalancer and ClusterIP services
│   ├── ingress.yaml                   # HTTPS routing and TLS
│   ├── configmap.yaml                 # Application configuration
│   ├── secret.yaml                    # Database credentials
│   ├── serviceaccount.yaml            # RBAC identity
│   ├── hpa.yaml                       # Horizontal Pod Autoscaler
│   ├── pdb.yaml                       # Pod Disruption Budget
│   ├── networkpolicy.yaml             # Network security
│   ├── resourcequota.yaml             # Resource limits
│   └── kustomization.yaml             # Kustomize overlay
├── helm/                              # Helm chart
│   ├── Chart.yaml                     # Chart metadata
│   ├── values.yaml                    # Default values
│   └── templates/                     # Templated manifests
│       ├── deployment.yaml
│       ├── service.yaml
│       ├── configmap.yaml
│       ├── secret.yaml
│       ├── serviceaccount.yaml
│       ├── hpa.yaml
│       └── _helpers.tpl
├── scripts/                           # Deployment scripts
│   ├── deploy.sh                      # Main deployment script
│   └── delete.sh                      # Cleanup script
├── .github/workflows/                 # GitHub Actions CI/CD
│   ├── build-and-deploy.yml           # Build, test, deploy pipeline
│   ├── helm-lint.yml                  # Helm validation
│   └── k8s-lint.yml                   # Kubernetes manifest validation
└── KUBERNETES.md                       # Detailed Kubernetes guide
```

---

## Containerization

### Dockerfile

The `Dockerfile` uses a **multi-stage build** approach:

1. **Builder Stage**
   - Uses Maven with OpenJDK 17
   - Builds the WAR artifact from source
   - Skips tests for faster builds (use CI for testing)

2. **Runtime Stage**
   - Uses Tomcat 10 with OpenJDK 17-slim
   - Minimal image footprint
   - Includes health check endpoint
   - Proper signal handling for graceful shutdown

### Build & Push

```bash
# Using deployment script
./scripts/deploy.sh build   # Build locally
./scripts/deploy.sh push    # Push to registry

# Or manually with Docker
docker build -t docker.io/your-username/java-security:1.0.0 .
docker push docker.io/your-username/java-security:1.0.0
```

### Image Details

- **Base Image**: tomcat:10-jdk17-openjdk-slim
- **Size**: ~300MB
- **Ports**: 8080 (HTTP), 8686 (JMX)
- **Health Check**: `/ping` endpoint
- **User**: tomcat (UID 1000, non-root)

---

## Kubernetes Deployment

### Architecture

```
┌──────────────────────────────────────┐
│    AWS EKS Cluster                   │
├──────────────────────────────────────┤
│                                      │
│  ┌─ Namespace: java-security ──┐   │
│  │                              │   │
│  │  ┌─ Deployment ─────────┐   │   │
│  │  │ Replicas: 2-10       │   │   │
│  │  │ (Auto-scaled)        │   │   │
│  │  │ ┌──────────────────┐ │   │   │
│  │  │ │ Pod 1: java-app  │ │   │   │
│  │  │ ├──────────────────┤ │   │   │
│  │  │ │ Pod 2: java-app  │ │   │   │
│  │  │ └──────────────────┘ │   │   │
│  │  └──────────────────────┘   │   │
│  │                              │   │
│  │  ┌─ Services ───────────┐   │   │
│  │  │ • LoadBalancer       │   │   │
│  │  │ • ClusterIP          │   │   │
│  │  └──────────────────────┘   │   │
│  │                              │   │
│  │  ┌─ Ingress ────────────┐   │   │
│  │  │ HTTPS TLS            │   │   │
│  │  │ Rate limiting        │   │   │
│  │  │ CORS enabled         │   │   │
│  │  └──────────────────────┘   │   │
│  │                              │   │
│  │  ┌─ Config & Secrets ───┐   │   │
│  │  │ • ConfigMap          │   │   │
│  │  │ • Secrets            │   │   │
│  │  │ • ServiceAccount     │   │   │
│  │  └──────────────────────┘   │   │
│  │                              │   │
│  └──────────────────────────────┘   │
│                                      │
│  ┌─ Policies & Quotas ───────────┐  │
│  │ • NetworkPolicy               │  │
│  │ • PodDisruptionBudget         │  │
│  │ • ResourceQuota               │  │
│  │ • HorizontalPodAutoscaler     │  │
│  └───────────────────────────────┘  │
│                                      │
└──────────────────────────────────────┘
        │                    ↓
        │         External Load Balancer
        └────────→ PostgreSQL Database
```

### Key Resources

| Resource | Purpose | Replicas |
|----------|---------|----------|
| **Deployment** | Manages Pod replicas | 2-10 (auto-scaled) |
| **Service (LB)** | External access | 1 |
| **Service (CIP)** | Internal access | 1 |
| **Ingress** | HTTP/HTTPS routing | 1 |
| **ConfigMap** | App configuration | 1 |
| **Secret** | DB credentials | 1 |
| **HPA** | Auto-scaling | 1 |
| **PDB** | High availability | 1 |
| **NetworkPolicy** | Network security | 1 |
| **ResourceQuota** | Resource limits | 1 |

### Security Features

- **Pod Security**: Non-root user, read-only filesystem, dropped capabilities
- **Network Isolation**: NetworkPolicy restricts traffic
- **RBAC**: ServiceAccount with minimal permissions
- **Secrets Management**: Encrypted credentials
- **Resource Limits**: CPU and memory bounds
- **Health Checks**: Liveness and readiness probes

---

## Helm Charts

Helm provides templating and packaging for Kubernetes manifests.

### Chart Structure

```
helm/
├── Chart.yaml              # Chart metadata
├── values.yaml             # Default configuration
└── templates/
    ├── deployment.yaml     # Templated deployment
    ├── service.yaml
    ├── configmap.yaml
    ├── secret.yaml
    ├── serviceaccount.yaml
    ├── hpa.yaml
    └── _helpers.tpl        # Template helpers
```

### Key Values

Edit `helm/values.yaml` to customize:

```yaml
replicaCount: 2
image:
  tag: "1.0.0"
resources:
  limits:
    memory: "512Mi"
    cpu: "500m"
autoscaling:
  minReplicas: 2
  maxReplicas: 10
```

### Install/Upgrade

```bash
# Install
helm install java-security ./helm -n java-security --create-namespace

# Upgrade
helm upgrade java-security ./helm -n java-security

# With custom values
helm upgrade java-security ./helm \
  --set replicaCount=5 \
  --set image.tag=2.0.0
```

---

## CI/CD Pipelines

### GitHub Actions Workflows

#### 1. Build and Deploy (`build-and-deploy.yml`)

**Triggers**: Push to main/develop, tags, pull requests

**Jobs**:
- **build**: Maven build + Docker build/push + Trivy scan
- **test**: Unit tests with PostgreSQL
- **deploy-dev**: Auto-deploy to development on push to develop
- **deploy-prod**: Auto-deploy to production on version tags

**Features**:
- Maven caching for faster builds
- Docker layer caching
- Vulnerability scanning with Trivy
- Code coverage upload to Codecov
- Multi-stage deployments

#### 2. Helm Lint (`helm-lint.yml`)

**Triggers**: Changes to helm/ directory

**Checks**:
- Helm chart validation
- Template rendering
- Kubernetes manifest validation

#### 3. Kubernetes Lint (`k8s-lint.yml`)

**Triggers**: Changes to k8s/ directory

**Checks**:
- kubeval manifest validation
- kubectl dry-run validation
- Kustomize build validation

### Pipeline Flow

```
┌─ Pull Request
│  ├─ Build ──────► Test
│  ├─ Helm Lint
│  └─ K8s Lint
│
├─ Push to main
│  ├─ Build ──────► Test
│  ├─ Build + Push Image (docker.io)
│  ├─ Helm Lint
│  └─ K8s Lint
│
├─ Push to develop
│  ├─ Build ──────► Test
│  ├─ Build + Push Image (docker.io:develop-SHA)
│  └─ Deploy to Dev EKS
│
└─ Create Tag (v1.0.0)
   ├─ Build ──────► Test
   ├─ Build + Push Image (docker.io:1.0.0)
   └─ Deploy to Prod EKS
```

### Required Secrets

Configure these in GitHub repository settings:

```
DOCKER_USERNAME          # Docker Hub username
DOCKER_PASSWORD          # Docker Hub token
AWS_ACCESS_KEY_ID        # Dev AWS credentials
AWS_SECRET_ACCESS_KEY
PROD_AWS_ACCESS_KEY_ID   # Prod AWS credentials
PROD_AWS_SECRET_ACCESS_KEY
HELM_REPO_URL           # Helm chart repository (optional)
```

---

## Local Development

### Docker Compose Setup

Launch entire stack locally:

```bash
docker-compose up -d
```

**Services**:
- **App**: http://localhost:8080
- **PostgreSQL**: localhost:5432
- **PgAdmin**: http://localhost:5050
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000

### Commands

```bash
# Start services
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop services
docker-compose down

# Clean up volumes
docker-compose down -v

# Rebuild image
docker-compose build --no-cache app
```

### Database Access

```bash
# Connect with psql
psql -h localhost -U java_user -d java_security

# Via PgAdmin
# URL: http://localhost:5050
# Email: admin@example.com
# Password: admin
```

---

## Quick Start Guide

### 1. Local Development

```bash
# Start development environment
docker-compose up -d

# Application is available at http://localhost:8080
# Database at localhost:5432

# View logs
docker-compose logs -f app
```

### 2. Build Docker Image

```bash
export DOCKER_REGISTRY=docker.io
export DOCKER_REPO=your-username/java-security
export IMAGE_TAG=1.0.0

./scripts/deploy.sh build
./scripts/deploy.sh push
```

### 3. Deploy to Kubernetes

```bash
# Configure kubectl to connect to your cluster
kubectl config use-context my-cluster

# Deploy with Helm
./scripts/deploy.sh

# Or manually:
helm install java-security ./helm \
  -n java-security \
  --create-namespace \
  --set image.tag=$IMAGE_TAG
```

### 4. Verify Deployment

```bash
# Check pods
kubectl get pods -n java-security

# Check services
kubectl get svc -n java-security

# View logs
kubectl logs -n java-security -l app=java-security -f

# Port forward to test locally
kubectl port-forward svc/java-security-internal 8080:8080 -n java-security
curl http://localhost:8080/ping
```

### 5. Configure CI/CD

```bash
# Add secrets to GitHub repository
gh secret set DOCKER_USERNAME
gh secret set DOCKER_PASSWORD
gh secret set AWS_ACCESS_KEY_ID
gh secret set AWS_SECRET_ACCESS_KEY

# Push code
git push

# Monitor workflow
gh run watch
```

---

## Production Considerations

### High Availability

- **Replicas**: Minimum 2 pods, auto-scales to 10
- **Pod Disruption Budget**: Ensures 1 pod always available
- **Pod Anti-Affinity**: Spreads pods across nodes
- **Multi-AZ**: Deploy across availability zones

### Security

- **Network Policy**: Restricts ingress/egress traffic
- **RBAC**: Least privilege service account
- **Secrets Encryption**: Enable etcd encryption at rest
- **Image Scanning**: Trivy scans for vulnerabilities
- **Pod Security Policy**: Enforces security standards

### Monitoring & Logging

```bash
# Install Prometheus monitoring
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm install prometheus prometheus-community/kube-prometheus-stack -n monitoring

# Install logging (ELK/Loki)
helm repo add grafana https://grafana.github.io/helm-charts
helm install loki grafana/loki -n logging
```

### Backup & Disaster Recovery

```bash
# Backup Helm release
helm get values java-security -n java-security > backup.yaml
kubectl get all -n java-security -o yaml > backup.yaml

# Restore
helm install java-security ./helm -f backup.yaml -n java-security
```

### Cost Optimization

```yaml
# In helm/values.yaml
resources:
  requests:
    cpu: 250m        # Actual needed
    memory: 256Mi
  limits:
    cpu: 500m        # Max allowed
    memory: 512Mi

autoscaling:
  minReplicas: 1     # Scale down when not needed
  maxReplicas: 5     # Control max spending
```

### Compliance

- **Audit Logging**: Enable Kubernetes API audit logs
- **RBAC**: Implement least privilege
- **Network Policies**: Segment traffic
- **Pod Security**: Enforce non-root, read-only
- **Secrets Management**: Use external secrets operator (ESO)

---

## Troubleshooting

### Common Issues

**Pods not starting**:
```bash
kubectl describe pod <pod-name> -n java-security
kubectl logs <pod-name> -n java-security
```

**Image pull errors**:
```bash
# Check image exists
docker pull docker.io/your-username/java-security:1.0.0

# Update image pull secret if using private registry
kubectl create secret docker-registry regcred \
  --docker-server=docker.io \
  --docker-username=your-username \
  --docker-password=your-token \
  -n java-security
```

**Database connection issues**:
```bash
# Verify database is reachable
kubectl run -it --rm debug --image=postgres:14 --restart=Never \
  -- psql -h postgres-host -U java_user -d java_security
```

---

## Further Reading

- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Helm Documentation](https://helm.sh/docs/)
- [Docker Documentation](https://docs.docker.com/)
- [GitHub Actions](https://docs.github.com/en/actions)
- See `KUBERNETES.md` for detailed Kubernetes guide
