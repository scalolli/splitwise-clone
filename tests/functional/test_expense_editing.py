import pytest
from flask import url_for
from app.models.expense import Expense
from app.models.expense_share import ExpenseShare

@pytest.fixture
def sample_expense(client, test_db):
    # Create a sample expense for testing
    expense = Expense(description="Test Expense", amount=100, payer_id=1, group_id=1)
    test_db.session.add(expense)
    test_db.session.commit()
    return expense

def test_edit_expense_get(client, sample_expense):
    # Test GET request to edit expense page
    response = client.get(url_for('expenses.edit_expense', expense_id=sample_expense.id))
    assert response.status_code == 200
    assert b"Edit Expense" in response.data

def test_edit_expense_post(client, sample_expense, test_db):
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
