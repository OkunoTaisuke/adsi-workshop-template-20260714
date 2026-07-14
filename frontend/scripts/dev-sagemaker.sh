#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
FRONTEND_DIR="$(dirname "$SCRIPT_DIR")"
PROJECT_DIR="$(dirname "$FRONTEND_DIR")"
PID_FILE="$PROJECT_DIR/.dev-sagemaker.pids"

# Stop existing processes via PID file (kill process groups)
echo "[dev-sagemaker] Stopping existing processes..."
if [[ -f "$PID_FILE" ]]; then
  while IFS= read -r pid; do
    kill -- "-$pid" 2>/dev/null || kill "$pid" 2>/dev/null || true
  done < "$PID_FILE"
  rm -f "$PID_FILE"
fi
sleep 1

export SAGEMAKER=1
export NEXT_PUBLIC_BASE_PATH=/codeeditor/default/absports/3000

# Start backend (H2, local profile) in its own process group
echo "[dev-sagemaker] Starting backend (H2)..."
cd "$PROJECT_DIR/backend"
setsid mvn spring-boot:run -Dspring-boot.run.profiles=local > /tmp/backend.log 2>&1 &
BACKEND_PID=$!
echo "[dev-sagemaker] Backend PID: $BACKEND_PID"

# Build and start Next.js on port 3001
echo "[dev-sagemaker] Building frontend..."
cd "$FRONTEND_DIR"
npx next build

echo "[dev-sagemaker] Starting Next.js on :3001..."
setsid npx next start -H 127.0.0.1 -p 3001 > /tmp/frontend-next.log 2>&1 &
NEXT_PID=$!
echo "[dev-sagemaker] Next.js PID: $NEXT_PID"
sleep 3

# Start restore proxy on port 3000
echo "[dev-sagemaker] Starting proxy on :3000..."
node "$SCRIPT_DIR/sagemaker-proxy.mjs" > /tmp/frontend-proxy.log 2>&1 &
PROXY_PID=$!
echo "[dev-sagemaker] Proxy PID: $PROXY_PID"
sleep 1

# Save PIDs for clean shutdown (used as process group IDs)
printf '%s\n' "$BACKEND_PID" "$NEXT_PID" "$PROXY_PID" > "$PID_FILE"

# Verify
echo "[dev-sagemaker] Verifying..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:3000/absports/3000/ || echo "000")
echo "[dev-sagemaker] GET /absports/3000/ -> $STATUS"

echo ""
echo "=== SageMaker Preview Ready ==="
echo "Backend:  http://localhost:8080 (PID: $BACKEND_PID)"
echo "Next.js:  http://localhost:3001 (PID: $NEXT_PID)"
echo "Proxy:    http://localhost:3000 (PID: $PROXY_PID)"
echo ""
echo "Browser:  https://<studio-domain>/codeeditor/default/absports/3000/"
echo ""
echo "To stop:  npm run dev:sagemaker:stop"
