import pytest
from app import create_app, db
from app.models.user import User
from app.models.group import Group
from app.models.expense import Expense
from app.models.expense_share import ExpenseShare
from app.models.settlement import Settlement
from config import Config
from datetime import datetime

class TestConfig(Config):
    TESTING = True
    SQLALCHEMY_DATABASE_URI = 'sqlite:///:memory:'

@pytest.fixture
def app():
    """Create and configure a Flask app for testing."""
    app = create_app(TestConfig)
    return app

@pytest.fixture
def test_data(app):
    """Create test data in the database."""
    with app.app_context():
        # Create all tables in the test database
        db.create_all()
        
        # Create test data
        # Users
        user1 = User(username='user1', email='user1@example.com', password='password')
        user2 = User(username='user2', email='user2@example.com', password='password')
        user3 = User(username='user3', email='user3@example.com', password='password')
        
        # Add users to session and flush to get IDs
        db.session.add_all([user1, user2, user3])
        db.session.flush()
        
        # Groups
        group1 = Group(name='Apartment', description='Apartment expenses', created_at=datetime.utcnow())
        group1.created_by_id = user1.id
        group1.members.append(user1)
        group1.members.append(user2)
        
        group2 = Group(name='Trip', description='Vacation expenses', created_at=datetime.utcnow())
        group2.created_by_id = user1.id
        group2.members.append(user1)
        group2.members.append(user2)
        group2.members.append(user3)
        
        # Add groups to session and flush to get IDs
        db.session.add_all([group1, group2])
        db.session.flush()
        
        # Expenses
        expense1 = Expense(
            description='Groceries', 
            amount=100.0, 
            date=datetime.utcnow(),
            payer_id=user1.id,
            group_id=group1.id
        )
        
        expense2 = Expense(
            description='Rent', 
            amount=1000.0, 
            date=datetime.utcnow(),
            payer_id=user2.id,
            group_id=group1.id
        )
        
        expense3 = Expense(
            description='Hotel', 
            amount=300.0, 
            date=datetime.utcnow(),
            payer_id=user1.id,
            group_id=group2.id
        )
        
        # Add expenses to session and flush to get IDs
        db.session.add_all([expense1, expense2, expense3])
        db.session.flush()
        
        # Expense Shares
        # Groceries split equally between user1 and user2
        share1 = ExpenseShare(expense_id=expense1.id, user_id=user1.id, amount=50.0)
        share2 = ExpenseShare(expense_id=expense1.id, user_id=user2.id, amount=50.0)
        
        # Rent split equally between user1 and user2
        share3 = ExpenseShare(expense_id=expense2.id, user_id=user1.id, amount=500.0)
        share4 = ExpenseShare(expense_id=expense2.id, user_id=user2.id, amount=500.0)
        
        # Hotel split equally among user1, user2, and user3
        share5 = ExpenseShare(expense_id=expense3.id, user_id=user1.id, amount=100.0)
        share6 = ExpenseShare(expense_id=expense3.id, user_id=user2.id, amount=100.0)
        share7 = ExpenseShare(expense_id=expense3.id, user_id=user3.id, amount=100.0)
        
        # Add all expense shares to session
        db.session.add_all([
            share1, share2, share3, share4, share5, share6, share7
        ])
        
        # Commit all changes
        db.session.commit()
        
        # Store references to test data for tests to use
        test_data = {
            'users': {
                'user1': user1,
                'user2': user2,
                'user3': user3
            },
            'groups': {
                'apartment': group1,
                'trip': group2
            },
            'expenses': {
                'groceries': expense1,
                'rent': expense2,
                'hotel': expense3
            }
        }
        
        yield test_data
        
        # Clean up
        db.session.remove()
        db.drop_all()

@pytest.fixture
def client(app, test_data):
    """Create a test client for the app."""
    with app.test_client() as client:
        yield client

# Login tests
def test_login_page_loads(client):
    """Test that the login page loads correctly"""
    response = client.get('/login')
    assert response.status_code == 200
    assert b'Login' in response.data

def test_login_with_valid_user(client):
    """Test login with a valid username"""
    response = client.post('/login', data={
        'username': 'user1',
        'password': 'password'  # Password isn't checked yet in our implementation
    }, follow_redirects=True)
    assert response.status_code == 200
    # Check that we're redirected to the index page
    assert b'Groups' in response.data or b'Home' in response.data

def test_login_with_invalid_user(client):
    """Test login with an invalid username"""
    response = client.post('/login', data={
        'username': 'nonexistentuser',
        'password': 'password'
    })
    assert response.status_code == 200
    assert b'Invalid username or password' in response.data

# Home page tests
def test_index_page_loads(client):
    """Test that the index page loads correctly"""
    response = client.get('/')
    assert response.status_code == 200
    # Check that users and groups are displayed
    assert b'user1' in response.data
    assert b'user2' in response.data
    assert b'user3' in response.data
    assert b'Apartment' in response.data
    assert b'Trip' in response.data

# Group detail tests
def test_group_detail_page_loads(client, test_data):
    """Test that the group detail page loads correctly"""
    group_id = test_data['groups']['apartment'].id
    response = client.get(f'/group/{group_id}')
    assert response.status_code == 200
    assert b'Apartment' in response.data
    assert b'user1' in response.data
    assert b'user2' in response.data
    assert b'Groceries' in response.data
    assert b'Rent' in response.data
    
    # user3 should not be in this group
    assert b'user3' not in response.data

def test_group_detail_shows_expenses(client, test_data):
    """Test that the group detail page shows expenses correctly"""
    group_id = test_data['groups']['apartment'].id
    response = client.get(f'/group/{group_id}')
    assert response.status_code == 200
    assert b'Groceries' in response.data
    assert b'100.0' in response.data
    assert b'Rent' in response.data
    assert b'1000.0' in response.data

def test_nonexistent_group(client):
    """Test accessing a group that doesn't exist"""
    response = client.get('/group/999')
    assert response.status_code == 404

# Model tests
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