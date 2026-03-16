# Testing Strategy

## The rule

No production code is written without a failing test to drive it.
Red → Green → Refactor. No exceptions.

---

## Test pyramid

```
           [ Acceptance ]      — fewest; full user journey through HTTP
          [  Integration  ]    — repository tests against real PostgreSQL
        [     Unit tests    ]  — most; domain logic, services, validation
```

### Unit tests (`test/kotlin/com/splitwise/domain/`, `test/kotlin/com/splitwise/service/`)
- Fast; no I/O, no DB, no HTTP
- Test `Money`, `BalanceCalculator`, `ExpenseValidator`, and all services
- Services are tested with fake/stub repository implementations
- One test class per production class
- Cover every validation rule, every edge case in balance math
- Run in milliseconds; should always be runnable locally

### Repository (integration) tests (`test/kotlin/com/splitwise/persistence/`)
- Test repository implementations against a real PostgreSQL database started from Docker
- Prefer Testcontainers with the official PostgreSQL image for automation and isolation
- Each test class creates a fresh schema via Flyway before each test or uses an isolated database/schema
- Test save, find, update, delete, constraint violations
- Slower than unit tests, but production-shaped and still suitable for local development
- Do not use fakes; test against the real `Exposed` implementation

### HTTP handler tests (`test/kotlin/com/splitwise/web/`)
- Use http4k's in-process test client (`MockHttp4kClient` or `http4kClient`)
- The entire app stack is wired in the test — handlers, filters, services, real repos
- Use disposable PostgreSQL for isolation via the shared test harness
- Test request/response contracts: status codes, redirects, flash messages, HTML content
- Seed data programmatically in each test setup; do not share state between tests
- Each test is independent and self-contained

### Acceptance tests (`test/kotlin/com/splitwise/acceptance/`)
- End-to-end tests for the most critical user journeys
- Use the same in-process http4k test client — no browser, no Playwright
- Cover: register → login → create group → add expense → settle → verify balance
- Kept small in number; these are smoke tests, not exhaustive suites

---

## What each test level covers

| Level | Covers | Does not cover |
|---|---|---|
| Unit | Business logic, validation rules, balance math | HTTP, DB, templates |
| Repository | SQL, constraints, ordering, cascades | HTTP, business logic |
| Handler | HTTP contracts, auth/authz, redirects, flash messages | Template rendering detail |
| Acceptance | Critical user journeys end-to-end | Exhaustive edge cases |

---

## Test naming convention

Use descriptive names that read as behavior statements:

```kotlin
// Good
@Test fun `balance reduces when settlement is recorded`() { ... }
@Test fun `editing an expense is forbidden for non-payer non-creator`() { ... }
@Test fun `registering with a duplicate username returns an error message`() { ... }

// Bad
@Test fun testEditExpense() { ... }
@Test fun test1() { ... }
```

---

## Test setup and isolation

- Every repository test uses a fresh PostgreSQL test database or schema initialized by Flyway
- Every handler test sets up its own users/groups/expenses via the repository directly
  (never via HTTP, to keep setup fast and explicit)
- No shared static state; no test ordering dependencies
- If a test needs authenticated state, set the session cookie directly in the request,
  do not replay the login flow in every test

## Docker requirement

- DB-backed tests require Docker locally
- Recommended implementation: Testcontainers PostgreSQL
- If Docker is unavailable, only pure unit tests should be considered runnable

---

## Fake vs mock

Prefer hand-written fakes over mocking frameworks:
- `FakeUserRepository`, `FakeGroupRepository`, `FakeExpenseRepository`,
  `FakeSettlementRepository` — implement the repository interfaces with in-memory maps
- Use these fakes in unit tests for services
- Use real implementations backed by disposable PostgreSQL for handler tests

Mocking frameworks (`mockk`, `Mockito`) may be used sparingly for verifying
interactions where a fake would be over-engineered, but fakes are preferred.

---

## Coverage expectations

Coverage is not the goal; behavior coverage is. However:
- Every validation rule from `02-behavior-spec.md` must have at least one dedicated test
- Every authorization rule from `02-behavior-spec.md` must have at least one test for
  both the allowed and the forbidden case
- Every happy path must have a test
- Error pages (404, 403) must have tests

---

## Running tests

```bash
# All tests
./gradlew test

# Unit tests only
./gradlew test --tests "com.splitwise.domain.*" --tests "com.splitwise.service.*"

# Handler tests only
./gradlew test --tests "com.splitwise.web.*"
```

Tests must always be green before committing. If a test is failing and you are not
actively working on it, it should not be committed. Never commit a `@Disabled` test
without a comment explaining why and a corresponding backlog item.
