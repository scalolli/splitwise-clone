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
    WTF_CSRF_ENABLED = False

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
        user1 = User(username='user1', email='user1@example.com')
        user1.set_password('password')
        
        user2 = User(username='user2', email='user2@example.com')
        user2.set_password('password')
        
        user3 = User(username='user3', email='user3@example.com')
        user3.set_password('password')
        
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

@pytest.fixture
def db_session(app):
    """Create a test database session."""    
    # Create the database and tables
    with app.app_context():
        db.create_all()
        
        yield db.session
        
        # Clean up
        db.session.remove()
        db.drop_all()        