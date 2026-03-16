# Splitwise Clone - Development Instructions

## Overview
This repository is now in the http4k rewrite phase. The active product direction is a ground-up Kotlin/http4k implementation. The legacy Flask application remains in the repository as a behavioral reference and is not the main delivery track.

## Active Project Direction
- Primary work happens in the Kotlin rewrite track
- Start with `docs/http4k-rewrite/README.md` for orientation
- Use `docs/http4k-rewrite/07-handoff.md` to find the exact next slice
- The next implementation step is `SLICE-001: Gradle project scaffold`
- The Python app in `app/` is effectively frozen and should only be used as a source of behavioral truth

## Rewrite Technology Direction
- Kotlin
- http4k
- Gradle
- Test-first implementation per slice

## Legacy Flask Database Models
- User: Core user accounts with authentication
- Group: Expense sharing groups with members
- Expense: Individual expenses paid by users
- ExpenseShare: How expenses are split among group members
- Settlement: Payments between users to settle debts

## Legacy Flask Implementation Status

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

### Legacy Pending Features
These are no longer the active roadmap for this repository. Equivalent or improved behavior should be delivered through the Kotlin/http4k rewrite backlog instead.

### Development Methodology
- Following Test-Driven Development (TDD)
- Iterative development with regular commits
- Focus on the rewrite slices first
- **ALL code changes must have corresponding tests**

## Testing Policy
This project follows strict Test-Driven Development (TDD). All new features, bug fixes, or validations MUST have corresponding tests written FIRST. No code changes are allowed without creating or updating relevant tests.

### Next Steps

#### 1. Start the Kotlin/http4k implementation
- Read `docs/http4k-rewrite/07-handoff.md`
- Read the `SLICE-001` entry in `docs/http4k-rewrite/04-iteration-backlog.md`
- Create `kotlin-app/`
- Write the failing `HealthCheckTest` first
- Implement the minimal `/health` endpoint to go green
- Update `docs/http4k-rewrite/07-handoff.md` when the slice is complete

---

## http4k Rewrite

The Python app rewrite is now the active phase of the repository. The full plan,
architectural decisions, slice-by-slice backlog, and handoff state live in:

**`docs/http4k-rewrite/`** — start with `README.md` for orientation.

The Kotlin app will live in `kotlin-app/`. The Python app in `app/` should be treated as a frozen legacy reference unless a documentation update explicitly says otherwise.

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

These numbers describe the legacy Flask app, not the rewrite progress.

## Key Implementation Notes
- Expense editing uses BaseExpenseForm with comprehensive validation
- All form validations include custom error messages
- Balance calculations are automatically updated after any expense changes
- Test coverage is mandatory for all new features
- Following Flask best practices with Blueprint organization

For active delivery decisions, prefer the rewrite docs over this legacy implementation detail section.
