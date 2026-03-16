#!/bin/bash
# ─────────────────────────────────────────────────────────────────────────────
# Expensio — Cloudflare Tunnel setup
# This exposes your home server to the internet for FREE.
# No port forwarding, no public IP needed.
#
# Prerequisites:
#   - Free Cloudflare account at https://cloudflare.com
#   - A domain added to Cloudflare (free .workers.dev subdomain also works)
#
# Run once:  bash server/tunnel.sh
# ─────────────────────────────────────────────────────────────────────────────
set -e

echo "──────────────────────────────────────"
echo " Cloudflare Tunnel Setup"
echo "──────────────────────────────────────"
echo ""
echo " This will open a browser to log in to Cloudflare."
echo " Press Enter to continue..."
read

# ── Step 1: Login to Cloudflare ───────────────────────────
cloudflared tunnel login
# This opens a browser. Click Authorize. A cert is saved to ~/.cloudflared/cert.pem

# ── Step 2: Create the tunnel ─────────────────────────────
TUNNEL_NAME="expensio"
cloudflared tunnel create $TUNNEL_NAME

# Get the tunnel ID
TUNNEL_ID=$(cloudflared tunnel list | grep $TUNNEL_NAME | awk '{print $1}')
echo ""
echo "  Tunnel ID: $TUNNEL_ID"

# ── Step 3: Create config file ────────────────────────────
mkdir -p ~/.cloudflared

cat > ~/.cloudflared/config.yml << EOF
tunnel: $TUNNEL_ID
credentials-file: /root/.cloudflared/$TUNNEL_ID.json

ingress:
  - hostname: api.YOURDOMAIN.COM   # ← Replace with your domain
    service: http://localhost:80
  - service: http_status:404
EOF

echo ""
echo "  Config written to ~/.cloudflared/config.yml"
echo ""
echo "  ⚠️  IMPORTANT: Edit the hostname in ~/.cloudflared/config.yml"
echo "      Replace api.YOURDOMAIN.COM with your actual domain"
echo ""
echo "  Then create a DNS record in Cloudflare:"
echo "    cloudflared tunnel route dns $TUNNEL_NAME api.YOURDOMAIN.COM"
echo ""

# ── Step 4: Install as a system service ──────────────────
echo " Installing tunnel as a systemd service (auto-starts on boot)..."
cloudflared service install

systemctl enable cloudflared
systemctl start cloudflared

echo ""
echo "──────────────────────────────────────"
echo " Tunnel setup complete!"
echo "──────────────────────────────────────"
echo ""
echo " Check tunnel status:   systemctl status cloudflared"
echo " View tunnel logs:      journalctl -u cloudflared -f"
echo ""
echo " After setting your domain, your API will be live at:"
echo " https://api.YOURDOMAIN.COM/api/v1/"
echo ""
echo " ── OR use a free workers.dev subdomain ──"
echo " Run: cloudflared tunnel run --url http://localhost:80 expensio"
echo " This gives you a free https://xxx.trycloudflare.com URL (no login needed)"
echo ""
