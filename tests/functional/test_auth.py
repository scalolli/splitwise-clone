import pytest

def test_login_page_loads(client):
    """Test that the login page loads correctly"""
    response = client.get('/login')
    assert response.status_code == 200
    assert b'Login' in response.data

def test_login_with_valid_user(client):
    """Test login with a valid username"""
    response = client.post('/login', data={
        'username': 'user1',
        'password': 'password'
    }, follow_redirects=True)
    assert response.status_code == 200
    # Check that we're redirected to the index page
    assert b'Groups' in response.data or b'Home' in response.data

def test_login_with_invalid_user(client):
    """Test login with an invalid username"""
    response = client.post('/login', data={
        'username': 'nonexistentuser',
        'password': 'password'
    })
    assert response.status_code == 200
    assert b'Invalid username or password' in response.data

def test_register_page_loads(client):
    """Test that the registration page loads correctly"""
    response = client.get('/register')
    assert response.status_code == 200
    assert b'Create Account' in response.data

def test_register_new_user(client, app):
    """Test registering a new user"""
    response = client.post('/register', data={
        'username': 'newuser',
        'email': 'newuser@example.com',
        'password': 'password123',
        'confirm_password': 'password123'
    }, follow_redirects=True)
    
    assert response.status_code == 200
    assert b'Your account has been created' in response.data
    
    # Verify the user was created in the database
    with app.app_context():
        from app.models.user import User
        user = User.query.filter_by(username='newuser').first()
        assert user is not None
        assert user.email == 'newuser@example.com'

def test_register_with_existing_username(client):
    """Test registering with an existing username"""
    response = client.post('/register', data={
        'username': 'user1',  # This username already exists
        'email': 'different@example.com',
        'password': 'password123',
        'confirm_password': 'password123'
    })
    
    assert response.status_code == 200
    assert b'Username already exists' in response.data

def test_logout(client):
    """Test logging out"""
    # First login
    client.post('/login', data={
        'username': 'user1',
        'password': 'password'
    })
    
    # Then logout
    response = client.get('/logout', follow_redirects=True)
    assert response.status_code == 200
    
    # Check that we're redirected to the index page
    assert b'You have been logged out' in response.data