import unittest
from app.models.user import User
from app.models.group import Group
from app.models.expense import Expense
from app.models.expense_share import ExpenseShare

def calculate_balances(group, expenses):
    # Initialize balances for all members
    balances = {member.id: 0 for member in group.members}
    
    for expense in expenses:
        # Add the full amount to payer's balance
        balances[expense.payer_id] += expense.amount
        
        # Subtract each person's share
        for share in expense.shares:
            balances[share.user_id] -= share.amount
    
    return balances

class TestBalanceCalculation(unittest.TestCase):
    def test_simple_split(self):
        # Set up two users
        user1 = User(id=1, username="User1")
        user2 = User(id=2, username="User2")
        group = Group(id=1, name="Test Group", members=[user1, user2])

        # Create one expense: User1 pays $100, split equally
        expense = Expense(
            id=1,
            description="Dinner",
            amount=100,
            payer_id=1,
            group_id=1,
            shares=[
                ExpenseShare(user_id=1, amount=50),
                ExpenseShare(user_id=2, amount=50)
            ]
        )

        balances = calculate_balances(group, [expense])
        expected_balances = {
            1: 50,   # User1 paid 100 but owes 50, so net +50
            2: -50   # User2 owes 50
        }
        self.assertEqual(balances, expected_balances)

    def test_multiple_expenses(self):
        # Set up three users
        user1 = User(id=1, username="User1")
        user2 = User(id=2, username="User2")
        user3 = User(id=3, username="User3")
        group = Group(id=1, name="Test Group", members=[user1, user2, user3])

        # Create two expenses
        expense1 = Expense(
            id=1,
            description="Lunch",
            amount=90,
            payer_id=1,
            group_id=1,
            shares=[
                ExpenseShare(user_id=1, amount=30),
                ExpenseShare(user_id=2, amount=30),
                ExpenseShare(user_id=3, amount=30)
            ]
        )
        expense2 = Expense(
            id=2,
            description="Snacks",
            amount=60,
            payer_id=2,
            group_id=1,
            shares=[
                ExpenseShare(user_id=1, amount=20),
                ExpenseShare(user_id=2, amount=20),
                ExpenseShare(user_id=3, amount=20)
            ]
        )

        balances = calculate_balances(group, [expense1, expense2])
        expected_balances = {
            1: 40,   # User1 paid 90, owes 50 (net +40)
            2: 10,   # User2 paid 60, owes 50 (net +10)
            3: -50   # User3 owes 50
        }
        self.assertEqual(balances, expected_balances)

    def test_unequal_split(self):
        # Set up three users
        user1 = User(id=1, username="User1")
        user2 = User(id=2, username="User2")
        user3 = User(id=3, username="User3")
        group = Group(id=1, name="Test Group", members=[user1, user2, user3])

        # Create one expense with unequal split
        expense = Expense(
            id=1,
            description="Trip",
            amount=120,
            payer_id=1,
            group_id=1,
            shares=[
                ExpenseShare(user_id=1, amount=20),  # User1 owes 20
                ExpenseShare(user_id=2, amount=50),  # User2 owes 50
                ExpenseShare(user_id=3, amount=50)   # User3 owes 50
            ]
        )

        balances = calculate_balances(group, [expense])
        expected_balances = {
            1: 100,  # User1 paid 120, owes 20 (net +100)
            2: -50,  # User2 owes 50
            3: -50   # User3 owes 50
        }
        self.assertEqual(balances, expected_balances)

    def test_no_expenses(self):
        # Set up three users
        user1 = User(id=1, username="User1")
        user2 = User(id=2, username="User2")
        user3 = User(id=3, username="User3")
        group = Group(id=1, name="Test Group", members=[user1, user2, user3])

        # No expenses
        balances = calculate_balances(group, [])
        expected_balances = {
            1: 0,  # No expenses, balance remains 0
            2: 0,  # No expenses, balance remains 0
            3: 0   # No expenses, balance remains 0
        }
        self.assertEqual(balances, expected_balances)

    def test_single_user(self):
        # Set up a single user
        user1 = User(id=1, username="User1")
        group = Group(id=1, name="Test Group", members=[user1])

        # Create one expense: User1 pays $100
        expense = Expense(
            id=1,
            description="Solo Expense",
            amount=100,
            payer_id=1,
            group_id=1,
            shares=[
                ExpenseShare(user_id=1, amount=100)  # User1 owes the full amount
            ]
        )

        balances = calculate_balances(group, [expense])
        expected_balances = {
            1: 0  # User1 paid and owes the same amount, so net 0
        }
        self.assertEqual(balances, expected_balances)

    def test_expense_with_no_shares(self):
        # Set up two users
        user1 = User(id=1, username="User1")
        user2 = User(id=2, username="User2")
        group = Group(id=1, name="Test Group", members=[user1, user2])

        # Create one expense with no shares
        expense = Expense(
            id=1,
            description="Invalid Expense",
            amount=100,
            payer_id=1,
            group_id=1,
            shares=[]  # No shares defined
        )

        balances = calculate_balances(group, [expense])
        expected_balances = {
            1: 100,  # User1 paid the full amount
            2: 0     # User2 owes nothing
        }
        self.assertEqual(balances, expected_balances)

    def test_negative_expense(self):
        # Set up two users
        user1 = User(id=1, username="User1")
        user2 = User(id=2, username="User2")
        group = Group(id=1, name="Test Group", members=[user1, user2])

        # Create one negative expense: User1 refunds $50 to User2
        expense = Expense(
            id=1,
            description="Refund",
            amount=-50,
            payer_id=1,
            group_id=1,
            shares=[
                ExpenseShare(user_id=1, amount=-25),  # User1 gets refunded $25
                ExpenseShare(user_id=2, amount=-25)  # User2 gets refunded $25
            ]
        )

        balances = calculate_balances(group, [expense])
        expected_balances = {
            1: -25,  # User1 owes $25
            2: 25    # User2 is owed $25
        }
        self.assertEqual(balances, expected_balances)