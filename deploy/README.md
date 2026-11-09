# Kubernetes and GitOps deployment

These manifests are a starter deployment, not a production certification. They deploy the API, Angular frontend, FastAPI service and a single PostgreSQL StatefulSet with persistent storage. Configure a real ingress/TLS, backups, network policies, autoscaling, monitoring and a managed secret provider before exposing this stack publicly.

## Prerequisites

- A Kubernetes cluster with a default StorageClass.
- Container images published to GHCR under the names referenced by the Deployments. Replace the image names/tags if your registry differs.
- kubectl and Kustomize (built into modern kubectl).
- Flux CD installed if you want GitOps reconciliation.

## Create the required secret

Do not commit credentials to Git. Create the secret in the devpulse namespace before applying the manifests:

    kubectl create namespace devpulse
    kubectl -n devpulse create secret generic devpulse-secrets --from-literal=DB_PASSWORD='REPLACE_WITH_A_LONG_RANDOM_DATABASE_PASSWORD' --from-literal=JWT_SECRET='REPLACE_WITH_A_RANDOM_SECRET_AT_LEAST_32_CHARACTERS'

The backend and PostgreSQL both reference this secret. Use your cluster secret manager (External Secrets, Sealed Secrets, SOPS, etc.) for a durable setup rather than storing plaintext values in shell history.

## Apply manually

    kubectl apply -k deploy/k8s/base
    kubectl -n devpulse get pods,services,pvc

The frontend Service is ClusterIP. Add an Ingress or Gateway with TLS and set CORS_ALLOWED_ORIGINS in deploy/k8s/base/configmap.yaml to the exact public frontend origin before using it.

## Enable Flux GitOps

After installing Flux controllers, apply the source and reconciliation objects:

    kubectl apply -f deploy/flux/gitrepository.yaml
    flux get sources git -n flux-system
    flux get kustomizations -n flux-system

The example tracks the repository's modernize/p1-quality branch. Change spec.ref.branch to the branch you intend to deploy after merging/releasing. The Flux Kustomization reconciles deploy/k8s/base; it does not create the devpulse-secrets secret.

## Operational caveats

- PostgreSQL is a single replica and needs tested backups/restore procedures.
- Image tags are placeholders until a CI workflow publishes the three images to GHCR.
- There is no Ingress, TLS certificate automation, NetworkPolicy, HPA or PodDisruptionBudget in this starter.
- Actuator health endpoints are used for probes. Prometheus scraping is configured in the Docker Compose observability stack; cluster-level metrics and dashboards still need to be wired to your cluster monitoring.
