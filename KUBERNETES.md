# Kubernetes Deployment Guide

This document describes how to deploy and manage the Java Security demo application on Kubernetes.

## Prerequisites

- Kubernetes cluster (1.21+)
- Docker for building images
- kubectl (1.21+)
- Helm 3+
- Access to a container registry (Docker Hub, ECR, GCR, etc.)

## Architecture Overview

```
┌─────────────────────────────────────────┐
│  Ingress (nginx)                        │
│  - TLS termination                      │
│  - Rate limiting                        │
└──────────────────┬──────────────────────┘
                   │
         ┌─────────┴─────────┐
         │                   │
    ┌────▼────┐         ┌────▼────┐
    │ Pod 1   │         │ Pod 2   │
    │ (java)  │◄───────►│ (java)  │
    └────┬────┘         └────┬────┘
         │                   │
         └─────────┬─────────┘
                   │
         ┌─────────▼─────────┐
         │ PostgreSQL DB     │
         │ (external)        │
         └───────────────────┘
```

## Quick Start

### 1. Build and Push Docker Image

```bash
# Set your registry
export DOCKER_REGISTRY=docker.io
export DOCKER_REPO=your-username/java-security
export IMAGE_TAG=1.0.0

# Run the deployment script
./scripts/deploy.sh build
./scripts/deploy.sh push
```

### 2. Deploy with Helm

```bash
# Deploy to Kubernetes
./scripts/deploy.sh

# Or manually with Helm:
helm install java-security ./helm \
  --namespace java-security \
  --create-namespace \
  --set image.registry=$DOCKER_REGISTRY \
  --set image.repository=$DOCKER_REPO \
  --set image.tag=$IMAGE_TAG
```

### 3. Verify Deployment

```bash
# Check pod status
kubectl get pods -n java-security

# Check services
kubectl get svc -n java-security

# Check ingress
kubectl get ingress -n java-security

# View logs
kubectl logs -n java-security -l app=java-security -f
```

## Configuration

### Using Helm Values

Edit `helm/values.yaml` to customize:

```yaml
# Number of replicas
replicaCount: 2

# Image settings
image:
  registry: docker.io
  repository: your-username/java-security
  tag: "1.0.0"

# Resource limits
resources:
  limits:
    cpu: 500m
    memory: 512Mi
  requests:
    cpu: 250m
    memory: 256Mi

# Autoscaling
autoscaling:
  enabled: true
  minReplicas: 2
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70

# Database credentials
database:
  url: "jdbc:postgresql://postgres-service:5432/java_security"
  username: "java_user"
  password: "your-secure-password"
```

### Override with Command Line

```bash
helm upgrade java-security ./helm \
  --set replicaCount=3 \
  --set resources.limits.memory=1Gi \
  --set autoscaling.maxReplicas=20
```

### Using Environment Variables

```bash
export NAMESPACE=java-security
export HELM_RELEASE=java-security
export DOCKER_REGISTRY=docker.io
export DOCKER_REPO=your-username/java-security
export IMAGE_TAG=1.0.0

./scripts/deploy.sh
```

## Kubernetes Objects

### Deployed Resources

The deployment creates the following Kubernetes objects:

- **Namespace**: `java-security` - Isolated environment for the application
- **Deployment**: `java-security` - Manages application pods with rolling updates
- **Service (LoadBalancer)**: `java-security-service` - External access on ports 80/443
- **Service (ClusterIP)**: `java-security-internal` - Internal cluster access
- **Ingress**: `java-security-ingress` - HTTP/HTTPS routing with TLS
- **HPA**: `java-security-hpa` - Autoscaling based on CPU and memory
- **PDB**: `java-security-pdb` - Ensures minimum availability during disruptions
- **NetworkPolicy**: `java-security-networkpolicy` - Restricts network traffic
- **ResourceQuota**: `java-security-quota` - Limits namespace resource consumption
- **ConfigMap**: `java-security-config` - Configuration values
- **Secret**: `java-security-db-credentials` - Database credentials
- **ServiceAccount**: `java-security` - RBAC identity
- **Role & RoleBinding**: RBAC permissions for the service account

## Managing the Deployment

### View Status

```bash
# Deployment status
kubectl get deployment -n java-security

# Pod replicas
kubectl get pods -n java-security

# Events
kubectl describe deployment java-security -n java-security

# Resource usage
kubectl top pods -n java-security
```

### Scaling

```bash
# Manual scaling
kubectl scale deployment java-security --replicas=5 -n java-security

# Autoscaling (enabled by default)
kubectl get hpa -n java-security
kubectl describe hpa java-security -n java-security
```

### Updating the Image

```bash
# Update image tag
kubectl set image deployment/java-security \
  java-security=docker.io/your-username/java-security:2.0.0 \
  -n java-security

# Watch the rollout
kubectl rollout status deployment/java-security -n java-security
```

### Rolling Back

```bash
# View rollout history
kubectl rollout history deployment/java-security -n java-security

# Rollback to previous version
kubectl rollout undo deployment/java-security -n java-security

# Rollback to specific revision
kubectl rollout undo deployment/java-security --to-revision=2 -n java-security
```

### View Logs

```bash
# Current logs
kubectl logs -n java-security -l app=java-security

# Tail logs
kubectl logs -n java-security -l app=java-security -f

# Logs from specific pod
kubectl logs pod-name -n java-security -f

# Previous pod logs (if crashed)
kubectl logs pod-name -n java-security --previous
```

### Port Forwarding

```bash
# Forward local port to pod
kubectl port-forward svc/java-security-internal 8080:8080 -n java-security

# Access at: http://localhost:8080
```

## Database Setup

### PostgreSQL Configuration

The application expects a PostgreSQL database with the following configuration:

```sql
CREATE DATABASE java_security;
CREATE USER java_user WITH PASSWORD 'changeme123';
GRANT ALL PRIVILEGES ON DATABASE java_security TO java_user;

-- Create tables (adjust based on your schema)
CREATE TABLE users (
  userid SERIAL PRIMARY KEY,
  username VARCHAR(255) NOT NULL,
  email VARCHAR(255)
);

CREATE TABLE transactions (
  transaction_id SERIAL PRIMARY KEY,
  amount DECIMAL(10,2),
  status VARCHAR(50),
  timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE activity_log (
  activity_id SERIAL PRIMARY KEY,
  action VARCHAR(255),
  performer VARCHAR(255),
  timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Update Database Credentials

Edit the Secret:

```bash
kubectl edit secret java-security-db-credentials -n java-security
```

Or use:

```bash
kubectl create secret generic java-security-db-credentials \
  --from-literal=db-url="jdbc:postgresql://your-host:5432/java_security" \
  --from-literal=db-username="java_user" \
  --from-literal=db-password="your-password" \
  --namespace=java-security \
  --dry-run=client -o yaml | kubectl apply -f -
```

## Monitoring and Observability

### Prometheus Metrics

The application exposes metrics on `/metrics`. Configure Prometheus:

```yaml
scrape_configs:
  - job_name: 'java-security'
    kubernetes_sd_configs:
      - role: pod
        namespaces:
          names:
            - java-security
    relabel_configs:
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
        action: keep
        regex: true
```

### Logging

View aggregated logs:

```bash
# Using kubectl
kubectl logs -n java-security -l app=java-security --tail=100

# Using a log aggregation stack (ELK, Loki, etc.)
# Configure based on your stack
```

### Health Checks

The deployment includes:

- **Liveness Probe**: Restarts pod if unhealthy (path: `/ping`)
- **Readiness Probe**: Removes pod from service if not ready (path: `/ping`)

## Security

### Network Policy

Traffic is restricted by default. Allowed connections:
- Ingress: From nginx ingress controller only
- Egress: To PostgreSQL (5432), DNS (53), HTTPS (443), and internal pods (8080)

Update `k8s/networkpolicy.yaml` to adjust rules.

### Pod Security

- Runs as non-root user (UID 1000)
- Read-only filesystem (except /tmp and /usr/local/tomcat/logs)
- No privilege escalation allowed
- Dropped all Linux capabilities

### Secrets Management

Database credentials are stored in Kubernetes Secrets. Consider:

- Use external secrets operator (ESO) for production
- Enable encryption at rest
- Implement RBAC restrictions
- Audit secret access

```bash
# View secret (base64 encoded)
kubectl get secret java-security-db-credentials -n java-security -o yaml

# Decode
kubectl get secret java-security-db-credentials -n java-security \
  -o jsonpath='{.data.db-password}' | base64 -d
```

## Troubleshooting

### Pod Won't Start

```bash
# Check pod status
kubectl describe pod <pod-name> -n java-security

# Check recent logs
kubectl logs <pod-name> -n java-security --tail=50

# Check events
kubectl get events -n java-security --sort-by='.lastTimestamp'
```

### Liveness/Readiness Probe Failures

```bash
# Check endpoint directly
kubectl port-forward svc/java-security-internal 8080:8080 -n java-security

# In another terminal:
curl http://localhost:8080/ping
```

### Database Connection Issues

```bash
# Test connection from pod
kubectl run -it --rm debug \
  --image=postgres:14 \
  --restart=Never \
  -n java-security \
  -- psql -h postgres-service -U java_user -d java_security
```

### Memory Issues

```bash
# Check resource usage
kubectl top pods -n java-security

# Check limits
kubectl describe node

# Increase limits in values.yaml and redeploy
```

## Cleanup

### Delete Entire Deployment

```bash
./scripts/delete.sh

# Or manually:
helm uninstall java-security -n java-security
kubectl delete namespace java-security
```

### Delete Specific Resources

```bash
kubectl delete pod <pod-name> -n java-security
kubectl delete svc <service-name> -n java-security
kubectl delete ingress <ingress-name> -n java-security
```

## Advanced Topics

### Custom Domains

Update `helm/values.yaml` ingress section:

```yaml
ingress:
  hosts:
    - host: myapp.example.com
      paths:
        - path: /
          pathType: Prefix
  tls:
    - secretName: myapp-tls
      hosts:
        - myapp.example.com
```

### Multi-Region Deployment

Deploy to multiple clusters:

```bash
for cluster in us-east eu-west; do
  kubectl config use-context $cluster
  helm install java-security ./helm -n java-security --create-namespace
done
```

### Backup and Restore

```bash
# Backup helm release
helm get values java-security -n java-security > backup.yaml
kubectl get all -n java-security -o yaml > backup.yaml

# Restore
helm install java-security ./helm -f backup.yaml
```

## Support

For issues or questions:
- Check the logs: `kubectl logs -n java-security -l app=java-security -f`
- Describe resources: `kubectl describe -n java-security`
- Check Kubernetes events: `kubectl get events -n java-security`
