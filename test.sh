#!/bin/bash
# MCP Streamable HTTP Test Script
# Usage: ./test.sh [github_repo_url]
# Requires: kubectl port-forward -n repo-explainer svc/repo-explainer-server 8084:8084

BASE_URL="http://localhost:8084/mcp"
REPO_URL="${1:-https://github.com/JustVugg/colibri}"

echo "============================================"
echo "  MCP Streamable HTTP Test"
echo "============================================"
echo ""

# Step 1: Initialize
echo "[1/4] Starting session..."
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
  echo "ERROR: Could not get Session ID!"
  echo "$RESPONSE"
  exit 1
fi

echo "  Session ID: $SESSION_ID"
echo ""

# Step 2: Send initialized notification
echo "[2/4] Sending initialized notification..."
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -H "Mcp-Session-Id: $SESSION_ID" \
  -d '{"jsonrpc":"2.0","method":"notifications/initialized"}' > /dev/null

echo "  Notification sent"
echo ""

# Step 3: List tools
echo "[3/4] Listing registered tools..."
TOOLS=$(curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -H "Mcp-Session-Id: $SESSION_ID" \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}')

# Extract JSON from SSE event format
TOOLS_JSON=$(echo "$TOOLS" | grep "^data:" | sed 's/^data://')
if [ -n "$TOOLS_JSON" ]; then
  echo "$TOOLS_JSON" | python3 -m json.tool 2>/dev/null || echo "$TOOLS_JSON"
else
  echo "$TOOLS" | python3 -m json.tool 2>/dev/null || echo "$TOOLS"
fi
echo ""

# Step 4: Call explainRepo
echo "[4/4] Calling explainRepo: $REPO_URL"
echo "  (Ollama LLM call, may take 30-90 seconds...)"
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
echo "  RESULT"
echo "============================================"
# Extract JSON from SSE event format
RESULT_JSON=$(echo "$RESULT" | grep "^data:" | sed 's/^data://')
if [ -n "$RESULT_JSON" ]; then
  echo "$RESULT_JSON" | python3 -m json.tool 2>/dev/null || echo "$RESULT_JSON"
else
  echo "$RESULT" | python3 -m json.tool 2>/dev/null || echo "$RESULT"
fi
