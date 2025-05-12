# LLM Context for Splitwise Clone Project

## Project Overview
We're building a Splitwise clone using Flask and SQLAlchemy. The application allows users to track shared expenses within groups and calculate who owes whom.

## Current Implementation

### Technology Stack
- Flask 2.0.1
- SQLAlchemy 1.4.46
- Flask-SQLAlchemy 2.5.1
- SQLite database

### Database Models
1. **User**: Represents application users
   - Fields: id, username, email, password
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
1. Basic Flask application setup with SQLAlchemy
2. Database models for all entities
3. Sample data generation for testing
4. Home page showing users and groups
5. Group detail page showing members and expenses
6. User authentication with login/register functionality
7. Forms for creating and joining groups
8. Forms for adding expenses with equal or custom splits
9. Balance calculations showing who owes whom within a group

### Project Structure
- app/
  - __init__.py: Application factory
  - models/: Database models
  - routes/: Route handlers
  - services/: Business logic
  - templates/: HTML templates
  - forms/: WTForms definitions
- tests/
  - unit/: Unit tests
  - functional/: Functional tests
  - conftest.py: Test fixtures
- config.py: Application configuration
- requirements.txt: Dependencies
- README.md: Project documentation

## Next Steps
Potential features to implement next:
1. Settlement tracking (recording payments between users)
2. Expense editing and deletion
3. User profile management
4. Email notifications
5. Improved UI/UX with CSS framework
6. API endpoints for mobile integration

## Development History
1. Initial commit: Basic Flask app with SQLAlchemy integration
2. Added models, views, and sample data generation
3. Implemented user authentication
4. Added group creation and joining functionality
5. Implemented expense creation with splitting options
6. Added balance calculation and display on group pages

## Development Approach
We're following a Test-Driven Development (TDD) approach:
1. Write tests first
2. Implement the minimal code needed to make tests pass
3. Refactor as needed

## GitHub Repository
The project is hosted on GitHub and includes:
- Source code
- README with setup instructions
- Requirements file
