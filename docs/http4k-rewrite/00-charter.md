# Charter — http4k Rewrite

## What this is

A ground-up rewrite of the Splitwise Clone in Kotlin using http4k. The Python/Flask app
is the behavioral reference, not a migration source. We are not translating Python to
Kotlin; we are building the right thing from scratch, informed by what the Python app does.

## What this is not

- A line-by-line port of the Flask app
- A strangler-fig migration (no traffic proxying, no shared database with the Python app)
- A feature expansion project — parity first, new features second
- An excuse to copy known bugs or design mistakes forward

## Where the new app lives

All Kotlin code lives in `kotlin-app/` at the repo root. The Python app in `app/` is
never modified during this rewrite. Both coexist in the same repo but are completely
independent.

## Rewrite approach: clean rewrite with selective fixes

We preserve the intended user-visible behavior of the Python app, but we deliberately
fix the following known problems instead of copying them:

| Problem in Python app | Fix in Kotlin app |
|---|---|
| `edit_expense` has no auth check — any user can edit any expense | Only the expense payer or group creator may edit an expense |
| Balance display ignores recorded settlements | Balances subtract settled amounts |
| Money stored as `Float` (rounding errors possible) | Money represented as `BigDecimal`, stored as DECIMAL in the database |
| No expense deletion | Expense deletion implemented with proper authorization |
| Manual per-handler session checks | Single session filter applied at the router level |
| No `@login_required` equivalent | All protected routes guarded by a centralized http4k filter |
| `db.create_all()` schema management | Flyway manages all schema migrations |

Any other divergence from Python behavior must be a conscious, documented decision.
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

### The Python app is the spec, not the gold standard
Consult it to understand intent, not to copy implementation.

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
