# Payment Risk Dashboard

[![CI](https://github.com/madhumitham/payment-dashboard/actions/workflows/ci.yml/badge.svg)](https://github.com/madhumitham/payment-dashboard/actions/workflows/ci.yml)

A full-stack fintech application combining microservices architecture, ML-powered risk scoring, and GenAI explainability — built to demonstrate how modern payment platforms can make AI-driven decisions transparent and queryable.

Built with **Spring Boot** · **MongoDB** · **React** · **Docker** · **Groq AI (Llama 3.1)** · **Python FastAPI (ML)**

---

## Architecture

```
┌──────────────────────────────────────────────┐
│              React Frontend  :3000            │
│  Dashboard · Transactions · AI Query tab      │
└───┬──────────────────────────────┬───────────┘
    │ REST                         │ REST
┌───▼────────────┐       ┌─────────▼──────────┐
│  Transaction   │       │   Intelligence      │
│  Service :8082 │       │   Service :8084     │
└───┬────────────┘       └─────────┬───────────┘
    │ REST                         │ HTTPS
┌───▼────────────┐       ┌─────────▼───────────┐
│  Risk Service  │──────▶│   Groq API           │
│  :8083         │  REST │  (Llama 3.1)         │
└───┬────────────┘       └─────────────────────┘
    │ REST
┌───▼────────────┐
│  Python FastAPI│  :8000  LSTM Autoencoder
└───┬────────────┘
    │
┌───▼────────────┐
│  User Service  │  :8081
└───┬────────────┘
    │
┌───▼────────────┐
│    MongoDB     │  :27017
└────────────────┘
```

**Risk Service** — hybrid scoring: rule-based (40%) + LSTM Autoencoder (60%), graceful fallback if ML API is down.

**Intelligence Service** — GenAI layer powered by Groq (Llama 3.1):
- `GET /api/intelligence/explain/{id}` — plain-English explanation of why a transaction was flagged
- `POST /api/intelligence/query` — natural language → structured filters → transaction search

---

## Quick Start

**Prerequisites:** Docker + Docker Compose + a free Groq API key ([get one at console.groq.com](https://console.groq.com) — no credit card required)

```bash
git clone https://github.com/madhumitham/payment-dashboard.git
cd payment-dashboard
GROQ_API_KEY=your_key_here docker compose up --build
```

Open **http://localhost:3000**

> The Python ML API is optional. If not running, the Risk Service gracefully falls back to rule-based scoring.

---

## Services

### User Service — `localhost:8081`

Manages user accounts.

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/users` | Create user |
| GET | `/api/users` | List all users |
| GET | `/api/users/{id}` | Get user by ID |
| GET | `/api/users/email/{email}` | Get user by email |
| PATCH | `/api/users/{id}/balance` | Update balance |
| PATCH | `/api/users/{id}/status` | Suspend/activate account |
| DELETE | `/api/users/{id}` | Delete user |
| GET | `/api/users/health` | Health check |

**Example — create user:**
```bash
curl -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice","email":"alice@example.com","accountType":"PERSONAL","balance":5000.0}'
```

---

### Transaction Service — `localhost:8082`

Records payments and triggers risk assessment on every transaction.

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/transactions` | Submit transaction (triggers risk scoring) |
| GET | `/api/transactions` | List all transactions |
| GET | `/api/transactions/{id}` | Get transaction by ID |
| GET | `/api/transactions/sender/{id}` | Transactions by sender |
| GET | `/api/transactions/flagged` | All HIGH-risk flagged transactions |
| GET | `/api/transactions/stats` | Counts by status |
| GET | `/api/transactions/search` | Search with filters (status, riskLevel, type, amountMin, amountMax) |
| GET | `/api/transactions/health` | Health check |

**Example — submit a transaction:**
```bash
curl -X POST http://localhost:8082/api/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "senderId": "user_001",
    "receiverId": "user_002",
    "amount": 15000.00,
    "type": "TRANSFER",
    "currency": "SGD",
    "description": "Large wire transfer"
  }'
```

Response includes `riskScore`, `riskLevel`, and `status` (COMPLETED or FLAGGED).

---

### Risk Service — `localhost:8083`

Hybrid rule-based + ML risk scoring engine.

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/risk/assess` | Assess transaction risk |
| GET | `/api/risk/stats` | Total assessments and high-risk count |
| GET | `/api/risk/health` | Health check |

**Scoring logic:**
```
Rule-based score (40%)
  + ML model score (60%)   ← calls Python LSTM Autoencoder
  = Final risk score (0.0 – 1.0)

< 0.4  → LOW   → Transaction COMPLETED
0.4–0.7 → MEDIUM → Transaction COMPLETED
> 0.7  → HIGH  → Transaction FLAGGED (auto-blocked)
```

The ML API call is non-blocking — if the Python service is down, the system continues with rule-based scoring only.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 18, Axios, nginx |
| Backend | Spring Boot 3.2, Java 17 (4 microservices) |
| GenAI | Groq API — Llama 3.1 8B (free tier) — transaction explainability + NL query |
| Database | MongoDB 7.0 |
| ML API | Python FastAPI + LSTM Autoencoder |
| Containerisation | Docker, Docker Compose |
| CI/CD | GitHub Actions |
| Build | Maven, npm |

---

## ML Integration

The Risk Service connects to a separately deployed Python FastAPI that runs an LSTM Autoencoder trained on transaction sequences for anomaly detection.

**Expected Python API contract (`POST /predict`):**
```json
// Request
{
  "window": [[0.15], [0.15], ...],   // 30-step normalised feature window
  "threshold": 6.28
}

// Response
{
  "anomaly_score": 3.7,
  "is_anomaly": false
}
```

To run the full stack with ML scoring:
```bash
# Start the Python ML API on port 8000
cd ../anomaly-detection
uvicorn app:app --port 8000

# Then start this project
docker compose up --build
```

---

## Local Development (without Docker)

Run each service independently against a local MongoDB instance.

```bash
# 1. Start MongoDB
docker compose up mongodb -d

# 2. Run user-service
cd user-service && mvn spring-boot:run

# 3. Run risk-service
cd risk-service && mvn spring-boot:run

# 4. Run transaction-service
cd transaction-service && mvn spring-boot:run

# 5. Run frontend
cd frontend && npm install && npm start
```

---

## CI/CD

GitHub Actions runs on every push to `main`:

- Builds all 4 Spring Boot services with Maven (risk-service and intelligence-service run tests)
- Builds the React frontend with npm
- Validates `docker-compose.yml` syntax

See [`.github/workflows/ci.yml`](.github/workflows/ci.yml).

---

## Project Structure

```
payment-dashboard/
├── docker-compose.yml
├── user-service/          # Spring Boot — account management
├── transaction-service/   # Spring Boot — payment processing + risk trigger
├── risk-service/          # Spring Boot — hybrid ML/rule risk scoring
├── intelligence-service/  # Spring Boot — Groq LLM explainability + NL query
└── frontend/              # React — dashboard + AI query interface
```

---

## Related Projects

- [anomaly-detection](https://github.com/madhumitham/anomaly-detection) — LSTM Autoencoder for transaction anomaly detection (Python/PyTorch)
# payment-risk-dashboard
