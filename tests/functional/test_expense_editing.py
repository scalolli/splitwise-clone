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
    updated_expense = Expense.query.get(sample_expense.id)
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
