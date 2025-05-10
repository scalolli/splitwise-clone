import pytest
from app import app, db, User, Group, Expense, ExpenseShare, Settlement
from datetime import datetime

@pytest.fixture
def client():
    # Configure app for testing
    app.config['TESTING'] = True
    app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///:memory:'
    
    # Create test client
    with app.test_client() as client:
        # Create application context
        with app.app_context():
            # Create all tables in the test database
            db.create_all()
            
            # Create test data
            # Users
            user1 = User(username='user1', email='user1@example.com', password='password')
            user2 = User(username='user2', email='user2@example.com', password='password')
            user3 = User(username='user3', email='user3@example.com', password='password')
            
            # Groups
            group1 = Group(name='Apartment', description='Apartment expenses', created_at=datetime.utcnow())
            group1.created_by_id = 1  # Will be user1's ID
            group1.members.append(user1)
            group1.members.append(user2)
            
            group2 = Group(name='Trip', description='Vacation expenses', created_at=datetime.utcnow())
            group2.created_by_id = 1  # Will be user1's ID
            group2.members.append(user1)
            group2.members.append(user2)
            group2.members.append(user3)
            
            # Expenses
            expense1 = Expense(
                description='Groceries', 
                amount=100.0, 
                date=datetime.utcnow(),
                payer_id=1,  # user1
                group_id=1   # Apartment group
            )
            
            expense2 = Expense(
                description='Rent', 
                amount=1000.0, 
                date=datetime.utcnow(),
                payer_id=2,  # user2
                group_id=1   # Apartment group
            )
            
            expense3 = Expense(
                description='Hotel', 
                amount=300.0, 
                date=datetime.utcnow(),
                payer_id=1,  # user1
                group_id=2   # Trip group
            )
            
            # Expense Shares
            # Groceries split equally between user1 and user2
            share1 = ExpenseShare(expense_id=1, user_id=1, amount=50.0)
            share2 = ExpenseShare(expense_id=1, user_id=2, amount=50.0)
            
            # Rent split equally between user1 and user2
            share3 = ExpenseShare(expense_id=2, user_id=1, amount=500.0)
            share4 = ExpenseShare(expense_id=2, user_id=2, amount=500.0)
            
            # Hotel split equally among user1, user2, and user3
            share5 = ExpenseShare(expense_id=3, user_id=1, amount=100.0)
            share6 = ExpenseShare(expense_id=3, user_id=2, amount=100.0)
            share7 = ExpenseShare(expense_id=3, user_id=3, amount=100.0)
            
            # Add all objects to the session
            db.session.add_all([
                user1, user2, user3, 
                group1, group2, 
                expense1, expense2, expense3,
                share1, share2, share3, share4, share5, share6, share7
            ])
            
            db.session.commit()
            
            yield client
            
            # Clean up
            db.session.remove()
            db.drop_all()

# Login tests (existing)
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
def test_group_detail_page_loads(client):
    """Test that the group detail page loads correctly"""
    response = client.get('/group/1')  # Apartment group
    assert response.status_code == 200
    assert b'Apartment' in response.data
    assert b'user1' in response.data
    assert b'user2' in response.data
    assert b'Groceries' in response.data
    assert b'Rent' in response.data
    
    # user3 should not be in this group
    assert b'user3' not in response.data

def test_group_detail_shows_expenses(client):
    """Test that the group detail page shows expenses correctly"""
    response = client.get('/group/1')  # Apartment group
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
def test_user_model(client):
    """Test User model relationships"""
    with app.app_context():
        user = User.query.get(1)  # user1
        assert user.username == 'user1'
        # Convert to list before using len()
        groups = list(user.groups)
        assert len(groups) == 2
        assert groups[0].name in ['Apartment', 'Trip']
        assert groups[1].name in ['Apartment', 'Trip']
        # Convert to list before using len()
        expenses_paid = list(user.expenses_paid)
        assert len(expenses_paid) > 0

def test_group_model(client):
    """Test Group model relationships"""
    with app.app_context():
        group = Group.query.get(1)  # Apartment group
        assert group.name == 'Apartment'
        # Convert to list before using len()
        members = list(group.members)
        assert len(members) == 2
        assert members[0].username in ['user1', 'user2']
        assert members[1].username in ['user1', 'user2']
        # Convert to list before using len()
        expenses = list(group.expenses)
        assert len(expenses) == 2
        assert expenses[0].description in ['Groceries', 'Rent']
        assert expenses[1].description in ['Groceries', 'Rent']

def test_expense_model(client):
    """Test Expense model relationships"""
    with app.app_context():
        expense = Expense.query.get(1)  # Groceries expense
        assert expense.description == 'Groceries'
        assert expense.amount == 100.0
        assert expense.payer.username == 'user1'
        assert expense.group.name == 'Apartment'
        # Convert to list before using len()
        shares = list(expense.shares)
        assert len(shares) == 2
        assert shares[0].amount + shares[1].amount == expense.amount

def test_expense_share_model(client):
    """Test ExpenseShare model relationships"""
    with app.app_context():
        share = ExpenseShare.query.get(1)
        assert share.amount == 50.0
        assert share.expense.description == 'Groceries'
        assert share.user.username == 'user1'