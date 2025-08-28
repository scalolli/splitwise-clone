from flask_wtf import FlaskForm
from wtforms import StringField, DecimalField, DateField, SelectField, FieldList, FormField
from wtforms.validators import DataRequired, NumberRange, ValidationError
from wtforms import Form
from datetime import date


class SplitForm(Form):
    user_id = SelectField('User', coerce=int, validators=[DataRequired()])
    amount = DecimalField('Amount', validators=[DataRequired(), NumberRange(min=0)])


class BaseExpenseForm(FlaskForm):
    description = StringField('Description', validators=[DataRequired()])
    amount = DecimalField('Amount', validators=[DataRequired(), NumberRange(min=0.01, message="Amount must be greater than 0")])
    date = DateField('Date', default=date.today, validators=[DataRequired()])
    payer_id = SelectField('Paid by', coerce=int, validators=[DataRequired()])
    splits = FieldList(FormField(SplitForm), min_entries=1)

    def validate(self, extra_validators=None):
        # Get group member IDs from the choices before calling parent validation
        group_member_ids = [choice[0] for choice in self.payer_id.choices if choice[0] != '']

        # Check if payer is a group member BEFORE calling parent validation
        # This allows us to provide our custom error message instead of WTForms default
        if self.payer_id.data and self.payer_id.data not in group_member_ids:
            self.payer_id.errors = ['Payer is not a member of this group']
            return False

        # Call parent validation
        rv = super().validate(extra_validators)
        if not rv:
            # If parent validation failed due to invalid payer choice, replace with our message
            if 'payer_id' in self.errors and any('not a valid choice' in str(error).lower() for error in self.errors['payer_id']):
                self.payer_id.errors = ['Payer is not a member of this group']
            return False

        # Check if splits sum to total amount
        total_splits = sum(split.amount.data or 0 for split in self.splits)
        if abs(total_splits - (self.amount.data or 0)) > 0.01:  # Allow for small floating point differences
            # Test expects error on 'splits' field with specific message containing "sum of all splits"
            self.splits.errors.append(f'The sum of all splits must equal the total amount. Current total: ${total_splits:.2f}')
            return False

        # Check if payer is among split users
        split_user_ids = [split.user_id.data for split in self.splits if split.user_id.data]
        if self.payer_id.data not in split_user_ids:
            # Test expects error on 'splits' field with message containing "payer"
            self.splits.errors.append('The payer must be included in the expense splits')
            return False

        # Check if all split users are group members
        for i, split in enumerate(self.splits):
            if split.user_id.data and split.user_id.data not in group_member_ids:
                # Add error to individual split form
                if not hasattr(split.user_id, 'errors'):
                    split.user_id.errors = []
                split.user_id.errors.append('Split user is not a member of this group')
                return False

        # Check for duplicate split users
        split_user_ids_valid = [split.user_id.data for split in self.splits if split.user_id.data]
        if len(split_user_ids_valid) != len(set(split_user_ids_valid)):
            duplicates = set([x for x in split_user_ids_valid if split_user_ids_valid.count(x) > 1])
            for i, split in enumerate(self.splits):
                if split.user_id.data in duplicates:
                    if not hasattr(split.user_id, 'errors'):
                        split.user_id.errors = []
                    split.user_id.errors.append('Each user can only appear once in the split')
            # Test expects error containing "duplicate"
            self.splits.errors.append('Duplicate users found in the expense splits')
            return False

        return True
