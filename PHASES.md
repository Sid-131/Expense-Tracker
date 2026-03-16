# Expensio — Build Phases

Each phase is independently deployable and testable.
Complete and verify each phase before moving to the next.

---

## Phase 1 — Backend Foundation + Auth
**Goal:** FastAPI server running in Docker. Login/signup working end-to-end.

### Backend Deliverables
- Docker Compose setup (FastAPI + PostgreSQL + Redis + Nginx)
- Alembic migrations for `users` table
- `POST /auth/signup` — email/password register
- `POST /auth/login` — email/password → JWT access + refresh tokens
- `POST /auth/google` — Google idToken → verify → JWT
- `POST /auth/otp/send` + `POST /auth/otp/verify` — phone OTP via Redis
- `POST /auth/refresh` — refresh JWT
- `GET /users/me` — protected route test
- Password hashed with bcrypt
- JWT middleware on all protected routes

### Android Deliverables
- Android project setup (Kotlin, Compose, Hilt, Room, Retrofit)
- Retrofit client with JWT interceptor + auto-refresh authenticator
- Login screen (email/password + Google Sign-In)
- Signup screen
- JWT stored in EncryptedSharedPreferences
- Bottom navigation shell (Home, Groups, Personal, Profile)
- Auth state checked on launch → route to Home or Login

### Test Checklist
- [ ] Docker Compose starts all services cleanly
- [ ] `POST /auth/signup` creates user in PostgreSQL
- [ ] `POST /auth/login` returns valid JWT
- [ ] `GET /users/me` with valid token → 200
- [ ] `GET /users/me` without token → 401
- [ ] Google Sign-In on Android → JWT returned from backend
- [ ] Phone OTP sent → verified → JWT returned
- [ ] JWT stored in EncryptedSharedPreferences
- [ ] Re-open app → stays logged in
- [ ] Logout clears token + goes to Login screen

---

## Phase 2 — Groups & Guest Users
**Goal:** Create groups, add registered members and guests.

### Backend Deliverables
- Migrations: `groups`, `group_members`, `guests` tables
- `POST /groups` — create group
- `GET /groups` — list my groups
- `GET /groups/{id}` — group detail + members
- `POST /groups/{id}/members` — add user (by email search) or guest (by name)
- `DELETE /groups/{id}/members/{memberId}` — remove member
- `GET /users/search?q=` — search registered users by email
- Authorization: only group members can read/modify group

### Android Deliverables
- Group list screen
- Create group screen
- Group detail screen (members list, registered vs guest badge)
- Add member: search by email OR add guest by name
- Leave group

### Test Checklist
- [ ] Create group → appears in list
- [ ] Add registered user to group by email
- [ ] Add guest (name only, no account) to group
- [ ] Guest shows with "Guest" badge in member list
- [ ] Non-member cannot fetch group data (403)
- [ ] Leave group → removed from list

---

## Phase 3 — Add & View Expenses
**Goal:** Add expenses with all 3 split types. Balances calculated server-side.

### Backend Deliverables
- Migrations: `expenses`, `expense_splits`, `balances` tables
- `POST /groups/{id}/expenses` — create expense
  - Runs split calculation
  - Updates balances atomically (DB transaction)
- `GET /groups/{id}/expenses` — list with pagination
- `GET /expenses/{id}` — expense detail
- `PATCH /expenses/{id}` — edit (recalculate balances)
- `DELETE /expenses/{id}` — delete (reverse balances)
- Split validation: sum of splits must equal total amount (±1 unit)

### Android Deliverables
- Add expense screen (title, amount, category, paid by, split type)
- Split type UI: Equal / Percentage / Exact
- Paid by: any group member (registered or guest)
- Expense list per group
- Expense detail screen
- Edit / delete expense

### Test Checklist
- [ ] Equal split: ₹900 / 3 = ₹300 each
- [ ] Percentage split: validates sum = 100%
- [ ] Exact split: validates sum = total amount
- [ ] Guest can be "paid by"
- [ ] Balances update correctly after expense added
- [ ] Edit expense → balances recalculate
- [ ] Delete expense → balances reversed

---

## Phase 4 — Balances & Settlements
**Goal:** View who owes what. Settle up.

### Backend Deliverables
- `GET /groups/{id}/balances` — all members' balances
- `GET /groups/{id}/settlements` — suggested settlements (greedy algorithm)
- `POST /groups/{id}/settlements` — record settlement
- `PATCH /settlements/{id}` — mark as completed → update balances
- Migration: `settlements` table

### Android Deliverables
- Balance summary screen (per group)
- Green = owed to you, Red = you owe
- Settlement suggestions list
- Settle up confirmation flow
- Settlement history

### Test Checklist
- [ ] Balance screen shows correct net amounts
- [ ] Settlement suggestions add up to zero all balances
- [ ] Mark settlement complete → balance goes to 0
- [ ] Guest balances shown and settleable

---

## Phase 5 — Offline Support (Android)
**Goal:** App fully usable without internet. Auto-syncs when back online.

### Android Deliverables
- Room entities for all data (expenses, groups, balances, guests)
- `SyncStatus` enum on all write entities
- `SyncWorker` (WorkManager) — pushes pending items to backend on network
- Optimistic UI — show changes immediately before sync
- Exponential backoff on sync failure
- "Pending sync" indicator in UI when offline changes exist
- Periodic background sync every 15 min

### Test Checklist
- [ ] Turn off wifi → add expense → appears in UI immediately
- [ ] Turn wifi on → expense syncs to backend
- [ ] Kill app offline → reopen → unsync'd data still visible
- [ ] Conflict on sync → server version wins, UI refreshes
- [ ] Pending indicator visible when offline changes exist

---

## Phase 6 — Recurring Expenses
**Goal:** Expenses that auto-generate on a schedule.

### Backend Deliverables
- Migrations: `recurring_expenses`, `recurring_expense_splits` tables
- `POST /groups/{id}/recurring` — create recurring expense
- `GET /groups/{id}/recurring` — list
- `PATCH /recurring/{id}` — edit / pause / resume / delete
- Background cron job (APScheduler inside FastAPI): runs daily
  - Queries `next_due_date <= today AND is_active = true`
  - Creates expense, updates balances, advances `next_due_date`
  - Sends `RECURRING_DUE` FCM notification

### Android Deliverables
- Create recurring expense screen (title, amount, split, frequency, start date, optional end date)
- Recurring expense list per group
- Pause / resume / delete
- "Recurring" badge on auto-generated expenses in list

### Test Checklist
- [ ] Create monthly recurring → stored with correct `next_due_date`
- [ ] Cron triggers → expense generated → `next_due_date` advances
- [ ] Pause recurring → no new expenses
- [ ] Delete recurring → future stops, past remains
- [ ] `RECURRING_DUE` notification received on generation

---

## Phase 7 — Personal Expenses & Analytics
**Goal:** Individual spending tracker with charts.

### Backend Deliverables
- Migration: `personal_expenses` table
- `POST /personal` — add personal expense
- `GET /personal` — list (filterable by month, category)
- `PATCH/DELETE /personal/{id}`
- `GET /personal/analytics` — monthly totals + category breakdown

### Android Deliverables
- Add personal expense screen (title, amount, category, date, note)
- Personal expense list (filter by month/category)
- Analytics screen:
  - Monthly total
  - Category pie chart (MPAndroidChart)
  - Monthly trend bar chart
- Budget alerts (set per-category limit, notify on exceed)

### Categories
`FOOD, TRANSPORT, SHOPPING, ENTERTAINMENT, HEALTH, UTILITIES, RENT, OTHER`

### Test Checklist
- [ ] Add personal expense → appears in list
- [ ] Filter by month/category works
- [ ] Pie chart shows correct percentages
- [ ] Bar chart shows trend across months
- [ ] Budget alert triggers when limit exceeded
- [ ] Personal expenses isolated from group views

---

## Phase 8 — Push Notifications (FCM)
**Goal:** Real-time push notifications for all key events.

### Backend Deliverables
- `firebase-admin` SDK setup with service account credentials
- `notification_service.py` — sends FCM to `users.fcm_token`
- Trigger FCM on:
  - Expense added (notify all group members except creator)
  - Added to group
  - Settlement marked complete
  - Recurring expense generated
- Migration: `notifications` table (inbox)
- `GET /notifications` — list notifications
- `PATCH /notifications/{id}/read` — mark read

### Android Deliverables
- FCM token saved to backend on login + token refresh
- `FcmService.kt` handles foreground (in-app banner) + background (system notification)
- Notifications inbox screen (mark read, clear all)
- Tapping notification → deep link to correct screen

### Test Checklist
- [ ] User A adds expense → User B gets push notification
- [ ] Added to group → `GROUP_INVITE` push received
- [ ] Recurring generated → `RECURRING_DUE` notification
- [ ] Notification tapped → opens correct screen
- [ ] In-app banner shown when app in foreground
- [ ] `notifications` inbox shows all received

---

## Phase 9 — WhatsApp Expense Bot
**Goal:** Add expenses from WhatsApp. No app needed.

### Backend Deliverables
- Migration: `whatsapp_users` table
- `GET /whatsapp/webhook` — Meta verification handshake
- `POST /whatsapp/webhook` — receive messages
  - Verify `X-Hub-Signature-256`
  - Look up `whatsapp_users` by phone
  - Parse message → expense data
  - Create expense via `expense_service`
  - Reply via WhatsApp Business API
- `POST /whatsapp/link` — link WhatsApp number to Expensio account
- Message parser handles:
  - Equal split: `Dinner 900 Rahul Aman`
  - Exact split: `Dinner 900 Rahul=400 Aman=500`
  - `balance` command
  - `UNDO` command (within 5 min)
  - `help` command

### Android Deliverables
- Link WhatsApp screen (enter phone → receive verification code via WhatsApp)
- WhatsApp-linked badge on profile
- Expenses added via bot visible in app with "via WhatsApp" label

### Test Checklist
- [ ] Meta webhook verification passes (GET challenge)
- [ ] Unknown phone → bot replies with link instructions
- [ ] `Dinner 900 Rahul Aman` → expense created, equal split
- [ ] `Dinner 900 Rahul=400 Aman=500` → exact split created
- [ ] `balance` command → correct balance shown
- [ ] `UNDO` within 5 min → expense deleted
- [ ] Webhook with invalid signature → 403 rejected
- [ ] User not in group → bot returns error message

---

## Phase 10 — Polish & Production
**Goal:** Production-ready. Security hardened. Play Store ready.

### Deliverables
- API rate limiting (Nginx + Redis)
- Input validation on all endpoints (Pydantic)
- Database indexes on all foreign keys + frequent query fields
- Query optimization (no N+1)
- HTTPS via Let's Encrypt (Certbot)
- Server monitoring: Uptime Kuma or similar
- Firebase Crashlytics integrated in Android
- Android performance profiling (cold start < 2s)
- Proguard / R8 minification
- App icon + splash screen finalized
- Play Store listing (screenshots, description, privacy policy)
- Signed release APK / AAB

### Test Checklist
- [ ] SQL injection attempt → blocked
- [ ] Unauthorized group access → 403
- [ ] WhatsApp webhook without signature → rejected
- [ ] API rate limit triggers on abuse
- [ ] HTTPS enforced (HTTP → 301 redirect)
- [ ] Cold start under 2 seconds
- [ ] Release build runs without crashes
- [ ] Crashlytics receives test crash correctly

---

## Phase Summary

| Phase | Focus | Key Output |
|-------|-------|------------|
| 1 | Backend + Auth | Docker running, JWT auth working |
| 2 | Groups + Guests | Groups created, guests supported |
| 3 | Expenses | All split types, balances calculated |
| 4 | Settlements | Settle up flow |
| 5 | Offline (Android) | Full offline + background sync |
| 6 | Recurring | Auto-generated on schedule |
| 7 | Personal + Analytics | Charts, budgets |
| 8 | Push Notifications | FCM for all events |
| 9 | WhatsApp Bot | Add expenses from WhatsApp |
| 10 | Polish + Launch | Play Store ready |

---

*Start with Phase 1. Do not move to the next phase until the test checklist is fully green.*
