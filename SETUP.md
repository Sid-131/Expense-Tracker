# Expensio — Phase 1 Setup Guide

## Backend

### 1. Clone and configure environment
```bash
cp .env.example .env
# Edit .env with your values:
#   DB_PASSWORD  — strong random password
#   JWT_SECRET   — run: python -c "import secrets; print(secrets.token_hex(32))"
#   GOOGLE_CLIENT_ID — your Google OAuth Web Client ID
```

### 2. Start all services
```bash
docker compose up --build
```

Services started:
- PostgreSQL on internal network
- Redis on internal network
- FastAPI backend on internal network
- Nginx on port 80 (proxies /api/ to backend)

### 3. Run database migration
```bash
# In a separate terminal while docker compose is running:
docker compose exec backend alembic upgrade head
```

### 4. Verify backend is working
```
GET http://localhost/health         → {"status": "ok"}
GET http://localhost/docs           → Swagger UI
POST http://localhost/api/v1/auth/signup
  Body: {"name":"Test","email":"test@test.com","password":"123456"}
```

---

## Android App

### 1. Firebase setup (for Google Sign-In + FCM)
1. Go to [Firebase Console](https://console.firebase.google.com)
2. Create project "Expensio"
3. Add Android app with package `com.expensio`
4. Download `google-services.json` → place in `app/`
5. Enable **Authentication → Google** sign-in method
6. Copy the **Web Client ID** from Auth → Google → Web SDK configuration
7. Paste it in `app/src/main/res/values/strings.xml` replacing `YOUR_GOOGLE_WEB_CLIENT_ID`

> Firebase is used **only** for Google Sign-In token generation + FCM push notifications.
> No Firebase Auth or Firestore is used — all data goes to our self-hosted backend.

### 2. Backend URL
- **Development (emulator):** already set to `http://10.0.2.2:80/` in `app/build.gradle.kts`
- **Physical device (dev):** change to your machine's local IP, e.g. `http://192.168.1.x:80/`
- **Production:** update `BASE_URL` in the `release` build type in `app/build.gradle.kts`

### 3. Open in Android Studio
- Open Android Studio → Open → select `Expense-Tracker/`
- Let it sync Gradle
- Run on emulator or device

---

## Phase 1 Test Checklist

- [ ] `docker compose up` starts without errors
- [ ] `alembic upgrade head` creates `users` table
- [ ] `POST /api/v1/auth/signup` creates user, returns JWT
- [ ] `POST /api/v1/auth/login` returns JWT
- [ ] `GET /api/v1/users/me` with valid token returns user
- [ ] `GET /api/v1/users/me` without token returns 401
- [ ] Android: Sign up with email → lands on Home
- [ ] Android: Sign in with email → lands on Home
- [ ] Android: Google Sign-In → lands on Home
- [ ] Android: Logout → back to Login
- [ ] Android: Re-open app → stays logged in (token persisted)
