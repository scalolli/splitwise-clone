import pytest
from datetime import datetime

def test_settlement_model_basic(app, populated_test_db):
    """Test that the basic Settlement model can be created and saved to the database"""
    from app import db
    from app.models.user import User
    from app.models.group import Group
    from app.models.settlement import Settlement
    
    with app.app_context():
        # Create test users
        user1 = User(username='Test User 1', email='test1@example.com')
        user1.set_password('password')
        user2 = User(username='Test User 2', email='test2@example.com')
        user2.set_password('password')
        db.session.add_all([user1, user2])
        db.session.commit()
        
        # Create test group
        group = Group(name='Test Group', created_by_id=user1.id)
        db.session.add(group)
        db.session.commit()
        
        # Create a settlement
        settlement = Settlement(
            from_user_id=user1.id,
            to_user_id=user2.id,
            amount=50.0,
            group_id=group.id,
            created_at=datetime.now()
        )
        db.session.add(settlement)
        db.session.commit()
        
        # Verify it was saved correctly
        saved_settlement = Settlement.query.get(settlement.id)
        assert saved_settlement is not None
        assert saved_settlement.from_user_id == user1.id
        assert saved_settlement.to_user_id == user2.id
        assert saved_settlement.amount == 50.0
        assert saved_settlement.group_id == group.id