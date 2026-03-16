# Roadmap

Each phase is a milestone with a defined exit gate. Do not start the next phase until
the current phase's exit criteria are fully met and tests are green.

---

## Phase 0 — Foundation
**Goal:** The Kotlin project exists, compiles, and has a working CI pipeline.

### Work
- Create the root Gradle project with standard Kotlin source structure
- Add http4k-core, http4k-server-jetty, JUnit 5, http4k test module to `build.gradle.kts`
- Implement `GET /health` returning `200 OK` with body `{"status":"ok"}`
- Configure GitHub Actions workflow for Kotlin: build + test on every push
- Add in-memory SQLite test config

### Exit criteria
- `./gradlew test` passes with a single health check test
- GitHub Actions runs the Kotlin job on every push and pull request
- No Kotlin compiler errors or warnings

---

## Phase 1 — Domain model
**Goal:** The core domain is fully defined and unit-tested in isolation, with no
framework dependencies.

### Work
- Value objects: `UserId`, `GroupId`, `ExpenseId`, `SettlementId`, `Money`
  - `Money` wraps `BigDecimal`, enforces positive values, supports add/subtract/compare
- Entities: `User`, `Group`, `Expense`, `ExpenseShare`, `Settlement`
- `BalanceCalculator`: implements pairwise debt simplification, accepts settlements
- `ExpenseValidator`: encodes all validation rules from `02-behavior-spec.md`

### Exit criteria
- `BalanceCalculator` fully unit-tested including settlement subtraction
- `ExpenseValidator` unit-tested for every validation rule and error message
- `Money` arithmetic unit-tested including edge cases (zero, equality, rounding)
- Zero framework imports in `domain/` package

---

## Phase 2 — Persistence
**Goal:** All domain entities can be persisted and retrieved via typed repositories.

### Work
- Flyway migration `V1__initial_schema.sql`: tables for users, groups, user_group,
  expenses, expense_shares, settlements
- Exposed DSL table definitions
- Repository implementations: `UserRepository`, `GroupRepository`,
  `ExpenseRepository`, `SettlementRepository`
- `Database` object: connects to SQLite, runs Flyway, provides transaction DSL

### Exit criteria
- Integration tests for every repository method against a real in-memory SQLite DB
- Flyway migration runs cleanly from scratch
- Repository tests are isolated (each test gets a fresh schema)
- No SQL hand-written outside of Flyway migration files and Exposed DSL

---

## Phase 3 — Authentication
**Goal:** Users can register, log in, and log out. Protected routes are enforced.

### Work
- `UserService`: register (hash password with BCrypt), authenticate (check hash)
- `SessionFilter`: reads session cookie; rejects unauthenticated requests to
  protected routes with redirect to `/login`
- `AuthHandler`: `GET/POST /login`, `GET/POST /register`, `GET /logout`
- Handlebars templates: `login.hbs`, `register.hbs`, `base.hbs`
- Flash message support in templates

### Exit criteria
- Register, login, logout HTTP tests pass
- Validation error messages match `02-behavior-spec.md` exactly
- Unauthenticated `GET /group/1` redirects to `/login`
- Password stored as BCrypt hash; plain text never persisted

---

## Phase 4 — Home page and read-only group view
**Goal:** A logged-in user can see all groups and view a group's details.

### Work
- `GET /` — lists all groups and all users (no login required)
- `GET /group/{id}` — shows group name, members, expenses, and calculated balances
  - `BalanceService` wires `BalanceCalculator` to the persistence layer
  - Balances subtract recorded settlements
- Handlebars templates: `index.hbs`, `group.hbs`

### Exit criteria
- Home page renders groups and users without login
- Group detail page shows all members, expenses, and correct balances
- Balance figures verified in tests against known fixture data
- Non-existent group returns 404

---

## Phase 5 — Group management
**Goal:** Users can create and edit groups and manage membership.

### Work
- `GET|POST /group/create`
- `GET|POST /group/{id}/edit`
- `POST /group/{id}/add_member`
- `POST /group/{id}/remove_member/{userId}`
- `GroupService`: create, edit, add member, remove member with all auth checks
- Templates: `create_group.hbs`, `edit_group.hbs`

### Exit criteria
- Full authorization enforced: creator-only mutations tested
- All flash messages match `02-behavior-spec.md`
- Cannot remove group creator; cannot add duplicate member
- All group management handler tests pass

---

## Phase 6 — Add expense
**Goal:** A group member can add an expense with custom or equal splits.

### Work
- `GET|POST /group/{id}/add_expense`
- `ExpenseService.addExpense(...)` with full validation
- Split UI: form with dynamic add/remove rows (JavaScript, Handlebars)
- Templates: `add_expense.hbs`

### Exit criteria
- All 7 validation rules from `02-behavior-spec.md` tested individually
- Expense and shares persisted atomically
- Only group members can access the add expense page
- Form re-renders with errors on invalid submission

---

## Phase 7 — Edit and delete expense
**Goal:** The expense payer or group creator can edit or delete an expense.

### Work
- `GET|POST /expenses/{id}/edit`
- `POST /expenses/{id}/delete`
- `ExpenseService.editExpense(...)` — replaces shares atomically
- `ExpenseService.deleteExpense(...)` — cascades to shares
- Authorization enforced for both actions
- Templates: `edit_expense.hbs`

### Exit criteria
- Edit and delete are only accessible to payer or group creator — tested
- Unauthenticated and unauthorized access returns appropriate response
- Shares replaced atomically on edit (old shares fully removed, new ones added)
- Balance recalculates correctly after deletion

---

## Phase 8 — Settlements
**Goal:** Users can record settlements; balances reflect them.

### Work
- `POST /group/{id}/settle`
- `GET /group/{id}/settlements`
- `SettlementService.record(...)`, `SettlementService.forGroup(...)`
- `BalanceCalculator` already supports settlements (from Phase 1); wire it up here
- Templates: `settlements.hbs`, settlement form on `group.hbs`

### Exit criteria
- Recording a settlement reduces displayed balance for that pair
- Settlement history lists all settlements in descending date order
- Balance correctly shows zero (or no entry) when fully settled
- Only group members can record settlements

---

## Phase 9 — Hardening and production readiness
**Goal:** The app handles errors gracefully and is deployable.

### Work
- Global `ErrorHandler` filter: renders 400/403/404/500 pages
- Logging: structured request log per request
- Config: all values from environment variables with documented defaults
- Dockerfile or Procfile for deployment
- Update `render.yaml` to point to the Kotlin app or add a separate service entry
- Review and close any known N+1 query issues

### Exit criteria
- `GET /nonexistent` returns 404 HTML page, not a stack trace
- All config values are externally injectable
- App boots in a clean environment with `./gradlew run`
- All tests remain green
