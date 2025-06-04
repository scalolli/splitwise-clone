from app import db
import pytest
from flask import url_for
from app.models.expense import Expense
from app.models.expense_share import ExpenseShare

@pytest.fixture
def sample_expense(client, empty_db_session):
    # Create a sample expense for testing
    expense = Expense(description="Test Expense", amount=100, payer_id=1, group_id=1)
    empty_db_session.add(expense)
    empty_db_session.commit()
    return expense

def test_edit_expense_get(client, sample_expense):
    # Test GET request to edit expense page
    response = client.get(url_for('expenses.edit_expense', expense_id=sample_expense.id))
    assert response.status_code == 200
    assert b"Edit Expense" in response.data

def test_edit_expense_post(client, sample_expense, empty_db_session):
    # Test POST request to update expense
    data = {
        'description': "Updated Expense",
        'amount': 150,
        'payer_id': 1,
        'splits-0-user_id': 1,
        'splits-0-amount': 150
    }
    response = client.post(url_for('expenses.edit_expense', expense_id=sample_expense.id), data=data, follow_redirects=True)
    assert response.status_code == 200
    updated_expense = db.session.get(Expense, sample_expense.id)
    assert updated_expense.description == "Updated Expense"
    assert updated_expense.amount == 150
    assert len(updated_expense.shares) == 1
    assert updated_expense.shares[0].amount == 150

def test_render_shared_expenses(client, sample_expense, empty_db_session):
    """Test if shared expenses are rendered properly on the edit expense page."""
    # Add shared expenses for the sample expense
    share1 = ExpenseShare(expense_id=sample_expense.id, user_id=1, amount=50)
    share2 = ExpenseShare(expense_id=sample_expense.id, user_id=2, amount=50)
    empty_db_session.add_all([share1, share2])
    empty_db_session.commit()

    # Navigate to the edit expense page
    response = client.get(url_for('expenses.edit_expense', expense_id=sample_expense.id))
    assert response.status_code == 200

    # print("printing the response******** {}".format(response.data))  # Debugging line to check the response data

    # Check if the shared expenses are rendered properly
    assert b"user_id" in response.data  # Check if user field is rendered
    assert b"50" in response.data       # Check if the amount is rendered
    assert b"user1" in response.data    # Check if 'user1' is present in the dropdown options
    assert b"user2" in response.data    # Check if 'user2' is present in the dropdown options
    assert b"<option value=\"1\">user1</option>" in response.data  # Check if 'user1' is present in the dropdown options

    # Assert that the sample expense exists in the database
    assert sample_expense is not None
    assert sample_expense.description == "Test Expense"

def test_edit_expense_form_errors_displayed(client, sample_expense):
    """Test that form validation errors ARE displayed on the edit expense page when invalid data is submitted."""
    # Submit invalid data: empty description and negative amount
    data = {
        'description': '',  # Required field left blank
        'amount': -10,      # Invalid negative amount
        'payer_id': 1,
        'splits-0-user_id': 1,
        'splits-0-amount': -10
    }
    response = client.post(
        url_for('expenses.edit_expense', expense_id=sample_expense.id),
        data=data,
        follow_redirects=True
    )
    assert response.status_code == 200
    # Check that common WTForms error messages ARE present
    assert (
        b'This field is required' in response.data or
        b'Not a valid' in response.data or
        b'Invalid' in response.data or
        b'Amount must be greater than' in response.data
    ), "Expected form error messages to be displayed, but none were found."

def test_edit_expense_add_split(client, sample_expense, empty_db_session):
    """Test adding a split when editing an expense (simulate real user flow)."""
    # Add initial split
    share1 = ExpenseShare(expense_id=sample_expense.id, user_id=1, amount=50)
    empty_db_session.add(share1)
    empty_db_session.commit()
    # Step 1: Add a new split via POST (simulate clicking 'Add Split')
    data_add = {
        'description': "Test Expense",
        'amount': 100,
        'payer_id': 1,
        'splits-0-user_id': 1,
        'splits-0-amount': 50,
        'add_split': '1'
    }
    response = client.post(url_for('expenses.edit_expense', expense_id=sample_expense.id), data=data_add)
    assert response.status_code == 200
    # Step 2: Fill out both splits and save (simulate clicking 'Save Changes')
    data_save = {
        'description': "Test Expense",
        'amount': 100,
        'payer_id': 1,
        'splits-0-user_id': 1,
        'splits-0-amount': 50,
        'splits-1-user_id': 2,
        'splits-1-amount': 50
    }
    response = client.post(url_for('expenses.edit_expense', expense_id=sample_expense.id), data=data_save, follow_redirects=True)
    assert response.status_code == 200
    updated_expense = db.session.get(Expense, sample_expense.id)
    assert len(updated_expense.shares) == 2
    amounts = sorted([s.amount for s in updated_expense.shares])
    assert amounts == [50, 50]

def test_edit_expense_remove_split(client, sample_expense, empty_db_session):
    """Test removing a split when editing an expense."""
    # Add two splits
    share1 = ExpenseShare(expense_id=sample_expense.id, user_id=1, amount=50)
    share2 = ExpenseShare(expense_id=sample_expense.id, user_id=2, amount=50)
    empty_db_session.add_all([share1, share2])
    empty_db_session.commit()
    # Remove the second split via POST
    data = {
        'description': "Test Expense",
        'amount': 50,
        'payer_id': 1,
        'splits-0-user_id': 1,
        'splits-0-amount': 50,
        'remove_split': '1'  # Simulate clicking 'Remove' on the second split
    }
    url = url_for('expenses.edit_expense', expense_id=sample_expense.id)
    print("url is ********** {}".format(url))  # Debugging line to check the URL
    response = client.post(url, data=data, follow_redirects=True)
    assert response.status_code == 200
    updated_expense = db.session.get(Expense, sample_expense.id)
    assert len(updated_expense.shares) == 1
    assert updated_expense.shares[0].user_id == 1
    assert updated_expense.shares[0].amount == 50

def test_edit_expense_add_split_and_save(client, sample_expense, empty_db_session):
    """Simulate adding a split client-side and saving; ensure both splits are persisted."""
    # Add initial split
    share1 = ExpenseShare(expense_id=sample_expense.id, user_id=1, amount=50)
    empty_db_session.add(share1)
    empty_db_session.commit()
    # Simulate user POSTing with two splits (as if added client-side)
    data = {
        'description': "Test Expense",
        'amount': 100,
        'payer_id': 1,
        'splits-0-user_id': 1,
        'splits-0-amount': 50,
        'splits-1-user_id': 2,
        'splits-1-amount': 50
    }
    response = client.post(url_for('expenses.edit_expense', expense_id=sample_expense.id), data=data, follow_redirects=True)
    assert response.status_code == 200
    updated_expense = db.session.get(Expense, sample_expense.id)
    assert len(updated_expense.shares) == 2
    user_ids = sorted([s.user_id for s in updated_expense.shares])
    assert user_ids == [1, 2]
    amounts = sorted([s.amount for s in updated_expense.shares])
    assert amounts == [50, 50]
