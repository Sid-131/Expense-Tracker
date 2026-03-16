# Expensio — Server Setup Guide

## Your Server
- **OS:** Fedora Linux 43 (Server Edition)
- **CPU:** AMD Ryzen 5 3550H
- **RAM:** 7.4 GB
- **Type:** Home server (no public IP → use Cloudflare Tunnel)

---

## How the internet reaches your home server

Your server is behind your home router (no public IP).
**Cloudflare Tunnel** solves this for free — it creates a secure outbound connection from your server to Cloudflare, which gives you a public HTTPS URL.

```
Android App
    │  HTTPS
    ▼
Cloudflare (free CDN + tunnel)
    │  secure tunnel (outbound from your server)
    ▼
Your Home Server (Fedora)
    │
    ▼
Docker: Nginx → FastAPI → PostgreSQL
```

No port forwarding. No public IP. **100% free.**

---

## Step 1 — Run setup script on your server

SSH into your Fedora server, then:

```bash
curl -o setup.sh https://raw.githubusercontent.com/YOUR_USERNAME/Expense-Tracker/main/server/setup.sh
bash setup.sh
```

This installs: **Docker, Docker Compose, cloudflared, firewall rules.**

---

## Step 2 — Clone your repo

```bash
git clone https://github.com/YOUR_USERNAME/Expense-Tracker.git /opt/expensio
cd /opt/expensio
```

---

## Step 3 — Fill in .env

```bash
cp .env.example .env
nano .env
```

### DB_PASSWORD
Any strong password. Used only inside Docker.
```bash
# Generate one:
openssl rand -base64 24
# Example result: K8mP2xQvN9rL4wTjF7hBcD==
```

### JWT_SECRET
Random string to sign login tokens.
```bash
# Generate one:
openssl rand -hex 32
# Example result: a3f8c2d1e9b4...
```

### GOOGLE_CLIENT_ID
For "Sign in with Google" on Android.

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Create project → **Expensio**
3. Add Android app → package: `com.expensio`
4. Authentication → Sign-in method → Enable **Google**
5. Web SDK configuration → copy **Web client ID**
6. It looks like: `123456789-abcdef.apps.googleusercontent.com`

Your `.env` should look like:
```
DB_PASSWORD=K8mP2xQvN9rL4wTjF7hBcD==
JWT_SECRET=a3f8c2d1e9b4...
GOOGLE_CLIENT_ID=123456789-abc.apps.googleusercontent.com
DEBUG=false
```

---

## Step 4 — Deploy

```bash
bash /opt/expensio/server/deploy.sh
```

Verify it's working locally:
```bash
curl http://localhost/health
# → {"status":"ok"}
```

---

## Step 5 — Expose to internet with Cloudflare Tunnel

### Option A — Quick test URL (no account needed, temporary)
```bash
cloudflared tunnel --url http://localhost:80
```
You get a random URL like `https://random-name.trycloudflare.com` — good for testing.

---

### Option B — Permanent free URL with your domain (recommended)

**Prerequisites:**
- Free Cloudflare account: [cloudflare.com](https://cloudflare.com)
- A domain added to Cloudflare (you can get a free `.is-a.dev` subdomain or similar, or buy `.com` for $10/yr)

```bash
bash /opt/expensio/server/tunnel.sh
```

Follow the prompts:
1. Browser opens → click **Authorize**
2. Edit `~/.cloudflared/config.yml` — replace `api.YOURDOMAIN.COM` with your domain
3. Create DNS record:
   ```bash
   cloudflared tunnel route dns expensio api.yourdomain.com
   ```

Your API will be permanently at `https://api.yourdomain.com/api/v1/`

---

## Step 6 — Update Android app with your URL

In `app/build.gradle.kts`, update the release build type:
```kotlin
buildConfigField("String", "BASE_URL", "\"https://api.yourdomain.com/\"")
```

For the debug build (testing on physical device connected to same WiFi):
```kotlin
// Use your server's local IP (find it with: ip addr show)
buildConfigField("String", "BASE_URL", "\"http://192.168.1.XXX/\"")
```

---

## Useful commands

```bash
# View live backend logs
docker compose -f /opt/expensio/docker-compose.yml logs -f backend

# Restart backend
docker compose -f /opt/expensio/docker-compose.yml restart backend

# Check all containers
docker compose -f /opt/expensio/docker-compose.yml ps

# Open database shell
docker compose -f /opt/expensio/docker-compose.yml exec postgres psql -U expensio

# Check Cloudflare Tunnel status
systemctl status cloudflared
journalctl -u cloudflared -f

# Update app (pull new code + redeploy)
cd /opt/expensio && git pull && bash server/deploy.sh
```

---

## Phase 1 Test Checklist

```bash
# On your server:
curl http://localhost/health
# → {"status":"ok"}

curl -X POST http://localhost/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","email":"test@test.com","password":"123456"}'
# → {"access_token":"...","refresh_token":"..."}

curl http://localhost/api/v1/users/me \
  -H "Authorization: Bearer <access_token_from_above>"
# → {"id":"...","name":"Test","email":"test@test.com",...}
```
