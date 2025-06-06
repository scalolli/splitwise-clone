from flask import Flask, render_template, redirect, url_for, request, flash
from flask_sqlalchemy import SQLAlchemy
from app.utils.datetime import utcnow

app = Flask(__name__)
app.config['SECRET_KEY'] = 'your-secret-key'
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///splitwise.db'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False

db = SQLAlchemy(app)

# Association table for many-to-many relationship between users and groups
group_members = db.Table('group_members',
    db.Column('user_id', db.Integer, db.ForeignKey('user.id'), primary_key=True),
    db.Column('group_id', db.Integer, db.ForeignKey('group.id'), primary_key=True)
)

class User(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(64), unique=True, nullable=False)
    email = db.Column(db.String(120), unique=True, nullable=False)
    password = db.Column(db.String(128), nullable=False)
    
    # Relationships
    groups = db.relationship('Group', secondary=group_members, backref=db.backref('members', lazy='dynamic'))
    expenses_paid = db.relationship('Expense', backref='payer', lazy='dynamic', foreign_keys='Expense.payer_id')
    
    def __repr__(self):
        return f'<User {self.username}>'

class Group(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(64), nullable=False)
    description = db.Column(db.String(256))
    created_at = db.Column(db.DateTime, default=utcnow)
    created_by_id = db.Column(db.Integer, db.ForeignKey('user.id'))
    
    # Relationships
    expenses = db.relationship('Expense', backref='group', lazy='dynamic')
    
    def __repr__(self):
        return f'<Group {self.name}>'

class Expense(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    description = db.Column(db.String(256), nullable=False)
    amount = db.Column(db.Float, nullable=False)
    date = db.Column(db.DateTime, default=utcnow)
    payer_id = db.Column(db.Integer, db.ForeignKey('user.id'), nullable=False)
    group_id = db.Column(db.Integer, db.ForeignKey('group.id'), nullable=False)
    
    # Relationships
    shares = db.relationship('ExpenseShare', backref='expense', lazy='dynamic', cascade='all, delete-orphan')
    
    def __repr__(self):
        return f'<Expense {self.description} ${self.amount}>'

class ExpenseShare(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    expense_id = db.Column(db.Integer, db.ForeignKey('expense.id'), nullable=False)
    user_id = db.Column(db.Integer, db.ForeignKey('user.id'), nullable=False)
    amount = db.Column(db.Float, nullable=False)
    
    # Relationship
    user = db.relationship('User')
    
    def __repr__(self):
        return f'<ExpenseShare ${self.amount}>'

class Settlement(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    payer_id = db.Column(db.Integer, db.ForeignKey('user.id'), nullable=False)
    receiver_id = db.Column(db.Integer, db.ForeignKey('user.id'), nullable=False)
    amount = db.Column(db.Float, nullable=False)
    date = db.Column(db.DateTime, default=utcnow)
    group_id = db.Column(db.Integer, db.ForeignKey('group.id'), nullable=False)
    
    # Relationships
    payer = db.relationship('User', foreign_keys=[payer_id])
    receiver = db.relationship('User', foreign_keys=[receiver_id])
    group = db.relationship('Group')
    
    def __repr__(self):
        return f'<Settlement ${self.amount}>'

@app.before_first_request
def create_tables():
    db.create_all()
    
    # Add some test data if the database is empty
    if User.query.count() == 0:
        # Create users
        user1 = User(username='alice', email='alice@example.com', password='password')
        user2 = User(username='bob', email='bob@example.com', password='password')
        user3 = User(username='charlie', email='charlie@example.com', password='password')
        
        db.session.add_all([user1, user2, user3])
        db.session.commit()
        
        # Create a group
        group = Group(name='Roommates', description='Expenses for our apartment', created_by_id=user1.id)
        group.members.extend([user1, user2, user3])
        
        db.session.add(group)
        db.session.commit()
        
        # Create an expense
        expense = Expense(
            description='Groceries',
            amount=60.0,
            payer_id=user1.id,
            group_id=group.id
        )
        
        db.session.add(expense)
        db.session.commit()
        
        # Create expense shares
        share1 = ExpenseShare(expense_id=expense.id, user_id=user1.id, amount=20.0)
        share2 = ExpenseShare(expense_id=expense.id, user_id=user2.id, amount=20.0)
        share3 = ExpenseShare(expense_id=expense.id, user_id=user3.id, amount=20.0)
        
        db.session.add_all([share1, share2, share3])
        db.session.commit()

@app.route('/group/<int:group_id>')
def view_group(group_id):
    group = Group.query.get_or_404(group_id)
    expenses = group.expenses.all()
    return render_template("group.html", group=group, expenses=expenses)

@app.route('/')
def index():
    users = User.query.all()
    groups = Group.query.all()
    return render_template('index.html', users=users, groups=groups)

@app.route('/login', methods=['GET', 'POST'])
def login():
    error = None
    if request.method == 'POST':
        username = request.form.get('username')
        password = request.form.get('password')
        
        # Simple validation - just check if user exists
        user = User.query.filter_by(username=username).first()
        
        if user:
            # For now, just redirect to home page
            # We'll implement proper password checking later
            return redirect(url_for('index'))
        else:
            error = "Invalid username or password"
    
    return render_template('login.html', error=error)

if __name__ == '__main__':
    app.run()