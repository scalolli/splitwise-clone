import pytest
from werkzeug.datastructures import MultiDict
from app.forms.edit_expense_form import EditExpenseForm

@pytest.mark.parametrize("formdata,expected_error_field", [
    # Description required
    (MultiDict({
        'description': '',
        'amount': 100,
        'payer_id': 1,
        'splits-0-user_id': 1,
        'splits-0-amount': 100
    }), 'description'),
    # Amount required
    (MultiDict({
        'description': 'Valid Description',
        'amount': '',
        'payer_id': 1,
        'splits-0-user_id': 1,
        'splits-0-amount': ''
    }), 'amount'),
    # Amount must be positive
    (MultiDict({
        'description': 'Valid Description',
        'amount': -50,
        'payer_id': 1,
        'splits-0-user_id': 1,
        'splits-0-amount': -50
    }), 'amount'),
])
def test_edit_expense_form_validation(formdata, expected_error_field, app):
    with app.app_context():
        form = EditExpenseForm(formdata)
        # Patch choices for SelectFields (simulate user1 and user2 as possible payers/members)
        user_choices = [(1, 'user1'), (2, 'user2')]
        form.payer_id.choices = user_choices
        # If splits is a FieldList of subforms with user_id as SelectField, patch those too
        if hasattr(form, 'splits'):
            for subform in form.splits:
                if hasattr(subform, 'user_id'):
                    subform.user_id.choices = user_choices
        valid = form.validate()
        print(f"Form data: {formdata}, Valid: {valid}, Errors: {form.errors}")
        assert not valid, "Form should not validate with invalid data."
        assert expected_error_field in form.errors, f"Expected error for field: {expected_error_field}, got: {form.errors}"
        # Optionally, check for specific error messages
        if expected_error_field == 'amount' and formdata['amount'] == '':
            assert any('required' in msg.lower() for msg in form.errors['amount'])
        if expected_error_field == 'amount' and str(formdata['amount']).startswith('-'):
            assert any('greater than' in msg.lower() or 'positive' in msg.lower() for msg in form.errors['amount'])
        if expected_error_field == 'description':
            assert any('required' in msg.lower() for msg in form.errors['description'])

def test_edit_expense_form_payer_must_be_group_member(app):
    with app.app_context():
        formdata = MultiDict({
            'description': 'Dinner',
            'amount': 50,
            'payer_id': 3,  # Not in choices
            'splits-0-user_id': 1,
            'splits-0-amount': 50
        })
        form = EditExpenseForm(formdata)
        # Only user1 and user2 are group members
        user_choices = [(1, 'user1'), (2, 'user2')]
        form.payer_id.choices = user_choices
        if hasattr(form, 'splits'):
            for subform in form.splits:
                if hasattr(subform, 'user_id'):
                    subform.user_id.choices = user_choices
        valid = form.validate()
        print(f"Payer not in group: Valid: {valid}, Errors: {form.errors}")
        assert not valid, "Form should not validate if payer is not a group member."
        assert 'payer_id' in form.errors, f"Expected error for payer_id, got: {form.errors}"
        assert any('not a member' in msg.lower() for msg in form.errors['payer_id']), f"Expected 'not a member' error, got: {form.errors['payer_id']}"            


def test_edit_expense_form_splits_must_sum_to_total(app):
    with app.app_context():
        formdata = MultiDict({
            'description': 'Lunch',
            'amount': 100,
            'payer_id': 1,
            'splits-0-user_id': 1,
            'splits-0-amount': 60,
            'splits-1-user_id': 2,
            'splits-1-amount': 30  # Only sums to 90, not 100
        })
        form = EditExpenseForm(formdata)
        user_choices = [(1, 'user1'), (2, 'user2')]
        form.payer_id.choices = user_choices
        if hasattr(form, 'splits'):
            for subform in form.splits:
                if hasattr(subform, 'user_id'):
                    subform.user_id.choices = user_choices
        valid = form.validate()
        print(f"Splits sum: Valid: {valid}, Errors: {form.errors}")
        assert not valid, "Form should not validate if splits do not sum to total."
        assert 'splits' in form.errors, f"Expected error for splits, got: {form.errors}"
        assert any('sum of all splits' in msg.lower() for msg in form.errors['splits']), f"Expected 'sum of all splits' error, got: {form.errors['splits']}"        