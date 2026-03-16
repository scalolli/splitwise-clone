# Iteration Backlog

Slices are ordered to deliver user-visible value as early as possible.
Each slice cuts through all layers (HTTP → service → repo → DB) and delivers
something a user can actually see or do.

**Status legend:** `todo` | `in-progress` | `done`

---

## Phase 0 — Foundation (done)

### SLICE-001 — Gradle project scaffold `done`
### SLICE-002 — Kotlin CI pipeline `done`
### SLICE-002A — Postgres test infrastructure `done`

---

## Phase 1 — Domain model (done)

### SLICE-003 — Money value object `done`
### SLICE-004 — Core domain entities `done`
### SLICE-005 — Balance calculator `done`
### SLICE-006 — Expense validator `done`

---

## Phase 2 — Persistence (done except settlements)

### SLICE-007 — Database setup and Flyway `done`
### SLICE-008 — User repository `done`
### SLICE-009 — Group repository `done`
### SLICE-010 — Expense repository `done`

---

## Phase 3 — First working app (vertical slices)

Each slice below is end-to-end: HTTP handler + service + template + any missing repo.
By the end of SLICE-V03 the app is fully usable.

---

### SLICE-V01 — Register, login, home page `todo`

**Outcome:** A user can sign up, log in, and see a list of groups. This is the first
slice where a browser can interact with the app end-to-end.

Tests to write first:
- `GET /register` → 200 with form
- `POST /register` valid → redirect to `/login`, flash "Your account has been created"
- `POST /register` missing fields → 400 with inline errors
- `POST /register` duplicate username → "Username already exists"
- `POST /register` duplicate email → "Email already exists"
- `POST /register` passwords don't match → "Passwords do not match"
- Password is BCrypt-hashed in DB
- `GET /login` → 200 with form
- `POST /login` valid → session cookie set, redirect to `/`
- `POST /login` invalid → "Invalid username or password"
- `GET /logout` → session cleared, redirect to `/`, flash "You have been logged out"
- `GET /` → 200 with group list and username list
- `GET /` works without a session cookie (public)
- Protected route without session → 302 to `/login` with flash

Implementation:
- `service/UserService.kt` — `register(...)`, `authenticate(...)`
- `web/AuthHandler.kt` — GET/POST `/register`, GET/POST `/login`, GET `/logout`
- `web/MainHandler.kt` — GET `/`
- `web/SessionFilter.kt` — injects `UserId` into request or redirects
- `templates/base.hbs`, `templates/register.hbs`, `templates/login.hbs`, `templates/index.hbs`
- BCrypt dependency added to `build.gradle.kts`

Done when: all handler tests pass; a real browser can register, log in, and see `/`.

---

### SLICE-V02 — Group detail page `todo`

**Outcome:** An authenticated user can view a group: its members, expenses, and
calculated balances. The core value proposition is visible for the first time.

Tests to write first:
- `GET /group/{id}` → 200 with members listed
- `GET /group/{id}` → expenses listed (description, amount, payer)
- `GET /group/{id}` → balance figures correct against known fixture data
- `GET /group/{id}` → 404 for non-existent group
- Unauthenticated → redirect to `/login`

Implementation:
- `service/BalanceService.kt` — wires `BalanceCalculator` + `ExpenseRepository` + `SettlementRepository`
- `persistence/SettlementRepository.kt` — built here, only when needed
- `web/GroupHandler.kt` — GET `/group/{id}`
- `templates/group.hbs`

Done when: handler tests pass including correct balance figures.

---

### SLICE-V03 — Create group and add expense `todo`

**Outcome:** An authenticated user can create a group and add an expense. The app
is now fully usable end-to-end for the happy path.

Tests to write first:
- `GET /group/create` → 200
- `POST /group/create` valid → redirect to `/group/{id}`, flash "Group created successfully"
- `POST /group/create` missing name → "Group name is required"
- Creator is a member of the new group
- `GET /group/{id}/add_expense` → 200 (member only)
- `POST /group/{id}/add_expense` valid → expense + shares saved, redirect to group page
- `POST /group/{id}/add_expense` — all 7 validation rules (see SLICE-006)
- Non-member cannot access add expense page

Implementation:
- `service/GroupService.kt` — `createGroup(...)`
- `service/ExpenseService.kt` — `addExpense(...)`
- `web/GroupHandler.kt` — GET/POST `/group/create`
- `web/ExpenseHandler.kt` — GET/POST `/group/{id}/add_expense`
- `templates/create_group.hbs`, `templates/add_expense.hbs`

Done when: all handler tests pass; a user can complete the full register → create
group → add expense flow in a browser.

---

## Phase 4 — Group and expense management

### SLICE-V04 — Edit group and manage members `todo`

**Outcome:** Group creator can rename the group, add members, and remove members.

Tests to write first:
- `GET /group/{id}/edit` → 200 for creator; redirect + flash for non-creator
- `POST /group/{id}/edit` valid → name/description updated, redirect to group page
- Add member via edit form → member appears in group
- Add non-existent user → "User not found"
- Add existing member → "User is already a member"
- `POST /group/{id}/add_member` valid → member added, redirect
- `POST /group/{id}/remove_member/{userId}` → member removed
- Cannot remove creator → "Cannot remove the group creator"
- Non-creator attempt → permission error

Implementation:
- `GroupService.editGroup(...)`, `GroupService.addMember(...)`, `GroupService.removeMember(...)`
- `web/GroupHandler.kt` — GET/POST `/group/{id}/edit`, POST `/group/{id}/add_member`,
  POST `/group/{id}/remove_member/{userId}`
- `templates/edit_group.hbs`

Done when: all handler tests pass.

---

### SLICE-V05 — Edit and delete expense `todo`

**Outcome:** Expense payer or group creator can edit or delete an expense.

Tests to write first:
- `GET /expenses/{id}/edit` → 200 for payer or creator; 403 for others; redirect for unauthenticated
- `POST /expenses/{id}/edit` valid → shares replaced atomically, redirect to group page
- Same 7 validation rules enforced
- `POST /expenses/{id}/delete` → expense + shares deleted, redirect to group page
- Non-payer/non-creator → 403
- 404 for non-existent expense

Implementation:
- `ExpenseService.editExpense(...)`, `ExpenseService.deleteExpense(...)`
- `web/ExpenseHandler.kt` — GET/POST `/expenses/{id}/edit`, POST `/expenses/{id}/delete`
- `templates/edit_expense.hbs`

Done when: all handler tests pass; cascade delete verified.

---

## Phase 5 — Settlements

### SLICE-V06 — Record settlement and settlement history `todo`

**Outcome:** Group member can record a settlement and view settlement history.
Balances on the group page reflect settlements.

Tests to write first:
- `POST /group/{id}/settle` valid → settlement persisted, redirect to group page
- Non-member cannot record settlement
- Amount must be > 0
- `from_user_id == to_user_id` → validation error
- Either user not a group member → validation error
- `GET /group/{id}/settlements` → 200 with list in descending date order
- Non-member cannot view settlement history
- Record settlement → balance on group page decreases
- Full settlement → no balance shown for that pair

Implementation:
- `service/SettlementService.kt` — `record(...)`, `forGroup(...)`
- `web/SettlementHandler.kt` — POST `/group/{id}/settle`, GET `/group/{id}/settlements`
- Settlement form on `group.hbs`, `templates/settlements.hbs`

Done when: all handler tests pass; end-to-end balance test passes.

---

## Phase 6 — Hardening

### SLICE-V07 — Error pages `todo`

**Outcome:** 400/403/404/500 return styled HTML pages, not stack traces.

Tests to write first:
- `GET /nonexistent` → 404 HTML page
- Accessing a resource without permission → 403 HTML page

Implementation:
- `web/ErrorHandler.kt` — applied as top-level filter
- `templates/error.hbs`

Done when: tests pass; no stack traces visible to end user.

---

### SLICE-V08 — Config and deployment `todo`

**Outcome:** App is configurable via environment variables and can be deployed.

Tests to write first:
- None (config smoke test only: app boots with all defaults)

Implementation:
- `config/AppConfig.kt` — reads `DATABASE_URL`, `PORT`, `SESSION_SECRET`
- `Procfile` or `Dockerfile`
- Update `render.yaml` to include Kotlin app service

Done when: `./gradlew run` boots with defaults; all existing tests still pass.

---

### SLICE-V09 — PWA manifest and service worker `todo`

**Outcome:** App is installable and caches the app shell for faster repeat visits.

Tests to write first:
- `GET /manifest.webmanifest` → 200 with valid content type
- Manifest includes name, start URL, display mode, icon entries
- `GET /service-worker.js` → 200

Implementation:
- `src/main/resources/public/manifest.webmanifest`
- `src/main/resources/public/service-worker.js`
- Static asset routing; base template registration script

Done when: browser install prompts recognise the app as installable.
