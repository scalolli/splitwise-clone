from app import db
import pytest
from flask import session

def test_group_creation_page_requires_login(client):
    """Test that the group creation page requires login"""
    # Try to access the group creation page without logging in
    response = client.get('/group/create', follow_redirects=True)
    
    # Should be redirected to login page
    assert response.status_code == 200
    assert b'Login' in response.data
    assert b'Please log in to access this page' in response.data

def test_group_creation_page_loads_when_logged_in(client):
    """Test that the group creation page loads when logged in"""
    # First login
    client.post('/login', data={
        'username': 'user1',
        'password': 'password'
    })
    
    # Then try to access the group creation page
    response = client.get('/group/create')
    
    assert response.status_code == 200
    assert b'Create New Group' in response.data
    assert b'Group Name' in response.data
    assert b'Description' in response.data

def test_create_new_group(client, app):
    """Test creating a new group"""
    # First login
    client.post('/login', data={
        'username': 'user1',
        'password': 'password'
    })
    
    # Then create a new group
    response = client.post('/group/create', data={
        'name': 'Test Group',
        'description': 'This is a test group'
    }, follow_redirects=True)
    
    assert response.status_code == 200
    assert b'Group created successfully' in response.data
    assert b'Test Group' in response.data
    
    # Verify the group was created in the database
    with app.app_context():
        from app.models.group import Group
        group = Group.query.filter_by(name='Test Group').first()
        assert group is not None
        assert group.description == 'This is a test group'
        
        # Verify the current user is the creator and a member
        from app.models.user import User
        user = User.query.filter_by(username='user1').first()
        assert group.created_by_id == user.id
        assert user in group.members

def test_create_group_with_invalid_data(client):
    """Test creating a group with invalid data"""
    # First login
    client.post('/login', data={
        'username': 'user1',
        'password': 'password'
    })
    
    # Try to create a group without a name
    response = client.post('/group/create', data={
        'name': '',
        'description': 'This is a test group'
    })
    
    assert response.status_code == 200
    assert b'Group name is required' in response.data

def test_add_member_to_group(client, app, populated_test_db):
    """Test adding a member to a group"""
    # First login as user1 (who created the apartment group)
    client.post('/login', data={
        'username': 'user1',
        'password': 'password'
    })
    
    # Get the group ID
    group_id = populated_test_db['groups']['apartment'].id
    
    # Add user3 to the group
    response = client.post(f'/group/{group_id}/add_member', data={
        'username': 'user3'
    }, follow_redirects=True)
    
    assert response.status_code == 200
    assert b'Member added successfully' in response.data
    assert b'user3' in response.data
    
    # Verify user3 was added to the group in the database
    with app.app_context():
        from app.models.group import Group
        from app.models.user import User
        
        group = db.session.get(Group, group_id)
        user3 = User.query.filter_by(username='user3').first()
        
        assert user3 in group.members

def test_add_nonexistent_member_to_group(client, populated_test_db):
    """Test adding a nonexistent user to a group"""
    # First login
    client.post('/login', data={
        'username': 'user1',
        'password': 'password'
    })
    
    # Get the group ID
    group_id = populated_test_db['groups']['apartment'].id
    
    # Try to add a nonexistent user to the group
    response = client.post(f'/group/{group_id}/add_member', data={
        'username': 'nonexistentuser'
    }, follow_redirects=True)
    
    assert response.status_code == 200
    assert b'User not found' in response.data

def test_remove_member_from_group(client, app, populated_test_db):
    """Test removing a member from a group"""
    # First login as user1 (who created the apartment group)
    client.post('/login', data={
        'username': 'user1',
        'password': 'password'
    })
    
    # Get the group ID and user2's ID
    group_id = populated_test_db['groups']['apartment'].id
    user2_id = populated_test_db['users']['user2'].id
    
    # Remove user2 from the group
    response = client.post(f'/group/{group_id}/remove_member/{user2_id}', 
                          follow_redirects=True)
    
    assert response.status_code == 200
    assert b'Member removed successfully' in response.data
    
    # Verify user2 was removed from the group in the database
    with app.app_context():
        from app.models.group import Group
        from app.models.user import User
        
        group = db.session.get(Group, group_id)
        user2 = User.query.filter_by(username='user2').first()
        
        assert user2 not in group.members        
        assert user2 not in group.members

def test_group_edit_by_creator(client, app):
    """Test that the group creator can edit group details"""
    # Login as group creator
    client.post('/login', data={'username': 'user1', 'password': 'password'})
    # Create a group
    client.post('/group/create', data={'name': 'Edit Group', 'description': 'Original desc'})
    # Get group id
    with app.app_context():
        from app.models.group import Group
        group = Group.query.filter_by(name='Edit Group').first()
        group_id = group.id
    # Access edit page
    response = client.get(f'/group/{group_id}/edit')
    assert response.status_code == 200
    assert b'Edit Group' in response.data
    # Submit edit form
    response = client.post(f'/group/{group_id}/edit', data={
        'name': 'Edited Group',
        'description': 'Updated desc'
    }, follow_redirects=True)
    assert b'Group updated successfully' in response.data
    assert b'Edited Group' in response.data
    # Check DB
    with app.app_context():
        group = db.session.get(Group, group_id)
        assert group.name == 'Edited Group'
        assert group.description == 'Updated desc'

def test_group_edit_denied_for_non_creator(client, app):
    """Test that non-creators cannot access the edit group page"""
    # Login as creator and create group
    client.post('/login', data={'username': 'user1', 'password': 'password'})
    client.post('/group/create', data={'name': 'NoEdit Group', 'description': 'desc'})
    with app.app_context():
        from app.models.group import Group
        group = Group.query.filter_by(name='NoEdit Group').first()
        group_id = group.id
    # Logout and login as another user
    client.get('/logout')
    client.post('/login', data={'username': 'user2', 'password': 'password'})
    # Try to access edit page
    response = client.get(f'/group/{group_id}/edit', follow_redirects=True)
    assert b'You do not have permission to edit this group' in response.data
    assert response.status_code == 200

def test_edit_group_add_member(client, app):
    """Test that the group creator can add a member via the edit group page"""
    # Login as group creator and create a group
    client.post('/login', data={'username': 'user1', 'password': 'password'})
    client.post('/group/create', data={'name': 'EditMembers Group', 'description': 'desc'})
    with app.app_context():
        from app.models.group import Group
        group = Group.query.filter_by(name='EditMembers Group').first()
        group_id = group.id
    # Add user3 via edit group page
    response = client.post(f'/group/{group_id}/edit', data={
        'name': 'EditMembers Group',
        'description': 'desc',
        'add_member_username': 'user3'
    }, follow_redirects=True)
    assert b'Member user3 added successfully' in response.data
    # Check DB
    with app.app_context():
        group = db.session.get(Group, group_id)
        from app.models.user import User
        user3 = User.query.filter_by(username='user3').first()
        assert user3 in group.members
    # Try adding a non-existent user
    response = client.post(f'/group/{group_id}/edit', data={
        'name': 'EditMembers Group',
        'description': 'desc',
        'add_member_username': 'nouser'
    }, follow_redirects=True)
    assert b'User not found' in response.data
    # Try adding an existing member
    response = client.post(f'/group/{group_id}/edit', data={
        'name': 'EditMembers Group',
        'description': 'desc',
        'add_member_username': 'user3'
    }, follow_redirects=True)
    assert b'User is already a member' in response.data

def test_edit_group_add_member_denied_for_non_creator(client, app):
    """Test that non-creators cannot add members via the edit group page"""
    # Login as creator and create group
    client.post('/login', data={'username': 'user1', 'password': 'password'})
    client.post('/group/create', data={'name': 'NoAdd Group', 'description': 'desc'})
    with app.app_context():
        from app.models.group import Group
        group = Group.query.filter_by(name='NoAdd Group').first()
        group_id = group.id
    client.get('/logout')
    # Login as non-creator
    client.post('/login', data={'username': 'user2', 'password': 'password'})
    # Try to add a member
    response = client.post(f'/group/{group_id}/edit', data={
        'name': 'NoAdd Group',
        'description': 'desc',
        'add_member_username': 'user3'
    }, follow_redirects=True)
    assert b'You do not have permission to edit this group' in response.data
