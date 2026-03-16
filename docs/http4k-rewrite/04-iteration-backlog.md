# Iteration Backlog

Slices are ordered by dependency. Each slice is the smallest independently
deliverable unit of behavior. Work top-to-bottom. Do not skip slices unless
explicitly marked as optional.

**Status legend:** `todo` | `in-progress` | `done`

---

## Phase 0 — Foundation

### SLICE-001 — Gradle project scaffold `todo`
**Outcome:** `kotlin-app/` exists and compiles with a `GET /health` endpoint.

Tests to write first:
- `HealthCheckTest`: `GET /health` returns `200 OK` with body `{"status":"ok"}`

Implementation:
- `build.gradle.kts` with http4k-core, http4k-server-jetty, http4k-testing-junit5, JUnit 5
- `settings.gradle.kts`
- `App.kt` — starts Jetty on configured port
- `HealthHandler.kt` — returns fixed JSON response
- `SplitwiseApp.kt` — root router

Done when:
- `./gradlew test` passes
- `./gradlew run` boots the server

---

### SLICE-002 — Kotlin CI pipeline `todo`
**Outcome:** GitHub Actions runs Kotlin build and tests on every push.

Tests to write first:
- None (CI config only)

Implementation:
- `.github/workflows/kotlin.yml` — runs `./gradlew test` on `ubuntu-latest`
- Separate from the Python workflow

Done when:
- Push to `main` triggers Kotlin CI and it passes

---

## Phase 1 — Domain model

### SLICE-003 — Money value object `todo`
**Outcome:** `Money` is a type-safe, `BigDecimal`-backed value object with arithmetic.

Tests to write first:
- `Money(10.00)` equals `Money(10.00)`
- `Money(10.00) + Money(5.00)` equals `Money(15.00)`
- `Money(10.00) - Money(3.00)` equals `Money(7.00)`
- `Money(0)` is valid; `Money(-1)` throws `IllegalArgumentException`
- Arithmetic preserves scale (2 decimal places)

Implementation:
- `domain/Money.kt`

Done when: all unit tests pass, no framework imports.

---

### SLICE-004 — Core domain entities `todo`
**Outcome:** `User`, `Group`, `Expense`, `ExpenseShare`, `Settlement` defined as
plain data classes with typed IDs.

Tests to write first:
- Value object identity: `UserId(1) == UserId(1)`, `UserId(1) != UserId(2)`
- Entity construction with all required fields

Implementation:
- `domain/UserId.kt`, `domain/GroupId.kt`, `domain/ExpenseId.kt`, `domain/SettlementId.kt`
- `domain/User.kt`, `domain/Group.kt`, `domain/Expense.kt`,
  `domain/ExpenseShare.kt`, `domain/Settlement.kt`

Done when: all entity and ID tests pass, zero external dependencies.

---

### SLICE-005 — Balance calculator `todo`
**Outcome:** `BalanceCalculator` correctly computes net pairwise balances from
expenses and subtracts settlements.

Tests to write first:
- Single expense: A pays £90 split equally (£30 each) among A, B, C → B owes A £30, C owes A £30
- Reciprocal debts: A owes B £30, B owes A £10 → A owes B £20
- No balance when fully settled
- Partial settlement reduces displayed balance
- Zero balance pair is not included in results
- Multiple expenses, multiple payers

Implementation:
- `domain/BalanceCalculator.kt`

Done when: all unit tests pass.

---

### SLICE-006 — Expense validator `todo`
**Outcome:** `ExpenseValidator` enforces all 7 validation rules with exact error messages.

Tests to write first (one test per rule):
- Missing description → error
- Amount ≤ 0 → error
- Payer not in member list → "Payer is not a member of this group"
- Payer not in splits → "The payer must be included in the expense splits"
- Split user not in member list → "Split user is not a member of this group"
- Duplicate split user → "Duplicate users found in the expense splits"
- Split sum ≠ amount (beyond tolerance) → "The sum of all splits must equal..."
- Valid expense → no errors

Implementation:
- `domain/ExpenseValidator.kt`
- Returns `ValidationResult` (sealed class: `Valid` | `Invalid(errors: List<String>)`)

Done when: all validation tests pass with exact error message strings.

---

## Phase 2 — Persistence

### SLICE-007 — Database setup and Flyway `todo`
**Outcome:** Flyway manages the schema; a `Database` object provides transaction support.

Tests to write first:
- Flyway runs `V1__initial_schema.sql` without errors on a fresh in-memory SQLite DB
- Re-running Flyway on an already-migrated DB is idempotent

Implementation:
- `db/migration/V1__initial_schema.sql` — all tables
- `persistence/Database.kt` — wraps Exposed + Flyway init
- `persistence/Tables.kt` — Exposed DSL table definitions

Done when: Flyway migration tests pass; schema matches domain model.

---

### SLICE-008 — User repository `todo`
**Outcome:** `UserRepository` can save, find by ID, find by username, find by email.

Tests to write first:
- Save user → can retrieve by ID
- Find by username (exists / not exists)
- Find by email (exists / not exists)
- Unique constraint: duplicate username throws

Implementation:
- `persistence/UserRepository.kt`

Done when: all repo tests pass against in-memory SQLite.

---

### SLICE-009 — Group repository `todo`
**Outcome:** `GroupRepository` can create groups, add/remove members, find by ID.

Tests to write first:
- Create group → retrieve by ID
- Add member → member appears in group
- Remove member → member removed
- Cannot add duplicate member (idempotent or guarded)
- Find all groups

Implementation:
- `persistence/GroupRepository.kt`

Done when: all repo tests pass.

---

### SLICE-010 — Expense repository `todo`
**Outcome:** `ExpenseRepository` can create, update, delete expenses with their shares.

Tests to write first:
- Create expense with shares → retrieve all shares
- Update expense → old shares replaced, new ones present
- Delete expense → shares cascade-deleted
- Find all expenses for a group

Implementation:
- `persistence/ExpenseRepository.kt`

Done when: all repo tests pass; cascade delete verified.

---

### SLICE-011 — Settlement repository `todo`
**Outcome:** `SettlementRepository` can record and retrieve settlements by group.

Tests to write first:
- Record settlement → retrieve by group ID
- Multiple settlements → returned in descending date order
- Filter by pair of users within a group

Implementation:
- `persistence/SettlementRepository.kt`

Done when: all repo tests pass.

---

## Phase 3 — Authentication

### SLICE-012 — Register `todo`
**Outcome:** User can register via `POST /register`.

Tests to write first:
- Valid registration → 302 redirect to `/login`
- Missing fields → 400 with inline errors
- Duplicate username → "Username already exists"
- Duplicate email → "Email already exists"
- Passwords don't match → "Passwords do not match"
- Password is BCrypt-hashed in DB (plain text not stored)

Implementation:
- `service/UserService.kt` — `register(...)`
- `web/AuthHandler.kt` — GET/POST `/register`
- `templates/register.hbs`, `templates/base.hbs`

Done when: all handler tests pass.

---

### SLICE-013 — Login and logout `todo`
**Outcome:** User can log in and log out.

Tests to write first:
- Valid credentials → session cookie set, redirect to `/`
- Invalid credentials → "Invalid username or password"
- Logout → session cleared, redirect to `/`

Implementation:
- `UserService.authenticate(...)`
- `web/AuthHandler.kt` — GET/POST `/login`, GET `/logout`
- `templates/login.hbs`

Done when: all handler tests pass.

---

### SLICE-014 — Session filter `todo`
**Outcome:** All protected routes require a valid session.

Tests to write first:
- `GET /group/create` without session → 302 to `/login` with flash message
- `GET /group/create` with session → 200
- `GET /group/{id}/add_expense` without session → 302 to `/login`

Implementation:
- `web/SessionFilter.kt` — reads cookie, injects `UserId` into request or redirects
- Applied in `SplitwiseApp` router to all non-public routes

Done when: all auth filter tests pass.

---

## Phase 4 — Read-only views

### SLICE-015 — Home page `todo`
**Outcome:** `GET /` renders a list of groups and users without requiring login.

Tests to write first:
- Response is 200
- Group names are present in the HTML
- Usernames are present in the HTML
- Works without a session cookie

Implementation:
- `web/MainHandler.kt`
- `templates/index.hbs`

Done when: handler tests pass with fixture data.

---

### SLICE-016 — Group detail page `todo`
**Outcome:** `GET /group/{id}` shows members, expenses, and balances.

Tests to write first:
- 200 with members listed
- 200 with expenses listed
- Balance section present
- Balance figures correct against known fixture data
- 404 for non-existent group

Implementation:
- `service/BalanceService.kt` — wires `BalanceCalculator` + repos
- `web/GroupHandler.kt` — GET `/group/{id}`
- `templates/group.hbs`

Done when: all handler tests pass including balance assertion.

---

## Phase 5 — Group management

### SLICE-017 — Create group `todo`
**Outcome:** Authenticated user can create a group and becomes its creator and first member.

Tests to write first:
- `GET /group/create` → 200
- `POST /group/create` valid → redirect to group page
- `POST /group/create` missing name → "Group name is required"
- Creator is a member of the new group

Implementation:
- `service/GroupService.kt` — `createGroup(...)`
- `web/GroupHandler.kt` — GET/POST `/group/create`
- `templates/create_group.hbs`

Done when: all handler tests pass.

---

### SLICE-018 — Edit group `todo`
**Outcome:** Group creator can rename the group and add members via the edit form.

Tests to write first:
- Creator can access edit page
- Non-creator redirected with permission error
- Valid edit → name/description updated, redirect to group page
- Add member via edit form → member appears in group
- Add non-existent user → "User not found"
- Add existing member → "User is already a member"

Implementation:
- `GroupService.editGroup(...)`, `GroupService.addMember(...)`
- `web/GroupHandler.kt` — GET/POST `/group/{id}/edit`
- `templates/edit_group.hbs`

Done when: all handler tests pass.

---

### SLICE-019 — Add/remove member `todo`
**Outcome:** Group creator can add and remove members via dedicated POST routes.

Tests to write first:
- Add valid user → member added
- Add non-existent user → "User not found"
- Add existing member → flash info
- Remove member → member gone
- Cannot remove creator → "Cannot remove the group creator"
- Non-creator attempt → permission error

Implementation:
- `GroupService.removeMember(...)`
- `web/GroupHandler.kt` — POST `/group/{id}/add_member`, POST `/group/{id}/remove_member/{userId}`

Done when: all handler tests pass.

---

## Phase 6 — Add expense

### SLICE-020 — Add expense form and submission `todo`
**Outcome:** Group member can add an expense with full validation.

Tests to write first (one per validation rule):
- Non-member cannot access add expense page
- All 7 validation rules exercised (see SLICE-006 for rule list)
- Valid submission → expense + shares persisted, redirect to group page
- Form re-renders with errors on failure

Implementation:
- `service/ExpenseService.kt` — `addExpense(...)`
- `web/ExpenseHandler.kt` — GET/POST `/group/{id}/add_expense`
- `templates/add_expense.hbs`

Done when: all handler tests pass, validation error messages match spec.

---

## Phase 7 — Edit and delete expense

### SLICE-021 — Edit expense `todo`
**Outcome:** Expense payer or group creator can edit an expense.

Tests to write first:
- Payer can access edit page
- Group creator can access edit page
- Other authenticated user → 403
- Unauthenticated → redirect to login
- Valid edit → shares replaced, redirect to group page
- Same 7 validation rules enforced
- 404 for non-existent expense

Implementation:
- `ExpenseService.editExpense(...)`
- `web/ExpenseHandler.kt` — GET/POST `/expenses/{id}/edit`
- `templates/edit_expense.hbs`

Done when: all handler tests pass.

---

### SLICE-022 — Delete expense `todo`
**Outcome:** Expense payer or group creator can delete an expense.

Tests to write first:
- Payer can delete
- Group creator can delete
- Other user → 403
- Shares are cascade-deleted
- Balance recalculates after deletion

Implementation:
- `ExpenseService.deleteExpense(...)`
- `web/ExpenseHandler.kt` — POST `/expenses/{id}/delete`

Done when: all handler tests pass; cascade delete verified.

---

## Phase 8 — Settlements

### SLICE-023 — Record settlement `todo`
**Outcome:** Group member can record a settlement between two group members.

Tests to write first:
- Valid settlement → persisted, redirect to group page
- Non-member cannot record settlement
- Amount must be > 0
- `from_user_id == to_user_id` → validation error
- Either user not a group member → validation error

Implementation:
- `service/SettlementService.kt` — `record(...)`
- `web/SettlementHandler.kt` — POST `/group/{id}/settle`
- Settlement form on `group.hbs`

Done when: all handler tests pass.

---

### SLICE-024 — Settlement history `todo`
**Outcome:** Group member can view all settlements for a group.

Tests to write first:
- `GET /group/{id}/settlements` → 200 with settlement list
- Settlements ordered by date descending
- Non-member cannot view

Implementation:
- `SettlementService.forGroup(...)`
- `web/SettlementHandler.kt` — GET `/group/{id}/settlements`
- `templates/settlements.hbs`

Done when: all handler tests pass.

---

### SLICE-025 — Balances reflect settlements `todo`
**Outcome:** `BalanceCalculator` output (already settlement-aware from SLICE-005) is
confirmed end-to-end: group detail page balances change after a settlement is recorded.

Tests to write first:
- Record settlement between A and B → balance on group page decreases by settlement amount
- Full settlement → no balance shown for that pair

Implementation:
- Acceptance test through the HTTP layer (register → create group → add expense → settle → check group page)

Done when: end-to-end acceptance test passes.

---

## Phase 9 — Hardening

### SLICE-026 — Error pages `todo`
**Outcome:** 400/403/404/500 return styled HTML pages, not stack traces.

Tests to write first:
- `GET /nonexistent` → 404 HTML page
- Accessing a resource without permission → 403 HTML page

Implementation:
- `web/ErrorHandler.kt` — applied as top-level filter
- `templates/error.hbs`

Done when: tests pass; no stack traces visible to end user.

---

### SLICE-027 — Config and deployment `todo`
**Outcome:** App is configurable via environment variables and can be deployed.

Tests to write first:
- None (config smoke test only: app boots with all defaults)

Implementation:
- `config/AppConfig.kt` — reads `DB_PATH`, `PORT`, `SESSION_SECRET`
- `Procfile` or `Dockerfile` for Kotlin app
- Update `render.yaml` to include Kotlin app service

Done when: `./gradlew run` boots with defaults; all existing tests still pass.
