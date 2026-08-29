# Cortex Mail — Current Situation Audit

**Audit date:** 29 August 2026  
**Repo:** [Austin-Joshua/Cortex-Mail](https://github.com/Austin-Joshua/Cortex-Mail)  
**Local path:** `Cortex-Mail/`  
**Product:** AI-assisted Gmail attention layer (web + PWA). Not a Gmail replacement.

---

## Verdict (one paragraph)

Tranches **1–2 are shipped and pushed** (`76770e8` on `master`/`main`): Supabase + Flyway + RLS, Gmail full + incremental sync, sanitized HTML/attachments, Gmail-first mutations, explainable Cortex Score. Mobile is gone. Locally you have **uncommitted polish** (mock-seed removal, null-safety, HTML/CSS/favicon) plus a large untracked production prompt. **Production deploy (Render/Vercel) is not done.** Docs still mix Velocity / Nexora / MySQL / dark mode. Next work is: commit polish → E2E locally on Supabase → deploy → then optional Pub/Sub, search parity, Brain RAG.

---

## 1. Repo structure

```
Cortex-Mail/
├── README.md, PRODUCTION.md, PROJECT_REPORT.md, deployment_guide.md
├── CURRENT_SITUATION_AUDIT.md          ← this file
├── CORTEX MAIL — COMPLETE PRODUCTION…  (untracked prompt)
├── .hintrc                             (untracked)
├── .vscode/settings.json               (Maven on, Gradle off)
└── nexora/
    ├── frontend/   React 19 + Vite + TypeScript + PWA
    ├── backend/    Spring Boot 3.2 / Java 17 (com.nexora)
    └── README.md   (still titled “Nexora”)
```

| Check | Status |
|-------|--------|
| `nexora/frontend` + `nexora/backend` | Present |
| `nexora/mobile` | **Removed** (web/PWA only) |
| Gradle / Android in repo | **None** (IDE ghost Gradle project was cleaned from JDT cache) |

---

## 2. Git state

| Item | Value |
|------|--------|
| Branch | `master` → `origin/master` |
| HEAD (pushed) | `76770e8` — *Ship Tranches 1-2: Supabase Flyway, history sync, MIME, mutations, score, RLS.* |
| `origin/main` | Same commit as `master` |
| Working tree | **Dirty** — local fixes not committed |

### Uncommitted modified files

| Path | Change |
|------|--------|
| `AuthService.java` | Removed dead `seedMockEmails` |
| `DashboardSummaryResponse.java` | Unused imports cleaned |
| `EmailService.java` | Deadline sort null-safety |
| `GmailSyncService.java` | Null-safe header map |
| `index.html` | DOCTYPE / PWA meta / safer `globalThis` |
| `favicon.svg` | Cortex mark (not purple placeholder) |
| `index.css`, `landing.css`, `bento.css` | Compat prefixes, `@supports (text-wrap)`, system font |

### Untracked

| Path | Notes |
|------|--------|
| `.hintrc` | Edge Tools / webhint ignores for progressive CSS/HTML |
| `CORTEX MAIL — COMPLETE PRODUCTION IMPLEMENTATION PROMPT.md` | Full later-phase prompt (Pub/Sub, RAG, branding, deploy) — commit only if you want it in git |

**Do not commit** `nexora/backend/.env` or `nexora/frontend/.env`.

---

## 3. What works today (DONE)

### Backend

- Google OAuth + JWT + encrypted Gmail tokens  
- **Gmail sync:** full + incremental `history.list`; INBOX / DRAFTS / capped archive  
- Post-sync **classification** (rules; Gemini/Claude if keys set)  
- **MIME:** plain text + sanitized `bodyHtml`; attachment metadata records  
- **Mutations (Gmail-first):** read/unread, star, archive, inbox, trash/restore  
- **Cortex Score** (0–100, explainable factors) on dashboard summary  
- Sync integrity / extract-check endpoint  
- Scheduler (~5 min incremental)  
- Flyway **V1** baseline schema + **V2** RLS (enable RLS; revoke `anon`/`authenticated`; Spring JDBC role still used)  
- Prod profile: Postgres, `ddl-auto: validate`, Flyway on  

### Frontend

- Landing, OAuth callback, dashboard (score + pipeline), inbox / priority / drafts / archive  
- Brain, analytics, settings, email detail, PWA manifest  
- Light theme only (dark mode removed from product chrome)  

### Data / local config

| Item | Status |
|------|--------|
| `nexora/backend/.env` | Present; wired to **Supabase Postgres** with `SPRING_PROFILES_ACTIVE=prod` |
| `nexora/frontend/.env` | Present; API points at **localhost** |
| Gemini / Claude keys | Present but **empty** → rules-only classify / weak Brain |

---

## 4. What is NOT done

| Item | Reality |
|------|---------|
| **Render backend deploy** | `render.yaml` stub only; DB/profile env must be set in Render UI |
| **Vercel frontend deploy** | `vercel.json` SPA rewrite only; local env still localhost |
| **Gmail Pub/Sub Watch** | Not implemented (polling only) |
| **Gmail search parity** | Local/client filter only |
| **Brain RAG** | Last ~20 emails into LLM — no embeddings / pgvector |
| **Analytics expansion** | Basic volume charts only |
| **Branding cleanup** | UI ≈ Cortex; README = Velocity; package/docs = Nexora |
| **Privacy copy** | Still implies read-only; code uses modify scopes for mutations |
| **PRODUCTION.md refresh** | Still mentions MySQL, dark mode, wrong repo name in places |

---

## 5. Docs drift (fix when you have time)

| Doc | Issue |
|-----|--------|
| `PROJECT_REPORT.md` | Mostly accurate; §2 still says prod intent MySQL while later sections say Supabase |
| `PRODUCTION.md` | Stale: Nexora name, MySQL checklist, dark mode tests |
| Root `README.md` | Still **Velocity** |
| `nexora/README.md` | Still **Nexora**; React 18 / H2-MySQL badges outdated |
| Privacy HTML | Nexora + misleading read-only claim |

---

## 6. Local ops (after processes stopped)

```bash
# Backend — port 8080 (uses Supabase via .env prod profile)
cd nexora/backend
.\mvnw.cmd spring-boot:run

# Frontend — port 5173
cd nexora\frontend
npm run dev
```

Then: Google login → Dashboard sync pipeline → Inbox / Drafts / Archive → star/archive → Brain.

---

## 7. Exact next actions (do in this order)

### A. Ship the working tree (today)

1. Review uncommitted diffs (AuthService, Gmail/Email null-safety, frontend polish).  
2. Commit those + optional `.hintrc`. **Skip** `.env`. Decide on the big production prompt MD.  
3. Push `master` (and `main` if you keep them mirrored).

### B. Prove the stack (today / tomorrow)

4. Restart backend + frontend (clean).  
5. Manual E2E: login → sync chip / extract check → mail appears in Supabase `emails` → mutate star/archive → confirm Gmail + DB.  
6. If demo quality matters: set **Gemini or Claude** key in backend `.env` and retest classify + Brain.  
7. If the DB password was ever shared in chat: **rotate** Supabase password and update `.env` (+ Render later).

### C. Production deploy (next milestone)

8. **Render:** Web service from `nexora/backend`; set `SPRING_PROFILES_ACTIVE=prod`, all `DB_*`, Google OAuth, JWT, ENCRYPTION, CORS, redirect URI.  
9. **Vercel:** root `nexora/frontend`; `VITE_API_BASE_URL` = Render URL; `VITE_GOOGLE_CLIENT_ID`.  
10. Google Cloud Console: production redirect URIs; scopes match modify behavior.  
11. Smoke production URL end-to-end.

### D. Docs + honesty (same milestone)

12. Rewrite `PRODUCTION.md` for Cortex Mail + Supabase + Flyway (drop MySQL-as-primary + dark mode).  
13. Fix root / `nexora` READMEs and privacy copy (no false “read-only”).  
14. Fix PROJECT_REPORT §2 MySQL line.

### E. Defer until A–D are green

15. Pub/Sub Watch  
16. Gmail search parity  
17. Brain RAG  
18. Analytics expansion  
19. Full package rename `com.nexora` → Cortex (optional, large)

---

## 8. Process cleanup (this session)

Stopped local app Java/Maven processes that were holding **port 8080** (Spring Boot). No Vite listener was found on 5173/5174. Cursor’s Java language server may still run (IDE only). If Problems still show a ghost `android` Gradle project after reload, run **Java: Clean Java Language Server Workspace** once — Gradle import is already disabled in `.vscode/settings.json`.

---

## 9. Out of scope reminder

Do not treat the untracked *COMPLETE PRODUCTION IMPLEMENTATION PROMPT* as already executed. Tranches 1–2 are done; later phases (Watch, RAG, full branding, deploy hardening) are still the backlog in §4 and §7E.
