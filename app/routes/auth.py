from flask import Blueprint, render_template, request, redirect, url_for
from app.models.user import User
from app import db

auth_bp = Blueprint('auth', __name__)

@auth_bp.route('/login', methods=['GET', 'POST'])
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
            return redirect(url_for('main.index'))
        else:
            error = "Invalid username or password"
    
    return render_template('login.html', error=error)