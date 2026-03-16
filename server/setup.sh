#!/bin/bash
# ─────────────────────────────────────────────────────────────────────────────
# Expensio — One-time server setup script
# Tested on: Fedora Linux 43 (Server Edition)
# Run once as root:  bash setup.sh
# ─────────────────────────────────────────────────────────────────────────────
set -e

echo "──────────────────────────────────────"
echo " Expensio Server Setup (Fedora)"
echo "──────────────────────────────────────"

# ── 1. System update ──────────────────────────────────────
echo "[1/6] Updating system packages..."
dnf update -y

# ── 2. Install basic tools ────────────────────────────────
echo "[2/6] Installing basic tools..."
dnf install -y curl git

# ── 3. Install Docker ─────────────────────────────────────
echo "[3/6] Installing Docker..."
if ! command -v docker &> /dev/null; then
  dnf install -y dnf-plugins-core
  dnf config-manager --add-repo https://download.docker.com/linux/fedora/docker-ce.repo
  dnf install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
  systemctl enable --now docker
  echo "  Docker installed and started."
else
  echo "  Docker already installed, skipping."
fi

# Verify
docker --version
docker compose version

# ── 4. Firewall (firewalld) ───────────────────────────────
echo "[4/6] Configuring firewall..."
systemctl enable --now firewalld
firewall-cmd --permanent --add-service=ssh
firewall-cmd --permanent --add-service=http
firewall-cmd --permanent --add-service=https
firewall-cmd --reload
firewall-cmd --list-all

# ── 5. Install Cloudflare Tunnel (cloudflared) ────────────
echo "[5/6] Installing Cloudflare Tunnel (cloudflared)..."
if ! command -v cloudflared &> /dev/null; then
  curl -L --output /tmp/cloudflared.rpm \
    "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-x86_64.rpm"
  dnf install -y /tmp/cloudflared.rpm
  rm /tmp/cloudflared.rpm
  echo "  cloudflared installed."
else
  echo "  cloudflared already installed, skipping."
fi
cloudflared --version

# ── 6. Create app directory ───────────────────────────────
echo "[6/6] Creating /opt/expensio..."
mkdir -p /opt/expensio

echo ""
echo "──────────────────────────────────────"
echo " Setup complete!"
echo "──────────────────────────────────────"
echo ""
echo " Next steps:"
echo "  1. git clone <your-repo-url> /opt/expensio"
echo "  2. cp /opt/expensio/.env.example /opt/expensio/.env"
echo "  3. Fill in .env values"
echo "  4. bash /opt/expensio/server/deploy.sh"
echo "  5. bash /opt/expensio/server/tunnel.sh   ← expose to internet (free)"
echo ""
