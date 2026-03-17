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
| App factory | `web/AppFactory.kt` | `buildApp(userRepository, groupRepository)` — wires all handlers; used by tests and `App.kt` |
| Templates | `src/main/resources/register.hbs`, `login.hbs`, `index.hbs`, `base.hbs` | Handlebars classpath templates; `index.hbs` and `base.hbs` have POST logout form button; register/login links removed from authenticated nav |
| Test infrastructure | `test/persistence/PostgresTestSupport.kt` | Singleton Testcontainers container; `freshDatabase()` for a migrated `Database` |
| DB smoke tests | `test/persistence/DatabaseIntegrationSmokeTest.kt` | Verifies connection and Flyway idempotency |
| CI/CD | `.github/workflows/kotlin.yml`, `Dockerfile`, `render.yaml` | `test` + `publish` jobs; Docker image pushed to ghcr.io; Render deploy hook wired; `production` environment secret configured; docker actions bumped to Node.js 24 compatible versions (`login-action@v4`, `build-push-action@v7`) |
| Security | `web/SessionToken.kt`, `web/SessionFilter.kt`, `web/AuthHandler.kt`, `service/UserService.kt`, `domain/User.kt`, `persistence/UserRepository.kt` | HMAC-SHA256 signed session token, password/email validation, secure cookie attributes, no passwordHash on domain model, hardcoded DB credentials removed |
| Local dev DB | `docker-compose.yml` | `docker compose up -d` starts Postgres on `5432`; credentials `splitwise/splitwise/splitwise` |

**Not yet started:** settlement repository, group detail page, create group/expense forms, edit/delete flows, error pages, deployment config, PWA assets.

---

## Next action

**Start SLICE-V02: Group detail page.**

CI/CD pipeline is verified and working end-to-end. Deployment to Render is confirmed.

### SLICE-V02: Group detail page

1. Read SLICE-V02 in `04-iteration-backlog.md`.
2. Write failing tests first: `GET /group/:id` renders group name, member list, expense list, and net balances.
3. Implement `SettlementRepository` (deferred from earlier — first needed here for balance display).
4. Implement `GroupHandler` and `group.hbs` template.
5. Run `./gradlew test` — all tests green.
6. Commit: `feat: group detail page`.

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
| SLICE-V02 | Group detail page | `todo` |
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

## Notes from this session

- Logout button added as `POST /logout` form in `index.hbs` and `base.hbs` — a `<a href="/logout">` (GET) would not match the POST route.
- `HandlebarsTemplates().CachingClasspath()` caches templates at startup — template changes require a server restart to take effect.
- Session cookie `maxAge` set to 86400 (1 day). The token is stateless (HMAC-signed), so there is no server-side revocation. Cookie theft risk is low in practice due to `httpOnly`, `secure`, and `sameSite=Strict`. If revocation is needed in future (force-logout, password reset), a server-side session store (DB or in-memory map) would be required.
- GitHub Actions docker actions bumped: `checkout@v4`, `login-action@v4`, `build-push-action@v7` — Node.js 24 compatible, avoids deprecation warnings from June 2026.

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
