import pytest
from app import create_app, db
from app.models.user import User
from app.models.group import Group

@pytest.fixture
def client():
    app = create_app()
    app.config['TESTING'] = True
    app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///:memory:'
    app.config['WTF_CSRF_ENABLED'] = False
    
    with app.test_client() as client:
        with app.app_context():
            db.create_all()
            
            # Create test users
            user1 = User(username='testuser1', email='test1@example.com', password='password')
            user2 = User(username='testuser2', email='test2@example.com', password='password')
            db.session.add_all([user1, user2])
            db.session.commit()
            
            # Create test group
            group = Group(name='Test Group', description='Test Description', created_by_id=user1.id)
            group.members.append(user1)
            group.members.append(user2)
            db.session.add(group)
            db.session.commit()
            
            yield client
            
            db.session.remove()
            db.drop_all()

def test_add_expense_button_on_group_page(client):
    """Test that the Add Expense button appears on the group page for logged-in users."""
    # Log in the user
    with client.session_transaction() as session:
        session['user_id'] = 1
    
    # Access the group page
    response = client.get('/group/1')
    
    # Check that the Add Expense button is present
    assert b'Add Expense' in response.data