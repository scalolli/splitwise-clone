import pytest
from app import create_app, db
from config import TestConfig
from app.models.user import User
from app.models.group import Group
from app.models.expense import Expense
from app.models.expense_share import ExpenseShare

@pytest.fixture
def client():
    app = create_app(TestConfig)  # Use the TestConfig class
    app.config['TESTING'] = True
    app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///:memory:'
    app.config['WTF_CSRF_ENABLED'] = False
    
    with app.test_client() as client:
        with app.app_context():
            db.create_all()
            
            # Create test users
            user1 = User(username='testuser1', email='test1@example.com', password='password')
            user2 = User(username='testuser2', email='test2@example.com', password='password')
            db.session.add_all([user1, user2])
            db.session.commit()
            
            # Create test group
            group = Group(name='Test Group', description='Test Description', created_by_id=user1.id)
            group.members.append(user1)
            group.members.append(user2)
            db.session.add(group)
            db.session.commit()
            
            yield client
            
            db.session.remove()
            db.drop_all()

def test_add_expense_page_requires_login(client):
    """Test that the add expense page requires login."""
    response = client.get('/group/1/add_expense')
    assert response.status_code == 302  # Redirect to login page

def test_add_expense_page_loads(client):
    """Test that the add expense page loads for logged in users."""
    # Log in the user
    with client.session_transaction() as session:
        session['user_id'] = 1  # Set user_id directly in the session
    
    # Access the add expense page
    response = client.get('/group/1/add_expense')
    assert response.status_code == 200
    assert b'Add New Expense' in response.data

def test_add_expense_creates_expense(client):
    """Test that submitting the add expense form creates a new expense."""
    # Log in the user
    with client.session_transaction() as session:
        session['user_id'] = 1  # Set user_id directly in the session
    
    # Submit the add expense form
    response = client.post('/group/1/add_expense', data={
        'description': 'Test Expense',
        'amount': '100.00'
    }, follow_redirects=True)
    
    assert response.status_code == 200
    assert b'Expense added successfully' in response.data
    
    # Check that the expense was created in the database
    with client.application.app_context():
        expense = Expense.query.filter_by(description='Test Expense').first()
        assert expense is not None
        assert expense.amount == 100.00
        assert expense.payer_id == 1
        assert expense.group_id == 1
        
        # Check that expense shares were created
        shares = ExpenseShare.query.filter_by(expense_id=expense.id).all()
        assert len(shares) == 2  # One share for each group member
        assert sum(share.amount for share in shares) == pytest.approx(100.00)  # Total should equal expense amount