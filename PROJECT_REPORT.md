# Cortex Mail — Full Project Report

**Document date:** 29 August 2026  
**Repository:** [Austin-Joshua/Cortex-Mail](https://github.com/Austin-Joshua/Cortex-Mail)  
**Default branch on GitHub:** `master` (also mirrored as `main` when pushed)  
**Code package / folder name:** `nexora/` (historical; product brand is Cortex Mail)

---

## 1. Name, meaning, and product relevance

| Name | Role | Meaning / relevance |
|------|------|---------------------|
| **Cortex Mail** | Current product brand (UI, landing, PWA) | *Cortex* = the brain’s outer layer (decision, priority, attention). The product is an **attention layer on top of Gmail**: it reads mail, scores load, groups by meaning, and surfaces what deserves action — without becoming another mailbox that sends/deletes for you by default. |
| **Cortex Score** | Core metric on the dashboard | A single “instrument” reading of inbox pressure (unread, important, actions, deadlines) so the user can see *how overloaded* they are, not just a list of messages. |
| **Nexora** | Java package (`com.nexora`), folder `nexora/`, older docs | Earlier internal / codebase name. Still visible in paths and class names; not the user-facing brand. |
| **Velocity** | Previous public brand (README / older commits) | Earlier positioning: “communication acceleration.” Superseded in the app UI by **Cortex Mail**; some docs may still say Velocity until fully rewritten. |

**One-line idea:** Connect Google → extract real Gmail → show all inbox first → compute Cortex Score → classify by source/content → group and display in Inbox / Drafts / Archive / Brain / Analytics.

---

## 2. What the project is (idea)

Cortex Mail is an **AI-assisted Gmail intelligence web app**. It does **not** replace Gmail as the system of record. It:

1. Authenticates with **Google OAuth**.
2. Syncs mail via the **Gmail API** into the app database.
3. Classifies and summarizes with **rules + optional Gemini/Claude**.
4. Presents a **dashboard instrument cluster**, categorized inbox, drafts, archive, Brain Q&A, analytics, and related tools.
5. Stays **read-oriented** on Gmail where possible (marketing claim: read your mail; do not silently send/delete/alter — reply/send paths exist in code and should be treated carefully in production policy).

**Stack (executed):**

| Layer | Technology |
|-------|------------|
| Frontend | React 19, TypeScript, Vite, TanStack Query, Zustand, React Router, PWA |
| Backend | Spring Boot 3, Java 17, Spring Security, JPA |
| Data (dev default) | H2 in-memory |
| Data (production) | Supabase Postgres + Flyway (`SPRING_PROFILES_ACTIVE=prod`) |
| External | Gmail API, Google Calendar API, Google Gemini (optional), Claude (optional) |
| Deploy (documented) | Frontend → Vercel; Backend → Render |

**Not in current product scope:** separate Flutter/native mobile client (removed from the repo; product is **responsive web + PWA**).

---

## 3. Who it is for (extent by user type)

| User type | How they use it | What works today | Gaps / caveats |
|-----------|-----------------|------------------|----------------|
| **Students** | Assignments, attendance, placements, hackathons, announcements | Category taxonomy and keyword/label rules lean academic (ASSIGNMENT, PLACEMENT, HACKATHON, etc.) | Classification quality depends on sender/subject language; many campus mails stay UNCATEGORIZED without Gemini |
| **Job seekers / early career** | Placement, internship, interview mail | PLACEMENT / INTERNSHIP rules + high priority when keywords match | No dedicated ATS / LinkedIn-deep integration beyond domain heuristics |
| **Professionals / knowledge workers** | Meetings, finance, promotions noise, priority unread | Gmail labels (Promotions/Social/Updates), Cortex Score, priority stream, Brain Q&A | Archive sync capped (~300); not a full Gmail client |
| **Heavy inbox users** | Volume analytics, sender leaderboard, weekly recap | Analytics volume APIs + UI pages exist | Real-time push / notification UX partially stripped from chrome (sidebar sync/bell removed); scheduled sync still exists server-side |
| **Privacy-conscious users** | Want insight without giving send rights | OAuth + JWT + encrypted tokens pattern; privacy page | Must verify live OAuth scopes vs marketing (“read-only”); reply endpoints exist |
| **Admins / operators** | Deploy and monitor | PRODUCTION.md, env-based config, health actuator | Production DB persistence, secrets, OAuth publish status are ops responsibilities |
| **Developers** | Extend classifiers, UI, APIs | Clear services: `GmailSyncService`, `EmailClassificationService`, `DashboardService`, `NexoraBrainService` | Naming mix (Cortex / Nexora / Velocity); H2 wipe on restart in local default |

---

## 4. End-to-end data flow

### 4.1 High-level diagram

```text
┌─────────────┐     OAuth2      ┌──────────────────┐
│   Google    │◄───────────────►│  AuthController  │
│  Identity   │                 │  AuthService     │
└─────────────┘                 └────────┬─────────┘
                                         │ JWT + user row
                                         ▼
┌─────────────┐  Gmail REST API  ┌──────────────────┐     JPA      ┌────────────┐
│  Gmail API  │◄────────────────►│ GmailSyncService │─────────────►│  Email DB  │
│  Labels API │                  │ (INBOX/DRAFT/    │              │  (H2/MySQL)│
│  Messages   │                  │  ARCHIVE query)  │              │  User row  │
└─────────────┘                  └────────┬─────────┘              └─────▲──────┘
                                          │ no classify in sync           │
                                          ▼                               │
                                 ┌──────────────────┐                     │
                                 │ EmailClassification│───────────────────┘
                                 │ Service (post-sync)│  category, priority,
                                 │ labels→domain→text │  summary, actions
                                 └────────┬─────────┘
                                          │
         ┌────────────────────────────────┼────────────────────────────────┐
         ▼                                ▼                                ▼
┌─────────────────┐            ┌─────────────────┐              ┌─────────────────┐
│ DashboardService│            │ EmailService    │              │ NexoraBrain     │
│ Cortex Score    │            │ inbox/drafts/   │              │ Service         │
│ unread+labels   │            │ archive APIs    │              │ Q&A over mail   │
└────────┬────────┘            └────────┬────────┘              └────────┬────────┘
         │                              │                                │
         └──────────────────────────────┼────────────────────────────────┘
                                        ▼
                              ┌──────────────────┐
                              │ React frontend   │
                              │ useInboxPipeline │
                              │ Inbox/Drafts/…   │
                              └──────────────────┘
```

### 4.2 Step-by-step process

1. **Login**  
   User hits Google OAuth → backend stores/refreshes Gmail tokens on `User` → frontend keeps JWT session.

2. **Extract (sync)** — `POST /api/emails/sync` via `GmailSyncService`  
   - Fetch & cache **label counts** (INBOX, DRAFT, IMPORTANT, SPAM, etc.).  
   - List/paginate **INBOX** messages → upsert local `Email` with headers, snippet/body, labels JSON, read/starred/important, `inInbox=true`.  
   - Sync **DRAFT** label → `isDraft=true`.  
   - Query archive-ish set (`-in:inbox -in:trash -in:spam -in:drafts`, capped) → `isArchived=true`.  
   - Mark local inbox rows that disappeared from Gmail INBOX as no longer in inbox.  
   - **Does not** auto-classify during sync; new mail defaults to `UNCATEGORIZED` / medium priority.

3. **Score** — `GET /api/dashboard/summary`  
   - Unread primarily from **Gmail INBOX label unread**.  
   - Plus local high-priority unread, deadlines, pending actions, category counts, meetings.  
   - UI shows **Cortex Score** gauge and “built from …” style explanation.

4. **Classify / separate** — `POST /api/emails/classify`  
   - Local rules: Gmail category labels → sender domain → subject/body keywords.  
   - Optional AI path elsewhere for richer summarization when keys exist.  
   - Writes `category`, `priority`, `aiSummary`, action items, explicit deadlines only when found.

5. **Display**  
   - Inbox: `GET /api/emails` (inbox-only).  
   - Drafts: `GET /api/emails/drafts`.  
   - Archive: `GET /api/emails/archived`.  
   - Categories: `GET /api/emails/categories`.  
   - Integrity: `GET /api/emails/sync-status` (Gmail totals vs local; samples; notes).  
   - Frontend **Dashboard** runs `useInboxPipeline`: sync → score → classify → invalidate queries; shows **Gmail extract check** panel.

6. **Optional downstream**  
   - Brain Q&A over stored mail.  
   - Calendar events for deadlines (when enabled).  
   - Analytics volume / sender stats.  
   - Reply draft / send endpoints (policy-sensitive).

### 4.3 What the user sees (outputs)

| Surface | Output |
|---------|--------|
| Landing | Brand story, Cortex Score concept, connect CTA |
| Onboarding | Role / preferences → unlocks app |
| Dashboard | Cortex Score, pipeline status, Gmail extract check, priority stream, categories, week activity |
| Inbox | Real synced messages, category tabs/filters, detail view |
| Drafts / Archive | Real Gmail drafts / archived extracts |
| Priority | High-signal / flagged focus list |
| Brain | Natural-language answers grounded in stored email |
| Analytics | Volume / distribution views |
| Settings / Help / Privacy | Account, docs, policy |

---

## 5. Execution status — how much of the idea is done

Rough product-completion estimate for the **current Cortex Mail vision** (Gmail extract → score → classify → display → assist):

| Area | Status | ~Done |
|------|--------|-------|
| Google OAuth + session | Working | 90% |
| Gmail INBOX full sync + label counts | Working (canonical upsert) | 95% |
| Incremental `history.list` sync | Working (Tranches 1–2) | 90% |
| Drafts sync + UI | Working | 85% |
| Archive sync + UI | Working (cap ~300) | 70% |
| MIME plain + **sanitized HTML** + attachment records | Working (Tranches 1–2) | 85% |
| Bidirectional Gmail mutations (read/star/archive/trash) | Working (Gmail→DB) | 85% |
| Post-sync classification + grouping | Working (local rules; AI optional) | 75% |
| Cortex Score (backend, explainable 0–100) | Working (Tranches 1–2) | 90% |
| Supabase Postgres + Flyway | Wired (prod profile) | 90% |
| Brain Q&A | Present | 65% |
| Analytics | Present | 70% |
| Pub/Sub Watch / search parity / classifier rewrite | Out of scope for T1–2 | 0% |

**Overall idea execution (single-user Gmail intelligence web app): ~80%** after Tranches 1–2.

### Done (executed)

- Real Gmail extraction (inbox, drafts, partial archive).  
- No fake seed mail on the happy path of sync.  
- Pipeline: extract → score → classify → UI refresh.  
- Category taxonomy + local classifier.  
- Dashboard instrument UI (bento), Cortex branding in app.  
- Sync integrity endpoint + dashboard “Gmail extract check”.  
- Web-only product after mobile removal.

### In progress / partial

- Classification quality (many UNCATEGORIZED without strong signals / Gemini).  
- Archive completeness vs large Gmail accounts.  
- Notification / sync chrome vs backend schedulers.  
- README still mixed Velocity/Nexora vs Cortex Mail.  
- “Read-only” product story vs reply/send code paths.

### Not executed / future

- Outlook / other providers.  
- Full Gmail parity (labels UX, filters, spam management in-app).  
- Native apps.  
- Org admin / multi-seat product.  
- Guaranteed durable local DB in default dev profile (H2 clears on restart).  
- Automated e2e tests against live Gmail in CI.

---

## 6. Architecture map (what you need to know as an owner)

### 6.1 Repo layout

```text
Cortex-Mail/
├── README.md              # Still largely “Velocity” narrative — treat carefully
├── PRODUCTION.md          # Deploy checklist (Vercel + Render)
├── PROJECT_REPORT.md      # This document
└── nexora/
    ├── frontend/          # React app (user-facing Cortex Mail)
    └── backend/           # Spring Boot API (package com.nexora)
```

### 6.2 Critical backend services

| Service | Responsibility |
|---------|----------------|
| `GmailSyncService` | Talk to Gmail; upsert mail; cache label counts |
| `EmailClassificationService` | Post-sync grouping; AI/local fallback |
| `EmailService` | Inbox/drafts/archive APIs; sync-status integrity |
| `DashboardService` | Summary + score inputs |
| `AuthService` | OAuth user lifecycle |
| `NexoraBrainService` | Brain Q&A |
| `CalendarService` | Deadline → calendar |
| `EmailSyncScheduler` | Periodic background sync |

### 6.3 Critical frontend hooks / pages

| Piece | Role |
|-------|------|
| `useInboxPipeline` | Auto sync → score → classify on dashboard |
| `useEmails` | Inbox query + post-sync classify helper |
| `DashboardPageNew` | Score + extract check + instrument tiles |
| `InboxPage` / `DraftsPage` / `ArchivePage` | Mailbox UIs |
| `BrainPage` | Q&A |

### 6.4 Local runtime (typical)

- Backend: `http://localhost:8080`  
- Frontend: `http://localhost:5173`  
- After backend restart with H2: **empty DB** → open Dashboard once to re-run pipeline (`cortex-pipeline-v3-*` session key).

### 6.5 Production notes

- Frontend env: `VITE_API_BASE_URL`, `VITE_GOOGLE_CLIENT_ID`  
- Backend: Google client secret, JWT secret, encryption key, CORS origins, Gemini/Claude keys, MySQL URL  
- Google Cloud Console: OAuth consent, authorized redirect URIs, Gmail scopes  
- Redeploy Vercel + Render after pushing for live sites to pick up changes  

---

## 7. Security & trust (must-know)

- Tokens should be encrypted at rest; never commit `.env`.  
- All email queries must stay scoped by authenticated `userId`.  
- CORS must allow only real frontends in production.  
- H2 console must be off in production.  
- Re-verify actual OAuth scopes vs “read-only” marketing before App Store / public launch claims.  
- Sync integrity (`/api/emails/sync-status`) is the quick check that extraction matches Gmail.

---

## 8. How to verify extraction → display → classification

1. Sign in with Google on the local or production frontend.  
2. Open **Dashboard** (pipeline runs).  
3. Read **Gmail extract check**: inbox/drafts aligned? notes? sample subject + category?  
4. Open **Inbox** — subjects should match Gmail; category tabs should group classified mail.  
5. Open **Drafts** / **Archive** — should show real synced items.  
6. If counts diverge: **Re-extract from Gmail**; if still short on archive, raise archive sync cap in `GmailSyncService`.

---

## 9. Branding / naming debt (awareness)

The product the user sees is **Cortex Mail**. The codebase still carries **Nexora** (packages) and some docs still say **Velocity**. When writing papers, demos, or investor notes, lead with **Cortex Mail** and treat Nexora/Velocity as historical aliases unless you intentionally rebrand code packages.

---

## 10. Summary scorecard

| Question | Answer |
|----------|--------|
| What is it? | AI-assisted Gmail attention / classification web app |
| Why “Cortex”? | Brain-layer metaphor for priority and grouping on top of mail |
| Core flow? | OAuth → Gmail extract → store → score → classify → UI |
| Primary users? | Students, job seekers, busy professionals with Gmail |
| Idea executed? | ~70–75% for single-user Gmail web product |
| Biggest remaining gaps? | Classification quality, archive completeness, doc/brand consistency, production durability, multi-provider |
| Where is code? | `nexora/frontend`, `nexora/backend` |
| How to prove sync health? | Dashboard **Gmail extract check** + `/api/emails/sync-status` |

---

*This report reflects the repository state around the Cortex Mail Gmail pipeline, sync integrity work, UI cleanup, and web-only product direction as of the document date.*
