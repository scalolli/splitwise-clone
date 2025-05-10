from flask import Blueprint, render_template
from app.models.user import User
from app.models.group import Group

main_bp = Blueprint('main', __name__)

@main_bp.route('/')
def index():
    users = User.query.all()
    groups = Group.query.all()
    return render_template('index.html', users=users, groups=groups)