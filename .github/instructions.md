# Splitwise Clone - Development Instructions

## Overview
This is a Splitwise-like expense sharing web application built with Flask, using SQLAlchemy for ORM and SQLite as the database.

## Technology Stack
- Flask 2.0.1
- SQLAlchemy 1.4.46
- Flask-SQLAlchemy 2.5.1
- SQLite database

## Database Models
- User: Core user accounts with authentication
- Group: Expense sharing groups with members
- Expense: Individual expenses paid by users
- ExpenseShare: How expenses are split among group members
- Settlement: Payments between users to settle debts

## Current Implementation Status

### ✅ Completed Features
1. Basic Flask app with SQLAlchemy integration
2. Complete database models (User, Group, Expense, ExpenseShare, Settlement)
3. Sample data generation
4. Home page showing users and groups
5. Group detail pages with members and expenses
6. User authentication (login/register)
7. Group management (create/join groups)
8. Expense management with split options (equal/custom splits)
9. Balance calculation system within groups
10. **Expense editing functionality (COMPLETED)**
    - Complete form validation with comprehensive error handling
    - Edit expense details (description, amount, date, payer)
    - Modify expense splits (add/remove users, update amounts)
    - Automatic balance recalculation after edits
    - All 65 tests passing with full test coverage

### Pending Features
1. **Expense deletion (HIGH PRIORITY)**
2. Settlement tracking system
3. User profile management
4. Email notification system
5. UI/UX improvements
6. API endpoints for mobile integration

### Development Methodology
- Following Test-Driven Development (TDD)
- Iterative development with regular commits
- Focus on core functionality first
- **ALL code changes must have corresponding tests**

## Testing Policy
This project follows strict Test-Driven Development (TDD). All new features, bug fixes, or validations MUST have corresponding tests written FIRST. No code changes are allowed without creating or updating relevant tests.

### Next Steps

#### 1. Implement Expense Deletion (HIGH PRIORITY)
- Create DELETE route for expenses (`/expenses/{id}/delete`)
- Add confirmation dialog/page before deletion
- Implement proper authorization (only payer or group admin can delete)
- Remove all related ExpenseShare records
- Update balance calculations after deletion
- Write comprehensive tests for deletion functionality

#### 2. Refactor Authentication System (MEDIUM PRIORITY)
- Implement Flask-Login with @login_required decorator
- Apply @login_required to all authenticated routes
- Standardize authentication checks across the application
- Improve login/logout flow and session management
- Add proper user session handling

#### 3. Enhance Group Management (MEDIUM PRIORITY)
- Add functionality to edit group details (name, description)
- Implement advanced member management (add/remove members from group page)
- Create admin controls for group owners
- Add permissions system for different member roles
- Implement group deletion functionality

#### 4. User Profile Management (MEDIUM PRIORITY)
- Create user profile pages
- Allow users to edit their details (username, email, password)
- Add profile picture support
- Implement account deletion functionality

#### 5. Settlement System Enhancements (LOW PRIORITY)
- Implement settlement tracking and history
- Add settlement suggestions based on optimized debt resolution
- Create settlement confirmation system
- Add payment method tracking

#### 6. UI/UX Improvements (ONGOING)
- Improve responsive design for mobile devices
- Add loading states and better error handling in UI
- Implement confirmation dialogs for destructive actions
- Add keyboard shortcuts for common actions
- Improve accessibility compliance

---

## http4k Migration Plan

The plan is to rewrite this application as a Kotlin/http4k app, delivered incrementally. The new app will live in a `kotlin-app/` subdirectory inside this repo. The Python app remains untouched during migration.

**Chosen stack:**
- Language: Kotlin (JVM)
- Framework: http4k
- Persistence: Exposed ORM + Flyway migrations + SQLite
- Templating: Handlebars (`http4k-template-handlebars`)
- Auth: http4k sessions + BCrypt
- Testing: JUnit 5 + http4k test module

### Phase 1 — Project Scaffolding
- Set up Kotlin/Gradle project in `kotlin-app/`
- Configure dependencies: http4k-core, http4k-server-jetty, Exposed, SQLite JDBC, BCrypt
- Set up test infrastructure: JUnit 5, http4k test module, in-memory SQLite
- GitHub Actions CI: build + test on every push
- **Deliverable:** `./gradlew test` passes with a `GET /health` smoke test

### Phase 2 — Domain Model (pure Kotlin, no persistence)
- Define value objects: `UserId`, `GroupId`, `ExpenseId`, `Money`
- Define domain entities: `User`, `Group`, `Expense`, `ExpenseShare`, `Settlement`
- Implement `BalanceCalculator` — port the pairwise simplification algorithm from `balance_service.py`
- No persistence, no HTTP — pure domain logic with unit tests
- **Deliverable:** Balance algorithm fully covered by unit tests

### Phase 3 — Persistence Layer
- Define Exposed table schemas
- Implement repository interfaces + Exposed implementations: `UserRepository`, `GroupRepository`, `ExpenseRepository`, `SettlementRepository`
- Schema management via Flyway (replaces `db.create_all()`)
- Integration tests against in-memory SQLite
- **Deliverable:** All repositories tested against real SQLite

### Phase 4 — Authentication
- `POST /register`, `POST /login`, `GET /logout`
- http4k session filter to protect routes — replaces the manual per-handler session checks in the Python app
- BCrypt password hashing
- **Deliverable:** Register, login, logout working with integration tests; protected routes return redirect to login

### Phase 5 — Groups
- `GET /` — list all groups
- `GET /group/{id}` — group detail with members, expenses, calculated balances
- `GET|POST /group/create` — create group
- `POST /group/{id}/add_member` — add member (creator only)
- `POST /group/{id}/remove_member/{userId}` — remove member (creator only)
- `GET|POST /group/{id}/edit` — edit group name/description
- **Deliverable:** Full group management with functional tests

### Phase 6 — Expenses
- `GET|POST /group/{id}/add_expense` — add expense with splits
- `GET|POST /expenses/{id}/edit` — edit expense (with auth guard — fixing the Python security gap)
- Port all validation from `BaseExpenseForm`: split sum check, payer-in-splits, membership check, duplicate check
- **Deliverable:** Expense creation and editing with full validation

### Phase 7 — Balances & Settlements
- Wire `BalanceCalculator` into the group detail page
- `GET /group/{id}/settlements` — settlement history
- `POST /group/{id}/settle` — record a settlement
- Update `BalanceCalculator` to subtract settled amounts (fixing the Python gap where settlements are stored but ignored)
- **Deliverable:** Balances display correctly; settlements reduce displayed balances

### Phase 8 — Polish & Hardening
- Fix all known Python gaps carried over (unauthenticated edit expense, settlements not wired up)
- Proper 400/404/500 error handling
- Replace N+1 balance queries with a single join query
- **Deliverable:** All functional tests green; no known security gaps

---

## Project Structure
```
app/
├── models/          # Database models
├── routes/          # Route handlers
├── services/        # Business logic
├── forms/          # WTForms validation
├── templates/      # Jinja2 templates
├── static/         # CSS, JS, images
└── utils/          # Helper utilities

tests/
├── functional/     # End-to-end tests
├── unit/          # Unit tests for individual components
└── conftest.py    # Test configuration and fixtures
```

## Current Test Status
- **65 tests passing, 1 skipped**
- **100% success rate on active tests**
- Full coverage for expense editing functionality
- Comprehensive form validation testing
- All authentication and group management tests passing

## Key Implementation Notes
- Expense editing uses BaseExpenseForm with comprehensive validation
- All form validations include custom error messages
- Balance calculations are automatically updated after any expense changes
- Test coverage is mandatory for all new features
- Following Flask best practices with Blueprint organization
