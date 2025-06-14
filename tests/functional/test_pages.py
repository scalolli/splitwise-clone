import pytest

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

def test_group_detail_page_loads(client, populated_test_db):
    """Test that the group detail page loads correctly"""
    # Log in the user
    # First login as user1 (who created the apartment group)
    client.post('/login', data={
        'username': 'user1',
        'password': 'password'
    })

    group_id = populated_test_db['groups']['apartment'].id
    response = client.get(f'/group/{group_id}')
    assert response.status_code == 200
    assert b'Apartment' in response.data
    assert b'user1' in response.data
    assert b'user2' in response.data
    assert b'Groceries' in response.data
    assert b'Rent' in response.data
    
    # user3 should not be in this group
    assert b'user3' not in response.data

def test_group_detail_shows_expenses(client, populated_test_db):
    """Test that the group detail page shows expenses correctly"""
    # Log in the user
    # First login as user1 (who created the apartment group)
    client.post('/login', data={
        'username': 'user1',
        'password': 'password'
    })

    group_id = populated_test_db['groups']['apartment'].id
    response = client.get(f'/group/{group_id}')
    assert response.status_code == 200
    assert b'Groceries' in response.data
    assert b'100.0' in response.data
    assert b'Rent' in response.data
    assert b'1000.0' in response.data

def test_nonexistent_group(client, populated_test_db):
    """Test accessing a group that doesn't exist"""
    # Log in the user
    # First login as user1 (who created the apartment group)
    client.post('/login', data={
        'username': 'user1',
        'password': 'password'
    })
    response = client.get('/group/999')
    assert response.status_code == 404