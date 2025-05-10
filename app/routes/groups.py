from flask import Blueprint, render_template
from app.models.group import Group

groups_bp = Blueprint('groups', __name__)

@groups_bp.route('/group/<int:group_id>')
def group(group_id):
    group = Group.query.get_or_404(group_id)
    # Make sure we're explicitly loading the expenses
    expenses = list(group.expenses)  # This forces the query to execute
    return render_template('group.html', group=group, expenses=expenses)