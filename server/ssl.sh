#!/bin/bash
# ─────────────────────────────────────────────────────────────────────────────
# Expensio — SSL setup with Let's Encrypt (Certbot)
# Run AFTER deploy.sh, once your domain's DNS points to this server.
# Usage:  bash server/ssl.sh yourdomain.com your@email.com
# ─────────────────────────────────────────────────────────────────────────────
set -e

DOMAIN=$1
EMAIL=$2

if [ -z "$DOMAIN" ] || [ -z "$EMAIL" ]; then
  echo "Usage: bash ssl.sh yourdomain.com your@email.com"
  exit 1
fi

echo "──────────────────────────────────────"
echo " Setting up SSL for $DOMAIN"
echo "──────────────────────────────────────"

# ── 1. Write Nginx config for the domain ─────────────────
echo "[1/3] Writing Nginx config..."
cat > /etc/nginx/sites-available/expensio << EOF
upstream expensio_backend {
    server 127.0.0.1:80;
}

server {
    listen 80;
    server_name $DOMAIN;

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    location / {
        return 301 https://\$host\$request_uri;
    }
}
EOF

ln -sf /etc/nginx/sites-available/expensio /etc/nginx/sites-enabled/expensio
nginx -t && systemctl reload nginx

# ── 2. Get certificate ───────────────────────────────────
echo "[2/3] Obtaining Let's Encrypt certificate..."
certbot certonly \
  --nginx \
  -d "$DOMAIN" \
  --email "$EMAIL" \
  --agree-tos \
  --non-interactive

# ── 3. Write HTTPS Nginx config ──────────────────────────
echo "[3/3] Writing HTTPS Nginx config..."
cat > /etc/nginx/sites-available/expensio << EOF
upstream expensio_backend {
    server backend:8000;
}

server {
    listen 80;
    server_name $DOMAIN;
    return 301 https://\$host\$request_uri;
}

server {
    listen 443 ssl;
    server_name $DOMAIN;

    ssl_certificate     /etc/letsencrypt/live/$DOMAIN/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/$DOMAIN/privkey.pem;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         HIGH:!aNULL:!MD5;

    client_max_body_size 10M;

    location /api/ {
        proxy_pass http://localhost;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_read_timeout 60s;
    }

    location /health {
        proxy_pass http://localhost/health;
    }
}
EOF

nginx -t && systemctl reload nginx

echo ""
echo "──────────────────────────────────────"
echo " SSL setup complete!"
echo " Your API is now live at:"
echo " https://$DOMAIN/api/v1/"
echo "──────────────────────────────────────"
echo ""
echo " Auto-renewal is handled by certbot's systemd timer."
echo " Check: systemctl status certbot.timer"
echo ""
