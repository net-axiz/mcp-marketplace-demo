#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# Cleanup function (called via trap)
# ─────────────────────────────────────────────
PF_PID=""

cleanup() {
  echo ""
  echo "Cleaning up..."
  if [ -n "$PF_PID" ] && kill -0 "$PF_PID" 2>/dev/null; then
    kill "$PF_PID" 2>/dev/null || true
    echo "  -> Port-forward process terminated (PID: $PF_PID)"
  fi
  echo "Done."
}

trap cleanup EXIT INT TERM

# ─────────────────────────────────────────────
# 1. Start Kubernetes cluster
# ─────────────────────────────────────────────
echo "Starting K8s cluster and services..."
./deploy-k8s.sh

# ─────────────────────────────────────────────
# 2. Start port forwarding in background
# ─────────────────────────────────────────────
echo ""
echo "Starting port forwarding (8084:8084)..."
kubectl port-forward -n repo-explainer svc/repo-explainer-server 8084:8084 > /dev/null 2>&1 &
PF_PID=$!

# Verify port-forward is active
echo "Verifying port-forward connection..."
for i in $(seq 1 10); do
  if curl -s --max-time 2 http://localhost:8084 > /dev/null 2>&1; then
    echo "  -> Port-forward active!"
    break
  fi
  if [ "$i" -eq 10 ]; then
    echo "  -> WARNING: Port-forward could not be verified, continuing anyway..."
  fi
  sleep 1
done

# ─────────────────────────────────────────────
# 3. Build and launch the UI
# ─────────────────────────────────────────────
echo ""
echo "Starting UI (RetroApp)..."
cd "$SCRIPT_DIR/legacy-client"

MVN_CMD="mvn"
if ! command -v mvn >/dev/null 2>&1; then
  if [ -n "$SUDO_USER" ] && [ -f "/home/$SUDO_USER/.sdkman/candidates/maven/current/bin/mvn" ]; then
    MVN_CMD="/home/$SUDO_USER/.sdkman/candidates/maven/current/bin/mvn"
  elif [ -f "$HOME/.sdkman/candidates/maven/current/bin/mvn" ]; then
    MVN_CMD="$HOME/.sdkman/candidates/maven/current/bin/mvn"
  elif [ -f "/home/ub_lahmac/.sdkman/candidates/maven/current/bin/mvn" ]; then
    MVN_CMD="/home/ub_lahmac/.sdkman/candidates/maven/current/bin/mvn"
  fi
fi

$MVN_CMD clean package -q -DskipTests

# Launch GUI app with X11 support
if [ -n "$SUDO_USER" ] && [ "$SUDO_USER" != "root" ]; then
  # Copy kubeconfig to original user if running as root
  mkdir -p "/home/$SUDO_USER/.kube"
  if [ -f "/root/.kube/config" ]; then
    cp "/root/.kube/config" "/home/$SUDO_USER/.kube/config"
    chown -R "$SUDO_USER:$SUDO_USER" "/home/$SUDO_USER/.kube"
  fi

  # Launch UI as original user with X11 env
  sudo -u "$SUDO_USER" bash -c "export DISPLAY=\"$DISPLAY\"; export XAUTHORITY=\"$XAUTHORITY\"; cd '$SCRIPT_DIR/legacy-client' && java -jar target/legacy-client-1.0-SNAPSHOT-jar-with-dependencies.jar"
else
  java -jar target/legacy-client-1.0-SNAPSHOT-jar-with-dependencies.jar
fi
