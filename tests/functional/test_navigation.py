import pytest
from flask import url_for

def test_edit_expense_navigation(client, populated_test_db):
    """Test navigation to edit expense page from group page."""
    # Log in as the user
    client.post('/login', data={
        'username': 'user1',
        'password': 'password'
    })

    # Navigate to the group page
    response = client.get(url_for('groups.group', group_id=populated_test_db['groups']['apartment'].id))
    assert response.status_code == 200

    # Check if the edit link is present
    edit_url = url_for('expenses.edit_expense', expense_id=populated_test_db['expenses']['groceries'].id)
    assert edit_url.encode() in response.data

    # Navigate to the edit expense page
    response = client.get(edit_url)
    assert response.status_code == 200
    assert b"Edit Expense" in response.data
