from flask import Blueprint, render_template, request, redirect, url_for, flash, session
from app.models.expense import Expense
from app.models.expense_share import ExpenseShare
from app.models.group import Group
from app import db
from app.utils.datetime import utcnow
from app.forms.edit_expense_form import EditExpenseForm
from app.forms.add_expense_form import AddExpenseForm

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

    form = AddExpenseForm()
    form.payer_id.choices = [(user.id, user.username) for user in group.members]
    for split_form in form.splits:
        split_form.user_id.choices = [(user.id, user.username) for user in group.members]

    if request.method == 'GET':
        # Prepopulate splits for all group members (equal split by default)
        form.splits.entries = []
        member_count = len(group.members)
        default_share = 0
        if member_count > 0:
            default_share = 1 / member_count
        for member in group.members:
            split_form = form.splits.append_entry()
            split_form.user_id.data = member.id
            split_form.amount.data = 0
            split_form.user_id.choices = [(user.id, user.username) for user in group.members]

    if form.validate_on_submit():
        description = form.description.data
        amount = float(form.amount.data)
        payer_id = form.payer_id.data
        # Create new expense
        expense = Expense(
            description=description,
            amount=amount,
            date=utcnow(),
            payer_id=payer_id,
            group_id=group_id
        )
        db.session.add(expense)
        db.session.flush()
        # Add splits
        for split in form.splits.data:
            share = ExpenseShare(
                expense_id=expense.id,
                user_id=split['user_id'],
                amount=split['amount']
            )
            db.session.add(share)
        db.session.commit()
        flash('Expense added successfully', 'success')
        return redirect(url_for('groups.group', group_id=group_id))

    return render_template('expenses/add.html', group=group, form=form)

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
    
    for entry in form.splits.entries:
        # Set choices for each split entry
        print("Setting choices for split entry:", entry.data)

    if form.validate_on_submit():
        # Update expense details
        print("Saving expense:", form.description.data, form.amount.data, form.date.data, form.payer_id.data)
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

    print("Form errors:", form.errors)
    return render_template('expenses/edit.html', form=form, expense=expense)

