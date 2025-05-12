from app import db
from datetime import datetime
from app.models.user import User  # Import to check the actual table name
from app.models.group import Group  # Import to check the actual table name

class Settlement(db.Model):
    __tablename__ = 'settlement'  # Adjust if you're using a different naming convention
    
    id = db.Column(db.Integer, primary_key=True)
    from_user_id = db.Column(db.Integer, db.ForeignKey(User.__tablename__ + '.id'), nullable=False)
    to_user_id = db.Column(db.Integer, db.ForeignKey(User.__tablename__ + '.id'), nullable=False)
    amount = db.Column(db.Float, nullable=False)
    group_id = db.Column(db.Integer, db.ForeignKey(Group.__tablename__ + '.id'), nullable=False)
    created_at = db.Column(db.DateTime, default=datetime.now, nullable=False)
    
    # Define relationships
    from_user = db.relationship('User', foreign_keys=[from_user_id])
    to_user = db.relationship('User', foreign_keys=[to_user_id])
    group = db.relationship('Group')
    
    def __repr__(self):
        return f'<Settlement {self.id}: {self.from_user_id} paid {self.amount} to {self.to_user_id}>'
