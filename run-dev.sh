#!/usr/bin/env bash
# Starts the full stack locally: Spring Boot backend + Vite frontend.
#
#   ./run-dev.sh              start both, then Ctrl-C to stop both
#   ./run-dev.sh --bypass     also open a demo session without Google OAuth
#
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND="$ROOT/nexora/backend"
FRONTEND="$ROOT/nexora/frontend"
LOGS="$ROOT/.dev-logs"
BYPASS=false
[[ "${1:-}" == "--bypass" ]] && BYPASS=true

c() { printf '\033[%sm%s\033[0m\n' "$1" "$2"; }
ok()   { c '0;32' "  ok    $1"; }
info() { c '0;36' "  ..    $1"; }
warn() { c '1;33' "  warn  $1"; }
die()  { c '0;31' "  fail  $1"; exit 1; }

echo; c '1;37' "Velocity — full stack"; echo

# ── prerequisites ────────────────────────────────────────────────────────────
command -v java >/dev/null || die "java not found (need JDK 17+)"
command -v node >/dev/null || die "node not found (need Node 18+)"
# Match the version line specifically: when JAVA_TOOL_OPTIONS is set the JVM
# prints a "Picked up JAVA_TOOL_OPTIONS: ..." line first, so head -1 reads that
# instead and the comparison below blows up under `set -u`.
JAVA_MAJOR="$(java -version 2>&1 | grep -E '(java|openjdk) version' | head -1 | sed -E 's/.*"1\.([0-9]+).*|.*"([0-9]+).*/\1\2/')"
[[ "$JAVA_MAJOR" =~ ^[0-9]+$ ]] || die "could not determine Java version from: $(java -version 2>&1 | tr '\n' ' ')"
(( JAVA_MAJOR >= 17 )) || die "Java $JAVA_MAJOR found, need 17+"
ok "java $JAVA_MAJOR, node $(node -v)"

MVN="mvn"
if ! command -v mvn >/dev/null; then
  [[ -x "$BACKEND/mvnw" ]] || die "neither mvn nor ./mvnw available"
  MVN="$BACKEND/mvnw"
fi

# ── backend env ──────────────────────────────────────────────────────────────
ENV_FILE="$BACKEND/.env"
if [[ ! -f "$ENV_FILE" ]]; then
  cp "$BACKEND/.env.example" "$ENV_FILE"
  warn "created backend/.env from the example — edit it to add real keys"
fi
set -a; source "$ENV_FILE"; set +a

# Dev-only defaults so the app boots before any real credentials exist.
# These are generated locally and never committed.
if [[ -z "${JWT_SECRET:-}" || "$JWT_SECRET" == your-* ]]; then
  export JWT_SECRET="$(head -c 48 /dev/urandom | base64 | tr -d '/+=' | head -c 48)"
  warn "JWT_SECRET unset — generated an ephemeral one (sessions drop on restart)"
fi
if [[ -z "${ENCRYPTION_KEY:-}" || "$ENCRYPTION_KEY" == your16* ]]; then
  export ENCRYPTION_KEY="$(head -c 24 /dev/urandom | base64 | tr -d '/+=' | head -c 16)"
  warn "ENCRYPTION_KEY unset — generated an ephemeral one"
fi
export GOOGLE_CLIENT_ID="${GOOGLE_CLIENT_ID:-}"
export GOOGLE_CLIENT_SECRET="${GOOGLE_CLIENT_SECRET:-}"
export PORT="${PORT:-8080}"
$BYPASS && export DEV_BYPASS_ENABLED=true

if [[ -z "$GOOGLE_CLIENT_ID" || "$GOOGLE_CLIENT_ID" == your_* ]]; then
  warn "GOOGLE_CLIENT_ID unset — 'Sign in with Google' will return 503"
  $BYPASS || warn "re-run as ./run-dev.sh --bypass to explore without OAuth"
fi
if [[ -z "${GEMINI_API_KEY:-}" || "$GEMINI_API_KEY" == your-* ]]; then
  warn "GEMINI_API_KEY unset — AI falls back to keyword matching"
else
  ok "gemini key present (model ${GEMINI_MODEL:-gemini-flash-latest})"
fi

mkdir -p "$LOGS"

cleanup() {
  echo; info "shutting down"
  # spring-boot:run forks a separate JVM, so killing the maven wrapper alone
  # leaves the app holding :$PORT and the next run fails to bind.
  for pid in "${BACK_PID:-}" "${FRONT_PID:-}"; do
    [[ -n "$pid" ]] || continue
    pkill -P "$pid" 2>/dev/null || true
    kill "$pid" 2>/dev/null || true
  done
  pkill -f "com.nexora.NexoraApplication" 2>/dev/null || true
  wait 2>/dev/null || true
  ok "stopped"
}
trap cleanup EXIT INT TERM

# ── backend ──────────────────────────────────────────────────────────────────
info "compiling backend (first run downloads dependencies)"
(cd "$BACKEND" && "$MVN" -q compile) || die "backend compile failed"
ok "backend compiled"

info "starting backend on :$PORT"
(cd "$BACKEND" && "$MVN" -q spring-boot:run) > "$LOGS/backend.log" 2>&1 &
BACK_PID=$!

for i in {1..90}; do
  curl -sf "http://localhost:$PORT/actuator/health" >/dev/null 2>&1 && break
  kill -0 "$BACK_PID" 2>/dev/null || { tail -30 "$LOGS/backend.log"; die "backend exited — see $LOGS/backend.log"; }
  sleep 1
  (( i == 90 )) && { tail -30 "$LOGS/backend.log"; die "backend did not become healthy"; }
done
ok "backend healthy at http://localhost:$PORT"

# ── frontend ─────────────────────────────────────────────────────────────────
[[ -f "$FRONTEND/.env" ]] || cp "$FRONTEND/.env.example" "$FRONTEND/.env"
if [[ ! -d "$FRONTEND/node_modules" ]]; then
  info "installing frontend dependencies"
  (cd "$FRONTEND" && npm install --silent) || die "npm install failed"
fi

info "starting frontend on :5173"
(cd "$FRONTEND" && npm run dev -- --host 0.0.0.0) > "$LOGS/frontend.log" 2>&1 &
FRONT_PID=$!

for i in {1..60}; do
  curl -sf http://localhost:5173/ >/dev/null 2>&1 && break
  kill -0 "$FRONT_PID" 2>/dev/null || { tail -30 "$LOGS/frontend.log"; die "frontend exited — see $LOGS/frontend.log"; }
  sleep 1
  (( i == 60 )) && { tail -30 "$LOGS/frontend.log"; die "frontend did not start"; }
done
ok "frontend ready"

# ── ready ────────────────────────────────────────────────────────────────────
echo
c '1;37' "  open  http://localhost:5173"
if $BYPASS; then
  echo
  c '1;33' "  demo session (no Google needed):"
  echo   "        http://localhost:$PORT/api/auth/bypass"
  echo   "        Opening that URL signs you in and redirects to the dashboard."
  echo   "        Its 5 emails carry PRE-WRITTEN summaries, not AI output — real"
  echo   "        Gmail sync is skipped for the demo account. Sign in with a real"
  echo   "        Google account to see Gemini classify your own mail."
fi
echo
echo "  logs   $LOGS/backend.log"
echo "         $LOGS/frontend.log"
echo
c '0;36' "  Ctrl-C to stop both"
echo

wait
