# Cortex Mail

AI-assisted **Gmail attention layer**: sync mail, score inbox load (Cortex Score), classify, and surface what needs action. Web + PWA only — no native mobile client.

**Repo:** [Austin-Joshua/Cortex-Mail](https://github.com/Austin-Joshua/Cortex-Mail)  
**Code root:** `nexora/` (historical package name `com.nexora`)

| | |
|---|---|
| Frontend | React 19 + TypeScript + Vite + PWA |
| Backend | Spring Boot 3 + Java 17 |
| Data (local default) | H2 in-memory |
| Data (prod / current local `.env`) | Supabase Postgres + Flyway |
| Deploy | Vercel (frontend) + Render (backend) |

## Layout

```
nexora/
├── frontend/   React app (Vite) — :5173
└── backend/    Spring Boot API — :8080
```

## Quick start

```bash
# Backend
cd nexora/backend
cp .env.example .env   # fill Google OAuth + secrets; optional DB_* for Supabase
.\mvnw.cmd spring-boot:run

# Frontend
cd nexora/frontend
cp .env.example .env   # VITE_API_BASE_URL=http://localhost:8080
npm install
npm run dev
```

Open http://localhost:5173 → Sign in with Google → Dashboard sync.

Production deploy notes (single backend instance, Flyway V2–V5, env checklist): [PRODUCTION.md](./PRODUCTION.md).

## What works now (Tranches 1–2)

- Gmail OAuth + full / incremental sync (`history.list`)
- Sanitized HTML bodies + attachment records
- Gmail-first mutations (read, star, archive, trash)
- Explainable Cortex Score on the dashboard
- Classification (rules; Gemini if key set)
- Supabase schema via Flyway + RLS (Spring DB role only)

## Docs

- [PRODUCTION.md](./PRODUCTION.md) — Vercel, Render, and Supabase deployment
- [VERIFICATION_RUNBOOK.md](./VERIFICATION_RUNBOOK.md) — build and runtime checks

## Not done yet

Pub/Sub Watch, Gmail search parity, Brain RAG, and live production deployment remain deferred.
