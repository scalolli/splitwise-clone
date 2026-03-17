# Architectural Decisions

Locked decisions for the http4k rewrite. Each entry records what was decided, why,
and what alternatives were rejected. Once locked, a decision must not be reversed
without a new entry superseding it.

---

## ADR-001 — Clean implementation, not a migration

**Status:** Locked

**Decision:** The Kotlin app is a clean implementation from the documented behavior,
not a port or a strangler-fig migration. No previous code is translated line-by-line.

**Rationale:** Earlier implementations contained known design mistakes such as missing
auth checks on `edit_expense`, imprecise money handling, and settlements not wired into
balances. A clean implementation lets us fix them without carrying forward accidental
design debt.

**Rejected alternatives:**
- Strangler-fig: requires shared infrastructure and introduces operational complexity with
  no benefit at this scale.
- Line-by-line port: copies bugs and produces Kotlin that reads like a different language.

---

## ADR-002 — Kotlin app lives at the repository root

**Status:** Locked

**Decision:** All Kotlin source lives at the repository root using the standard Gradle
project layout.

**Rationale:** The root layout keeps builds, CI, and deployment simple and makes the
Kotlin application the single active delivery track.

---

## ADR-003 — Money is `BigDecimal` everywhere; stored as DECIMAL in the database

**Status:** Locked

**Decision:** The `Money` value object wraps `BigDecimal` with 2 decimal places (HALF_UP
rounding). The database column type is `DECIMAL(10,2)`. `Float` and `Double` are
prohibited for any monetary calculation.

**Rationale:** IEEE 754 floating-point values are not suitable for financial calculations.
`BigDecimal` with fixed scale is the standard JVM fix.

**Rejected alternatives:**
- `Long` (store pence/cents): correct but requires a currency denomination decision and
  complicates the display layer. Out of scope for a parity rewrite.
- `Double`: same problem as `Float`.

---

## ADR-004 — Flyway for schema management

**Status:** Locked

**Decision:** Flyway manages all schema changes via versioned migration scripts in
`src/main/resources/db/migration/`. The application never calls any
equivalent of `db.create_all()` at startup.

**Rationale:** Startup schema creation is not repeatable and provides no migration history.
Flyway gives us a versioned, auditable migration chain
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

**Rationale:** Manual auth guards scattered across handlers are brittle. A filter applied
at the router boundary makes it impossible to accidentally add an unguarded protected route.

**Rejected alternatives:**
- Annotating handlers with a `@loginRequired` decorator: Kotlin doesn't support this
  pattern as cleanly; filter composition at the router level is the http4k idiom.

---

## ADR-008 — Edit expense authorization: payer OR group creator

**Status:** Locked

**Decision:** An expense may be edited or deleted only by (a) the user who paid the
expense, or (b) the group's creator. Any other authenticated user receives a 403.
Unauthenticated requests are redirected to login.

**Rationale:** Allowing any authenticated user to edit any expense is a security gap. The
rule "payer or group creator" reflects the natural ownership model for a financial record
while preserving administrative override capability.

**Rejected alternatives:**
- Anyone in the group can edit: too permissive; this is a financial record.
- Only the payer can edit: group creator needs override capability for administration.

---

## ADR-009 — Settlements are subtracted from displayed balances

**Status:** Locked

**Decision:** `BalanceCalculator` accepts both the list of expenses (with shares) and
the list of recorded settlements. Settlements reduce the net balance between pairs of
users. A fully settled pair shows no balance.

**Rationale:** If balances do not subtract settlements, the core debt-settling workflow
does not work. Settlement-aware balances are therefore a required behavior.

**Rejected alternatives:**
- Ignore settlements in balances: rejected; this is a broken feature, not a design decision.

---

## ADR-010 — Handlebars for server-side templating

**Status:** Locked

**Decision:** HTML is rendered server-side using Handlebars via `http4k-template-handlebars`.
No JavaScript framework is used. Templates live in `src/main/resources/templates/`.

**Rationale:** Handlebars provides the server-rendered templating model needed here and is
the best-supported http4k
templating option and requires no JavaScript build pipeline.

**Rejected alternatives:**
- Thymeleaf: works, but adds Spring-ism aesthetics and is heavier.
- Pebble: less community support for http4k.
- Client-side rendering (React/Vue): out of scope; adds JavaScript build complexity.

---

## ADR-011 — PostgreSQL for the shared database

**Status:** Locked

**Decision:** PostgreSQL is the database for the Kotlin app in all real environments.
The application connects via `DATABASE_URL`. Local DB-backed tests run against a
containerized PostgreSQL instance.

**Rationale:** The application is a centrally hosted multi-user system, so it needs a
database that handles shared access, concurrent writes, managed hosting, and clean cloud
deployment. PostgreSQL fits that model directly.

**Rejected alternatives:**
- SQLite: acceptable for single-node local prototypes, but not the preferred central
  multi-user production database.
- H2: suitable for tests but differs from PostgreSQL in important SQL and constraint behavior.

---

## ADR-012 — Server-rendered PWA on the same backend

**Status:** Locked

**Decision:** The http4k application serves the HTML frontend, form posts, static assets,
PWA manifest, and service worker from the same origin. Public JSON APIs are not required
for v1.

**Rationale:** This keeps authentication, routing, validation, and deployment simpler while
still delivering an installable mobile-friendly experience.

**Rejected alternatives:**
- API-first SPA: adds frontend build and client-state complexity too early.
- Separate frontend and backend deployments: unnecessary operational split for v1.

---

## ADR-013 — Expense deletion is implemented

**Status:** Locked

**Decision:** The Kotlin app implements `POST /expenses/{id}/delete`. Only the expense
payer or the group creator may delete. Shares are cascade-deleted. Balances recalculate
automatically.

**Rationale:** Expense deletion is a necessary part of a usable expense-management flow.
Implementing it is consistent with the "fix known gaps" approach declared in the charter.

**Authorization rule:** Same as ADR-008 — payer or group creator.

---

## ADR-014 — No `Float` or `Double` in test assertions for money

**Status:** Locked

**Decision:** Test assertions involving money values use `Money` equality, not raw
`Float`/`Double` comparisons. No `assertEquals(0.3, result, 0.001)` style assertions.

**Rationale:** Consistent with ADR-003. If tests use float comparisons, they hide
precision bugs instead of catching them.

---

## ADR-015 — DB-backed tests use containerized PostgreSQL

**Status:** Locked

**Decision:** Repository, handler, and acceptance tests that need a real database use
containerized PostgreSQL, preferably via Testcontainers.

**Rationale:** DB-backed tests should exercise the same database family used in deployment.
This reduces drift between tests and production while keeping setup reproducible.

**Rejected alternatives:**
- In-memory SQLite or H2: too much behavior drift from PostgreSQL.
- Shared long-running local database: brittle, stateful, and harder for agents to run safely.

---

## ADR-016 — Testcontainers singleton with `freshDatabase()` for test isolation

**Status:** Locked

**Decision:** A single `PostgreSQLContainer` is started once per JVM via a Kotlin `object`
with `lazy` initialization (`PostgresTestSupport`). Each test calls `freshDatabase()` which
creates a UUID-named database, connects, and runs Flyway migrations — returning a
ready-to-use `Database`. Tests never share database state.

`docker-compose.yml` exists separately for running the application locally with a persistent
database. It is not used by the test suite.

**`freshConfig()` is preserved** for tests that need to verify Flyway behaviour directly
(e.g., `DatabaseIntegrationSmokeTest`). All other tests use `freshDatabase()`.

**Rationale:**
- One container start per JVM avoids repeated Docker pull/start overhead while keeping
  complete isolation between tests via separate named databases.
- `freshDatabase()` removes boilerplate from every repository test — connect + migrate is
  done once in one place.
- Docker Compose for local dev and Testcontainers for automated tests serve different
  purposes; conflating them would make tests depend on external state.

**Rejected alternatives:**
- Docker Compose for tests: requires "did you start Compose?" discipline; breaks CI
  without extra setup steps; shared state between test runs unless manually cleaned.
- One container per test class: slower startup; offers no isolation benefit over
  per-test UUID databases on a shared container.
- Truncating tables between tests: fragile; requires enumerating tables; breaks if a new
  table is added without updating the truncation list.

---

## ADR-017 — Group visibility is membership-scoped

**Status:** Locked

**Decision:** A signed-in user may only see groups they belong to on the home page, and
may only view a group detail page if they are a member of that group. Non-members do not
have read access to group details.

**Rationale:** Group pages contain member names, expenses, and balances, which are private
financial records. Exposing them to arbitrary authenticated users is an authorization bug,
not a product feature. The home page should likewise avoid revealing unrelated groups.

**Rejected alternatives:**
- Any authenticated user can view any group: rejected due to privacy and authorization risk.
- Home page shows all groups for discovery: rejected because group existence is itself
  sensitive data in this product.
