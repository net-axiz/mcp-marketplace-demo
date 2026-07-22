## MCP Marketplace Demo ##

New project bring new sleepless nights.

## Project Overview

The MCP Marketplace Demo is a comprehensive, Kubernetes-native microservices architecture designed to showcase the Model Context Protocol (MCP). It features a distributed orchestration system that delegates tasks to specialized MCP servers (such as Git operations, SQL query analysis, and file reading) and leverages a self-hosted local Large Language Model (LLM) for processing.

By running entirely within a local Kubernetes cluster (like kind or minikube), the project ensures data privacy, avoids dependency on paid external LLM APIs, and demonstrates how AI models can interact with specialized backend tools through a standardized protocol.

## Architecture and Components

The system is composed of several independent components deployed as Kubernetes pods:

### 1. Orchestrator and Core Services

* **repoExplainer**: A Spring Boot application acting as the primary orchestration layer. It receives requests to analyze GitHub repositories, fetches the README or file tree using GitHub APIs, and orchestrates the prompt to the local LLM. It exposes an MCP Streamable HTTP endpoint as well as a legacy REST API for older clients.
* **Ollama**: A local LLM server deployed within the cluster. It provides a private inference engine (defaulting to models like llama3.2) accessible only to the internal cluster network.

### 2. Specialized MCP Servers

* **McpGit**: An MCP server dedicated to interacting with Git repositories.
* **sqlmcp**: An MCP server that explains and processes SQL queries.
* **FileReader**: An MCP server responsible for reading documentation and files from designated paths.

### 3. Clients

* **legacy-client**: A classic Java Swing desktop application (RetroApp) designed with the Metal Look and Feel. It provides a simple graphical interface to test the `repoExplainer` REST API and features a built-in, tabbed live log streaming console that separately tails Spring Boot logs and Kubernetes cluster events in real-time.

## Production Optimizations

The project is designed to be production-ready and optimized for constrained local environments:

* **Docker Multi-stage Builds:** All custom Java microservices utilize multi-stage builds. The final runtime images use lightweight alpine base images, significantly reducing memory and disk footprint. Furthermore, Maven dependencies are cached aggressively to minimize rebuild times.
* **Kubernetes Probes:** To ensure reliable orchestration, each Java microservice is equipped with a startup probe to grant sufficient initialization time, along with tightly calibrated liveness and readiness probes to maintain continuous health monitoring.

## Prerequisites

To run this project, you need the following installed on your host machine:

* Docker and Kubernetes (e.g., kind, minikube, or Docker Desktop K8s)
* kubectl
* Java 21 and Maven (for building the Java components)

## Deployment Instructions

### 1. Cluster Setup

The entire stack is configured to run inside a Kubernetes namespace named `repo-explainer`. A deployment script is provided to automate the image building and manifest application.

Execute the all-in-one run script (deploys K8s, starts port-forward, launches RetroApp):

```bash
./run.sh
```

Or, to deploy only the K8s cluster without the UI:

```bash
./deploy-k8s.sh
```

This script will build the necessary Docker images, load them into the local cluster, and apply the YAML manifests found in the `k8s/` directory sequentially.

### 2. Configuration (Secrets)

You must provide a valid GitHub Personal Access Token for the `repoExplainer` service to interact with the GitHub API.
Edit the `k8s/01-secret.yaml` file to include your base64-encoded token, then apply it:

```bash
kubectl apply -f k8s/01-secret.yaml
```

### 3. Verifying the Deployment

Check the status of the pods to ensure all services and the Ollama model pull job have completed successfully:

```bash
kubectl get pods -n repo-explainer -w
```

Wait until all deployment pods show a `1/1 Running` status.

## Testing and Usage

### Command Line Interface

A test script is available to simulate an MCP client handshake and tool invocation. To test the Streamable HTTP endpoint:

```bash
./test.sh https://github.com/OpenCut-app/OpenCut
```

### Desktop Client (RetroApp)

For a graphical interface, you can build and run the legacy Java Swing client.
First, ensure you have an active port-forward to the orchestrator service:

```bash
kubectl port-forward -n repo-explainer svc/repo-explainer-server 8084:8084 &
```

Then, build and run the desktop application:

```bash
cd legacy-client
mvn clean package
java -jar target/legacy-client-1.0-SNAPSHOT-jar-with-dependencies.jar
```

Within the application, you can check the "Debug Modunu Ac" box to view live Kubernetes logs and cluster events streaming directly into categorized tabs.
