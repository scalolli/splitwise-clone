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

### Project Structure
- app.py: Main application file with routes and models
- templates/index.html: Home page template
- templates/group.html: Group detail page template
- requirements.txt: Dependencies
- README.md: Project documentation

## Next Steps
Potential features to implement next:
1. User authentication
2. Forms for creating groups
3. Forms for adding expenses
4. Balance calculations
5. Settlement tracking
6. Improved UI/UX

## Development History
1. Initial commit: Basic Flask app with SQLAlchemy integration
2. Second commit: Added models, views, and sample data generation
3. Added README.md

## GitHub Repository
The project is hosted on GitHub and includes:
- Source code
- README with setup instructions
- Requirements file
