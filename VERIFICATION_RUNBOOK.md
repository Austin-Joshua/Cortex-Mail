# Cortex Mail — Verification Runbook (Final Product)

**Scope:** full connected product after Phases 0–5 (auth hardening, shared pipeline, triage, cookies/jobs, Watch, drafts send, focus/digest, ranked search).  
**Deferred:** full Gmail search parity, Brain RAG, multi-instance Redis.

---

## 1. Build gates

```bash
cd nexora/backend && ./mvnw -DskipTests compile
cd nexora/frontend && npx tsc -b && npm test
```

No secrets in git. Confirm Flyway `V6__jobs_locks_watch_prefs.sql` is present.

## 2. Local E2E checklist

Servers: backend `:8080` · frontend `:5173`

| Step | Verify |
|------|--------|
| Google sign-in with OAuth `state` | Landing → Connect → `/auth/callback` → Home |
| Soft logout | Settings → Log out (keep Gmail) → sign-in works without full consent when possible |
| Hard revoke | Settings → Revoke access → must reconnect Gmail |
| Session refresh | Wait past access TTL or force 401 → `/api/auth/refresh` restores session |
| Shared sync chip | Visible on Home, Inbox, Priority, Drafts, Deadlines, Triage |
| Initial + incremental sync | Home Sync now; second sync uses history when `gmail_history_id` set |
| Typed next actions | Home score CTA buttons navigate to Triage / Deadlines / Inbox |
| Triage | `/triage` lists follow-ups; Done + Snooze 24h work |
| Deadlines | Overdue + upcoming; open mail |
| Draft send | Cortex draft → Send succeeds (or clear error if Gmail rejects) |
| Calendar link | After deadline export, Email Detail shows Open in Calendar |
| Quiet hours / digest | Settings prefs persist via `/api/auth/profile` |
| Ranked search | Inbox search returns high-priority / unread first when applicable |
| HTML body | DOMPurify-rendered; no script execution |
| Health | `GET /actuator/health` → UP |

## 3. Watch (optional)

If `GOOGLE_PUBSUB_TOPIC` is set:

1. Enable Pub/Sub + push subscription → `https://YOUR-API/api/gmail/push`
2. Confirm watch renew in logs / `users.watch_expiration`
3. Send yourself mail → incremental job enqueued without waiting for 5‑min scheduler

If unset: scheduler + client sync remain the path (supported).

## 4. Deploy smoke

Follow [PRODUCTION.md](./PRODUCTION.md). After Render+Vercel:

1. Sign in from production origin  
2. Sync → Supabase `emails` grows; `background_jobs` may show rows  
3. Triage + Score CTAs  
4. Cookie `Secure` on HTTPS  

## 5. Explicitly deferred

- Gmail `messages.list` q= search parity  
- Brain RAG / vectors  
- Horizontal multi-instance Redis cluster  
