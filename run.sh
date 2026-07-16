#!/bin/bash
docker-compose down
docker-compose up -d --build

echo "Waiting for ollama server to start..."
sleep 5
echo "Pulling llama3.2 model..."
docker exec ollama-server ollama pull llama3.2
echo "Model pulled and ready!"
