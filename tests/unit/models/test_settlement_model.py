import pytest
from datetime import datetime
from app import db
from app.models.user import User
from app.models.group import Group
from app.models.settlement import Settlement

def test_settlement_model_basic(app, populated_test_db):
    """Test that the basic Settlement model can be created and saved to the database"""
    with app.app_context():        
        user1 = User(username='Test User 1', email='test1@example.com')
        user1.set_password('password')
        user2 = User(username='Test User 2', email='test2@example.com')
        user2.set_password('password')
        db.session.add_all([user1, user2])
        db.session.commit()
        
        group = Group(name='Test Group', created_by_id=user1.id)
        db.session.add(group)
        db.session.commit()
        
        settlement = Settlement(
            from_user_id=user1.id,
            to_user_id=user2.id,
            amount=50.0,
            group_id=group.id,
            created_at=datetime.now()
        )
        db.session.add(settlement)
        db.session.commit()
        
        saved_settlement = db.session.get(Settlement, settlement.id)
        assert saved_settlement is not None
        assert saved_settlement.from_user_id == user1.id
        assert saved_settlement.to_user_id == user2.id
        assert saved_settlement.amount == 50.0
        assert saved_settlement.group_id == group.id        
        assert saved_settlement.group_id == group.id