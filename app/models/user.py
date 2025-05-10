from app import db

class User(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(80), unique=True, nullable=False)
    email = db.Column(db.String(120), unique=True, nullable=False)
    password = db.Column(db.String(120), nullable=False)
    
    # Relationships
    groups = db.relationship('Group', secondary='user_group', back_populates='members')
    expenses_paid = db.relationship('Expense', back_populates='payer')
    
    def __repr__(self):
        return f'<User {self.username}>'