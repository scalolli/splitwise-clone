import unittest
from datetime import datetime
from app.models.user import User
from app.models.group import Group
from app.models.settlement import Settlement
from app.services.settlement_service import record_settlement

class TestSettlementService(unittest.TestCase):
    def setUp(self):
        # Create test users
        self.user1 = User(id=1, username="user1")
        self.user2 = User(id=2, username="user2")
        
        # Create test group
        self.group = Group(
            id=1,
            name="Test Group",
            members=[self.user1, self.user2]
        )

    def test_record_simple_settlement(self):
        # Test recording a simple payment between two users
        settlement = record_settlement(
            payer_id=1,
            receiver_id=2,
            amount=50.00,
            group_id=1,
            date=datetime(2024, 1, 1)
        )

        # Verify settlement was recorded correctly
        self.assertIsInstance(settlement, Settlement)
        self.assertEqual(settlement.payer_id, 1)
        self.assertEqual(settlement.receiver_id, 2)
        self.assertEqual(settlement.amount, 50.00)
        self.assertEqual(settlement.group_id, 1)
        self.assertEqual(settlement.date, datetime(2024, 1, 1))