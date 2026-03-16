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
| Test infrastructure | `test/persistence/PostgresTestSupport.kt` | Singleton Testcontainers container; `freshConfig()` for raw DB config; `freshDatabase()` for a migrated `Database` ready for repository tests |
| DB smoke tests | `test/persistence/DatabaseIntegrationSmokeTest.kt` | Verifies connection and Flyway idempotency |
| CI | `.github/workflows/kotlin.yml` | Runs `./gradlew test` on `ubuntu-latest` (Docker available); green |
| Local dev DB | `docker-compose.yml` | `docker compose up -d` starts Postgres on `5432`; credentials `splitwise/splitwise/splitwise` |

**Not yet started:** service layer, repository implementations, all HTTP handlers beyond `/health`, Handlebars templates, BCrypt auth, session filter, PWA assets.

---

## Next action

**Start SLICE-009: Group repository.**

1. Read SLICE-009 in `04-iteration-backlog.md`.
2. Use `PostgresTestSupport.freshDatabase()` for all persistence tests.
3. Write failing tests first: create group → find by ID, add member → member appears, remove member, idempotent add, find all groups.
4. Implement `src/main/kotlin/com/splitwise/persistence/GroupRepository.kt` against `Database`, `GroupsTable`, and `GroupMembersTable` defined in `Tables.kt`.
5. Run `./gradlew test` — all tests must be green before committing.
6. Commit: `feat: add group repository`.

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
| SLICE-009 | Group repository | `todo` |
| SLICE-010 | Expense repository | `todo` |
| SLICE-011 | Settlement repository | `todo` |
| SLICE-012 | Register | `todo` |
| SLICE-013 | Login and logout | `todo` |
| SLICE-014 | Session filter | `todo` |
| SLICE-015 | Home page | `todo` |
| SLICE-016 | Group detail page | `todo` |
| SLICE-017 | Create group | `todo` |
| SLICE-018 | Edit group | `todo` |
| SLICE-019 | Add/remove member | `todo` |
| SLICE-020 | Add expense | `todo` |
| SLICE-021 | Edit expense | `todo` |
| SLICE-022 | Delete expense | `todo` |
| SLICE-023 | Record settlement | `todo` |
| SLICE-024 | Settlement history | `todo` |
| SLICE-025 | Balances reflect settlements | `todo` |
| SLICE-026 | Error pages | `todo` |
| SLICE-027 | Config and deployment | `todo` |
| SLICE-028 | PWA manifest and icons | `todo` |
| SLICE-029 | Service worker for app shell caching | `todo` |

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
