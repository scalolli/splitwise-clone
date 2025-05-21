import pytest
from datetime import datetime
from app.models.settlement import Settlement
from app.models.user import User
from app.models.group import Group

def test_create_settlement(app, populated_test_db):
    """Test creating a new settlement between users"""
    from app import db
    
    with app.app_context():
        # Create test users and group
        user1 = User(username='Test User 1', email='test1@example.com')
        user1.set_password('password')
        user2 = User(username='Test User 2', email='test2@example.com')
        user2.set_password('password')
        db.session.add_all([user1, user2])
        db.session.commit()
        
        group = Group(name='Test Group', created_by_id=user1.id)
        db.session.add(group)
        db.session.commit()
        
        # Import the service after models are created
        from app.services.settlement_service import SettlementService
        
        # Act
        settlement = SettlementService.create_settlement(
            from_user_id=user1.id,
            to_user_id=user2.id,
            amount=50.0,
            group_id=group.id
        )
        
        # Assert
        assert settlement is not None
        assert settlement.from_user_id == user1.id
        assert settlement.to_user_id == user2.id
        assert settlement.amount == 50.0
        assert settlement.group_id == group.id
        
        # Verify it was saved to the database
        db_settlement = Settlement.query.get(settlement.id)
        assert db_settlement is not None

def test_get_settlements_for_group(app, populated_test_db):
    """Test retrieving all settlements for a specific group"""
    from app import db
    
    with app.app_context():
        # Create test users and group
        user1 = User(username='Test User 1', email='test1@example.com')
        user1.set_password('password')
        user2 = User(username='Test User 2', email='test2@example.com')
        user2.set_password('password')
        db.session.add_all([user1, user2])
        db.session.commit()
        
        group = Group(name='Test Group', created_by_id=user1.id)
        db.session.add(group)
        db.session.commit()
        
        # Create two settlements in the group
        settlement1 = Settlement(
            from_user_id=user1.id,
            to_user_id=user2.id,
            amount=25.0,
            group_id=group.id,
            created_at=datetime.now()
        )
        settlement2 = Settlement(
            from_user_id=user2.id,
            to_user_id=user1.id,
            amount=15.0,
            group_id=group.id,
            created_at=datetime.now()
        )
        db.session.add_all([settlement1, settlement2])
        db.session.commit()
        
        # Import the service
        from app.services.settlement_service import SettlementService
        
        # Act
        settlements = SettlementService.get_settlements_for_group(group.id)
        
        # Assert
        assert len(settlements) == 2
        assert settlements[0].group_id == group.id
        assert settlements[1].group_id == group.id

def test_get_settlements_for_user(app, populated_test_db):
    """Test retrieving all settlements involving a specific user"""
    from app import db
    
    with app.app_context():
        # Create test users and group
        user1 = User(username='Test User 1', email='test1@example.com')
        user1.set_password('password')
        user2 = User(username='Test User 2', email='test2@example.com')
        user2.set_password('password')
        user3 = User(username='Test User 3', email='test3@example.com')
        user3.set_password('password')
        db.session.add_all([user1, user2, user3])
        db.session.commit()
        
        group = Group(name='Test Group', created_by_id=user1.id)
        db.session.add(group)
        db.session.commit()
        
        # Create settlements involving user1
        settlement1 = Settlement(
            from_user_id=user1.id,
            to_user_id=user2.id,
            amount=30.0,
            group_id=group.id,
            created_at=datetime.now()
        )
        settlement2 = Settlement(
            from_user_id=user3.id,
            to_user_id=user1.id,
            amount=20.0,
            group_id=group.id,
            created_at=datetime.now()
        )
        # Create a settlement not involving user1
        settlement3 = Settlement(
            from_user_id=user2.id,
            to_user_id=user3.id,
            amount=10.0,
            group_id=group.id,
            created_at=datetime.now()
        )
        db.session.add_all([settlement1, settlement2, settlement3])
        db.session.commit()
        
        # Import the service
        from app.services.settlement_service import SettlementService
        
        # Act
        settlements = SettlementService.get_settlements_for_user(user1.id)
        
        # Assert
        assert len(settlements) == 2
        # Check that all returned settlements involve user1
        for settlement in settlements:
            assert settlement.from_user_id == user1.id or settlement.to_user_id == user1.id