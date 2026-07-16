#!/bin/bash
# MCP Streamable HTTP Test Script
# Kullanım: ./test-k8s.sh [explainRepo_url]
#
# Önce port-forward açık olmalı:
#   kubectl port-forward -n repo-explainer svc/repo-explainer-server 8084:8084

BASE_URL="http://localhost:8084/mcp"
REPO_URL="${1:-https://github.com/JustVugg/colibri}"

echo "============================================"
echo "  MCP Streamable HTTP Test"
echo "============================================"
echo ""

# Adım 1: Initialize — Session ID al
echo "[1/4] Session başlatılıyor..."
RESPONSE=$(curl -s -i -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -d '{
    "jsonrpc":"2.0",
    "id":1,
    "method":"initialize",
    "params":{
      "protocolVersion":"2024-11-05",
      "clientInfo":{"name":"test-cli","version":"1.0"}
    }
  }')

SESSION_ID=$(echo "$RESPONSE" | grep -i "Mcp-Session-Id" | tr -d '\r' | awk '{print $2}')

if [ -z "$SESSION_ID" ]; then
  echo "HATA: Session ID alınamadı!"
  echo "$RESPONSE"
  exit 1
fi

echo "  ✅ Session ID: $SESSION_ID"
echo ""

# Adım 2: Initialized bildirimi
echo "[2/4] Initialized bildirimi gönderiliyor..."
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -H "Mcp-Session-Id: $SESSION_ID" \
  -d '{"jsonrpc":"2.0","method":"notifications/initialized"}' > /dev/null

echo "  ✅ Bildirim gönderildi"
echo ""

# Adım 3: Tool listesi
echo "[3/4] Kayıtlı tool'lar listeleniyor..."
TOOLS=$(curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -H "Mcp-Session-Id: $SESSION_ID" \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}')

# SSE event formatından JSON'u çıkar
TOOLS_JSON=$(echo "$TOOLS" | grep "^data:" | sed 's/^data://')
if [ -n "$TOOLS_JSON" ]; then
  echo "$TOOLS_JSON" | python3 -m json.tool 2>/dev/null || echo "$TOOLS_JSON"
else
  echo "$TOOLS" | python3 -m json.tool 2>/dev/null || echo "$TOOLS"
fi
echo ""

# Adım 4: explainRepo çağrısı
echo "[4/4] explainRepo çağrılıyor: $REPO_URL"
echo "  (Ollama LLM çağrısı yapılacak, 30-90 saniye sürebilir...)"
echo ""

RESULT=$(curl -s --max-time 600 -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -H "Mcp-Session-Id: $SESSION_ID" \
  -d "{
    \"jsonrpc\":\"2.0\",
    \"id\":3,
    \"method\":\"tools/call\",
    \"params\":{
      \"name\":\"explainRepo\",
      \"arguments\":{
        \"githubUrl\":\"$REPO_URL\"
      }
    }
  }")

echo "============================================"
echo "  SONUÇ"
echo "============================================"
# SSE event formatından JSON'u çıkar
RESULT_JSON=$(echo "$RESULT" | grep "^data:" | sed 's/^data://')
if [ -n "$RESULT_JSON" ]; then
  echo "$RESULT_JSON" | python3 -m json.tool 2>/dev/null || echo "$RESULT_JSON"
else
  echo "$RESULT" | python3 -m json.tool 2>/dev/null || echo "$RESULT"
fi
