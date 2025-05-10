import pytest
from app import app, db, User

@pytest.fixture
def client():
    # Configure app for testing
    app.config['TESTING'] = True
    app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///:memory:'
    
    # Create test client
    with app.test_client() as client:
        # Create application context
        with app.app_context():
            # Create all tables in the test database
            db.create_all()
            
            # Create a test user with a password
            test_user = User(username='testuser', email='test@example.com', password='password')
            db.session.add(test_user)
            db.session.commit()
            
            yield client
            
            # Clean up
            db.session.remove()
            db.drop_all()

def test_login_page_loads(client):
    """Test that the login page loads correctly"""
    response = client.get('/login')
    assert response.status_code == 200
    assert b'Login' in response.data

def test_login_with_valid_user(client):
    """Test login with a valid username"""
    response = client.post('/login', data={
        'username': 'testuser',
        'password': 'password'  # Password isn't checked yet in our implementation
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