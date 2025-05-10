# LLM Context for Splitwise Clone Project

## Project Overview
We're building a Splitwise clone using Flask and SQLAlchemy. The application allows users to track shared expenses within groups and calculate who owes whom.

## Development Approach
We're following Test-Driven Development (TDD) principles:
1. Write tests first to define expected behavior
2. Implement the minimum code needed to make tests pass
3. Refactor code while maintaining test coverage
4. Repeat for each new feature

## Current Implementation

### Technology Stack
- Flask 2.0.1
- SQLAlchemy 1.4.46
- Flask-SQLAlchemy 2.5.1
- SQLite database
- Pytest for testing

### Database Models
1. **User**: Represents application users
   - Fields: id, username, email, password (hashed)
   - Relationships: groups, expenses_paid

2. **Group**: Represents expense sharing groups
   - Fields: id, name, description, created_at, created_by_id
   - Relationships: members, expenses

3. **Expense**: Represents money spent by a user
   - Fields: id, description, amount, date, payer_id, group_id
   - Relationships: payer, group, shares

4. **ExpenseShare**: Represents how an expense is split
   - Fields: id, expense_id, user_id, amount
   - Relationships: expense, user

5. **Settlement**: Represents payments between users to settle debts
   - Fields: id, payer_id, receiver_id, amount, date, group_id
   - Relationships: payer, receiver, group

### Implemented Features
1. Complete Flask application setup with SQLAlchemy
2. Database models for all entities
3. User authentication (registration, login, logout)
4. Group creation and management
5. Group member management (add/remove members)
6. Home page showing users and groups
7. Group detail page showing members and expenses
8. Comprehensive test suite following TDD principles

### Project Structure
- app/
  - __init__.py: Application factory
  - models/: Database models
  - routes/: Route handlers
  - static/: CSS and other static files
  - templates/: HTML templates
- tests/
  - unit/: Unit tests for models
  - functional/: Functional tests for routes
  - conftest.py: Test fixtures
- config.py: Application configuration
- run.py: Application entry point

## Next Steps
Features to implement next:
1. Expense creation and management (in progress)
2. Balance calculations
3. Settlement tracking
4. User dashboard
5. Improved UI/UX

## Development History
1. Initial commit: Basic Flask app with SQLAlchemy integration
2. Added models, views, and sample data generation
3. Added README.md
4. Implemented authentication system
5. Refactored tests into a more maintainable structure
6. Implemented group creation and member management

## GitHub Repository
The project is hosted on GitHub and includes:
- Source code
- README with setup instructions
- Requirements file
- Comprehensive test suite
