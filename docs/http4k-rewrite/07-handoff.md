# Handoff — http4k Rewrite

This is the operational baton. Update it at the end of every session.
The next agent (or human) picks up exactly from "Next action".

---

## Current state

All planning documentation is written and committed. No Kotlin code exists yet.
The `kotlin-app/` directory does not exist.

## What was done (planning sessions)

- Critically reviewed the old migration plan in `.github/instructions.md`
- Chose "clean rewrite with selective fixes" as the approach
- Identified all known Python bugs to fix (see `00-charter.md` fix table)
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

## Next action

**Start SLICE-001: Gradle project scaffold.**

1. Read `docs/http4k-rewrite/04-iteration-backlog.md` SLICE-001 entry in full.
2. Create `kotlin-app/` directory structure.
3. Write the failing test first: `HealthCheckTest` — `GET /health` returns `200 OK`
   with body `{"status":"ok"}`.
4. Write minimal implementation to make it pass.
5. Verify: `./gradlew test` passes, `./gradlew run` boots.
6. Commit: `feat: SLICE-001 Gradle scaffold with health endpoint`.
7. Update this file: mark SLICE-001 done, set next action to SLICE-002.

## Slice status

| Slice | Title | Status |
|---|---|---|
| SLICE-001 | Gradle project scaffold | `todo` |
| SLICE-002 | Kotlin CI pipeline | `todo` |
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

## Key reference files

| File | Purpose |
|---|---|
| `docs/http4k-rewrite/00-charter.md` | Scope, principles, fix table |
| `docs/http4k-rewrite/02-behavior-spec.md` | Auth matrix, validation rules, exact error messages |
| `docs/http4k-rewrite/04-iteration-backlog.md` | Slice definitions — read before starting any slice |
| `docs/http4k-rewrite/06-decisions.md` | Locked ADRs — check before making any architectural choice |
| `app/forms/base_expense_form.py` | Python source of truth for all 7 validation rules |
| `app/services/balance_service.py` | Shows the settlement gap that must be fixed |

## Non-negotiable rules

- TDD: failing test before any production code. No exceptions.
- Update this file (07-handoff.md) at the end of every session.
- New architectural decisions go in `06-decisions.md` before they are implemented.
- Do not modify anything under `app/`.
- Commit when a slice is green. One commit per slice minimum.
