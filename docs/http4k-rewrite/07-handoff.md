# Handoff — http4k Implementation

This is the operational baton. Update it at the end of every session.
The next agent (or human) picks up exactly from "Next action".

---

## Current state

The root Gradle project exists with a working `GET /health` endpoint and a dedicated
Kotlin GitHub Actions workflow. The docs have now been realigned to a server-rendered
PWA backed by centrally hosted PostgreSQL.

## What was done

- Established the Kotlin/http4k implementation direction
- Wrote `docs/http4k-rewrite/` documentation suite:
  - `README.md` — index and ground rules
  - `00-charter.md` — scope, principles, success criteria
  - `01-target-architecture.md` — package layout, tech stack, dependency rules
  - `02-behavior-spec.md` — route-by-route spec, auth matrix, validation rules
  - `03-roadmap.md` — 10 phases with exit criteria
  - `04-iteration-backlog.md` — 27 slices (SLICE-001 to SLICE-027) in dependency order
  - `05-testing-strategy.md` — test pyramid, naming, isolation rules
  - `06-decisions.md` — 13 locked ADRs
  - `07-handoff.md` — this file
- Updated `.github/instructions.md` — replaced old migration plan with pointer to docs
- Updated `Readme.md` — added rewrite section pointing to docs
- Completed `SLICE-001` — root Gradle scaffold with health endpoint
- Completed `SLICE-002` — Kotlin GitHub Actions workflow
- Revised the architecture docs to use PostgreSQL, server-rendered PWA delivery, and
  containerized Postgres for DB-backed tests

## Next action

**Start SLICE-003: Money value object.**

1. Read `docs/http4k-rewrite/04-iteration-backlog.md` SLICE-003 entry in full.
2. Write the failing `Money` tests first.
3. Implement the minimal `Money` value object.
4. Verify `./gradlew test` passes.
5. Commit the green slice.
6. Keep `SLICE-002A` in mind as the next foundation task before persistence work.

## Slice status

| Slice | Title | Status |
|---|---|---|
| SLICE-001 | Gradle project scaffold | `done` |
| SLICE-002 | Kotlin CI pipeline | `done` |
| SLICE-002A | Postgres test infrastructure | `todo` |
| SLICE-003 | Money value object | `todo` |
| SLICE-004 | Core domain entities | `todo` |
| SLICE-005 | Balance calculator | `todo` |
| SLICE-006 | Expense validator | `todo` |
| SLICE-007 | Database setup and Flyway | `todo` |
| SLICE-008 | User repository | `todo` |
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
