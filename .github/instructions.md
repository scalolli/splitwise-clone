## Testing Policy
- All new features, bug fixes, or validations must be accompanied by corresponding unit or functional tests.
- Tests should be written before implementing or modifying application code (Test-Driven Development).
- No code changes should be made without first creating or updating relevant tests.

## Current Implementation Status

### Completed Features
1. Basic Flask application setup with SQLAlchemy integration
2. Database models for all core entities (User, Group, Expense, ExpenseShare, Settlement)
3. Sample data generation for testing purposes
4. Home page implementation showing users and groups
5. Group detail page with members and expenses
6. User authentication system (login/register)
7. Group management (creation and joining)
8. Expense management with split options (equal/custom)
9. Balance calculation system within groups

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

### Pending Features
1. Settlement tracking system
2. Expense editing and deletion
3. User profile management
4. Email notification system
5. UI/UX improvements
6. API endpoints for mobile integration

### Development Methodology
- Following Test-Driven Development (TDD)
- Iterative development with regular commits
- Focus on core functionality first

### Next Steps
1. Implement expense editing functionality
   - Create forms and routes for editing expense details
   - Allow modification of expense splits
   - Update balance calculations after edits

2. Implement expense deletion
   - Add confirmation process for deletion
   - Ensure all related expense shares are removed
   - Update balance calculations after deletion

3. Enhance group management
   - Add functionality to edit group details (name, description)
   - Implement member management (add/remove members)
   - Create admin controls for group owners
   - Add permissions system for different member roles

4. Refactor authentication system
   - Implement Flask-Login with @login_required decorator
   - Apply @login_required to all authenticated routes
   - Standardize authentication checks across the application
   - Improve login/logout flow and session management

5. Complete settlement tracking system
   - Finalize settlement form and routes
   - Integrate settlements with balance calculations
   - Add settlement history views
