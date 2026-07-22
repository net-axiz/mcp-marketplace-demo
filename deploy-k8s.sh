#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# Colors and helpers
# ─────────────────────────────────────────────
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

section() {
  echo ""
  echo -e "${CYAN}=========================================${NC}"
  echo -e "${CYAN}  $1${NC}"
  echo -e "${CYAN}=========================================${NC}"
}

step() {
  echo -e "${GREEN}[$1] $2${NC}"
}

warn() {
  echo -e "${YELLOW}[WARN] $1${NC}"
}

# Image definitions
# ─────────────────────────────────────────────
declare -A IMAGES=(
  ["mcp-git-image"]="./McpGit"
  ["sqlmcp-image"]="./sqlmcp"
  ["file-reader-image"]="./FileReader"
  ["repo-explainer-image"]="./repoExplainer"
)

CLUSTER_NAME="repo-explainer"
NAMESPACE="repo-explainer"

# SECTION 1: Kind Cluster Setup
# ─────────────────────────────────────────────
section "SECTION 1: Kind Cluster Setup"

step "1/3" "Checking for existing '$CLUSTER_NAME' cluster..."
if kind get clusters 2>/dev/null | grep -q "$CLUSTER_NAME"; then
  echo "  -> Existing cluster found, continuing..."
else
  step "2/3" "Creating kind cluster..."
  kind create cluster --name "$CLUSTER_NAME"
fi

step "3/3" "Verifying cluster..."
kubectl cluster-info
kubectl get nodes

# SECTION 2: Image Build & Load
# ─────────────────────────────────────────────
section "SECTION 2: Image Build & Load"

TOTAL=${#IMAGES[@]}
CURRENT=0

for IMAGE_NAME in "${!IMAGES[@]}"; do
  CURRENT=$((CURRENT + 1))
  BUILD_CONTEXT="${IMAGES[$IMAGE_NAME]}"
  step "$CURRENT/$TOTAL" "Building $IMAGE_NAME..."
  docker build -t "${IMAGE_NAME}:latest" "$BUILD_CONTEXT"
done

step "$TOTAL/$TOTAL" "Loading images into kind cluster..."
for IMAGE_NAME in "${!IMAGES[@]}"; do
  kind load docker-image "${IMAGE_NAME}:latest" --name "$CLUSTER_NAME"
done

# SECTION 3: K8s Manifest Apply
# ─────────────────────────────────────────────
section "SECTION 3: K8s Manifest Apply"

step "1/7" "Creating namespace..."
kubectl apply -f k8s/00-namespace.yaml

step "2/7" "Creating secret..."
# Read token dynamically from .env or .env.example
if [ -f ".env" ]; then
  ENV_FILE=".env"
elif [ -f ".env.example" ]; then
  ENV_FILE=".env.example"
fi

if [ -n "$ENV_FILE" ]; then
  # Extract GITHUB_TOKEN value (strip quotes)
  GH_TOKEN=$(grep -E "^GITHUB_TOKEN=" "$ENV_FILE" | cut -d '=' -f2- | tr -d '"' | tr -d "'" | tr -d '\r')
  
  if [ -n "$GH_TOKEN" ] && [[ "$GH_TOKEN" != *"your_"* ]]; then
    echo "  -> Updating secret with GitHub token from '$ENV_FILE'..."
    kubectl create secret generic repo-explainer-secrets \
      --namespace="$NAMESPACE" \
      --from-literal=GITHUB_TOKEN="$GH_TOKEN" \
      --dry-run=client -o yaml | kubectl apply -f -
  else
    warn "No valid GITHUB_TOKEN found (may still be a placeholder). Applying default yaml..."
    kubectl apply -f k8s/01-secret.yaml
  fi
else
  kubectl apply -f k8s/01-secret.yaml
fi

step "3/7" "Creating ConfigMap..."
kubectl apply -f k8s/02-configmap.yaml

step "4/7" "Deploying mcp-git..."
kubectl apply -f k8s/03-mcp-git.yaml

step "5/7" "Deploying sqlmcp..."
kubectl apply -f k8s/04-sqlmcp.yaml

step "6/7" "Deploying file-reader..."
kubectl apply -f k8s/05-file-reader.yaml

step "7/7" "Deploying Ollama..."
kubectl apply -f k8s/07-ollama.yaml

echo ""
echo "Waiting for Ollama pod to be Running..."
kubectl wait --for=condition=Ready pod -l app=ollama -n "$NAMESPACE" --timeout=600s

# SECTION 4: Ollama Model Pull Job
# ─────────────────────────────────────────────
section "SECTION 4: Ollama Model Pull Job"

echo "Cleaning up old model pull Job (if any)..."
kubectl delete job ollama-model-pull -n "$NAMESPACE" --ignore-not-found

echo "Starting model pull Job..."
kubectl apply -f k8s/08-ollama-model-pull-job.yaml

echo "Waiting for model download Job to complete (this may take a few minutes)..."
kubectl wait --for=condition=complete job/ollama-model-pull -n "$NAMESPACE" --timeout=600s

echo "Job completed! Logs:"
kubectl logs -n "$NAMESPACE" job/ollama-model-pull

# SECTION 5: repo-explainer Deploy
# ─────────────────────────────────────────────
section "SECTION 5: repo-explainer Deploy"

echo "Deploying repo-explainer..."
kubectl apply -f k8s/06-repo-explainer.yaml

echo "Restarting deployments to pick up new images..."
kubectl rollout restart deployment -n "$NAMESPACE" mcp-git sqlmcp file-reader repo-explainer

echo "Waiting for MCP servers to be ready..."
kubectl rollout status deployment/mcp-git -n "$NAMESPACE" --timeout=120s
kubectl rollout status deployment/sqlmcp -n "$NAMESPACE" --timeout=120s
kubectl rollout status deployment/file-reader -n "$NAMESPACE" --timeout=120s

echo "Waiting for repo-explainer to be ready..."
kubectl rollout status deployment/repo-explainer -n "$NAMESPACE" --timeout=180s

# SECTION 6: Verification
# ─────────────────────────────────────────────
section "SECTION 6: Verification"

echo ""
echo "--- All Pods ---"
kubectl get pods -n "$NAMESPACE"

echo ""
echo "--- MCP Git Server JSON-RPC ---"
kubectl exec -n "$NAMESPACE" deploy/mcp-git -- \
  wget -qO- --post-data='{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","clientInfo":{"name":"test","version":"1.0"}}}' \
  --header='Content-Type: application/json' \
  http://localhost:8081/mcp 2>&1 || warn "MCP Git test failed"

echo ""
echo "--- repo-explainer Logs ---"
kubectl logs -n "$NAMESPACE" deployment/repo-explainer --tail=30 | grep -i "tool\|client\|mcp\|ollama" || echo "No relevant log lines found"

section "DONE!"
echo ""
echo "Auto-start:"
echo "  ./run.sh"
echo ""
echo "Manual test:"
echo "  kubectl port-forward -n $NAMESPACE svc/repo-explainer-server 8084:8084"
echo "  ./test.sh [github_url]"
