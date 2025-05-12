from app import db
from app.models.settlement import Settlement
from datetime import datetime
from sqlalchemy import or_

class SettlementService:
    @staticmethod
    def create_settlement(from_user_id, to_user_id, amount, group_id):
        """
        Create a new settlement between users
        
        Args:
            from_user_id: ID of the user making the payment
            to_user_id: ID of the user receiving the payment
            amount: Amount being settled
            group_id: ID of the group in which the settlement occurs
            
        Returns:
            The newly created Settlement object
        """
        settlement = Settlement(
            from_user_id=from_user_id,
            to_user_id=to_user_id,
            amount=amount,
            group_id=group_id,
            created_at=datetime.now()
        )
        
        db.session.add(settlement)
        db.session.commit()
        return settlement
    
    @staticmethod
    def get_settlements_for_group(group_id):
        """
        Get all settlements for a specific group
        
        Args:
            group_id: ID of the group
            
        Returns:
            List of Settlement objects for the group
        """
        settlements = Settlement.query.filter_by(group_id=group_id).order_by(Settlement.created_at.desc()).all()
        return settlements
    
    @staticmethod
    def get_settlements_for_user(user_id):
        """
        Get all settlements involving a specific user (either as payer or receiver)
        
        Args:
            user_id: ID of the user
            
        Returns:
            List of Settlement objects involving the user
        """
        settlements = Settlement.query.filter(
            or_(
                Settlement.from_user_id == user_id,
                Settlement.to_user_id == user_id
            )
        ).order_by(Settlement.created_at.desc()).all()
        
        return settlements
    
    @staticmethod
    def get_settlements_between_users(user1_id, user2_id, group_id=None):
        """
        Get all settlements between two specific users, optionally filtered by group
        
        Args:
            user1_id: ID of the first user
            user2_id: ID of the second user
            group_id: Optional ID of the group to filter by
            
        Returns:
            List of Settlement objects between the two users
        """
        query = Settlement.query.filter(
            or_(
                # user1 paid user2
                (Settlement.from_user_id == user1_id) & (Settlement.to_user_id == user2_id),
                # user2 paid user1
                (Settlement.from_user_id == user2_id) & (Settlement.to_user_id == user1_id)
            )
        )
        
        if group_id:
            query = query.filter_by(group_id=group_id)
            
        settlements = query.order_by(Settlement.created_at.desc()).all()
        return settlements
    
    @staticmethod
    def get_total_settled_amount(from_user_id, to_user_id, group_id=None):
        """
        Calculate the net amount settled between two users
        
        Args:
            from_user_id: ID of the first user
            to_user_id: ID of the second user
            group_id: Optional ID of the group to filter by
            
        Returns:
            Net amount settled (positive if from_user has paid more to to_user)
        """
        query = Settlement.query
        
        if group_id:
            query = query.filter_by(group_id=group_id)
        
        # Amount paid from from_user to to_user
        paid = query.filter_by(
            from_user_id=from_user_id,
            to_user_id=to_user_id
        ).with_entities(db.func.sum(Settlement.amount)).scalar() or 0
        
        # Amount paid from to_user to from_user
        received = query.filter_by(
            from_user_id=to_user_id,
            to_user_id=from_user_id
        ).with_entities(db.func.sum(Settlement.amount)).scalar() or 0
        
        # Net amount (positive if from_user paid more)
        return paid - received