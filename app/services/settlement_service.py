from datetime import datetime
from ..models.settlement import Settlement

def record_settlement(
    payer_id: int,
    receiver_id: int,
    amount: float,
    group_id: int,
    date: datetime
) -> Settlement:
    """
    Record a settlement payment between two users in a group.
    
    Args:
        payer_id: ID of the user making the payment
        receiver_id: ID of the user receiving the payment
        amount: Amount being paid
        group_id: ID of the group where the settlement occurs
        date: Date of the settlement
        
    Returns:
        Settlement: The created settlement record
    """
    settlement = Settlement(
        payer_id=payer_id,
        receiver_id=receiver_id,
        amount=amount,
        group_id=group_id,
        date=date
    )
    
    return settlement