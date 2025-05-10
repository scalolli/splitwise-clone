import pytest
from app import db

def test_user_model(app, test_data):
    """Test User model relationships"""
    with app.app_context():
        # Use the user from test_data instead of querying
        user = test_data['users']['user1']
        assert user is not None
        assert user.username == 'user1'
        
        # Convert to list before using len()
        groups = list(user.groups)
        assert len(groups) == 2
        group_names = [g.name for g in groups]
        assert 'Apartment' in group_names
        assert 'Trip' in group_names
        
        # Convert to list before using len()
        expenses_paid = list(user.expenses_paid)
        assert len(expenses_paid) > 0

def test_group_model(app, test_data):
    """Test Group model relationships"""
    with app.app_context():
        # Use the group from test_data instead of querying
        group = test_data['groups']['apartment']
        assert group is not None
        assert group.name == 'Apartment'
        
        # Convert to list before using len()
        members = list(group.members)
        assert len(members) == 2
        member_usernames = [m.username for m in members]
        assert 'user1' in member_usernames
        assert 'user2' in member_usernames
        
        # Convert to list before using len()
        expenses = list(group.expenses)
        assert len(expenses) == 2
        expense_descriptions = [e.description for e in expenses]
        assert 'Groceries' in expense_descriptions
        assert 'Rent' in expense_descriptions

def test_expense_model(app, test_data):
    """Test Expense model relationships"""
    with app.app_context():
        # Use the expense from test_data instead of querying
        expense = test_data['expenses']['groceries']
        assert expense is not None
        assert expense.description == 'Groceries'
        assert expense.amount == 100.0
        assert expense.payer.username == 'user1'
        assert expense.group.name == 'Apartment'
        
        # Convert to list before using len()
        shares = list(expense.shares)
        assert len(shares) == 2
        assert shares[0].amount + shares[1].amount == expense.amount

def test_expense_share_model(app, test_data):
    """Test ExpenseShare model relationships"""
    with app.app_context():
        # Get the first expense share from the groceries expense
        expense = test_data['expenses']['groceries']
        shares = list(expense.shares)
        assert len(shares) > 0
        
        share = shares[0]
        assert share is not None
        assert share.amount == 50.0
        assert share.expense.description == 'Groceries'
        assert share.user.username in ['user1', 'user2']

def test_user_password_hashing(app):
    """Test password hashing functionality"""
    with app.app_context():
        from app.models.user import User
        
        user = User(username='testuser', email='test@example.com')
        user.set_password('password123')
        
        assert user.password != 'password123'  # Password should be hashed
        assert user.check_password('password123')  # Should verify correct password
        assert not user.check_password('wrongpassword')  # Should reject wrong password