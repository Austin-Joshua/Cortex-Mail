# Cortex Mail — Verification & Deploy Runbook (post Tranches 1–2)

**Stage:** verification / deployment hardening — **not** Pub/Sub, search parity, or Brain RAG.  
**Code HEAD:** `1d8d696` on `master` / `main` (Tranches 1–2 landed in `76770e8`; polish + navy branding followed).

---

## 1. Commit / push — DONE

- Secrets (`.env`) never committed.
- Latest: navy logo + landing branding (`1d8d696`).
- Untracked only: large production prompt MD (optional; leave out of git unless you want it).

## 2. Local E2E checklist (you in the browser)

Servers: backend http://127.0.0.1:8080 · frontend http://127.0.0.1:5173

| Step | How to verify |
|------|----------------|
| Google sign-in | Landing → Connect Gmail → land on dashboard |
| Initial sync | Dashboard sync / pipeline chip completes |
| Supabase rows | Table `emails` grows; `email_attachments` if MIME has files |
| Second sync → `history.list` | After first sync, `users.gmail_history_id` set; next sync logs incremental |
| HTML rendering | Open a message with HTML body in Email Detail |
| Attachments | Detail shows attachment metadata |
| Read / unread | Toggle; Gmail + DB `is_read` |
| Star / unstar | Toggle; Gmail + DB `is_starred` |
| Archive / inbox | Toggle; Gmail labels + DB flags |
| Trash / restore | Toggle; Gmail + DB |
| Cortex Score breakdown | Dashboard “Why this score” / factors |

### Supabase snapshot (automated, this session)

Project `cortex-mail` (`svnqngplmzqfgtqmnbkn`) ACTIVE:

| Metric | Value |
|--------|--------|
| `users` | ≥ 1 |
| `emails` | growing (was ~12–23 during checks) |
| `with body_html` | all sampled rows had HTML |
| `email_attachments` | 4+ (PDF/PNG present) |
| RLS | enabled on app tables |
| Flyway | V1 + V2 recorded |

Re-check `users.gmail_history_id` and `last_synced_at` after a successful Dashboard sync in the browser.

## 3. AI / security

- **AI key:** optional. Leave empty for rules-only classify/Brain. Add `GEMINI_API_KEY` or `CLAUDE_API_KEY` to `nexora/backend/.env` only if you need better AI quality — **do not commit**.
- **DB password:** if the Supabase password was ever pasted into chat or a screenshot, rotate it now:
  1. Supabase → Project Settings → Database → reset password
  2. Update local `nexora/backend/.env` `DB_PASSWORD` / JDBC URL
  3. Update the same secret on Render when you deploy  
  This agent will **not** write a new password into `.env` for you.

## 4–5. Deploy (dashboard — no CLI on this machine)

Follow **[PRODUCTION.md](./PRODUCTION.md)**:

1. **Render** — Web Service from `Austin-Joshua/Cortex-Mail`, root `nexora/backend`, env from `render.yaml` + secrets (`DB_*`, OAuth, JWT, CORS, `SPRING_PROFILES_ACTIVE=prod`).
2. **Vercel** — Import same repo, root `nexora/frontend`, set `VITE_API_BASE_URL` to Render URL + `VITE_GOOGLE_CLIENT_ID`.
3. Google Cloud — production redirect URI + JS origin.

## 6. Docs / privacy — in progress this pass

Fixed / aligned:

- Root README, `PRODUCTION.md`, privacy HTML, Help/Settings/PrivacyPolicy pages, `deployment_guide.md`

Still legacy (historical, non-blocking): `nexora/nexora-audit-report.md`, `nexora/nexora-complete-redesign-prompt.md` (old prompts/reports). Optional cleanup later.

## 7. Explicitly deferred

Pub/Sub Watch · Gmail search parity · Brain RAG — **after** E2E green + Render/Vercel live.
