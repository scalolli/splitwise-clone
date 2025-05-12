import pytest
from app import create_app, db
from app.models.user import User
from app.models.group import Group

@pytest.fixture
def client():
    app = create_app()
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

def test_add_expense_button_on_group_page(client):
    """Test that the Add Expense button appears on the group page for logged-in users."""
    # Log in the user
    with client.session_transaction() as session:
        session['user_id'] = 1
    
    # Access the group page
    response = client.get('/group/1')
    
    # Check that the Add Expense button is present
    assert b'Add Expense' in response.data

def test_add_expense_page_loads(client):
    """Test that the add expense page loads correctly."""
    # Log in the user
    with client.session_transaction() as session:
        session['user_id'] = 1
    
    # Access the add expense page
    response = client.get('/group/1/add_expense')
    
    # Check that the page loads correctly
    assert response.status_code == 200
    assert b'Add New Expense' in response.data
    assert b'Description:' in response.data
    assert b'Amount:' in response.data

from app.models.expense import Expense

def test_submit_add_expense_form(client):
    """Test that submitting the add expense form creates a new expense."""
    # Log in the user
    with client.session_transaction() as session:
        session['user_id'] = 1
    
    # Submit the add expense form
    response = client.post('/group/1/add_expense', data={
        'description': 'Test Expense',
        'amount': '50.00'
    }, follow_redirects=True)
    
    # Check that the form submission was successful
    assert response.status_code == 200
    assert b'Expense added successfully' in response.data
    
    # Check that the expense was created in the database
    with client.application.app_context():
        expense = Expense.query.filter_by(description='Test Expense').first()
        assert expense is not None
        assert expense.amount == 50.00
        assert expense.payer_id == 1
        assert expense.group_id == 1

from app.models.expense_share import ExpenseShare

def test_expense_shares_creation(client):
    """Test that expense shares are created when an expense is added."""
    # Log in the user
    with client.session_transaction() as session:
        session['user_id'] = 1
    
    # Submit the add expense form
    client.post('/group/1/add_expense', data={
        'description': 'Shared Expense',
        'amount': '100.00'
    })
    
    # Check that expense shares were created
    with client.application.app_context():
        expense = Expense.query.filter_by(description='Shared Expense').first()
        shares = ExpenseShare.query.filter_by(expense_id=expense.id).all()
        
        # There should be one share for each group member (2 in this case)
        assert len(shares) == 2
        
        # The total of all shares should equal the expense amount
        total_shares = sum(share.amount for share in shares)
        assert total_shares == 100.00
        
        # Each share should be equal (for equal splitting)
        assert shares[0].amount == 50.00
        assert shares[1].amount == 50.00

def test_add_expense_with_custom_split(client):
    """Test adding an expense with custom split amounts."""
    # Log in the user
    with client.session_transaction() as session:
        session['user_id'] = 1
    
    # Submit the add expense form with custom split
    response = client.post('/group/1/add_expense', data={
        'description': 'Custom Split Expense',
        'amount': '100.00',
        'split_type': 'custom',
        'split_amounts[1]': '70.00',  # User 1 pays 70%
        'split_amounts[2]': '30.00'   # User 2 pays 30%
    }, follow_redirects=True)
    
    # Check that the form submission was successful
    assert response.status_code == 200
    assert b'Expense added successfully' in response.data
    
    # Check that the expense was created with correct shares
    with client.application.app_context():
        expense = Expense.query.filter_by(description='Custom Split Expense').first()
        assert expense is not None
        assert expense.amount == 100.00
        
        shares = ExpenseShare.query.filter_by(expense_id=expense.id).all()
        assert len(shares) == 2
        
        # Find shares by user_id
        user1_share = next((s for s in shares if s.user_id == 1), None)
        user2_share = next((s for s in shares if s.user_id == 2), None)
        
        assert user1_share is not None
        assert user2_share is not None
        assert user1_share.amount == 70.00
        assert user2_share.amount == 30.00