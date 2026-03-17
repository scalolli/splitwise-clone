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
| Group repository | `persistence/GroupRepository.kt` | `create`, `findById`, `findAll`, `findByMember`, `addMember`, `removeMember`; all tests pass |
| Expense repository | `persistence/ExpenseRepository.kt` | `create`, `findById`, `findByGroup`, `update`, `delete`; all tests pass |
| User service | `service/UserService.kt` | `register` (BCrypt hash, validation) + `authenticate`; used by AuthHandler |
| Session filter | `web/SessionFilter.kt` | Reads `session` cookie (value = userId Long); redirects to `/login` if absent |
| Auth handler | `web/AuthHandler.kt` | `GET/POST /register`, `GET/POST /login`, `POST /logout`; flash cookie for messages; session `maxAge=86400` (1 day); logout redirects to `/login`; cleared cookie now mirrors secure attributes |
| Main handler | `web/MainHandler.kt` | `GET /`; only shows groups the current user belongs to |
| Settlement repository | `persistence/SettlementRepository.kt` | `create`, `findByGroup`; used by BalanceService |
| Balance service | `service/BalanceService.kt` | wires `BalanceCalculator` + `ExpenseRepository` + `SettlementRepository` |
| Group handler | `web/GroupHandler.kt` | `GET /group/{id}`: 200 with members/expenses/balances, 403 for authenticated non-members, 404 for unknown group, 302 to `/login` if unauthenticated |
| App factory | `web/AppFactory.kt` | `buildApp(userRepository, groupRepository, expenseRepository, settlementRepository)` — all four repos now required; `/group/create` placeholder restored as protected route |
| Templates | `src/main/resources/group.hbs` added | Renders group name, member list, expense table (description/amount/payer), balance list |
| Templates | `src/main/resources/register.hbs`, `login.hbs`, `index.hbs`, `base.hbs` | Handlebars classpath templates; `index.hbs` and `base.hbs` have POST logout form button; register/login links removed from authenticated nav; home no longer shows a global users list; all forms include `<input type="hidden" name="_csrf" value="{{csrfToken}}">` |
| Test infrastructure | `test/persistence/PostgresTestSupport.kt` | Singleton Testcontainers container; `freshDatabase()` for a migrated `Database` |
| DB smoke tests | `test/persistence/DatabaseIntegrationSmokeTest.kt` | Verifies connection and Flyway idempotency |
| CI/CD | `.github/workflows/kotlin.yml`, `Dockerfile`, `render.yaml` | `test` + `publish` jobs; Docker image pushed to ghcr.io; Render deploy hook triggers deploy; workflow polls Render API until `live` or `failed`; all actions on latest Node.js 24 compatible versions (`checkout@v6`, `login-action@v4`, `build-push-action@v7`) |
| Security | `web/SessionToken.kt`, `web/SessionFilter.kt`, `web/AuthHandler.kt`, `web/CsrfToken.kt`, `web/CsrfFilter.kt`, `service/UserService.kt`, `domain/User.kt`, `persistence/UserRepository.kt` | HMAC-SHA256 signed session token, password/email validation, secure cookie attributes, no passwordHash on domain model, hardcoded DB credentials removed, CSRF double-submit cookie on all POST routes |
| Local dev DB | `docker-compose.yml` | `docker compose up -d` starts Postgres on `5432`; credentials `splitwise/splitwise/splitwise` |

**Known gaps to fix next:** None — CSRF is done. Ready to start SLICE-V03.

**Not yet started:** create group/expense forms, edit/delete flows, error pages, deployment config, PWA assets.

---

## Next action

**Start SLICE-V03: Create group and add expense.**

All tests green. Latest commit: `9c9b68f fix: add CSRF protection to all POST routes`.

### SLICE-V03 plan (from `04-iteration-backlog.md`)

Write failing tests first:
- `GET /group/create` → 200
- `POST /group/create` valid → redirect to `/group/{id}`, flash "Group created successfully"
- `POST /group/create` missing name → 400 with "Group name is required"
- Creator is automatically a member of the new group
- `GET /group/{id}/add_expense` → 200 (member only)
- `POST /group/{id}/add_expense` valid → expense + shares saved, redirect to group page
- `POST /group/{id}/add_expense` — all 7 validation rules from `ExpenseValidator`
- Non-member cannot access `GET /group/{id}/add_expense` (403)

All POST tests must use `TestHelpers.getCsrfToken()` + `Cookie("csrf", ...)` pattern established in this session.

Implementation order:
1. `service/GroupService.kt` — `createGroup(name, creatorId): Group`
2. `service/ExpenseService.kt` — `addExpense(...): Expense`
3. `web/GroupHandler.kt` — add `GET/POST /group/create` routes
4. `web/ExpenseHandler.kt` — `GET/POST /group/{id}/add_expense`
5. `templates/create_group.hbs`, `templates/add_expense.hbs`
6. Wire into `AppFactory.kt`

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
| SLICE-CSRF | CSRF hardening | `done` |
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

## Review findings to address next

None outstanding. All known hardening items (group visibility, logout cookie flags, CSRF) are done.

## Notes from CSRF hardening session

- `CsrfToken.kt` — generates a URL-safe base64 nonce (32 random bytes via `SecureRandom`). Validates POST requests by constant-time comparison of the `_csrf` form field against the `csrf` cookie value. Form field extraction parses the raw URL-encoded body manually (no http4k lens re-read, which would consume the body).
- `CsrfFilter.kt` — a top-level `Filter` wrapping the entire non-health route tree. Returns 403 immediately for any POST with missing or mismatched token. GET requests pass through unchanged.
- GET handlers (`/register`, `/login`) now generate a nonce, set a `csrf` cookie (`HttpOnly=true`, `Secure=true`, `SameSite=Strict`, `Max-Age=3600`), and inject the nonce value into their ViewModel so templates can render `<input type="hidden" name="_csrf" value="{{csrfToken}}>`.
- Failure re-render paths (validation errors, bad credentials) also generate a fresh nonce so the re-rendered form remains submittable.
- `base.hbs` logout form also gets the `_csrf` hidden field; the nonce is injected by `MainHandler` and `GroupHandler` into their ViewModels (to be done when those handlers render `base.hbs` — currently `base.hbs` is used by group/index templates; those handlers need to pass `csrfToken` in their model).
- **Important:** `base.hbs` `{{csrfToken}}` will render blank until `MainHandler` and `GroupHandler` pass it. The logout button will be broken in the browser until those handlers are updated in SLICE-V03. The CSRF filter protects the POST — a missing token returns 403. Fix in SLICE-V03: pass `csrfToken` from a fresh `CsrfToken.generate()` into every ViewModel that extends `base.hbs`.
- ~~Fix applied immediately after CSRF commit~~: `IndexViewModel` and `GroupViewModel` now carry `csrfToken`; both handlers generate a nonce, set the `csrf` cookie, and inject the nonce. `index.hbs` logout form now includes `<input type="hidden" name="_csrf">`. Logout works end-to-end from both pages. Tests added to `MainHandlerTest` to cover this.
- `TestHelpers.kt` added to the test package: `registerAndLogin`, `registerUser`, `loginUser`, `getCsrfToken` — shared across `AuthHandlerTest`, `GroupHandlerTest`, `MainHandlerTest`, `SessionFilterTest`. All new handler tests must use this helper for any POST flow.
- ADR-018 locked in `06-decisions.md`.

## Notes from this session (auth/privacy hardening)

- Added ADR-017: group visibility is membership-scoped. Group details are private to members, and the home page only shows groups the current user belongs to.
- Aligned `02-behavior-spec.md` with the product/security decision: group detail is member-only, and home no longer exposes all groups/users.
- `GroupRepository.findByMember(...)` added so `MainHandler` can render only the current user's groups.
- `GroupHandler` now returns 403 for authenticated non-members.
- `index.hbs` no longer renders a global Users section.
- Logout clearing now preserves `Secure`, `HttpOnly`, and `SameSite=Strict` on the cleared cookie.
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
- Session cookie is HMAC-SHA256 signed via `SessionToken.kt`; it is not a raw unsigned user id.
- `HandlebarsTemplates().CachingClasspath()` loads `.hbs` files from the classpath root using `/{templateName}.hbs`.

---

- TDD: failing test before any production code. No exceptions.
- Update this file (07-handoff.md) at the end of every session.
- New architectural decisions go in `06-decisions.md` before they are implemented.
- Commit when a slice is green. One commit per slice minimum.
