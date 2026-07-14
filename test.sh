#!/bin/bash
curl -s -X POST http://localhost:8084/mcp -H "Content-Type: application/json" -H "Accept: application/json, text/event-stream" -d '{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"explainRepo","arguments":{"githubUrl":"https://github.com/JustVugg/colibri"}}}' > result.json
cat result.json
