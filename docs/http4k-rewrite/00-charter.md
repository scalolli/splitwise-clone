# Charter — http4k Rewrite

## What this is

A Kotlin/http4k implementation of the Splitwise Clone built from a clean, documented
behavior contract. We are not translating an older codebase line-by-line; we are
building the application from the specification in this directory.

## What this is not

- A line-by-line port of an older implementation
- A strangler-fig migration
- A feature expansion project — parity first, new features second
- An excuse to copy known bugs or design mistakes forward

## Where the new app lives

The Kotlin application lives at the repository root using the standard Gradle project
layout.

## Rewrite approach: clean rewrite with selective fixes

We preserve the intended user-visible behavior captured in the specification, and we
deliberately fix the following known problems instead of carrying them forward:

| Problem | Fix in Kotlin app |
|---|---|
| Expense edit lacked proper auth checks | Only the expense payer or group creator may edit an expense |
| Balance display ignored recorded settlements | Balances subtract settled amounts |
| Money used imprecise floating-point arithmetic | Money represented as `BigDecimal`, stored as DECIMAL in the database |
| No expense deletion | Expense deletion implemented with proper authorization |
| Manual per-handler session checks | Single session filter applied at the router level |
| No `@login_required` equivalent | All protected routes guarded by a centralized http4k filter |
| `db.create_all()` schema management | Flyway manages all schema migrations |

Any other divergence from the documented behavior must be a conscious, documented decision.
If it is not documented in `06-decisions.md`, it is a bug, not a fix.

## Principles

### TDD — no exceptions
Every slice starts with a failing test. No production code is written without a failing
test to drive it. The Red-Green-Refactor cycle is the unit of work.

### Vertical slices
Each iteration delivers a complete, user-visible behavior end-to-end: domain logic,
persistence, HTTP handler, and template. No layer-by-layer approach.

### Small, committed increments
Commit when a slice is green. Do not accumulate work. Each commit should be independently
deployable in principle.

### Authorization is first-class, not polish
Auth and permission checks are designed alongside routes, not added afterward.
See `02-behavior-spec.md` for the authorization matrix.

### The spec is the source of truth
Consult the documents in this directory to understand intent, not any previous
implementation details.

## Non-goals

- Email notifications
- Mobile API endpoints
- User profile management (avatar, password change)
- Real-time updates
- Multi-currency support
- Full feature parity with real Splitwise

## Success criteria

The rewrite is "done" when:

1. A user can register, log in, and log out.
2. A user can create a group and manage members.
3. A user can add, edit, and delete expenses with splits.
4. Balances are calculated correctly and reflect recorded settlements.
5. A user can record a settlement between two group members.
6. All protected routes redirect unauthenticated users to login.
7. Authorization rules are enforced: only permitted users can mutate each resource.
8. All tests are green.
9. The app boots locally with `./gradlew run` and tests pass with `./gradlew test`.
