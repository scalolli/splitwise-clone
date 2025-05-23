import pytest
import datetime
from app.utils.datetime import utcnow
from app import db
from app.models.user import User
from app.models.group import Group
from app.models.expense import Expense
from app.models.expense_share import ExpenseShare
from app.services.balance_service import calculate_balances

def test_calculate_balances_equal_split(app):
    """Test balance calculation with equally split expenses."""
    with app.app_context():
        # Create all tables
        db.create_all()
        
        # Create test users
        user1 = User(username="user1", email="user1@example.com", password="password")
        user2 = User(username="user2", email="user2@example.com", password="password")
        db.session.add_all([user1, user2])
        db.session.flush()
        
        # Create test group
        group = Group(name="Test Group", description="Test group for balance calculation", 
                      created_by_id=user1.id)
        group.members.append(user1)
        group.members.append(user2)
        db.session.add(group)
        db.session.flush()
        
        # Create test expense (user1 pays $100, split equally)
        expense = Expense(
            description="Test Expense",
            amount=100.0,
            date=utcnow(),
            payer_id=user1.id,
            group_id=group.id
        )
        db.session.add(expense)
        db.session.flush()
        
        # Create expense shares (50/50 split)
        share1 = ExpenseShare(expense_id=expense.id, user_id=user1.id, amount=50.0)
        share2 = ExpenseShare(expense_id=expense.id, user_id=user2.id, amount=50.0)
        db.session.add_all([share1, share2])
        db.session.commit()
        
        # Calculate balances
        balances = calculate_balances(group.id)
        
        # Verify balances
        assert len(balances) == 1  # Only one balance entry for two users
        
        # user2 owes user1 $50
        balance = balances[0]
        assert balance['from_user_id'] == user2.id
        assert balance['to_user_id'] == user1.id
        assert balance['amount'] == 50.0
        
        # Clean up
        db.session.remove()
        db.drop_all()

def test_calculate_balances_custom_split(empty_db_session):
    """Test balance calculation with custom split expenses."""
    # Create test users
    user1 = User(username="user1", email="user1@example.com", password="password")
    user2 = User(username="user2", email="user2@example.com", password="password")
    empty_db_session.add_all([user1, user2])
    empty_db_session.flush()
    
    # Create test group
    group = Group(name="Test Group", description="Test group for balance calculation", 
                  created_by_id=user1.id)
    group.members.append(user1)
    group.members.append(user2)
    empty_db_session.add(group)
    empty_db_session.flush()
    
    # Create test expense (user1 pays $100, custom split 30/70)
    expense = Expense(
        description="Test Expense",
        amount=100.0,
        date=utcnow(),
        payer_id=user1.id,
        group_id=group.id
    )
    empty_db_session.add(expense)
    empty_db_session.flush()
    
    # Create expense shares (30/70 split)
    share1 = ExpenseShare(expense_id=expense.id, user_id=user1.id, amount=30.0)
    share2 = ExpenseShare(expense_id=expense.id, user_id=user2.id, amount=70.0)
    empty_db_session.add_all([share1, share2])
    empty_db_session.commit()
    
    # Calculate balances
    balances = calculate_balances(group.id)
    
    # Verify balances
    assert len(balances) == 1  # Only one balance entry for two users
    
    # user2 owes user1 $70
    balance = balances[0]
    assert balance['from_user_id'] == user2.id
    assert balance['to_user_id'] == user1.id
    assert balance['amount'] == 70.0

def test_calculate_balances_multiple_expenses(empty_db_session):
    """Test balance calculation with multiple expenses."""
    # Create test users
    user1 = User(username="user1", email="user1@example.com", password="password")
    user2 = User(username="user2", email="user2@example.com", password="password")
    empty_db_session.add_all([user1, user2])
    empty_db_session.flush()
    
    # Create test group
    group = Group(name="Test Group", description="Test group for balance calculation", 
                  created_by_id=user1.id)
    group.members.append(user1)
    group.members.append(user2)
    empty_db_session.add(group)
    empty_db_session.flush()
    
    # Create first expense (user1 pays $100, split 50/50)
    expense1 = Expense(
        description="Expense 1",
        amount=100.0,
        date=utcnow(),
        payer_id=user1.id,
        group_id=group.id
    )
    empty_db_session.add(expense1)
    empty_db_session.flush()
    
    # Create expense shares for first expense
    share1_1 = ExpenseShare(expense_id=expense1.id, user_id=user1.id, amount=50.0)
    share1_2 = ExpenseShare(expense_id=expense1.id, user_id=user2.id, amount=50.0)
    empty_db_session.add_all([share1_1, share1_2])
    
    # Create second expense (user2 pays $60, split 50/50)
    expense2 = Expense(
        description="Expense 2",
        amount=60.0,
        date=utcnow(),
        payer_id=user2.id,
        group_id=group.id
    )
    empty_db_session.add(expense2)
    empty_db_session.flush()
    
    # Create expense shares for second expense
    share2_1 = ExpenseShare(expense_id=expense2.id, user_id=user1.id, amount=30.0)
    share2_2 = ExpenseShare(expense_id=expense2.id, user_id=user2.id, amount=30.0)
    empty_db_session.add_all([share2_1, share2_2])
    
    empty_db_session.commit()
    
    # Calculate balances
    balances = calculate_balances(group.id)
    
    # Verify balances
    assert len(balances) == 1  # Only one balance entry for two users
    
    # user2 owes user1 $20 ($50 from first expense minus $30 from second expense)
    balance = balances[0]
    assert balance['from_user_id'] == user2.id
    assert balance['to_user_id'] == user1.id
    assert balance['amount'] == 20.0