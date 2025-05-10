from flask import Blueprint, render_template, request, redirect, url_for, flash, session
from app.models.user import User
from app import db
from werkzeug.security import generate_password_hash, check_password_hash

auth_bp = Blueprint('auth', __name__)

@auth_bp.route('/login', methods=['GET', 'POST'])
def login():
    error = None
    if request.method == 'POST':
        username = request.form.get('username')
        password = request.form.get('password')
        
        user = User.query.filter_by(username=username).first()
        
        if user and user.check_password(password):
            session['user_id'] = user.id
            session['username'] = user.username
            flash('You have been logged in successfully!', 'success')
            return redirect(url_for('main.index'))
        else:
            error = "Invalid username or password"
    
    return render_template('login.html', error=error)

@auth_bp.route('/register', methods=['GET', 'POST'])
def register():
    error = None
    if request.method == 'POST':
        username = request.form.get('username')
        email = request.form.get('email')
        password = request.form.get('password')
        confirm_password = request.form.get('confirm_password')
        
        # Validate input
        if not username or not email or not password:
            error = "All fields are required"
        elif password != confirm_password:
            error = "Passwords do not match"
        elif User.query.filter_by(username=username).first():
            error = "Username already exists"
        elif User.query.filter_by(email=email).first():
            error = "Email already exists"
        else:
            # Create new user with hashed password
            new_user = User(username=username, email=email)
            new_user.set_password(password)
            db.session.add(new_user)
            db.session.commit()
            
            flash('Your account has been created! You can now log in.', 'success')
            return redirect(url_for('auth.login'))
    
    return render_template('register.html', error=error)

@auth_bp.route('/logout')
def logout():
    session.pop('user_id', None)
    session.pop('username', None)
    flash('You have been logged out.', 'info')
    return redirect(url_for('main.index'))