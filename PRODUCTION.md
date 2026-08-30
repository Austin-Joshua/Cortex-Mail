# Cortex Mail — Production Deployment

Deploy the **frontend** on Vercel and the **backend** on Render. Database is **Supabase Postgres** (not MySQL). Product UI is **light-only** (no dark mode).

Repo: [Austin-Joshua/Cortex-Mail](https://github.com/Austin-Joshua/Cortex-Mail)

---

## Pre-deploy checklist

### Code
- [ ] `cd nexora/frontend && npm run build`
- [ ] `cd nexora/backend && ./mvnw -q -DskipTests package`
- [ ] No secrets in git (`.env` gitignored)

### Security
- [ ] Google OAuth app published (or test users listed)
- [ ] `JWT_SECRET` ≥ 32 characters
- [ ] `ENCRYPTION_KEY` exactly 16 characters (AES-GCM for Gmail tokens)
- [ ] `CORS_ALLOWED_ORIGINS` = production frontend origin(s) only
- [ ] `H2_CONSOLE_ENABLED=false`
- [ ] Privacy policy matches real Gmail scopes (sync + label mutations)
- [ ] Prod profile fail-fast: Postgres `DB_URL`, Google OAuth, JWT, encryption key

### Database (Supabase)
- [ ] Project active; Session pooler JDBC URL ready
- [ ] `SPRING_PROFILES_ACTIVE=prod` so Flyway runs and `ddl-auto=validate`
- [ ] Flyway V2–V5 applied (RLS, tenant FKs, oauth_exchange_codes, token_version)
- [ ] Keep `nexora/backend/.env.example` and `nexora/frontend/.env.example` tracked (no secrets)

### Runtime
- [ ] Prefer **one** backend instance — Gmail sync locks and JWT revoke registry are in-memory (JVM-local)
- [ ] OAuth callback uses opaque one-time codes (encrypted user id), never stores JWT in DB
- [ ] PWA install is optional (`vite-plugin-pwa`); WebSocket `/ws` proxy removed (no backend WS)

### Product smoke (local or staging)
- [ ] Google login
- [ ] Dashboard sync / extract check
- [ ] Inbox / Drafts / Archive
- [ ] Star / archive mutate Gmail + DB
- [ ] Cortex Score loads
- [ ] PWA / responsive layout
- [ ] Brain responds (rules-only without AI keys)
- [ ] Priority page loads `/api/priority`
- [ ] Settings → Reconnect Gmail works

---

## 1. Frontend — Vercel

1. [vercel.com](https://vercel.com) → **Add New** → **Project**
2. Import **`Austin-Joshua/Cortex-Mail`**
3. Settings:

```
Root Directory:   nexora/frontend
Framework:        Vite
Build Command:    npm run build
Output Directory: dist
```

4. Environment variables:

```
VITE_API_BASE_URL=https://YOUR-RENDER-SERVICE.onrender.com
VITE_GOOGLE_CLIENT_ID=your_google_client_id
```

No trailing slash. Do **not** append `/api`.

5. Deploy → copy the `*.vercel.app` URL.

---

## 2. Backend — Render

1. [render.com](https://render.com) → **New** → **Web Service**
2. Connect **`Austin-Joshua/Cortex-Mail`**
3. Settings (Docker, root `nexora/backend` — see `render.yaml`):

```
Name:           cortex-mail-backend
Root Directory: nexora/backend
Runtime:        Docker
```

4. Environment (required):

```
SPRING_PROFILES_ACTIVE=prod
PORT=8080
H2_CONSOLE_ENABLED=false

DB_URL=jdbc:postgresql://aws-0-REGION.pooler.supabase.com:5432/postgres?sslmode=require
DB_USERNAME=postgres.YOUR_PROJECT_REF
DB_PASSWORD=your_supabase_db_password
DB_DRIVER=org.postgresql.Driver
DB_DIALECT=org.hibernate.dialect.PostgreSQLDialect
FLYWAY_ENABLED=true

GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
GOOGLE_REDIRECT_URI=https://YOUR-RENDER-SERVICE.onrender.com/api/auth/google/callback

JWT_SECRET=...                 # ≥ 32 chars
ENCRYPTION_KEY=...             # exactly 16 chars
CORS_ALLOWED_ORIGINS=https://YOUR-FRONTEND.vercel.app

# Optional AI
GEMINI_API_KEY=
```

5. Deploy → wait until **Live**. Copy the service URL into Vercel `VITE_API_BASE_URL` and redeploy frontend if needed.

**Notes**
- Flyway creates/validates schema on boot (`application-prod.yml`).
- Do not put DB credentials on Vercel.
- RLS: app tables have RLS enabled; `anon`/`authenticated` are revoked. Spring connects as the DB role.

---

## 3. Google Cloud

1. Enable **Gmail API** and **Google Calendar API**
2. OAuth Web client redirect URIs:
   - `http://localhost:8080/api/auth/google/callback`
   - `https://YOUR-RENDER-SERVICE.onrender.com/api/auth/google/callback`
3. Authorized JavaScript origins: local `http://localhost:5173` and the Vercel origin
4. Scopes in use include mail modify (read/star/archive/trash). Keep privacy copy honest.

---

## 4. Post-deploy smoke

1. Open Vercel URL → **Sign in with Google**
2. Dashboard → run sync → extract check OK
3. Confirm rows in Supabase `emails`
4. Star/archive one message → verify in Gmail
5. Hit `https://YOUR-RENDER/actuator/health` → `UP`

---

## Local development

```bash
# Backend (with nexora/backend/.env — often prod profile + Supabase)
cd nexora/backend
.\mvnw.cmd spring-boot:run

# Frontend
cd nexora/frontend
npm run dev
```

Default without DB vars: H2 in-memory (`application.yml`). With `SPRING_PROFILES_ACTIVE=prod` + `DB_*`: Supabase.

---

## Out of scope (not required for this deploy)

- Gmail Pub/Sub Watch (polling/scheduler only today)
- Full Gmail search parity
- Brain RAG / embeddings
- Package rename `com.nexora` → Cortex
