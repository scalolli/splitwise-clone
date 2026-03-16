# Target Architecture

## Repository layout

```
splitwise-clone/
├── build.gradle.kts
├── settings.gradle.kts
├── src/
│   ├── main/kotlin/com/splitwise/
│   │   ├── App.kt              # Entry point; wires everything together
│   │   ├── domain/             # Pure domain model — no I/O dependencies
│   │   ├── persistence/        # Exposed table definitions and repositories
│   │   ├── service/            # Application services — orchestrate domain + repos
│   │   ├── web/                # http4k handlers, filters, routing
│   │   └── config/             # Config loading, dependency wiring
│   ├── main/resources/
│   │   ├── db/migration/       # Flyway SQL migration files
│   │   ├── public/             # CSS, JS, icons, manifest, service worker
│   │   └── templates/          # Handlebars templates
│   └── test/kotlin/com/splitwise/
│       ├── domain/             # Pure unit tests — no DB, no HTTP
│       ├── persistence/        # Repository integration tests — real PostgreSQL
│       ├── service/            # Service unit tests — mocked repos
│       └── web/                # HTTP handler tests — in-process http4k client
└── docs/http4k-rewrite/        # This documentation
```

## Package responsibilities

### `domain/`
- Value objects: `UserId`, `GroupId`, `ExpenseId`, `SettlementId`, `Money`
- Entities: `User`, `Group`, `Expense`, `ExpenseShare`, `Settlement`
- Domain logic: `BalanceCalculator`
- Validation rules: `ExpenseValidator`
- No imports from `persistence`, `service`, `web`, or any framework

### `persistence/`
- Exposed table definitions (DSL style, not DAO)
- Repository interfaces defined here, implemented here
- `UserRepository`, `GroupRepository`, `ExpenseRepository`, `SettlementRepository`
- Transaction management at repository boundary
- No imports from `web` or `service`

### `service/`
- Application-level use cases: `AuthService`, `GroupService`, `ExpenseService`, `BalanceService`, `SettlementService`
- Orchestrates domain logic and repository calls
- No HTTP types; no Exposed or database-driver types
- Depends on `domain/` and repository interfaces from `persistence/`

### `web/`
- http4k route handlers (one file per resource: `AuthHandler`, `GroupHandler`, `ExpenseHandler`, `SettlementHandler`)
- Filters: `SessionFilter` (auth enforcement), `ErrorHandler`
- Router: `SplitwiseApp` — assembles all routes and filters
- Depends on `service/` only; never touches `persistence/` directly
- Template rendering via Handlebars
- Serves PWA assets: `manifest.webmanifest`, `service-worker.js`, icons, and static files

### `config/`
- `AppConfig` — reads environment variables and configuration
- `Dependencies` — wires all layers together (poor-man's DI)
- `App.kt` — starts the Jetty server

### `public/`
- `manifest.webmanifest` — install metadata for the PWA
- `service-worker.js` — cache strategy for static assets and app shell
- icons, CSS, and small JavaScript enhancements

## Tech stack

| Concern | Choice | Reason |
|---|---|---|
| Language | Kotlin | JVM, concise, null-safe |
| HTTP framework | http4k | Functional, testable without a running server, zero magic |
| Server | http4k-server-jetty | Simple, production-ready |
| Templating | http4k-template-handlebars | Server-rendered HTML, logic-less templates |
| Frontend delivery | Server-rendered PWA | Same app serves HTML, form posts, and installable web assets |
| ORM / query DSL | Exposed (DSL mode) | Type-safe SQL, no reflection magic |
| Schema migrations | Flyway | Explicit, versioned, reproducible |
| Database | PostgreSQL | Shared, concurrent, production-ready relational database |
| Password hashing | BCrypt (jBCrypt) | Industry standard |
| Sessions | http4k `ClientCookieBasedSessions` | Stateless, no server-side session store needed for this scale |
| Testing | JUnit 5 + http4k test + Testcontainers | In-process HTTP tests and disposable PostgreSQL for DB-backed tests |
| Build | Gradle (Kotlin DSL) | Standard JVM toolchain |

## Dependency rule

```
web → service → domain
persistence → domain
config → everything
```

`domain` has zero external dependencies. `service` depends on domain and persistence
interfaces. `web` depends on service only. This keeps the application testable while
allowing the same backend to serve the full PWA.

## Session model

Sessions store `userId: Int` in a signed cookie. The `SessionFilter` reads the cookie on
every request to a protected route and rejects with a redirect to `/login` if absent or
invalid. The filter is applied at the router level, not per handler.

## Delivery model

- The application is same-origin: HTML pages, form posts, static assets, and any future
  JSON endpoints are served by the same http4k process.
- v1 is server-rendered and online-first.
- The PWA caches static assets and app shell resources for fast reloads and installability.
- Public JSON APIs are not required for the core delivery plan.

## Configuration

All config is read from environment variables at startup. Defaults are provided for
local development. No config files in the classpath other than `application.conf` for
local defaults (not committed with secrets).

| Variable | Default | Purpose |
|---|---|---|
| `DATABASE_URL` | none | PostgreSQL connection string |
| `PORT` | `8080` | HTTP server port |
| `SESSION_SECRET` | `changeme` | Cookie signing secret — must be changed in production |

## Error handling

- `404` — rendered as an HTML error page, not a stack trace
- `400` — form validation errors are shown inline on the form page
- `403` — rendered as a short "not permitted" page
- `500` — generic error page; stack trace logged server-side only
- All errors handled by a single `ErrorHandler` filter at the top level
