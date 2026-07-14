#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
FRONTEND_DIR="$(dirname "$SCRIPT_DIR")"
PROJECT_DIR="$(dirname "$FRONTEND_DIR")"

# Stop existing processes
echo "[dev-sagemaker] Stopping existing processes..."
if command -v lsof &>/dev/null; then
  kill "$(lsof -ti :3000)" 2>/dev/null || true
  kill "$(lsof -ti :3001)" 2>/dev/null || true
  kill "$(lsof -ti :8080)" 2>/dev/null || true
elif command -v fuser &>/dev/null; then
  fuser -k 3000/tcp 2>/dev/null || true
  fuser -k 3001/tcp 2>/dev/null || true
  fuser -k 8080/tcp 2>/dev/null || true
else
  pkill -f "sagemaker-proxy.mjs" 2>/dev/null || true
  pkill -f "next start.*3001" 2>/dev/null || true
  pkill -f "spring-boot:run.*local" 2>/dev/null || true
fi
sleep 1

export SAGEMAKER=1
export NEXT_PUBLIC_BASE_PATH=/codeeditor/default/absports/3000

# Start backend (H2, local profile)
echo "[dev-sagemaker] Starting backend (H2)..."
cd "$PROJECT_DIR/backend"
mvn spring-boot:run -Dspring-boot.run.profiles=local > /tmp/backend.log 2>&1 &
BACKEND_PID=$!
echo "[dev-sagemaker] Backend PID: $BACKEND_PID"

# Build and start Next.js on port 3001
echo "[dev-sagemaker] Building frontend..."
cd "$FRONTEND_DIR"
npx next build

echo "[dev-sagemaker] Starting Next.js on :3001..."
npx next start -H 127.0.0.1 -p 3001 > /tmp/frontend-next.log 2>&1 &
NEXT_PID=$!
echo "[dev-sagemaker] Next.js PID: $NEXT_PID"
sleep 3

# Start restore proxy on port 3000
echo "[dev-sagemaker] Starting proxy on :3000..."
node "$SCRIPT_DIR/sagemaker-proxy.mjs" > /tmp/frontend-proxy.log 2>&1 &
PROXY_PID=$!
echo "[dev-sagemaker] Proxy PID: $PROXY_PID"
sleep 1

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
echo "To stop:  kill $BACKEND_PID $NEXT_PID $PROXY_PID"
