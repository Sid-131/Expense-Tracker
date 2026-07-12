# Expensio — Telegram-First Expense Tracker + Splitwise (Design Spec)

**Date:** 2026-07-06
**Revised:** 2026-07-09 — vision import and web dashboard pulled into first release
**Status:** Approved design, ready for implementation planning
**Author:** Siddh (with Claude)

---

## 1. Summary

Evolve Expensio from a group-splitting backend into a **Telegram-first** product that
combines a **personal expense tracker** with the existing **Splitwise-style group
splitting**, plus a **web dashboard** for viewing. **Recording** happens on the bot
(text or receipt screenshot); **viewing** happens on the dashboard.

The Android app is retained but its personal-tracker screens come later; all clients
share one FastAPI backend.

## 2. Decisions (locked)

| Area | Decision |
|---|---|
| Primary interface | Telegram bot for recording (text + screenshots); web dashboard for viewing; Android app deferred |
| Product model | **Two separate buckets**: personal tracker vs. existing group engine; unioned only for analytics reads |
| Bot connection | New thin-client container, **long-polling**, calls existing API with a cached JWT |
| Entry (text) | Lightweight **rule parser** now; LLM later behind the same interface |
| Splitting | Real users **and** guest contacts; inline **group name** (`split flat`) with create-if-missing |
| Images | Vision parser + itemized "exclude X" editing — **in first release**; provider-agnostic interface, **cloud (Claude vision API) at launch**, local ollama swap-in later |
| Web dashboard | New **read-focused** container/app; auth via **Telegram Login Widget**, reuses existing JWT issuance — no separate password |
| AI (text) | Phase 2, local ollama (:11434), cloud-switchable via config |
| First release | Bot (text + screenshot recording, personal + group splitting) **and** web dashboard (viewing) |

## 3. Architecture

New container **`expensio-telegram-bot`** (python-telegram-bot, long-polling) on the
existing compose project and `internal` network. It holds no database; it is a thin
client over the API.

Auth flow (reuses existing JWT auth):
1. On `/start`: bot calls `POST /api/v1/auth/telegram {telegram_id, name, username}`,
   protected by an internal `X-Service-Token`. Backend upserts a `User` keyed by
   `telegram_id` and returns a JWT.
2. Bot caches `telegram_id -> JWT` in the existing Redis.
3. All later actions call existing endpoints (`/expenses`, `/groups`, `/settlements`)
   plus new `/personal-expenses` as that user with the JWT.

Rationale: single source of truth (bot never touches Postgres), reuses JWT
authorization, zero-friction onboarding, and an isolated parser module as the AI seam.

Long-polling is chosen because the server sits behind home NAT; no public inbound or
SSL/webhook is required.

**Web dashboard** — new container `expensio-web` (Next.js, matches the existing stack),
added to the compose project, served behind the existing `expensio-nginx-1` reverse
proxy (new path/subdomain, no new public port). Read-focused: history, balances,
per-category analytics, unified monthly summary. Recording stays on the bot; the
dashboard does not duplicate the parser or confirm-card flows.

Auth (no new user store):
1. Dashboard embeds the **Telegram Login Widget**. Telegram handles the login UI and
   returns a signed payload (id, name, username, auth_date, hash) to the frontend.
2. Frontend posts that payload to `POST /api/v1/auth/telegram-widget`, a new endpoint
   that verifies the Telegram hash (HMAC with the bot token) instead of trusting an
   internal service token, then upserts/looks up the `User` by `telegram_id` — same
   row the bot's `/auth/telegram` upserts — and returns a JWT.
3. Frontend stores the JWT (httpOnly cookie) and calls the existing read endpoints
   (`/expenses`, `/personal-expenses`, `/groups`, `/balances`, analytics) directly.

This means a user who has only ever talked to the bot can log into the dashboard with
zero setup — same `telegram_id`, same account, no password ever created.

## 4. Data model

**`users` — add two nullable columns (Alembic migration):**
- `telegram_id` BIGINT, unique, nullable
- `telegram_username` VARCHAR(64), nullable

**New table `personal_expenses`:**
| column | type | notes |
|---|---|---|
| id | UUID PK | |
| user_id | UUID FK->users | owner |
| title | VARCHAR(200) | |
| amount | NUMERIC(10,2) | |
| category | VARCHAR(50) | default OTHER |
| spent_at | DATE | defaults today |
| note | VARCHAR(500) | nullable |
| source | VARCHAR(20) | telegram / app, default telegram |
| created_at / updated_at | timestamptz | |

**Categories:** strings (no table), one shared canonical list used by both buckets:
`FOOD, GROCERIES, TRANSPORT, SHOPPING, BILLS, ENTERTAINMENT, HEALTH, TRAVEL, OTHER`.
Defined once in `categories.py`, shared by parser + analytics.

**Unified analytics:** "this month" = a read that UNIONs `personal_expenses` (this user)
with this user's own share from `expense_splits`. Implemented as one new query in the
existing `analytics_service`. This is the ONLY place the two buckets meet, and it is
read-only.

No changes to `guests`, `groups`, `expenses`, `expense_splits`, `balances`,
`settlements`.

## 5. Separation of the two buckets

- **Storage:** distinct tables. Personal has `user_id` only, no group, no splits, no
  balances. Group requires `group_id` and carries splits/balances/settlements.
- **Code:** `personal_expense_service.py` + `/api/v1/personal-expenses` (plain CRUD, no
  split math) vs. existing `expense_service.py` / `settlement_service.py`.
- **Bot UX:** the parser routes on "is anyone else involved?" — a split keyword / group
  name / people -> group bucket; otherwise -> personal bucket. Explicit Personal vs
  Groups menus remove ambiguity.

## 6. Bot flows

- **Onboarding** `/start` -> register user (telegram_id), cache JWT, show Personal/Groups menu.
- **Log personal** `Coffee 200` -> confirm card (category/date/delete buttons) -> store in `personal_expenses`.
- **Split in a group** `Dinner 900 split flat`:
  - resolve group `flat` for the user; if missing, bot asks who's in it and creates it,
    then logs the expense.
  - split equally, update balances, reply with per-person breakdown.
  - `Dinner 900 split Rahul Aman` (names, no group) -> offer to add to a group or one-off split; remember the people.
- **Balances** Groups menu -> per-group "who owes whom" from existing balances +
  settlement-minimization.
- **Settle** `Settle flat` / button -> record `Settlement`, update balances.
- **Monthly summary** Personal menu -> unified union view (total + per-category).

Equal split is the MVP default; explicit amounts (`split flat 500 300 100`) are a later extension.

## 7. NLP parser and AI seam

Single stable interface:
```
parse(text) -> ParsedExpense { kind: personal|split, title, amount, category, target, spent_at }
```
Everything downstream depends only on this shape.

**Phase 1 — `RuleParser` (no AI):** regex amount; split target after `split`/`with`;
keyword->category map; date words (`yesterday`/`today`); if amount missing, ask one
follow-up rather than guess.

**Phase 2 — `LLMParser` on local ollama:** same interface; config flag
`PARSER=rule|llm` selects it; bot flows unchanged. Can point at a cloud model (e.g.
Anthropic API) via config for higher accuracy.

## 8. Screenshot / receipt import (first release, vision)

- Telegram photo message -> bot downloads image -> **vision parser**
  `parse_image(image) -> ParsedReceipt { merchant, items[{name, price}], total, category }`.
- Bot shows an **itemized confirm card** with running total.
- **Item-level editing** in natural language ("exclude rice", "remove milk", "add 20
  tip") mutates a **pending-expense session held in Redis**; total recomputes live.
- On confirm, store the **final adjusted expense** in the normal bucket (personal or
  split). Items are transient; an optional `receipt_items` JSONB snapshot column can be
  added later if itemized history is wanted (YAGNI for MVP).
- **Provider-agnostic** behind an interface; **default cloud (Claude vision via the
  Anthropic API) at launch** for accuracy on real-world receipts, config switch to local
  ollama once quality is proven there.
- **Money-accuracy mitigations:** always show the itemized total for human confirmation
  before saving; provider is configurable so accuracy can be dialed up or costs dialed
  down later.

## 9. Phasing

**Phase 1 (first release):** bot container (`RuleParser`, text entry); vision receipt
import via cloud API + itemized editing; `personal_expenses` table/service/endpoints;
balances, settle, unified monthly summary; Personal/Groups menus; **web dashboard**
(`expensio-web`, Telegram Login Widget auth, read-only views: history, balances,
analytics).

**Phase 2 (AI upgrade + cost):** `LLMParser` (text) on ollama; option to swap vision
from cloud to local ollama; NL queries + weekly insights.

**Later:** Android app gains personal-tracker screens on the same API. No bot or
dashboard dependency.

## 10. Out of scope (for now)

- Custom user-defined categories (category table)
- Persisted itemized receipt history
- Unequal/percentage splits via bot (backend already supports the data; bot UX later)
- Public webhook delivery / tunnel for the bot
- Recording/editing expenses from the web dashboard (dashboard is view-only in first
  release; all writes go through the bot)
- Android personal-tracker screens (deferred to Later)
