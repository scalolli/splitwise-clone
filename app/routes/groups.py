from flask import Blueprint, render_template, request, redirect, url_for, flash, session, abort
from app.models.group import Group
from app.models.user import User
from app import db
import datetime
from app.services.balance_service import calculate_balances

groups_bp = Blueprint('groups', __name__)

@groups_bp.route('/group/<int:group_id>')
def group(group_id):
    # Check if user is logged in
    if 'user_id' not in session:
        flash('Please log in to access this page', 'error')
        return redirect(url_for('auth.login'))
    
    group = db.session.get(Group, group_id)
    if not group:
        abort(404)
    
    # Calculate balances for the group
    balances = calculate_balances(group_id)
    
    # Get user information for displaying balances
    user_ids = set()
    for balance in balances:
        user_ids.add(balance['from_user_id'])
        user_ids.add(balance['to_user_id'])
    
    users = db.session.execute(
        db.select(User).filter(User.id.in_(user_ids))
    ).scalars().all()
    user_map = {user.id: user for user in users}
    
    # Add user objects to balances for easy access in template
    for balance in balances:
        balance['from_user'] = user_map.get(balance['from_user_id'])
        balance['to_user'] = user_map.get(balance['to_user_id'])
    
    return render_template('group.html', group=group, balances=balances)

@groups_bp.route('/group/create', methods=['GET', 'POST'])
def create_group():
    # Check if user is logged in
    if 'user_id' not in session:
        flash('Please log in to access this page', 'error')
        return redirect(url_for('auth.login'))
    
    error = None
    if request.method == 'POST':
        name = request.form.get('name')
        description = request.form.get('description')
        
        # Validate input
        if not name:
            error = "Group name is required"
        else:
            # Create new group
            new_group = Group(
                name=name,
                description=description,
                created_at=datetime.datetime.now(datetime.timezone.utc),
                created_by_id=session['user_id']
            )
            
            # Add current user as a member
            current_user = db.session.get(User, session['user_id'])
            new_group.members.append(current_user)
            
            db.session.add(new_group)
            db.session.commit()
            
            flash('Group created successfully', 'success')
            return redirect(url_for('groups.group', group_id=new_group.id))
    
    return render_template('create_group.html', error=error)

@groups_bp.route('/group/<int:group_id>/add_member', methods=['POST'])
def add_member(group_id):
    # Check if user is logged in
    if 'user_id' not in session:
        flash('Please log in to access this page', 'error')
        return redirect(url_for('auth.login'))
    
    group = db.session.get(Group, group_id)
    if not group:
        abort(404)
    
    # Check if current user is the group creator
    if group.created_by_id != session['user_id']:
        flash('You do not have permission to add members to this group', 'error')
        return redirect(url_for('groups.group', group_id=group_id))
    
    username = request.form.get('username')
    user = db.session.execute(
        db.select(User).filter_by(username=username)
    ).scalar_one_or_none()
    
    if not user:
        flash('User not found', 'error')
        return redirect(url_for('groups.group', group_id=group_id))
    
    if user in group.members:
        flash('User is already a member of this group', 'info')
        return redirect(url_for('groups.group', group_id=group_id))
    
    group.members.append(user)
    db.session.commit()
    
    flash('Member added successfully', 'success')
    return redirect(url_for('groups.group', group_id=group_id))

@groups_bp.route('/group/<int:group_id>/remove_member/<int:user_id>', methods=['POST'])
def remove_member(group_id, user_id):
    # Check if user is logged in
    if 'user_id' not in session:
        flash('Please log in to access this page', 'error')
        return redirect(url_for('auth.login'))
    
    group = db.session.get(Group, group_id)
    if not group:
        abort(404)
    
    # Check if current user is the group creator
    if group.created_by_id != session['user_id']:
        flash('You do not have permission to remove members from this group', 'error')
        return redirect(url_for('groups.group', group_id=group_id))
    
    # Cannot remove the creator
    if user_id == group.created_by_id:
        flash('Cannot remove the group creator', 'error')
        return redirect(url_for('groups.group', group_id=group_id))
    
    user = db.session.get(User, user_id)
    if not user:
        abort(404)
    
    if user not in group.members:
        flash('User is not a member of this group', 'error')
        return redirect(url_for('groups.group', group_id=group_id))
    
    group.members.remove(user)
    db.session.commit()
    
    flash('Member removed successfully', 'success')
    return redirect(url_for('groups.group', group_id=group_id))