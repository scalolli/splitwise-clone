from flask import Blueprint, render_template
from app import db
from app.models.user import User
from app.models.group import Group

main_bp = Blueprint('main', __name__)

@main_bp.route('/')
def index():
    users = db.session.execute(db.select(User)).scalars().all()
    groups = db.session.execute(db.select(Group)).scalars().all()
    return render_template('index.html', users=users, groups=groups)