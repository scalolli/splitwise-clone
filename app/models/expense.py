from datetime import datetime
from app import db

class Expense(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    description = db.Column(db.String(200), nullable=False)
    amount = db.Column(db.Float, nullable=False)
    date = db.Column(db.DateTime, default=datetime.utcnow)
    payer_id = db.Column(db.Integer, db.ForeignKey('user.id'), nullable=False)
    group_id = db.Column(db.Integer, db.ForeignKey('group.id'), nullable=False)
    
    # Relationships
    payer = db.relationship('User', back_populates='expenses_paid')
    group = db.relationship('Group', back_populates='expenses')
    shares = db.relationship('ExpenseShare', back_populates='expense', cascade="all, delete-orphan")
    
    def __repr__(self):
        return f'<Expense {self.description} ${self.amount}>'