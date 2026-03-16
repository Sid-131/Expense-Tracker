# Expensio — Architecture Document

**App**: Expensio (Splitwise-like Expense Splitting App)
**Platform**: Android + WhatsApp Bot
**Scale**: 5,000–20,000 users
**Date**: March 2026

---

## 1. High-Level System Architecture

```
┌─────────────────────────────────────────┐
│      Android App (Kotlin + Compose)     │
│  Room DB ←── WorkManager Sync           │
└───────────────┬─────────────────────────┘
                │ HTTPS + JWT
        ┌───────▼─────────────────────────┐
        │       Nginx Reverse Proxy        │
        │       (SSL termination)          │
        └───────┬─────────────────────────┘
                │
        ┌───────▼─────────────────────────┐
        │     FastAPI Backend (Python)     │
        │     REST API + JWT Auth          │
        └───┬──────────────┬──────────────┘
            │              │
   ┌────────▼───┐   ┌──────▼──────┐
   │ PostgreSQL │   │    Redis     │
   │  (primary) │   │  (cache/OTP) │
   └────────────┘   └─────────────┘
            │
   ┌────────▼───────────────────┐
   │  Firebase Cloud Messaging   │
   │  (push notifications only)  │
   └────────────────────────────┘

┌─────────────────────────────────────────┐
│        WhatsApp Business API            │
│  User sends: "Dinner 900 Rahul Aman"    │
└───────────────┬─────────────────────────┘
                │ webhook
        ┌───────▼─────────────────────────┐
        │    POST /whatsapp/webhook        │
        │    WhatsApp Bot Service          │
        │    (inside FastAPI backend)      │
        └───────┬─────────────────────────┘
                │
        ┌───────▼─────────────────────────┐
        │     Expense Creation API         │
        │     PostgreSQL Database          │
        └─────────────────────────────────┘
```

---

## 2. Backend Architecture (FastAPI)

```
backend/
├── app/
│   ├── main.py                  FastAPI app init, router registration
│   ├── config.py                Settings (env vars, JWT secret, DB URL)
│   ├── database.py              SQLAlchemy engine + session
│   │
│   ├── models/                  SQLAlchemy ORM models
│   │   ├── user.py
│   │   ├── guest.py
│   │   ├── group.py
│   │   ├── expense.py
│   │   ├── balance.py
│   │   ├── settlement.py
│   │   ├── recurring_expense.py
│   │   ├── personal_expense.py
│   │   ├── notification.py
│   │   └── whatsapp_user.py
│   │
│   ├── schemas/                 Pydantic request/response models
│   │   ├── auth.py
│   │   ├── user.py
│   │   ├── group.py
│   │   ├── expense.py
│   │   ├── balance.py
│   │   ├── settlement.py
│   │   └── whatsapp.py
│   │
│   ├── api/v1/                  Route handlers
│   │   ├── auth.py              /auth/login, /auth/signup, /auth/google, /auth/otp
│   │   ├── users.py             /users/me, /users/{id}
│   │   ├── groups.py            /groups CRUD
│   │   ├── expenses.py          /expenses CRUD
│   │   ├── balances.py          /balances
│   │   ├── settlements.py       /settlements
│   │   ├── personal.py          /personal-expenses
│   │   ├── notifications.py     /notifications
│   │   └── whatsapp.py          /whatsapp/webhook
│   │
│   ├── services/                Business logic
│   │   ├── auth_service.py      JWT generation, Google OAuth verify, OTP
│   │   ├── expense_service.py   Split calculation, balance updates
│   │   ├── balance_service.py   Settlement algorithm
│   │   ├── notification_service.py  FCM push
│   │   ├── recurring_service.py     Recurring expense processor
│   │   └── whatsapp/
│   │       ├── webhook_handler.py   Receive + validate webhook
│   │       ├── message_parser.py    Parse text → expense data
│   │       └── bot_response.py      Format + send reply
│   │
│   ├── core/
│   │   ├── security.py          JWT encode/decode, password hashing
│   │   └── dependencies.py      get_current_user, get_db
│   │
│   └── utils/
│       └── fcm.py               Firebase Admin SDK (FCM only)
│
├── alembic/                     Database migrations
├── tests/
├── docker/
│   ├── Dockerfile
│   └── nginx.conf
├── docker-compose.yml
└── requirements.txt
```

---

## 3. Android App Architecture (MVVM + Clean Architecture)

```
┌────────────────────────────────────────────────┐
│  UI Layer (Jetpack Compose)                    │
│  Composable Screens → UI State (sealed class)  │
└─────────────────────┬──────────────────────────┘
                      │ observes StateFlow
┌─────────────────────▼──────────────────────────┐
│  ViewModel Layer                               │
└─────────────────────┬──────────────────────────┘
                      │ calls
┌─────────────────────▼──────────────────────────┐
│  Domain Layer (Use Cases, pure Kotlin)         │
└─────────────────────┬──────────────────────────┘
                      │ calls
┌─────────────────────▼──────────────────────────┐
│  Repository Layer (local-first)                │
└──────────┬──────────────────────┬──────────────┘
           │                      │
┌──────────▼──────┐   ┌───────────▼──────────────┐
│  Room (Local)   │   │  Retrofit API Client      │
│  Offline cache  │   │  → FastAPI Backend        │
└─────────────────┘   └──────────────────────────┘
```

### Android Module Structure
```
com.expensio/
├── data/
│   ├── local/           Room DB, DAOs, entities
│   ├── remote/          Retrofit API services
│   │   ├── api/         ExpenseApi, GroupApi, AuthApi...
│   │   └── dto/         Request/response DTOs
│   ├── repository/      Repository implementations
│   └── mapper/          DTO ↔ Domain ↔ Entity mappers
├── domain/
│   ├── model/           Pure Kotlin domain models
│   ├── repository/      Repository interfaces
│   └── usecase/         One class per use case
├── ui/
│   ├── auth/            Login, Signup screens
│   ├── home/            Dashboard
│   ├── groups/          Group CRUD, detail
│   ├── expenses/        Add/view/edit expenses
│   ├── guests/          Guest user management
│   ├── recurring/       Recurring expense management
│   ├── settlements/     Balance view, settle up
│   ├── personal/        Personal expenses + analytics
│   └── components/      Reusable composables
├── sync/                WorkManager sync tasks
├── notifications/       FCM service
├── di/                  Hilt modules
└── utils/               Extensions, constants
```

### UI State Pattern
```kotlin
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
```

---

## 4. Authentication Architecture

### Supported Methods
| Method | Flow |
|--------|------|
| Email/Password | Hash with bcrypt → JWT |
| Google OAuth | Google Sign-In SDK → send idToken to backend → verify with Google → JWT |
| Phone OTP | Send OTP via SMS → verify → JWT |

### JWT Flow
```
User logs in
      │
Backend validates credentials
      │
Backend returns:
  access_token  (expires: 24h)
  refresh_token (expires: 30d)
      │
Android stores in EncryptedSharedPreferences
      │
All API calls: Authorization: Bearer <access_token>
      │
Token expired → use refresh_token to get new access_token
```

### OTP Flow (Phone)
```
User enters phone number
      │
POST /auth/otp/send → backend generates OTP → stores in Redis (TTL 5min)
      │
SMS sent to user (via Twilio / MSG91)
      │
User enters OTP
      │
POST /auth/otp/verify → backend checks Redis → returns JWT
```

### Android Auth Storage
- JWT tokens: `EncryptedSharedPreferences`
- Auto-attach token: OkHttp Interceptor (`AuthInterceptor`)
- Token refresh: OkHttp Authenticator (auto-retry on 401)

---

## 5. PostgreSQL Database Schema

### `users`
```sql
CREATE TABLE users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(255) UNIQUE,
    phone       VARCHAR(20) UNIQUE,
    profile_pic VARCHAR(500),
    google_id   VARCHAR(255) UNIQUE,
    password_hash VARCHAR(255),           -- null for Google/OTP users
    fcm_token   VARCHAR(500),
    created_at  TIMESTAMPTZ DEFAULT NOW()
);
```

### `guests`
```sql
CREATE TABLE guests (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name           VARCHAR(100) NOT NULL,
    phone          VARCHAR(20),
    email          VARCHAR(255),
    created_by     UUID NOT NULL REFERENCES users(id),
    linked_user_id UUID REFERENCES users(id),    -- set when guest registers
    created_at     TIMESTAMPTZ DEFAULT NOW()
);
```

### `groups`
```sql
CREATE TABLE groups (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    created_by  UUID NOT NULL REFERENCES users(id),
    currency    VARCHAR(10) DEFAULT 'INR',
    created_at  TIMESTAMPTZ DEFAULT NOW()
);
```

### `group_members`
```sql
CREATE TABLE group_members (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id    UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    member_id   UUID NOT NULL,
    member_type VARCHAR(10) NOT NULL CHECK (member_type IN ('user', 'guest')),
    role        VARCHAR(20) DEFAULT 'member',
    joined_at   TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (group_id, member_id, member_type)
);
```

### `expenses`
```sql
CREATE TABLE expenses (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id     UUID NOT NULL REFERENCES groups(id),
    title        VARCHAR(200) NOT NULL,
    amount       DECIMAL(12,2) NOT NULL,
    paid_by      UUID NOT NULL,
    paid_by_type VARCHAR(10) NOT NULL CHECK (paid_by_type IN ('user', 'guest')),
    split_type   VARCHAR(20) NOT NULL CHECK (split_type IN ('EQUAL','PERCENTAGE','EXACT')),
    category     VARCHAR(50),
    note         TEXT,
    recurring_id UUID REFERENCES recurring_expenses(id),
    created_by   UUID NOT NULL REFERENCES users(id),
    created_at   TIMESTAMPTZ DEFAULT NOW()
);
```

### `expense_splits`
```sql
CREATE TABLE expense_splits (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    expense_id   UUID NOT NULL REFERENCES expenses(id) ON DELETE CASCADE,
    member_id    UUID NOT NULL,
    member_type  VARCHAR(10) NOT NULL,
    share_amount DECIMAL(12,2) NOT NULL,
    percentage   DECIMAL(5,2)    -- only for PERCENTAGE split type
);
```

### `balances`
```sql
CREATE TABLE balances (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id    UUID NOT NULL REFERENCES groups(id),
    member_id   UUID NOT NULL,
    member_type VARCHAR(10) NOT NULL,
    balance     DECIMAL(12,2) NOT NULL DEFAULT 0,
    updated_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (group_id, member_id, member_type)
);
```

### `settlements`
```sql
CREATE TABLE settlements (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id       UUID NOT NULL REFERENCES groups(id),
    from_member_id UUID NOT NULL,
    to_member_id   UUID NOT NULL,
    amount         DECIMAL(12,2) NOT NULL,
    status         VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING','COMPLETED')),
    created_at     TIMESTAMPTZ DEFAULT NOW()
);
```

### `recurring_expenses`
```sql
CREATE TABLE recurring_expenses (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id      UUID NOT NULL REFERENCES groups(id),
    title         VARCHAR(200) NOT NULL,
    amount        DECIMAL(12,2) NOT NULL,
    paid_by       UUID NOT NULL,
    paid_by_type  VARCHAR(10) NOT NULL,
    split_type    VARCHAR(20) NOT NULL,
    category      VARCHAR(50),
    frequency     VARCHAR(20) NOT NULL CHECK (frequency IN ('DAILY','WEEKLY','MONTHLY','YEARLY')),
    start_date    DATE NOT NULL,
    next_due_date DATE NOT NULL,
    end_date      DATE,
    is_active     BOOLEAN DEFAULT TRUE,
    created_by    UUID NOT NULL REFERENCES users(id),
    created_at    TIMESTAMPTZ DEFAULT NOW()
);
```

### `recurring_expense_splits`
```sql
CREATE TABLE recurring_expense_splits (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recurring_id UUID NOT NULL REFERENCES recurring_expenses(id) ON DELETE CASCADE,
    member_id    UUID NOT NULL,
    member_type  VARCHAR(10) NOT NULL,
    share_amount DECIMAL(12,2),
    percentage   DECIMAL(5,2)
);
```

### `personal_expenses`
```sql
CREATE TABLE personal_expenses (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users(id),
    title      VARCHAR(200) NOT NULL,
    amount     DECIMAL(12,2) NOT NULL,
    category   VARCHAR(50),
    note       TEXT,
    date       DATE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
```

### `notifications`
```sql
CREATE TABLE notifications (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users(id),
    type       VARCHAR(50) NOT NULL,
    title      VARCHAR(200),
    body       TEXT,
    metadata   JSONB,
    is_read    BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
```

### `whatsapp_users`
```sql
CREATE TABLE whatsapp_users (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone_number VARCHAR(20) NOT NULL UNIQUE,
    user_id      UUID NOT NULL REFERENCES users(id),
    is_verified  BOOLEAN DEFAULT FALSE,
    created_at   TIMESTAMPTZ DEFAULT NOW()
);
```

---

## 6. REST API Endpoints

### Auth
```
POST   /api/v1/auth/signup              Email/password register
POST   /api/v1/auth/login               Email/password login → JWT
POST   /api/v1/auth/google              Google idToken → JWT
POST   /api/v1/auth/otp/send            Send OTP to phone
POST   /api/v1/auth/otp/verify          Verify OTP → JWT
POST   /api/v1/auth/refresh             Refresh JWT
POST   /api/v1/auth/logout
```

### Users
```
GET    /api/v1/users/me                 Current user profile
PATCH  /api/v1/users/me                 Update profile
GET    /api/v1/users/search?q=email     Search user to add to group
```

### Groups
```
GET    /api/v1/groups                   List my groups
POST   /api/v1/groups                   Create group
GET    /api/v1/groups/{id}              Group detail + members
PATCH  /api/v1/groups/{id}              Edit group
DELETE /api/v1/groups/{id}
POST   /api/v1/groups/{id}/members      Add member (user or guest)
DELETE /api/v1/groups/{id}/members/{memberId}
```

### Expenses
```
GET    /api/v1/groups/{id}/expenses     List expenses in group
POST   /api/v1/groups/{id}/expenses     Add expense
GET    /api/v1/expenses/{id}            Expense detail
PATCH  /api/v1/expenses/{id}            Edit expense
DELETE /api/v1/expenses/{id}
```

### Balances & Settlements
```
GET    /api/v1/groups/{id}/balances     All balances in group
GET    /api/v1/groups/{id}/settlements  Settlement suggestions
POST   /api/v1/groups/{id}/settlements  Create settlement
PATCH  /api/v1/settlements/{id}         Mark as completed
```

### Recurring Expenses
```
GET    /api/v1/groups/{id}/recurring    List recurring expenses
POST   /api/v1/groups/{id}/recurring    Create recurring expense
PATCH  /api/v1/recurring/{id}           Edit / pause / resume
DELETE /api/v1/recurring/{id}
```

### Personal Expenses
```
GET    /api/v1/personal                 List personal expenses
POST   /api/v1/personal                 Add personal expense
PATCH  /api/v1/personal/{id}
DELETE /api/v1/personal/{id}
GET    /api/v1/personal/analytics       Monthly totals + category breakdown
```

### WhatsApp Bot
```
GET    /api/v1/whatsapp/webhook         Webhook verification (Meta handshake)
POST   /api/v1/whatsapp/webhook         Receive incoming messages
POST   /api/v1/whatsapp/link            Link WhatsApp number to account
```

---

## 7. WhatsApp Bot Architecture

### Full Flow
```
User sends WhatsApp message: "Dinner 900 Rahul Aman"
        │
WhatsApp Business API → POST /api/v1/whatsapp/webhook
        │
webhook_handler.py
  1. Verify X-Hub-Signature-256 header
  2. Extract phone number + message text
  3. Look up phone in whatsapp_users table
  4. If not found → reply "Link your account at expensio.app/link"
        │
message_parser.py
  1. Parse message text into expense data
  2. Resolve member names against group members
        │
expense_service.py
  1. Validate group membership
  2. Create expense + splits
  3. Update balances
        │
bot_response.py
  Format and send reply via WhatsApp API
```

### Message Parser Logic

**Equal split (default)**
```
Input:  "Dinner 900 Rahul Aman"

Parsed:
  title   = "Dinner"
  amount  = 900
  members = ["Rahul", "Aman"] + sender
  split   = EQUAL → 300 each (3 people)
```

**Exact split**
```
Input:  "Dinner 900 Rahul=400 Aman=500"

Parsed:
  title   = "Dinner"
  amount  = 900
  splits  = {Rahul: 400, Aman: 500}
  split   = EXACT
  validation: 400 + 500 == 900 ✓
```

**Paid by someone else**
```
Input:  "Dinner 900 paid:Rahul Aman"

Parsed:
  title   = "Dinner"
  amount  = 900
  paid_by = "Rahul"
  members = ["Rahul", "Aman"] + sender
```

**Bot reply format**
```
✅ Expense Added

📋 Dinner
💰 Total: ₹900

Split:
• Rahul   ₹300
• Aman    ₹300
• You     ₹300

Reply UNDO to cancel
```

**Error replies**
```
Account not linked:
  "👋 Hi! Link your WhatsApp to Expensio:
   expensio.app/link
   Then try again!"

Could not parse message:
  "❌ Couldn't understand that.
   Try: Dinner 900 Rahul Aman
   Or:  Dinner 900 Rahul=400 Aman=500"

Not in group:
  "❌ You must be in a group to add expenses.
   Open the app to create or join one."
```

### WhatsApp Bot Commands
| Command | Action |
|---------|--------|
| `<title> <amount> <members>` | Add equal split expense |
| `<title> <amount> <name>=<amt>...` | Add exact split expense |
| `balance` | Show your current balances |
| `UNDO` | Cancel last expense (within 5 min) |
| `help` | Show command guide |

---

## 8. Expense Calculation Engine (Backend Service)

Runs inside `expense_service.py` on every expense creation/edit/delete.

### Split Calculation
```python
# Equal
share = round(amount / member_count, 2)
# adjust last member for rounding remainder

# Percentage
share_i = round(amount * (pct_i / 100), 2)

# Exact
# validate sum(shares) == amount (allow ±1 unit)
```

### Balance Update (atomic DB transaction)
```python
with db.begin():
    # paidBy gets credit
    balance[paid_by] += (amount - payer_share)
    # each other member gets debited
    for member, share in splits.items():
        if member != paid_by:
            balance[member] -= share
```

---

## 9. Settlement Algorithm (Backend)

Simple greedy net balance approach (MVP).

```python
creditors = sorted([m for m in balances if m.balance > 0], reverse=True)
debtors   = sorted([m for m in balances if m.balance < 0])

while creditors and debtors:
    c, d = creditors[0], debtors[0]
    amount = min(c.balance, abs(d.balance))
    settlements.append(Settlement(from=d, to=c, amount=amount))
    c.balance -= amount
    d.balance += amount
    if c.balance == 0: creditors.pop(0)
    if d.balance == 0: debtors.pop(0)
```

---

## 10. Offline Support (Android)

```
User Action (add expense, etc.)
        │
Write to Room DB immediately (SyncStatus = PENDING)
        │
Show in UI instantly (optimistic)
        │
WorkManager SyncWorker (requires network)
        │
POST to FastAPI backend
    ├── 2xx → SyncStatus = SYNCED
    └── fail → SyncStatus = FAILED → exponential backoff retry
```

```kotlin
enum class SyncStatus { SYNCED, PENDING, FAILED }
```

WorkManager retry: 30s → 1m → 2m → 4m (exponential, max 10 retries)
Periodic background sync: every 15 min when network available

---

## 11. Push Notifications (FCM)

Firebase used **only** for push notifications.

```
Event occurs (expense added, settlement, etc.)
        │
FastAPI backend — notification_service.py
        │
Firebase Admin SDK → FCM
        │
Android device → FcmService.kt
    ├── Foreground → in-app banner
    └── Background → system notification
```

FCM token stored in `users.fcm_token`, updated on app start.

### Notification Types
| Type | Trigger |
|------|---------|
| `EXPENSE_ADDED` | New group expense |
| `GROUP_INVITE` | Added to a group |
| `SETTLEMENT_DONE` | Settlement completed |
| `RECURRING_DUE` | Recurring expense generated |
| `WHATSAPP_EXPENSE` | Expense added via WhatsApp bot |

---

## 12. Docker Deployment

### `docker-compose.yml`
```yaml
services:

  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: expensio
      POSTGRES_USER: expensio
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks: [internal]

  redis:
    image: redis:7-alpine
    networks: [internal]

  backend-api:
    build: ./backend
    environment:
      DATABASE_URL: postgresql://expensio:${DB_PASSWORD}@postgres/expensio
      REDIS_URL: redis://redis:6379
      JWT_SECRET: ${JWT_SECRET}
      WHATSAPP_VERIFY_TOKEN: ${WHATSAPP_VERIFY_TOKEN}
      WHATSAPP_API_TOKEN: ${WHATSAPP_API_TOKEN}
      FIREBASE_CREDENTIALS: /app/firebase-credentials.json
    depends_on: [postgres, redis]
    volumes:
      - ./firebase-credentials.json:/app/firebase-credentials.json:ro
    networks: [internal]

  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./docker/nginx.conf:/etc/nginx/conf.d/default.conf:ro
      - ./ssl:/etc/nginx/ssl:ro
    depends_on: [backend-api]
    networks: [internal]

volumes:
  postgres_data:

networks:
  internal:
    driver: bridge
```

**Rule:** Only Nginx exposes ports. All other services are internal.

### `docker/nginx.conf`
```nginx
upstream backend {
    server backend-api:8000;
}

server {
    listen 80;
    server_name yourdomain.com;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl;
    server_name yourdomain.com;

    ssl_certificate     /etc/nginx/ssl/cert.pem;
    ssl_certificate_key /etc/nginx/ssl/key.pem;

    location /api/ {
        proxy_pass http://backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

---

## 13. Backend Security

| Concern | Solution |
|---------|----------|
| API auth | JWT Bearer tokens (HS256, 24h expiry) |
| Password storage | bcrypt (rounds=12) |
| WhatsApp webhook | Verify `X-Hub-Signature-256` header |
| DB injection | SQLAlchemy ORM (parameterised queries) |
| Rate limiting | Nginx rate limiting + Redis |
| CORS | FastAPI CORS middleware (whitelist Android app) |
| Secrets | `.env` file, never committed to git |
| HTTPS | Nginx SSL + Let's Encrypt |

**Authorization rules (enforced in API layer):**
- Users can only read groups they belong to
- Only group members can add expenses to that group
- Only expense creator or group admin can edit/delete
- WhatsApp users must be verified + in the target group

---

## 14. Android Key Libraries

| Purpose | Library |
|---------|---------|
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose |
| DI | Hilt |
| Local DB | Room |
| Async | Kotlin Coroutines + Flow |
| HTTP client | Retrofit + OkHttp |
| Auth token storage | EncryptedSharedPreferences |
| Background sync | WorkManager |
| Push notifications | Firebase Messaging (FCM only) |
| Google Sign-In | play-services-auth |
| Charts | MPAndroidChart |
| Image loading | Coil |
| Logging | Timber |
| Crash reporting | Firebase Crashlytics |

---

## 15. Backend Key Libraries

```
fastapi
uvicorn[standard]
sqlalchemy[asyncio]
asyncpg                 # async PostgreSQL driver
alembic                 # DB migrations
pydantic[email]
python-jose[cryptography]  # JWT
passlib[bcrypt]         # password hashing
redis[asyncio]          # Redis client
httpx                   # async HTTP (WhatsApp API calls)
firebase-admin          # FCM push notifications
google-auth             # verify Google idToken
python-dotenv
```

---

## 16. Estimated Server Cost (Self-Hosted)

| Resource | Spec |
|----------|------|
| Server | VPS 2 vCPU / 4GB RAM (Hetzner / DigitalOcean ~$10–15/month) |
| Storage | 40GB SSD |
| Bandwidth | Usually included |
| FCM | Free |
| WhatsApp Business API | Free tier: 1000 conversations/month |
| **Total** | **~$10–20/month** |

---

## 17. Confirmed Product Decisions

| Decision | Value |
|----------|-------|
| App name | **Expensio** |
| Package name | `com.expensio` |
| Backend | Self-hosted FastAPI + PostgreSQL (Docker) |
| Firebase usage | FCM push notifications only |
| Auth | Email/password + Google OAuth + Phone OTP (JWT) |
| Guest users | Yes — `guests` table, member_type field |
| Recurring expenses | Yes — `recurring_expenses` table, processed by cron |
| WhatsApp bot | Yes — webhook parser, `whatsapp_users` table |

---

*Last updated: March 2026*
