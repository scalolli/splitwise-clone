from flask import Blueprint, render_template
from app.models.group import Group

groups_bp = Blueprint('groups', __name__)

@groups_bp.route('/group/<int:group_id>')
def group(group_id):
    group = Group.query.get_or_404(group_id)
    return render_template('group.html', group=group)