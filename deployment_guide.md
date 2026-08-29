# Cortex Mail — Deployment & Google OAuth

Canonical deploy steps live in **[PRODUCTION.md](./PRODUCTION.md)** (Render backend + Vercel frontend + **Supabase Postgres**).

**Repo:** [Austin-Joshua/Cortex-Mail](https://github.com/Austin-Joshua/Cortex-Mail)

## Quick pointers

| Piece | Where |
|-------|--------|
| Backend | Render · root `nexora/backend` · Docker / `render.yaml` · `SPRING_PROFILES_ACTIVE=prod` |
| Frontend | Vercel · root `nexora/frontend` · `VITE_API_BASE_URL` + `VITE_GOOGLE_CLIENT_ID` |
| Database | Supabase Postgres + Flyway (not MySQL) |
| OAuth callback | `https://YOUR-RENDER-URL/api/auth/google/callback` |

## Google OAuth publish notes

Cortex Mail uses Gmail scopes that allow **sync and user-initiated mailbox mutations** (read/star/archive/trash), plus Calendar where enabled. Privacy copy must match that — do not claim pure read-only if modify scopes are enabled.

1. [OAuth consent screen](https://console.cloud.google.com/apis/credentials/consent) → publish when ready for non–test users.
2. Add production redirect URI and Vercel JS origin.
3. For Advanced Protection / sensitive-scope verification, submit Google’s verification with a privacy URL and demo video if you go public.

## Temporary testing

If an Advanced Protection account is blocked before verification, test with a standard Google account, or temporarily unenroll Advanced Protection (can take hours to propagate).
