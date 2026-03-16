#!/bin/bash
# ─────────────────────────────────────────────────────────────────────────────
# Expensio — Deploy / Update script
# Fedora + Docker Compose
# Usage:  bash server/deploy.sh
# ─────────────────────────────────────────────────────────────────────────────
set -e

APP_DIR="/opt/expensio"

echo "──────────────────────────────────────"
echo " Expensio Deploy"
echo "──────────────────────────────────────"

# ── Guard: must be in app dir with .env ──────────────────
if [ ! -f "$APP_DIR/.env" ]; then
  echo "ERROR: $APP_DIR/.env not found."
  echo "Run:  cp $APP_DIR/.env.example $APP_DIR/.env  and fill in the values."
  exit 1
fi

cd "$APP_DIR"

# ── 1. Pull latest code ───────────────────────────────────
echo "[1/5] Pulling latest code..."
git pull origin main

# ── 2. Build and start containers ─────────────────────────
echo "[2/5] Building and starting containers..."
docker compose pull postgres redis
docker compose up --build -d

# ── 3. Wait for PostgreSQL ────────────────────────────────
echo "[3/5] Waiting for PostgreSQL..."
for i in {1..15}; do
  if docker compose exec -T postgres pg_isready -U expensio > /dev/null 2>&1; then
    echo "  PostgreSQL is ready."
    break
  fi
  echo "  Waiting... ($i/15)"
  sleep 3
done

# ── 4. Run migrations ─────────────────────────────────────
echo "[4/5] Running Alembic migrations..."
docker compose exec -T backend alembic upgrade head

# ── 5. Health check ───────────────────────────────────────
echo "[5/5] Health check..."
sleep 2
STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost/health 2>/dev/null || echo "000")
if [ "$STATUS" = "200" ]; then
  echo "  ✓ Backend is up (HTTP 200)"
else
  echo "  ✗ Health check returned HTTP $STATUS"
  echo "    Logs: docker compose logs --tail=30 backend"
fi

echo ""
echo "──────────────────────────────────────"
echo " Deploy complete!"
echo "──────────────────────────────────────"
echo ""
echo " Commands:"
echo "  Logs:      docker compose logs -f backend"
echo "  Restart:   docker compose restart backend"
echo "  Stop:      docker compose down"
echo "  DB shell:  docker compose exec postgres psql -U expensio"
echo ""
