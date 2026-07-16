#!/bin/bash
set -e

echo "========================================="
echo "  BÖLÜM 2: Kind Cluster Kurulumu"
echo "========================================="

# Mevcut cluster varsa sil (temiz başlangıç)
echo "[1/3] Mevcut 'repo-explainer' cluster kontrol ediliyor..."
if kind get clusters 2>/dev/null | grep -q "repo-explainer"; then
  echo "  -> Mevcut cluster siliniyor..."
  kind delete cluster --name repo-explainer
fi

echo "[2/3] Kind cluster oluşturuluyor..."
kind create cluster --name repo-explainer

echo "[3/3] Cluster doğrulanıyor..."
kubectl cluster-info
kubectl get nodes

echo ""
echo "========================================="
echo "  BÖLÜM 3: Image Build & Load"
echo "========================================="

echo "[1/5] mcp-git image build ediliyor..."
docker build -t mcp-git-image:latest ./McpGit

echo "[2/5] sqlmcp image build ediliyor..."
docker build -t sqlmcp-image:latest ./sqlmcp

echo "[3/5] file-reader image build ediliyor..."
docker build -t file-reader-image:latest ./FileReader

echo "[4/5] repo-explainer image build ediliyor..."
docker build -t repo-explainer-image:latest ./repoExplainer

echo "[5/5] Image'lar kind cluster'a yükleniyor..."
kind load docker-image mcp-git-image:latest --name repo-explainer
kind load docker-image sqlmcp-image:latest --name repo-explainer
kind load docker-image file-reader-image:latest --name repo-explainer
kind load docker-image repo-explainer-image:latest --name repo-explainer

echo ""
echo "========================================="
echo "  BÖLÜM 5: K8s Manifest Apply"
echo "========================================="

echo "[1/7] Namespace oluşturuluyor..."
kubectl apply -f k8s/00-namespace.yaml

echo "[2/7] Secret oluşturuluyor..."
kubectl apply -f k8s/01-secret.yaml

echo "[3/7] ConfigMap oluşturuluyor..."
kubectl apply -f k8s/02-configmap.yaml

echo "[4/7] mcp-git deploy ediliyor..."
kubectl apply -f k8s/03-mcp-git.yaml

echo "[5/7] sqlmcp deploy ediliyor..."
kubectl apply -f k8s/04-sqlmcp.yaml

echo "[6/7] file-reader deploy ediliyor..."
kubectl apply -f k8s/05-file-reader.yaml

echo "[7/7] Ollama deploy ediliyor..."
kubectl apply -f k8s/07-ollama.yaml

echo ""
echo "Ollama pod'unun Running olması bekleniyor..."
kubectl wait --for=condition=Ready pod -l app=ollama -n repo-explainer --timeout=120s

echo ""
echo "========================================="
echo "  Ollama Model Pull Job"
echo "========================================="

echo "Model pull Job başlatılıyor..."
kubectl apply -f k8s/08-ollama-model-pull-job.yaml

echo "Model indirme Job'ı tamamlanması bekleniyor (bu birkaç dakika sürebilir)..."
kubectl wait --for=condition=complete job/ollama-model-pull -n repo-explainer --timeout=600s

echo "Job tamamlandı! Loglar:"
kubectl logs -n repo-explainer job/ollama-model-pull

echo ""
echo "========================================="
echo "  repo-explainer Deploy"
echo "========================================="

echo "repo-explainer deploy ediliyor..."
kubectl apply -f k8s/06-repo-explainer.yaml

echo "repo-explainer pod'unun Running olması bekleniyor..."
kubectl wait --for=condition=Ready pod -l app=repo-explainer -n repo-explainer --timeout=120s

echo ""
echo "========================================="
echo "  BÖLÜM 6: Doğrulama Testleri"
echo "========================================="

echo ""
echo "--- Test 1: Tüm Pod'lar ---"
kubectl get pods -n repo-explainer

echo ""
echo "--- Test 2: MCP Git Server JSON-RPC ---"
kubectl exec -n repo-explainer deploy/mcp-git -- \
  wget -qO- --post-data='{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","clientInfo":{"name":"test","version":"1.0"}}}' \
  --header='Content-Type: application/json' \
  http://localhost:8081/mcp 2>&1 || echo "UYARI: Test 2 başarısız"

echo ""
echo "--- Test 3: repo-explainer Logları ---"
kubectl logs -n repo-explainer deployment/repo-explainer --tail=50 | grep -i "tool\|client\|mcp\|ollama" || echo "İlgili log satırı bulunamadı"

echo ""
echo "--- Test 4: 30 saniye sonra crash-loop kontrolü ---"
echo "(30 saniye bekleniyor...)"
sleep 30
kubectl get pods -n repo-explainer | grep repo-explainer

echo ""
echo "========================================="
echo "  TAMAMLANDI!"
echo "========================================="
echo ""
echo "Test 5 (dışarıdan erişim) için ayrı bir terminalde şu komutu çalıştır:"
echo "  kubectl port-forward -n repo-explainer svc/repo-explainer-server 8084:8084"
echo ""
echo "Sonra başka bir terminalde:"
echo '  curl -X POST http://localhost:8084/mcp \'
echo '    -H "Content-Type: application/json" \'
echo "    -d '{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2024-11-05\",\"clientInfo\":{\"name\":\"test\",\"version\":\"1.0\"}}}'"
