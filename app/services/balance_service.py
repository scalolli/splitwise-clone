from app.models.expense import Expense
from app.models.expense_share import ExpenseShare
from app.models.group import Group
from app.models.user import User
from app import db
from sqlalchemy import func

def calculate_balances(group_id):
    """
    Calculate balances for a group.
    
    Args:
        group_id: The ID of the group
        
    Returns:
        List of dictionaries with from_user_id, to_user_id, and amount
    """
    # Get the group and its members
    group = db.session.get(Group, group_id)
    if not group:
        from flask import abort
        abort(404)
    members = group.members
    
    # Initialize a dictionary to track balances between users
    # Key is a tuple of (user_id, other_user_id), value is the amount user_id owes other_user_id
    balances = {}
    
    # Get all expenses for the group
    expenses = db.session.execute(
        db.select(Expense).filter_by(group_id=group_id)
    ).scalars().all()
    
    for expense in expenses:
        payer_id = expense.payer_id
        # Get all shares for this expense
        shares = db.session.execute(
            db.select(ExpenseShare).filter_by(expense_id=expense.id)
        ).scalars().all()
        for share in shares:
            user_id = share.user_id
            share_amount = share.amount
            # Skip if the user is the payer (they don't owe themselves)
            if user_id == payer_id:
                continue
            # User owes the payer for their share
            key = (user_id, payer_id)
            balances[key] = balances.get(key, 0) + share_amount
    
    # Simplify balances by combining reciprocal debts
    simplified_balances = []
    processed_pairs = set()
    
    for (from_id, to_id), amount in balances.items():
        # Skip if this pair has already been processed
        if (from_id, to_id) in processed_pairs or (to_id, from_id) in processed_pairs:
            continue
        
        # Check if there's a reciprocal debt
        reciprocal_amount = balances.get((to_id, from_id), 0)
        
        # Calculate the net amount
        net_amount = amount - reciprocal_amount
        
        if net_amount > 0:
            # from_id owes to_id
            simplified_balances.append({
                'from_user_id': from_id,
                'to_user_id': to_id,
                'amount': net_amount
            })
        elif net_amount < 0:
            # to_id owes from_id
            simplified_balances.append({
                'from_user_id': to_id,
                'to_user_id': from_id,
                'amount': abs(net_amount)
            })
        # If net_amount is 0, no debt exists
        
        # Mark this pair as processed
        processed_pairs.add((from_id, to_id))
        processed_pairs.add((to_id, from_id))
    
    return simplified_balances