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
6. "Add Expense" button on group page
7. "Add Expense" page with form
8. Form submission handling for adding expenses
9. Equal expense splitting
10. Custom expense splitting

### Project Structure
- app.py: Main application file with routes and models
- templates/index.html: Home page template
- templates/group.html: Group detail page template
- templates/expenses/add.html: Add expense form template
- requirements.txt: Dependencies
- README.md: Project documentation

## Development Approach
We are following strict Test-Driven Development (TDD):
1. Write a failing test for a small piece of functionality
2. Implement the minimal code needed to make the test pass
3. Refactor if necessary
4. Repeat for the next small piece of functionality

## Current Progress
1. Implemented "Add Expense" button on group page (Step 1)
2. Implemented "Add Expense" page with route and template (Step 2)
3. Implemented form submission handling for adding expenses with equal expense shares (Step 3)
4. Implemented flexible expense splitting with custom split option (Step 4)

## Pending Functionality
Here are the remaining features to implement, in recommended order:

### Step 5: Implement Balance Calculation
1. Write tests for balance calculation
2. Implement a method to calculate balances between group members
3. Display these balances on the group page
4. Show who owes whom and how much

### Step 6: Implement Expense Editing
1. Write tests for the edit expense functionality
2. Create an edit expense page and route
3. Implement the form submission handling for updating expenses
4. Update expense shares when an expense is edited

### Step 7: Implement Expense Deletion
1. Write tests for expense deletion
2. Add a delete button/form to the group page
3. Implement the route to handle expense deletion
4. Ensure expense shares are deleted when an expense is deleted

### Step 8: Implement Settlements
1. Write tests for recording settlements
2. Create a settlement form
3. Implement the route to handle settlement creation
4. Update balance calculations to account for settlements

### Step 9: Implement User Authentication Enhancements
1. Implement password reset functionality
2. Add email verification
3. Implement "remember me" functionality
4. Add profile management

### Step 10: Improve UI/UX
1. Add proper styling with CSS
2. Implement responsive design
3. Add confirmation dialogs for important actions
4. Improve form validation and error messages

### Step 11: Add Advanced Features
1. Implement expense categories and tags
2. Add expense filtering and sorting
3. Implement expense recurring functionality
4. Add expense attachments (receipts, etc.)
5. Implement expense comments

## Development History
1. Initial commit: Basic Flask app with SQLAlchemy integration
2. Second commit: Added models, views, and sample data generation
3. Added README.md
4. Step 1: Add "Add Expense" button to group page
5. Step 2: Implement "Add Expense" page with route and template
6. Step 3: Implement form submission handling for adding expenses with equal expense shares
7. Step 4: Implement flexible expense splitting with custom split option

## GitHub Repository
The project is hosted on GitHub and includes:
- Source code
- README with setup instructions
- Requirements file
