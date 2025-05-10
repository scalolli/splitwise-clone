from app import db

class ExpenseShare(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    expense_id = db.Column(db.Integer, db.ForeignKey('expense.id'), nullable=False)
    user_id = db.Column(db.Integer, db.ForeignKey('user.id'), nullable=False)
    amount = db.Column(db.Float, nullable=False)
    
    # Relationships
    expense = db.relationship('Expense', back_populates='shares')
    user = db.relationship('User')
    
    def __repr__(self):
        return f'<ExpenseShare ${self.amount}>'