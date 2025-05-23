import pytest
from werkzeug.datastructures import MultiDict
from app.forms.edit_expense_form import EditExpenseForm
from app.forms.add_expense_form import AddExpenseForm

@pytest.mark.parametrize("form_class", [EditExpenseForm, AddExpenseForm])
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
def test_expense_form_validation(form_class, formdata, expected_error_field, app):
    with app.app_context():
        form = form_class(formdata)
        user_choices = [(1, 'user1'), (2, 'user2')]
        patch_user_choices(form, user_choices)
        valid = form.validate()
        print(f"Form: {form_class.__name__}, Data: {formdata}, Valid: {valid}, Errors: {form.errors}")
        assert not valid, f"{form_class.__name__} should not validate with invalid data."
        assert expected_error_field in form.errors, f"Expected error for field: {expected_error_field}, got: {form.errors}"
        # Optionally, check for specific error messages
        if expected_error_field == 'amount' and formdata['amount'] == '':
            assert any('required' in msg.lower() for msg in form.errors['amount'])
        if expected_error_field == 'amount' and str(formdata['amount']).startswith('-'):
            assert any('greater than' in msg.lower() or 'positive' in msg.lower() for msg in form.errors['amount'])
        if expected_error_field == 'description':
            assert any('required' in msg.lower() for msg in form.errors['description'])

@pytest.mark.parametrize("form_class", [EditExpenseForm, AddExpenseForm])
def test_expense_form_payer_must_be_group_member(form_class, app):
    with app.app_context():
        formdata = MultiDict({
            'description': 'Dinner',
            'amount': 50,
            'payer_id': 3,  # Not in choices
            'splits-0-user_id': 1,
            'splits-0-amount': 50
        })
        form = form_class(formdata)
        user_choices = [(1, 'user1'), (2, 'user2')]
        patch_user_choices(form, user_choices)
        valid = form.validate()
        print(f"Form: {form_class.__name__}, Payer not in group: Valid: {valid}, Errors: {form.errors}")
        assert not valid, f"{form_class.__name__} should not validate if payer is not a group member."
        assert 'payer_id' in form.errors, f"Expected error for payer_id, got: {form.errors}"
        assert any('not a member' in msg.lower() for msg in form.errors['payer_id']), f"Expected 'not a member' error, got: {form.errors['payer_id']}"

@pytest.mark.parametrize("form_class", [EditExpenseForm, AddExpenseForm])
def test_expense_form_splits_must_sum_to_total(form_class, app):
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
        form = form_class(formdata)
        user_choices = [(1, 'user1'), (2, 'user2')]
        patch_user_choices(form, user_choices)
        valid = form.validate()
        print(f"Form: {form_class.__name__}, Splits sum: Valid: {valid}, Errors: {form.errors}")
        assert not valid, f"{form_class.__name__} should not validate if splits do not sum to total."
        assert 'splits' in form.errors, f"Expected error for splits, got: {form.errors}"
        assert any('sum of all splits' in msg.lower() for msg in form.errors['splits']), f"Expected 'sum of all splits' error, got: {form.errors['splits']}"

def patch_user_choices(form, user_choices):
    form.payer_id.choices = user_choices
    if hasattr(form, 'splits'):
        for subform in form.splits:
            if hasattr(subform, 'user_id'):
                subform.user_id.choices = user_choices

def _assert_duplicate_user_ids_invalid(form_class, app):
    with app.app_context():
        formdata = MultiDict({
            'description': 'Test Expense',
            'amount': 100,
            'payer_id': 1,
            'splits-0-user_id': 1,
            'splits-0-amount': 60,
            'splits-1-user_id': 1,  # Duplicate user_id
            'splits-1-amount': 40
        })
        form = form_class(formdata)
        user_choices = [(1, 'user1'), (2, 'user2')]
        patch_user_choices(form, user_choices)
        valid = form.validate()
        assert not valid, f"{form_class.__name__} should not validate when splits have duplicate user_ids."
        assert 'splits' in form.errors, f"Expected error on splits for duplicate user_ids in {form_class.__name__}."
        assert any('duplicate' in msg.lower() or 'unique' in msg.lower() for msg in form.errors['splits']), f"Expected duplicate user_id error message in {form_class.__name__}."

def test_expense_forms_duplicate_split_user_ids(app):
    _assert_duplicate_user_ids_invalid(EditExpenseForm, app)
    _assert_duplicate_user_ids_invalid(AddExpenseForm, app)
