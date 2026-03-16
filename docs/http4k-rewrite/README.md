# http4k Rewrite — Documentation Index

This directory contains the complete plan, specification, and working notes for the
ground-up Kotlin/http4k rewrite of the Splitwise Clone app.

The Python/Flask app in `app/` remains untouched and serves as the behavioral
reference only. All new development happens in `kotlin-app/`.

## Read these in order (first time)

| Document | Purpose |
|---|---|
| [00-charter.md](./00-charter.md) | Scope, non-goals, rewrite principles, what "clean rewrite with selective fixes" means |
| [01-target-architecture.md](./01-target-architecture.md) | Package structure, layers, tech stack, dependency rules |
| [02-behavior-spec.md](./02-behavior-spec.md) | Behaviors to preserve, intentional fixes, auth matrix, validation rules |
| [03-roadmap.md](./03-roadmap.md) | Phased plan with milestones and exit criteria |
| [04-iteration-backlog.md](./04-iteration-backlog.md) | Vertical slices in priority order — the queue of work |
| [05-testing-strategy.md](./05-testing-strategy.md) | TDD expectations, test pyramid, what each level covers |
| [06-decisions.md](./06-decisions.md) | Locked architectural decisions (ADR-style) — do not reopen without good reason |

## If you are resuming work (agent or human)

**Start here:** [07-handoff.md](./07-handoff.md)

It tells you exactly what is done, what the next slice is, acceptance criteria,
and what to read first.

## Ground rules

- Never modify the Python app to suit the Kotlin rewrite. It is a reference only.
- Every slice starts with a failing test.
- Do not move to the next slice until the current one has green tests.
- When you complete a slice, update `07-handoff.md` before finishing the session.
- When you make or change a significant architectural decision, record it in `06-decisions.md`.
