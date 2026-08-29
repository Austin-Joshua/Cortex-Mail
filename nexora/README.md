# Cortex Mail (`nexora/`)

Application code for **Cortex Mail**. Folder/package name `nexora` / `com.nexora` is historical.

| Layer | Stack |
|-------|--------|
| Frontend | React 19 + TypeScript + Vite + Tailwind + PWA |
| Backend | Spring Boot 3 + Java 17 |
| Database | H2 (default) / **Supabase Postgres** (`SPRING_PROFILES_ACTIVE=prod` + Flyway) |
| Email | Gmail API (sync + label mutations: read/star/archive/trash) |
| AI | Optional Gemini / Claude; keyword fallback |
| Calendar | Google Calendar API |

## Quick start

```bash
# Backend — http://localhost:8080
cd backend
cp .env.example .env
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run

# Frontend — http://localhost:5173
cd ../frontend
cp .env.example .env
npm install && npm run dev
```

## Google OAuth

1. Enable Gmail API + Calendar API in Google Cloud
2. OAuth Web client redirect: `http://localhost:8080/api/auth/google/callback`
3. Put Client ID/Secret in `backend/.env`; Client ID in `frontend/.env` as `VITE_GOOGLE_CLIENT_ID`

## Features

- Gmail full + incremental sync, drafts, capped archive
- Cortex Score + category classification
- Brain Q&A over recent mail (not RAG yet)
- Analytics volume charts
- Responsive web / PWA (no Flutter client)

## Deploy

See root [PRODUCTION.md](../PRODUCTION.md) (Vercel + Render + Supabase).

## Privacy note

The app requests Gmail scopes that allow **label/mailbox mutations** (archive, star, trash), not read-only. Keep product and privacy copy aligned.
