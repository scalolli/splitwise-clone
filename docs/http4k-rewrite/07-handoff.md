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
| User repository | `persistence/UserRepository.kt` | `save`, `findById`, `findByUsername`, `findByEmail`; all tests pass |
| Group repository | `persistence/GroupRepository.kt` | `create`, `findById`, `findAll`, `addMember`, `removeMember`; all tests pass |
| Expense repository | `persistence/ExpenseRepository.kt` | `create`, `findById`, `findByGroup`, `update`, `delete`; all tests pass |
| Test infrastructure | `test/persistence/PostgresTestSupport.kt` | Singleton Testcontainers container; `freshConfig()` for raw DB config; `freshDatabase()` for a migrated `Database` ready for repository tests |
| DB smoke tests | `test/persistence/DatabaseIntegrationSmokeTest.kt` | Verifies connection and Flyway idempotency |
| CI | `.github/workflows/kotlin.yml` | Runs `./gradlew test` on `ubuntu-latest` (Docker available); green |
| Local dev DB | `docker-compose.yml` | `docker compose up -d` starts Postgres on `5432`; credentials `splitwise/splitwise/splitwise` |

**Not yet started:** service layer, expense/settlement repository implementations, all HTTP handlers beyond `/health`, Handlebars templates, BCrypt auth, session filter, PWA assets.

---

## Next action

**Start SLICE-V01: Register, login, home page.**

1. Read SLICE-V01 in `04-iteration-backlog.md`.
2. Add BCrypt dependency to `build.gradle.kts` before writing any service code.
3. Write failing tests first — work handler by handler: register, then login/logout, then home page, then session filter.
4. Implement `UserService`, `AuthHandler`, `MainHandler`, `SessionFilter`, and all four templates.
5. Run `./gradlew test` — all tests must be green before committing.
6. Commit: `feat: register, login, home page`.

**Key note on slice ordering:** The backlog has been restructured into vertical slices
(SLICE-V01 through SLICE-V09). Each slice delivers end-to-end user-visible value.
`SettlementRepository` is deferred to SLICE-V02 where it is first needed.

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
| SLICE-V01 | Register, login, home page | `todo` |
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

## Non-negotiable rules

- TDD: failing test before any production code. No exceptions.
- Update this file (07-handoff.md) at the end of every session.
- New architectural decisions go in `06-decisions.md` before they are implemented.
- Commit when a slice is green. One commit per slice minimum.
