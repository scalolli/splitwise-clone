from datetime import datetime
from app import db

class Settlement(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    payer_id = db.Column(db.Integer, db.ForeignKey('user.id'), nullable=False)
    receiver_id = db.Column(db.Integer, db.ForeignKey('user.id'), nullable=False)
    amount = db.Column(db.Float, nullable=False)
    date = db.Column(db.DateTime, default=datetime.utcnow)
    group_id = db.Column(db.Integer, db.ForeignKey('group.id'), nullable=False)
    
    # Relationships
    payer = db.relationship('User', foreign_keys=[payer_id])
    receiver = db.relationship('User', foreign_keys=[receiver_id])
    group = db.relationship('Group')
    
    def __repr__(self):
        return f'<Settlement ${self.amount}>'