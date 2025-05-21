import pytest
from app import create_app, db
from app.models.user import User
from app.models.group import Group
from app.models.expense import Expense
from app.models.expense_share import ExpenseShare
from config import Config
import datetime

class TestConfig(Config):
    TESTING = True
    SQLALCHEMY_DATABASE_URI = 'sqlite:///:memory:'
    WTF_CSRF_ENABLED = False
    SERVER_NAME = 'localhost.localdomain'

@pytest.fixture(scope='session')
def app():
    """Create and configure a Flask app for testing."""
    app = create_app(TestConfig)
    return app


# --- Helper functions for test data creation ---
def _create_user(username, email, password):
    user = User(username=username, email=email)
    user.set_password(password)
    return user

def _create_group(name, description, created_by_id, members):
    group = Group(name=name, description=description, created_at=datetime.datetime.now(datetime.timezone.utc))
    group.created_by_id = created_by_id
    for member in members:
        group.members.append(member)
    return group

def _create_expense(description, amount, date, payer_id, group_id):
    return Expense(
        description=description,
        amount=amount,
        date=date,
        payer_id=payer_id,
        group_id=group_id
    )

def _create_share(expense_id, user_id, amount):
    return ExpenseShare(expense_id=expense_id, user_id=user_id, amount=amount)

@pytest.fixture
def test_data(app):
    """Create test data in the database."""
    with app.app_context():
        db.create_all()

        # Users
        user1 = _create_user('user1', 'user1@example.com', 'password')
        user2 = _create_user('user2', 'user2@example.com', 'password')
        user3 = _create_user('user3', 'user3@example.com', 'password')
        db.session.add_all([user1, user2, user3])
        db.session.flush()

        # Groups
        group1 = _create_group('Apartment', 'Apartment expenses', user1.id, [user1, user2])
        group2 = _create_group('Trip', 'Vacation expenses', user1.id, [user1, user2, user3])
        db.session.add_all([group1, group2])
        db.session.flush()

        # Expenses
        now = datetime.datetime.now(datetime.timezone.utc)
        expense1 = _create_expense('Groceries', 100.0, now, user1.id, group1.id)
        expense2 = _create_expense('Rent', 1000.0, now, user2.id, group1.id)
        expense3 = _create_expense('Hotel', 300.0, now, user1.id, group2.id)
        db.session.add_all([expense1, expense2, expense3])
        db.session.flush()

        # Expense Shares
        shares = [
            _create_share(expense1.id, user1.id, 50.0),
            _create_share(expense1.id, user2.id, 50.0),
            _create_share(expense2.id, user1.id, 500.0),
            _create_share(expense2.id, user2.id, 500.0),
            _create_share(expense3.id, user1.id, 100.0),
            _create_share(expense3.id, user2.id, 100.0),
            _create_share(expense3.id, user3.id, 100.0),
        ]
        db.session.add_all(shares)
        db.session.commit()

        test_data = {
            'users': {'user1': user1, 'user2': user2, 'user3': user3},
            'groups': {'apartment': group1, 'trip': group2},
            'expenses': {'groceries': expense1, 'rent': expense2, 'hotel': expense3}
        }
        yield test_data
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

@pytest.fixture(scope='session')
def test_db(app):
    """Provide a database session for tests."""
    with app.app_context():
        db.create_all()
        yield db
        db.session.remove()
        db.drop_all()