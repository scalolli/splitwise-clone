import pytest
from datetime import datetime
from app import db
from app.services.settlement_service import record_settlement


def test_record_valid_settlement(app, test_data):
    """Test recording a valid settlement between two users in a group"""
    with app.app_context():
        # Get test users and group from test_data
        user1 = test_data['users']['user1']
        user2 = test_data['users']['user2']
        group = test_data['groups']['apartment']
        
        # Record settlement using db instance
        settlement = record_settlement(
            db=db,  # Use the db instance instead of app.db
            payer_id=user1.id,
            receiver_id=user2.id,
            amount=50.00,
            group_id=group.id,
            date=datetime(2024, 1, 1)
        )

        # Verify settlement was recorded correctly
        assert settlement.payer_id == user1.id
        assert settlement.receiver_id == user2.id
        assert settlement.amount == 50.00
        assert settlement.group_id == group.id
        assert settlement.date == datetime(2024, 1, 1)