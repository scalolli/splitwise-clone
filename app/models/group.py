from datetime import datetime
from app import db

# Association table for many-to-many relationship between User and Group
user_group = db.Table('user_group',
    db.Column('user_id', db.Integer, db.ForeignKey('user.id'), primary_key=True),
    db.Column('group_id', db.Integer, db.ForeignKey('group.id'), primary_key=True)
)

class Group(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(100), nullable=False)
    description = db.Column(db.Text)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    created_by_id = db.Column(db.Integer, db.ForeignKey('user.id'))
    
    # Relationships
    members = db.relationship('User', secondary=user_group, back_populates='groups')
    expenses = db.relationship('Expense', back_populates='group')
    
    def __repr__(self):
        return f'<Group {self.name}>'