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

def test_edit_group_button_visible_to_creator(client, populated_test_db):
    """Test that the Edit Group button is visible to the group creator on the group detail page."""
    group_id = populated_test_db['groups']['apartment'].id
    # Log in as the group creator
    client.post('/login', data={
        'username': 'user1',
        'password': 'password'
    })
    response = client.get(f'/group/{group_id}')
    assert response.status_code == 200
    # The edit group button should be present
    assert f'/group/{group_id}/edit'.encode() in response.data
    assert b'Edit Group' in response.data
    client.get('/logout')

def test_edit_group_button_not_visible_to_non_creator(client, populated_test_db):
    """Test that the Edit Group button is NOT visible to non-creators on the group detail page."""
    group_id = populated_test_db['groups']['apartment'].id
    # Log in as a non-creator
    client.post('/login', data={
        'username': 'user2',
        'password': 'password'
    })
    response = client.get(f'/group/{group_id}')
    assert response.status_code == 200
    # The edit group button should NOT be present
    assert f'/group/{group_id}/edit'.encode() not in response.data
    assert b'Edit Group' not in response.data
