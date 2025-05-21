from flask import Blueprint, render_template, request, redirect, url_for, flash, session
from app.models.expense import Expense
from app.models.expense_share import ExpenseShare
from app.models.group import Group
from app import db
import datetime
from app.forms.edit_expense_form import EditExpenseForm

expenses_bp = Blueprint('expenses', __name__)

@expenses_bp.route('/group/<int:group_id>/add_expense', methods=['GET', 'POST'])
def add_expense(group_id):
    # Check if user is logged in
    if 'user_id' not in session:
        flash('Please log in to access this page', 'error')
        return redirect(url_for('auth.login'))
    
    group = db.session.get(Group, group_id)
    if not group:
        from flask import abort
        abort(404)
    
    # Check if user is a member of the group
    current_user_id = session['user_id']
    if current_user_id not in [member.id for member in group.members]:
        flash('You are not a member of this group', 'error')
        return redirect(url_for('main.index'))
    
    if request.method == 'POST':
        description = request.form.get('description')
        amount_str = request.form.get('amount')
        split_type = request.form.get('split_type', 'equal')
        
        # Validate input
        if not description or not amount_str:
            flash('Description and amount are required', 'error')
            return render_template('expenses/add.html', group=group)
        
        try:
            amount = float(amount_str)
            if amount <= 0:
                flash('Amount must be positive', 'error')
                return render_template('expenses/add.html', group=group)
        except ValueError:
            flash('Amount must be a valid number', 'error')
            return render_template('expenses/add.html', group=group)
        
        # Create new expense
        expense = Expense(
            description=description,
            amount=amount,
            date=datetime.datetime.now(datetime.timezone.utc),
            payer_id=current_user_id,
            group_id=group_id
        )
        db.session.add(expense)
        db.session.flush()  # Get the expense ID before committing
        
        # Create expense shares based on split type
        if split_type == 'equal':
            # Equal split
            member_count = len(group.members)
            share_amount = amount / member_count
            
            for member in group.members:
                share = ExpenseShare(
                    expense_id=expense.id,
                    user_id=member.id,
                    amount=share_amount
                )
                db.session.add(share)
        
        elif split_type == 'custom':
            # Custom split
            total_split = 0
            shares = []
            
            for member in group.members:
                share_amount_str = request.form.get(f'split_amounts[{member.id}]', '0')
                try:
                    share_amount = float(share_amount_str)
                    if share_amount < 0:
                        flash('Share amounts cannot be negative', 'error')
                        return render_template('expenses/add.html', group=group)
                    
                    total_split += share_amount
                    
                    share = ExpenseShare(
                        expense_id=expense.id,
                        user_id=member.id,
                        amount=share_amount
                    )
                    shares.append(share)
                except ValueError:
                    flash('Share amounts must be valid numbers', 'error')
                    return render_template('expenses/add.html', group=group)
            
            # Validate total split amount
            if abs(total_split - amount) > 0.01:  # Allow small rounding errors
                flash(f'Total of share amounts ({total_split:.2f}) must equal the expense amount ({amount:.2f})', 'error')
                return render_template('expenses/add.html', group=group)
            
            # Add all shares to the session
            for share in shares:
                db.session.add(share)
        
        db.session.commit()
        
        flash('Expense added successfully', 'success')
        return redirect(url_for('groups.group', group_id=group_id))
    
    return render_template('expenses/add.html', group=group)

@expenses_bp.route('/expenses/<int:expense_id>/edit', methods=['GET', 'POST'])
def edit_expense(expense_id):
    # Eagerly load the group and its members
    stmt = (
        db.select(Expense)
        .options(db.joinedload(Expense.group).joinedload(Group.members), db.joinedload(Expense.shares))
        .filter(Expense.id == expense_id)
    )
    result = db.session.execute(stmt)
    expense = result.unique().scalar_one_or_none()
    if not expense:
        from flask import abort
        abort(404)
    form = EditExpenseForm()

    # Populate choices for SelectFields
    form.payer_id.choices = [(user.id, user.username) for user in expense.group.members]
    # Populate choices for user_id in each split form
    for split_form in form.splits:
        split_form.user_id.choices = [(user.id, user.username) for user in expense.group.members]

    # Populate form with existing data on GET request
    if request.method == 'GET':
        form.description.data = expense.description
        form.amount.data = expense.amount
        form.date.data = expense.date
        form.payer_id.data = expense.payer_id
        
        # Correctly populate form.splits.entries with FormField instances
        form.splits.entries = []
        for share in expense.shares:
            split_form = form.splits.append_entry()
            split_form.user_id.data = share.user_id
            split_form.amount.data = share.amount
            split_form.user_id.choices = [(user.id, user.username) for user in expense.group.members]

    if form.validate_on_submit():
        # Update expense details
        expense.description = form.description.data
        expense.amount = form.amount.data
        expense.date = form.date.data
        expense.payer_id = form.payer_id.data

        # Update expense splits
        db.session.execute(
            db.delete(ExpenseShare).where(ExpenseShare.expense_id == expense.id)
        )
        for split in form.splits.data:
            new_share = ExpenseShare(
                expense_id=expense.id,
                user_id=split['user_id'],
                amount=split['amount']
            )
            db.session.add(new_share)

        db.session.commit()
        flash('Expense updated successfully!', 'success')
        return redirect(url_for('groups.group', group_id=expense.group_id))

    return render_template('expenses/edit.html', form=form, expense=expense)