# ☁️ CloudFlow

> Multi-tenant distributed job scheduling platform built with Spring Boot 3, Kafka, Redis, React, and Kubernetes.

[![CI](https://github.com/YOUR_USERNAME/cloudflow/actions/workflows/ci.yml/badge.svg)](https://github.com/YOUR_USERNAME/cloudflow/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-17-orange)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.3-green)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue)](https://reactjs.org/)
[![Docker](https://img.shields.io/badge/Docker-ready-blue)](https://hub.docker.com/)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-ready-blue)](https://kubernetes.io/)

---

# 📋 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Quick Start](#quick-start)
- [API Reference](#api-reference)
- [Kubernetes Deployment](#kubernetes-deployment)
- [Running Tests](#running-tests)
- [Project Structure](#project-structure)
- [Environment Variables](#environment-variables)
- [CI/CD Pipeline](#cicd-pipeline)
- [Minikube + GitHub Actions Notes](#minikube--github-actions-notes)
- [15-Day Build Log](#15-day-build-log)
- [License](#license)

---

# Overview

CloudFlow is a production-grade job scheduling platform that lets teams create, schedule, monitor, and alert on HTTP-based jobs across multiple tenants.

Built as a 15-day learning project covering:

- Distributed systems
- Event-driven architecture
- Modern Java backend engineering
- Cloud-native deployment
- Kubernetes orchestration
- CI/CD automation

## What it does

- Schedule HTTP jobs with Quartz cron expressions
- Execute jobs via Kafka for decoupled, reliable processing
- Retry failed jobs with configurable backoff
- Alert via webhook when jobs exceed failure thresholds
- Live dashboard updates via WebSocket
- Multi-tenant isolation
- Rate limiting per tenant via Redis
- Metrics exposed to Prometheus + Grafana

---

# Architecture

```text
┌─────────────────────────────────────────────────────────┐
│                     React Frontend                     │
│              (Vite + Zustand + React Query)            │
│                  WebSocket (STOMP/SockJS)              │
└────────────────────────┬────────────────────────────────┘
                         │ HTTP / WebSocket
┌────────────────────────▼────────────────────────────────┐
│                  Spring Boot 3.x API                   │
│ JWT Auth │ Rate Limiting │ Multi-tenancy │ Swagger UI  │
└─────┬──────┬──────┬────────┬──────────────┬─────────────┘
      │      │      │        │              │
 PostgreSQL Redis  Kafka   Quartz      WebSocket
   (data)   (cache) (events) (cron)   (live updates)
      │             │
      └─────────────┘

Job Pipeline:
Quartz → Kafka → Worker → Retry → Alert
```

---

# Features

| Feature | Details |
|---|---|
| **Multi-tenancy** | Company-level isolation via JWT + ThreadLocal TenantContext |
| **Job Scheduling** | Quartz with PostgreSQL clustering |
| **Kafka Pipeline** | Job trigger → execution → result → alert |
| **Retry Logic** | Configurable retry limit + delay |
| **Alerting** | Webhook alerts on repeated failures |
| **Rate Limiting** | Redis INCR/EXPIRE pattern |
| **JWT Blacklist** | Logout invalidates token via Redis TTL |
| **Live Dashboard** | WebSocket broadcasts job results |
| **Prometheus Metrics** | Job counts + execution durations |
| **Kubernetes** | HPA, health probes, rolling updates |
| **CI/CD** | GitHub Actions + Docker Hub |

---

# Tech Stack

## Backend

- Java 17
- Spring Boot 3.2.3
- Spring Security
- Spring Data JPA + Hibernate
- Flyway
- Quartz Scheduler
- Apache Kafka
- Redis
- Spring WebSocket
- Micrometer + Prometheus

## Frontend

- React 18
- Vite
- Zustand
- React Query
- Axios
- STOMP.js + SockJS
- Tailwind CSS
- React Hot Toast

## Infrastructure

- PostgreSQL 15
- Docker + Docker Compose
- Kubernetes
- Minikube
- GitHub Actions
- Docker Hub

---

# Quick Start

## Prerequisites

- Java 17
- Node.js 18+
- Docker Desktop
- Minikube (optional for Kubernetes)

---

## 1. Clone Repository

```bash
git clone https://github.com/YOUR_USERNAME/cloudflow.git
cd cloudflow
```

---

## 2. Start Infrastructure

```bash
docker-compose up -d
```

---

## 3. Start Backend

```bash
./mvnw spring-boot:run
```

API:

```text
http://localhost:8080
```

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

---

## 4. Start Frontend

```bash
cd cloudflow-ui
npm install
npm run dev
```

Frontend:

```text
http://localhost:5173
```

---

## 5. Register Company

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "companyName": "Acme Corp",
    "slug": "acmecorp",
    "email": "admin@acme.com",
    "password": "password123"
  }'
```

---

## 6. Login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "slug": "acmecorp",
    "email": "admin@acme.com",
    "password": "password123"
  }'
```

---

## 7. Create Job

```bash
curl -X POST http://localhost:8080/api/v1/jobs \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Health Check",
    "cronExpression": "0 * * * * ?",
    "targetUrl": "https://httpbin.org/get",
    "httpMethod": "GET",
    "timezone": "UTC",
    "timeoutSeconds": 30,
    "retryLimit": 3,
    "retryDelaySeconds": 5
  }'
```

---

# API Reference

Full interactive docs:

```text
http://localhost:8080/swagger-ui.html
```

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/auth/register` | Register company |
| POST | `/api/v1/auth/login` | Login |
| POST | `/api/v1/auth/logout` | Logout |
| GET | `/api/v1/jobs` | List jobs |
| POST | `/api/v1/jobs` | Create job |
| GET | `/api/v1/jobs/{id}` | Get job |
| PUT | `/api/v1/jobs/{id}` | Update job |
| DELETE | `/api/v1/jobs/{id}` | Delete job |
| POST | `/api/v1/jobs/{id}/trigger` | Manual trigger |
| POST | `/api/v1/jobs/{id}/pause` | Pause job |
| POST | `/api/v1/jobs/{id}/resume` | Resume job |
| GET | `/api/v1/jobs/{id}/executions` | Execution history |
| PUT | `/api/v1/jobs/{id}/alert-config` | Alert config |
| GET | `/api/v1/alerts` | Alert logs |
| GET | `/actuator/health` | Health check |
| GET | `/actuator/prometheus` | Metrics |

---

# Kubernetes Deployment

## Start Minikube

```bash
minikube start --driver=docker --memory=6144 --cpus=4
```

---

## Build Image Inside Minikube

### PowerShell

```powershell
& minikube -p minikube docker-env --shell powershell | Invoke-Expression
docker build -t cloudflow:latest .
```

---

## Deploy Resources

```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secrets.yaml
kubectl apply -f k8s/postgres/
kubectl apply -f k8s/redis/
kubectl apply -f k8s/kafka/
kubectl apply -f k8s/app/
```

---

## Get Application URL

```bash
minikube service cloudflow-service -n cloudflow --url
```

---

# Running Tests

## Unit Tests

```bash
./mvnw test \
  -Dtest="CronValidatorTest,AuthServiceTest,JobServiceTest" \
  -DfailIfNoTests=false
```

---

## All Tests

```bash
./mvnw test
```

Expected:

```text
Tests run: 18, Failures: 0, Errors: 0, Skipped: 2
```

---

# Project Structure

```text
cloudflow/
├── src/main/java/com/cloudflow/cloudflow/
│   ├── auth/
│   ├── alert/
│   ├── config/
│   ├── execution/
│   ├── job/
│   ├── kafka/
│   ├── multitenancy/
│   ├── scheduler/
│   ├── tenant/
│   ├── user/
│   ├── websocket/
│   └── worker/
│
├── src/main/resources/
│   ├── application.yml
│   ├── application-k8s.yml
│   └── db/migration/
│
├── src/test/
│
├── cloudflow-ui/
│   └── src/
│       ├── api/
│       ├── components/
│       ├── hooks/
│       ├── pages/
│       └── store/
│
├── k8s/
│   ├── namespace.yaml
│   ├── configmap.yaml
│   ├── secrets.yaml.template
│   ├── postgres/
│   ├── redis/
│   ├── kafka/
│   └── app/
│
├── .github/workflows/
│   ├── ci.yml
│   ├── cd.yml
│   └── pr-check.yml
│
├── Dockerfile
├── docker-compose.yml
└── pom.xml
```

---

# Environment Variables

| Variable | Default | Description |
|---|---|---|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5433` | PostgreSQL port |
| `DB_NAME` | `cloudflow` | Database name |
| `DB_USERNAME` | `cloudflow_user` | DB username |
| `DB_PASSWORD` | `cloudflow_pass` | DB password |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka bootstrap |
| `JWT_SECRET` | Development secret | JWT signing secret |
| `JWT_EXPIRATION_MS` | `86400000` | Token TTL |
| `RATE_LIMIT_RPM` | `100` | Requests per minute |

---

# CI/CD Pipeline

## CI Workflow

GitHub Actions pipeline:

```text
Push/PR → Unit Tests → Build JAR → Build Docker Image → Push to Docker Hub
```

### CI Features

- Maven dependency caching
- Docker layer caching
- Artifact upload/download
- Multi-stage Docker builds
- Automatic image tagging

---

## CD Workflow

Deployment workflow:

```text
Docker Hub Image → Kubernetes Deployment → Rollout Verification
```

### Kubernetes Deployment Features

- Rolling updates
- Health checks
- Automatic rollback on failure
- Namespace isolation

---

# Minikube + GitHub Actions Notes

## Important

GitHub-hosted runners cannot directly access local Minikube clusters.

Why:

```text
Minikube runs on your machine.
GitHub Actions runs on remote cloud VMs.
```

If your kubeconfig contains:

```text
https://127.0.0.1:XXXXX
```

GitHub Actions will fail because `127.0.0.1` points to the runner itself, not your machine.

---

## Recommended Solutions

### Option 1 — Self-hosted Runner (Recommended)

Run GitHub Actions directly on the same machine as Minikube.

```yaml
runs-on: self-hosted
```

---

### Option 2 — Use Managed Kubernetes

Use a cloud cluster instead of Minikube:

- GKE
- EKS
- AKS

---

## Correct Kubeconfig Generation

Generate portable kubeconfig:

```bash
kubectl config view --raw --flatten > kubeconfig.yaml
```

Encode:

```bash
base64 -w 0 kubeconfig.yaml
```

Store output in GitHub secret:

```text
KUBE_CONFIG
```


# License

MIT License — see `LICENSE` for details.