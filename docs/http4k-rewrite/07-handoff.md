# Handoff — http4k Implementation

This is the operational baton. Update it at the end of every session.
The next agent (or human) picks up exactly from "Next action".

---

## Current state

**What exists and is working:**

| Area | Files | Status |
|---|---|---|
| HTTP server | `src/main/kotlin/com/splitwise/App.kt`, `web/SplitwiseApp.kt`, `web/HealthHandler.kt` | `GET /health` returns `{"status":"ok"}` |
| Domain model | `domain/Money.kt`, `UserId`, `GroupId`, `ExpenseId`, `SettlementId`, `User`, `Group`, `Expense`, `ExpenseShare`, `Settlement` | All unit tested, zero dependencies |
| Business logic | `domain/BalanceCalculator.kt`, `domain/ExpenseValidator.kt` | Fully unit tested |
| Database layer | `persistence/Database.kt`, `persistence/Tables.kt` | Wraps Exposed DSL + Flyway; provides `connect()`, `migrate()`, `transaction {}` |
| Schema | `src/main/resources/db/migration/V1__initial_schema.sql` | 6 tables: `users`, `groups`, `group_members`, `expenses`, `expense_shares`, `settlements` |
| User repository | `persistence/UserRepository.kt` | `save`, `findAll`, `findById`, `findByUsername`, `findByEmail`; all tests pass |
| Group repository | `persistence/GroupRepository.kt` | `create`, `findById`, `findAll`, `addMember`, `removeMember`; all tests pass |
| Expense repository | `persistence/ExpenseRepository.kt` | `create`, `findById`, `findByGroup`, `update`, `delete`; all tests pass |
| User service | `service/UserService.kt` | `register` (BCrypt hash, validation) + `authenticate`; used by AuthHandler |
| Session filter | `web/SessionFilter.kt` | Reads `session` cookie (value = userId Long); redirects to `/login` if absent |
| Auth handler | `web/AuthHandler.kt` | `GET/POST /register`, `GET/POST /login`, `POST /logout`; flash cookie for messages; session `maxAge=86400` (1 day); logout redirects to `/login` |
| Main handler | `web/MainHandler.kt` | `GET /`; lists all groups and users via Handlebars template |
| Settlement repository | `persistence/SettlementRepository.kt` | `create`, `findByGroup`; used by BalanceService |
| Balance service | `service/BalanceService.kt` | wires `BalanceCalculator` + `ExpenseRepository` + `SettlementRepository` |
| Group handler | `web/GroupHandler.kt` | `GET /group/{id}`: 200 with members/expenses/balances, 404 for unknown group, 302 to `/login` if unauthenticated |
| App factory | `web/AppFactory.kt` | `buildApp(userRepository, groupRepository, expenseRepository, settlementRepository)` — all four repos now required; `/group/create` placeholder restored as protected route |
| Templates | `src/main/resources/group.hbs` added | Renders group name, member list, expense table (description/amount/payer), balance list |
| Templates | `src/main/resources/register.hbs`, `login.hbs`, `index.hbs`, `base.hbs` | Handlebars classpath templates; `index.hbs` and `base.hbs` have POST logout form button; register/login links removed from authenticated nav |
| Test infrastructure | `test/persistence/PostgresTestSupport.kt` | Singleton Testcontainers container; `freshDatabase()` for a migrated `Database` |
| DB smoke tests | `test/persistence/DatabaseIntegrationSmokeTest.kt` | Verifies connection and Flyway idempotency |
| CI/CD | `.github/workflows/kotlin.yml`, `Dockerfile`, `render.yaml` | `test` + `publish` jobs; Docker image pushed to ghcr.io; Render deploy hook triggers deploy; workflow polls Render API until `live` or `failed`; all actions on latest Node.js 24 compatible versions (`checkout@v6`, `login-action@v4`, `build-push-action@v7`) |
| Security | `web/SessionToken.kt`, `web/SessionFilter.kt`, `web/AuthHandler.kt`, `service/UserService.kt`, `domain/User.kt`, `persistence/UserRepository.kt` | HMAC-SHA256 signed session token, password/email validation, secure cookie attributes, no passwordHash on domain model, hardcoded DB credentials removed |
| Local dev DB | `docker-compose.yml` | `docker compose up -d` starts Postgres on `5432`; credentials `splitwise/splitwise/splitwise` |

**Not yet started:** create group/expense forms, edit/delete flows, error pages, deployment config, PWA assets.

---

## Next action

**Start SLICE-V03: Create group and add expense.**

All tests green. Commit: `569bd33 feat: SLICE-V02 group detail page with members, expenses and balances`.

### SLICE-V03: Create group and add expense

1. Read SLICE-V03 in `04-iteration-backlog.md`.
2. Write failing tests first (see spec for the full list including all 7 expense validation rules).
3. Implement `GroupService.createGroup(...)`.
4. Implement `ExpenseService.addExpense(...)`.
5. Implement `POST /group/create` in `GroupHandler.kt` and `POST /group/{id}/add_expense` in a new `ExpenseHandler.kt`.
6. Add `create_group.hbs` and `add_expense.hbs` templates.
7. Run `./gradlew test` — all tests green.
8. Commit: `feat: create group and add expense`.

## Slice status

| Slice | Title | Status |
|---|---|---|
| SLICE-001 | Gradle project scaffold | `done` |
| SLICE-002 | Kotlin CI pipeline | `done` |
| SLICE-002A | Postgres test infrastructure | `done` |
| SLICE-003 | Money value object | `done` |
| SLICE-004 | Core domain entities | `done` |
| SLICE-005 | Balance calculator | `done` |
| SLICE-006 | Expense validator | `done` |
| SLICE-007 | Database setup and Flyway | `done` |
| SLICE-008 | User repository | `done` |
| SLICE-009 | Group repository | `done` |
| SLICE-010 | Expense repository | `done` |
| SLICE-V01 | Register, login, home page | `done` |
| SLICE-SEC | Security hardening | `done` |
| SLICE-DEPLOY | CI/CD + Render deployment | `done` |
| SLICE-V02 | Group detail page | `done` |
| SLICE-V03 | Create group and add expense | `todo` |
| SLICE-V04 | Edit group and manage members | `todo` |
| SLICE-V05 | Edit and delete expense | `todo` |
| SLICE-V06 | Record settlement and history | `todo` |
| SLICE-V07 | Error pages | `todo` |
| SLICE-V08 | Config and deployment | `todo` |
| SLICE-V09 | PWA manifest and service worker | `todo` |

## Key reference files

| File | Purpose |
|---|---|
| `docs/http4k-rewrite/00-charter.md` | Scope, principles, fix table |
| `docs/http4k-rewrite/02-behavior-spec.md` | Auth matrix, validation rules, exact error messages |
| `docs/http4k-rewrite/04-iteration-backlog.md` | Slice definitions — read before starting any slice |
| `docs/http4k-rewrite/06-decisions.md` | Locked ADRs — check before making any architectural choice |
| `docs/http4k-rewrite/08-functionality-checklist.md` | Consolidated functionality checklist |
| `docs/http4k-rewrite/05-testing-strategy.md` | Postgres/Testcontainers testing approach |

## Notes from SLICE-V02

- `buildApp` signature now requires all four repos explicitly (`userRepository`, `groupRepository`, `expenseRepository`, `settlementRepository`). All test files updated accordingly.
- `GroupHandler` resolves member usernames and payer names from `UserRepository` before passing data to the template — the template receives plain `Map<String, Any?>` lists, keeping the view model simple.
- `BalanceService` is a thin coordinator: it delegates to `BalanceCalculator.calculate(expenses, settlements)` — no business logic lives in the service layer.
- `SettlementRepository` is minimal for now (`create` + `findByGroup`) — extended in SLICE-V06 when the settle form is added.
- `SessionFilterTest` relies on `GET /group/create` returning 200 for an authenticated user. This placeholder is kept in `AppFactory` until SLICE-V03 replaces it with the real handler.

## Notes from this session

- Logout button added as `POST /logout` form in `index.hbs` and `base.hbs` — a `<a href="/logout">` (GET) would not match the POST route.
- `HandlebarsTemplates().CachingClasspath()` caches templates at startup — template changes require a server restart to take effect.
- Session cookie `maxAge` set to 86400 (1 day). The token is stateless (HMAC-signed), so there is no server-side revocation. Cookie theft risk is low in practice due to `httpOnly`, `secure`, and `sameSite=Strict`. If revocation is needed in future (force-logout, password reset), a server-side session store (DB or in-memory map) would be required.
- Render deploy polling added to CI: after triggering the hook, the workflow fetches the latest deploy ID via `GET /v1/services/{id}/deploys?limit=1` (response shape: `.[0].deploy.id`) then polls `GET /v1/services/{id}/deploys/{deployId}` (response shape: `.status`) every 10s. Confirmed against live API using local `RENDER_API_KEY`.
- Render API key must be set as `RENDER_API_KEY` in the GitHub `production` environment secret. The workflow verifies auth before triggering the deploy hook.
- GitHub Actions bumped to latest: `checkout@v6`, `login-action@v4`, `build-push-action@v7`, `setup-java@v5`, `setup-gradle@v5` — no Node.js deprecation warnings.

## Notes from SLICE-V01

- `Response.cookie(name)` does not exist in http4k 5.x — use `response.cookies().find { it.name == "..." }` for Response. The `cookie(name)` getter exists only for `Request`.
- `routes()` only accepts `vararg RoutingHttpHandler` — handler factory functions must declare return type as `RoutingHttpHandler`, not `HttpHandler`.
- `Filter.then(RoutingHttpHandler)` returns `RoutingHttpHandler`; `Filter.then(HttpHandler)` returns plain `Function1<Request, Response>`. Use the `RoutingHttpHandler` form when the result must be passed to `routes()`.
- Session cookie value is the user's `id` (a Long) serialized as a String — simple, no signing. Acceptable for development; revisit before production in SLICE-V08.
- `HandlebarsTemplates().CachingClasspath()` loads `.hbs` files from the classpath root using `/{templateName}.hbs`.

---

- TDD: failing test before any production code. No exceptions.
- Update this file (07-handoff.md) at the end of every session.
- New architectural decisions go in `06-decisions.md` before they are implemented.
- Commit when a slice is green. One commit per slice minimum.
