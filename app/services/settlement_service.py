from datetime import datetime
from flask_sqlalchemy import SQLAlchemy
from ..models.settlement import Settlement

def record_settlement(
    db: SQLAlchemy,
    payer_id: int,
    receiver_id: int,
    amount: float,
    group_id: int,
    date: datetime
) -> Settlement:
    """Record a settlement payment between users"""
    settlement = Settlement(
        payer_id=payer_id,
        receiver_id=receiver_id,
        amount=amount,
        group_id=group_id,
        date=date
    )
    
    db.session.add(settlement)
    db.session.commit()
    
    return settlement