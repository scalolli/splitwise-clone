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
| Group service | `service/GroupService.kt` | `createGroup(name, creatorId): Group` — delegates to GroupRepository |
| Expense service | `service/ExpenseService.kt` | `addExpense(...)` — validates via ExpenseValidator, saves via ExpenseRepository; returns `Result<Expense>`; throws `ValidationException` on invalid |
| Session filter | `web/SessionFilter.kt` | Reads `session` cookie (value = userId Long); redirects to `/login` if absent |
| Auth handler | `web/AuthHandler.kt` | `GET/POST /register`, `GET/POST /login`, `POST /logout`; flash cookie for messages; session `maxAge=86400` (1 day); logout redirects to `/login`; cleared cookie now mirrors secure attributes |
| Main handler | `web/MainHandler.kt` | `GET /`; only shows groups the current user belongs to |
| Settlement repository | `persistence/SettlementRepository.kt` | `create`, `findByGroup`; used by BalanceService |
| Balance service | `service/BalanceService.kt` | wires `BalanceCalculator` + `ExpenseRepository` + `SettlementRepository` |
| Group handler | `web/GroupHandler.kt` | `GET /group/{id}`: 200 with members/expenses/balances, 403 for authenticated non-members, 404 for unknown group, 302 to `/login` if unauthenticated; `GET/POST /group/create`: form renders with CSRF, POST validates name, creates group, redirects to `/group/{id}` with flash |
| Expense handler | `web/ExpenseHandler.kt` | NEW: `GET/POST /group/{id}/add_expense`: member-only (403 for non-members), validates all 7 rules via ExpenseValidator, saves expense + shares, redirects to group page with flash |
| Form body parser | `web/FormBodyParser.kt` | NEW: `parseFormBody(body)` — parses URL-encoded body to `Map<String, List<String>>`; handles repeated keys (for splits) |
| App factory | `web/AppFactory.kt` | `buildApp(...)` — placeholder `/group/create` removed; real groupHandler + expenseHandler wired |
| Templates | `src/main/resources/create_group.hbs` | NEW: create group form with CSRF hidden field and inline error list |
| Templates | `src/main/resources/add_expense.hbs` | NEW: add expense form with payer select, per-member split inputs, CSRF hidden field, error list |
| Templates | `src/main/resources/group.hbs` added | Renders group name, member list, expense table (description/amount/payer), balance list |
| Templates | `src/main/resources/register.hbs`, `login.hbs`, `index.hbs`, `base.hbs` | Handlebars classpath templates; all forms include `<input type="hidden" name="_csrf" value="{{csrfToken}}">` |
| Test infrastructure | `test/persistence/PostgresTestSupport.kt` | Singleton Testcontainers container; `freshDatabase()` for a migrated `Database` |
| DB smoke tests | `test/persistence/DatabaseIntegrationSmokeTest.kt` | Verifies connection and Flyway idempotency |
| CI/CD | `.github/workflows/kotlin.yml`, `Dockerfile`, `render.yaml` | `test` + `publish` jobs; Docker image pushed to ghcr.io; Render deploy hook triggers deploy |
| Security | `web/SessionToken.kt`, `web/SessionFilter.kt`, `web/AuthHandler.kt`, `web/CsrfToken.kt`, `web/CsrfFilter.kt`, `service/UserService.kt` | HMAC-SHA256 signed session token, password/email validation, secure cookie attributes, CSRF double-submit cookie on all POST routes |
| Local dev DB | `docker-compose.yml` | `docker compose up -d` starts Postgres on `5432`; credentials `splitwise/splitwise/splitwise` |

**Known gaps:** None. SLICE-V03 is complete. Ready to start SLICE-V04.

**Not yet started:** edit/delete group, edit/delete expense, settlements, error pages, deployment config, PWA assets.

---

## Next action

**Start SLICE-V04: Edit group and manage members.**

All tests green. 107 tests passing. Latest slice: `SLICE-V03 done`.

### SLICE-V04 plan (from `04-iteration-backlog.md`)

Write failing tests first (`EditGroupHandlerTest.kt`):
- `GET /group/{id}/edit` → 200 for creator
- `GET /group/{id}/edit` → redirect + flash for non-creator
- `GET /group/{id}/edit` → redirect to `/login` for unauthenticated
- `POST /group/{id}/edit` valid → name updated, redirect to `/group/{id}` with flash "Group updated successfully"
- `POST /group/{id}/edit` by non-creator → redirect + flash "You do not have permission to edit this group"
- `POST /group/{id}/add_member` valid → member added, redirect with flash "Member added successfully"
- `POST /group/{id}/add_member` non-existent user → flash "User not found"
- `POST /group/{id}/add_member` already a member → flash "User is already a member of this group"
- `POST /group/{id}/add_member` non-creator → redirect + flash "You do not have permission to add members to this group"
- `POST /group/{id}/remove_member/{userId}` → member removed, redirect + flash "Member removed successfully"
- `POST /group/{id}/remove_member/{userId}` → cannot remove creator, flash "Cannot remove the group creator"
- `POST /group/{id}/remove_member/{userId}` non-creator → flash "You do not have permission to remove members from this group"

All POST tests must use `TestHelpers.getCsrfToken(app, "/group/{id}/edit", sessionCookie)` + `Cookie("csrf", ...)` pattern.

Implementation order:
1. Add `GroupRepository.update(id, name)` (or `update(id, name, description)`)
2. `GroupService.editGroup(id, name, requesterId): Result<Group>` — creator-only, updates name
3. `GroupService.addMember(id, username, requesterId): Result<Unit>` — creator-only, validates user exists + not already member
4. `GroupService.removeMember(id, targetUserId, requesterId): Result<Unit>` — creator-only, blocks removing creator
5. `web/GroupHandler.kt` — add `GET/POST /group/{id}/edit`, `POST /group/{id}/add_member`, `POST /group/{id}/remove_member/{userId}`
6. `templates/edit_group.hbs`

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
| SLICE-V03 | Create group and add expense | `done` |
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

## Notes from SLICE-V03

- `GroupService` and `ExpenseService` are thin coordinators: no business logic beyond delegation to `ExpenseValidator` (in `ExpenseService`) and `GroupRepository` (in `GroupService`).
- `ExpenseService.addExpense` returns `Result<Expense>`: `Result.success(expense)` on valid, `Result.failure(ValidationException(errors))` on invalid. Handler uses `.fold(onSuccess, onFailure)`.
- `ValidationException(errors: List<String>)` lives in `service/ExpenseService.kt`. If more services need it in future, move to a shared `service/` file.
- `FormBodyParser.kt` — package-level `parseFormBody(body: String): Map<String, List<String>>`. Handles repeated keys needed for split arrays (`split_user_id`, `split_amount`). Used by `GroupHandler` (POST /group/create) and `ExpenseHandler` (POST /group/{id}/add_expense). Do NOT use http4k `webForm` lenses for forms with repeated keys.
- `TestHelpers.getCsrfToken` now accepts an optional `sessionCookie: String?` parameter. Required for any GET of a protected page to obtain a CSRF token before POSTing. All existing callers pass `null` (or omit the param) and continue to work.
- `GroupId` is an inline class — `lateinit` is not allowed. Use `private var groupId: GroupId = GroupId(0)` with `@BeforeEach` assignment instead.
- Route ordering in `AppFactory`: `groupHandler` contains `/group/create` and `/group/{id}` routes. http4k matches routes by specificity — `/group/create` (literal) wins over `/group/{id}` (path param). No ordering issue in practice.
- `add_expense.hbs` renders per-member split inputs as repeated `split_user_id` + `split_amount` fields. The form always pre-renders all group members. The `parseFormBody` utility collects these as lists for the handler to zip into `ExpenseShare` objects.

## Notes from SLICE-V02

- `buildApp` signature now requires all four repos explicitly (`userRepository`, `groupRepository`, `expenseRepository`, `settlementRepository`). All test files updated accordingly.
- `GroupHandler` resolves member usernames and payer names from `UserRepository` before passing data to the template — the template receives plain `Map<String, Any?>` lists, keeping the view model simple.
- `BalanceService` is a thin coordinator: it delegates to `BalanceCalculator.calculate(expenses, settlements)` — no business logic lives in the service layer.
- `SettlementRepository` is minimal for now (`create` + `findByGroup`) — extended in SLICE-V06 when the settle form is added.

## Notes from CSRF hardening session

- `CsrfToken.kt` — generates a URL-safe base64 nonce (32 random bytes via `SecureRandom`). Validates POST requests by constant-time comparison of the `_csrf` form field against the `csrf` cookie value. Form field extraction parses the raw URL-encoded body manually (no http4k lens re-read, which would consume the body).
- `CsrfFilter.kt` — a top-level `Filter` wrapping the entire non-health route tree. Returns 403 immediately for any POST with missing or mismatched token. GET requests pass through unchanged.
- `TestHelpers.kt` added to the test package: `registerAndLogin`, `registerUser`, `loginUser`, `getCsrfToken` — shared across all handler tests. All new handler tests must use this helper for any POST flow.
- ADR-018 locked in `06-decisions.md`.

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

