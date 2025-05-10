from app import db
from werkzeug.security import generate_password_hash, check_password_hash

class User(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(80), unique=True, nullable=False)
    email = db.Column(db.String(120), unique=True, nullable=False)
    password = db.Column(db.String(255), nullable=False)
    
    # Relationships
    groups = db.relationship('Group', secondary='user_group', back_populates='members')
    expenses_paid = db.relationship('Expense', back_populates='payer')
    
    def set_password(self, password):
        """Set password hash from plain text password"""
        self.password = generate_password_hash(password)
        
    def check_password(self, password):
        """Check if provided password matches the hash"""
        return check_password_hash(self.password, password)
    
    def __repr__(self):
        return f'<User {self.username}>'