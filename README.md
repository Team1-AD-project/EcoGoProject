# EcoGo — Green Mobility Platform (Backend + Android + Web + Chatbot)

EcoGo is a green mobility platform that combines **trip tracking**, **eco-points & badges**, and an **AI chatbot** for Singapore localized green-travel Q&A and guided actions (e.g., trip booking / deep links / notifications).

This repository contains:
- **Java Backend** (`Backend/`): Spring Boot API, MongoDB persistence, JWT security, and an integrated mobile chatbot endpoint (RAG/tool orchestration supported).
- **Android App** (`Android/`): mobile client for EcoGo features and chatbot UI.
- **Web Admin/Management UI** (`Web/`): React + TypeScript + Vite based management system.
- **Python Chatbot Stack** (`chatbot/`, optional): FastAPI backend + RAG knowledge base (FAISS) + model training/serving utilities.

## Key Features

- **User, points, badges, orders, activities** (Java backend)
- **Trip APIs** (start/complete/cancel/history) and navigation history integration
- **Chatbot (mobile)**:
  - Singapore green-travel **knowledge Q&A** with citations (RAG)
  - Multi-intent orchestration (booking / bus queries / user update with RBAC + audit logs)
  - UI actions for the app (suggestions / forms / confirmations / deep links)
- **Observability & tooling**: health checks, logs, local test guide, monitoring stack (Prometheus/Grafana) and performance testing assets

## Repository Structure

```text
.
├── Backend/                 # Spring Boot backend (MongoDB, JWT, chatbot API)
├── Android/                 # Android app (EcoGo)
├── Web/                     # React + TS + Vite web management UI
├── chatbot/                 # Optional Python chatbot stack (FastAPI/RAG/model)
├── monitoring/              # Prometheus/Grafana docker-compose
├── performance-tests/       # Load testing assets
├── scripts/                 # Local verification/test scripts
├── STARTUP_GUIDE.md         # Windows one-click startup scripts guide
├── SETUP_GUIDE.md           # Backend + Android setup notes
└── docs/                    # Architecture diagrams, IEEE paper, extra docs
```

## Quick Start (Windows)

The fastest way to run the core services locally is the all-in-one script:

```powershell
.\start-all-services.ps1
```

What it does (configurable via flags):
- Starts an **SSH tunnel for MongoDB** (if enabled)
- Starts the **Python FastAPI service** (if enabled)
- Starts the **Java Spring Boot backend**

See `STARTUP_GUIDE.md` for details and flags like `-SkipSSH`, `-SkipPython`, `-SkipJava`.

## Manual Start (Core Services)

### 1) MongoDB

Option A (local Docker):

```bash
docker run -d -p 27017:27017 --name ecogo-mongo mongo:latest
```

Option B (SSH tunnel to a remote MongoDB):
- Use the SSH command in `STARTUP_GUIDE.md` (requires a PEM key and network access).

### 2) Java Backend (Spring Boot)

```bash
cd Backend
.\mvnw.cmd spring-boot:run
```

Default endpoints:
- Health: `http://localhost:8090/actuator/health`

### 3) (Optional) Python FastAPI (Chatbot stack)

If you want to run the Python service:

```bash
.\start-python-backend.bat
```

Or manually:

```powershell
cd chatbot\backend
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

Docs: `http://127.0.0.1:8000/docs`

## Web Admin UI (Vite)

```bash
cd Web
npm install
npm run dev
```

## Android App

Open `Android/android-app` in Android Studio and run on emulator/device.

Note: If running the backend locally, the Android client may need the correct base URL:
- Emulator → `http://10.0.2.2:8090/`
- Physical device → your LAN IP (e.g. `http://192.168.x.x:8090/`)

See `SETUP_GUIDE.md` for the exact file to change.

## Ports

| Service | Default Port | Notes |
|---|---:|---|
| Java Backend (Spring Boot) | 8090 | Core API + health endpoint |
| Python FastAPI (optional) | 8000 | Chatbot stack / model tooling |
| MongoDB | 27017 | Local or via SSH tunnel |
| Web UI (Vite) | 5173 | Default Vite dev server port |

## Documentation Index

- **Startup & setup**
  - `STARTUP_GUIDE.md` — one-click startup scripts (Windows)
  - `SETUP_GUIDE.md` — backend + Android setup and common issues
- **Chatbot**
  - `CHATBOT_ECOGO_INTEGRATION_PLAN.md` — architecture & integration plan (Java/Python/Android, JWT, MongoDB)
  - `docs/Architecture_Diagrams.md` — architecture diagrams (Mermaid/TikZ)
- **Trip & navigation**
  - `TRIP_API_COMPLETE_GUIDE.md` — trip API integration guide
  - `MAP_ACTIVITY_API_INTEGRATION.md` — MapActivity integration notes
- **Testing**
  - `LOCAL-TEST-GUIDE.md` — local end-to-end verification workflow
  - `PERFORMANCE-TESTING.md` — performance testing notes

## Notes

- **Secrets / keys**: do not commit private keys or tokens. Prefer environment variables and local-only config files.
- **MongoDB host**: if you use a remote MongoDB, update the connection URI in `Backend/src/main/resources/application.yaml`.
