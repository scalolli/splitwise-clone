# Architectural Decisions

Locked decisions for the http4k rewrite. Each entry records what was decided, why,
and what alternatives were rejected. Once locked, a decision must not be reversed
without a new entry superseding it.

---

## ADR-001 — Clean rewrite, not a migration

**Status:** Locked

**Decision:** The Kotlin app is a ground-up rewrite informed by the Python app's behavior,
not a port or a strangler-fig migration. No Python code is translated line-by-line.

**Rationale:** The Python app contains known design mistakes (missing auth check on
`edit_expense`, float money, settlements not wired into balances). Carrying those forward
would mean intentionally copying bugs. A clean rewrite lets us fix them without archaeological
work on the existing codebase.

**Rejected alternatives:**
- Strangler-fig: requires shared infrastructure and introduces operational complexity with
  no benefit at this scale.
- Line-by-line port: copies bugs and produces Kotlin that reads like Python.

---

## ADR-002 — Kotlin app lives in `kotlin-app/`; Python app is untouched

**Status:** Locked

**Decision:** All Kotlin source lives under `kotlin-app/` at the repo root. The Python
`app/` directory is never modified during this rewrite. Both coexist in the same repo.

**Rationale:** Keeping the Python app intact lets us diff behavior at any point. It
serves as the behavioral spec. Modifying it during the rewrite would destroy that
reference.

---

## ADR-003 — Money is `BigDecimal` everywhere; stored as DECIMAL in the database

**Status:** Locked

**Decision:** The `Money` value object wraps `BigDecimal` with 2 decimal places (HALF_UP
rounding). The database column type is `DECIMAL(10,2)`. `Float` and `Double` are
prohibited for any monetary calculation.

**Rationale:** The Python app stores amounts as `Float`, which is subject to IEEE 754
rounding errors (e.g., `0.1 + 0.2 ≠ 0.3`). While this hasn't caused visible bugs yet,
it is a time bomb in a financial application. `BigDecimal` with fixed scale is the
standard JVM fix.

**Rejected alternatives:**
- `Long` (store pence/cents): correct but requires a currency denomination decision and
  complicates the display layer. Out of scope for a parity rewrite.
- `Double`: same problem as `Float`.

---

## ADR-004 — Flyway for schema management

**Status:** Locked

**Decision:** Flyway manages all schema changes via versioned migration scripts in
`kotlin-app/src/main/resources/db/migration/`. The application never calls any
equivalent of `db.create_all()` at startup.

**Rationale:** The Python app uses `db.create_all()`, which is not repeatable and
provides no migration history. Flyway gives us a versioned, auditable migration chain
that is safe to run in production.

**Rejected alternatives:**
- Liquibase: heavier, XML-centric, no benefit at this scale.
- Manual SQL at startup: same problem as `db.create_all()`.
- Exposed's `SchemaUtils.create()`: not migration-aware; destroys data on schema change.

---

## ADR-005 — Exposed DSL (not DAO) for persistence

**Status:** Locked

**Decision:** Persistence is implemented using the Exposed DSL layer (table objects +
`select`, `insert`, `update`, `deleteWhere`). The Exposed DAO layer (entity classes
extending `IntEntity`) is not used.

**Rationale:** The DSL layer keeps the mapping explicit and close to SQL, which makes
query behavior predictable and easy to test. The DAO layer introduces magic (lazy loading,
entity cache) that makes integration tests harder to reason about.

**Rejected alternatives:**
- Exposed DAO: rejected due to hidden lazy-loading behaviour and session coupling.
- JOOQ: correct choice but heavier; code generation adds build complexity.
- JPA/Hibernate: too much magic; conflicts with the "explicit over implicit" principle.

---

## ADR-006 — http4k `ClientCookieBasedSessions` for session management

**Status:** Locked

**Decision:** Sessions are stored in a signed cookie using http4k's built-in
`ClientCookieBasedSessions`. A `SESSION_SECRET` environment variable provides the
signing key.

**Rationale:** Eliminates the need for server-side session storage (no Redis, no DB
sessions table). Suitable for a single-instance application of this scale. The cookie
is signed, so clients cannot tamper with session contents.

**Rejected alternatives:**
- Server-side sessions (DB-backed): unnecessary complexity for this scale.
- JWT tokens: stateless tokens are a good fit for APIs; this is a server-rendered app
  with standard cookie semantics.

---

## ADR-007 — Centralized `SessionFilter` replaces per-handler auth checks

**Status:** Locked

**Decision:** A single http4k filter (`SessionFilter`) is applied at the router level
to all protected routes. It reads the session cookie, resolves the `UserId`, and injects
it into the request as a lens value. Handlers do not perform their own session checks.

**Rationale:** The Python app has manual `if 'user_id' not in session: return redirect('/login')`
guards scattered across every handler. This is brittle: any new handler that forgets the
check is silently unauthenticated. A filter applied at the router boundary makes it
impossible to accidentally add an unguarded protected route.

**Rejected alternatives:**
- Annotating handlers with a `@loginRequired` decorator: Kotlin doesn't support this
  pattern as cleanly as Python; filter composition at the router level is the http4k idiom.

---

## ADR-008 — Edit expense authorization: payer OR group creator

**Status:** Locked

**Decision:** An expense may be edited or deleted only by (a) the user who paid the
expense, or (b) the group's creator. Any other authenticated user receives a 403.
Unauthenticated requests are redirected to login.

**Rationale:** The Python `edit_expense` route has no authorization check — any
authenticated user can edit any expense. This is a security gap. The rule "payer or
group creator" mirrors the pattern used elsewhere in the Python app for other
creator-only operations, extended to include the payer as a natural owner of their expense.

**Rejected alternatives:**
- Anyone in the group can edit: too permissive; this is a financial record.
- Only the payer can edit: group creator needs override capability for administration.

---

## ADR-009 — Settlements are subtracted from displayed balances

**Status:** Locked

**Decision:** `BalanceCalculator` accepts both the list of expenses (with shares) and
the list of recorded settlements. Settlements reduce the net balance between pairs of
users. A fully settled pair shows no balance.

**Rationale:** The Python `BalanceService` computes balances from expenses only.
`SettlementService` exists but is never called from the balance calculation path. This
means balances never decrease after a settlement is recorded — the core feature of
settling debts does not work. This is the most impactful behavioral fix in the rewrite.

**Rejected alternatives:**
- Keep the Python behavior (ignore settlements in balances): rejected; this is a broken
  feature, not a design decision.

---

## ADR-010 — Handlebars for server-side templating

**Status:** Locked

**Decision:** HTML is rendered server-side using Handlebars via `http4k-template-handlebars`.
No JavaScript framework is used. Templates live in `kotlin-app/src/main/resources/templates/`.

**Rationale:** The Python app uses Jinja2, which has comparable semantics to Handlebars
(variable interpolation, partials, iteration). Handlebars is the best-supported http4k
templating option and requires no JavaScript build pipeline.

**Rejected alternatives:**
- Thymeleaf: works, but adds Spring-ism aesthetics and is heavier.
- Pebble: less community support for http4k.
- Client-side rendering (React/Vue): out of scope; adds JavaScript build complexity and
  diverges from the Python app's architecture.

---

## ADR-011 — SQLite for the database (same as Python app)

**Status:** Locked

**Decision:** SQLite is the database for the Kotlin app. The Kotlin app uses a separate
database file from the Python app (`kotlin-app/splitwise.db` or configured via `DB_PATH`).

**Rationale:** SQLite is the simplest deployable option and is used by the Python app.
There is no operational benefit to switching databases during a parity rewrite.

**Rejected alternatives:**
- PostgreSQL: correct production choice but adds Docker or a hosted service as a
  development dependency, creating friction for a parity rewrite.
- H2: suitable for tests but cannot be used in production without a mode flag, and
  behaves differently from SQLite in edge cases.

---

## ADR-012 — Expense deletion is implemented (new behavior vs Python app)

**Status:** Locked

**Decision:** The Kotlin app implements `POST /expenses/{id}/delete`. Only the expense
payer or the group creator may delete. Shares are cascade-deleted. Balances recalculate
automatically.

**Rationale:** Expense deletion is listed as a "Pending Feature (HIGH PRIORITY)" in
`.github/instructions.md`. Its absence in the Python app is an acknowledged gap, not a
deliberate product decision. Implementing it in the Kotlin rewrite is consistent with
the "fix known gaps" approach declared in the charter.

**Authorization rule:** Same as ADR-008 — payer or group creator.

---

## ADR-013 — No `Float` or `Double` in test assertions for money

**Status:** Locked

**Decision:** Test assertions involving money values use `Money` equality, not raw
`Float`/`Double` comparisons. No `assertEquals(0.3, result, 0.001)` style assertions.

**Rationale:** Consistent with ADR-003. If tests use float comparisons, they hide
precision bugs instead of catching them.
