from flask_wtf import FlaskForm
from wtforms import StringField, DecimalField, SelectField, FieldList, FormField, Form, ValidationError
from wtforms.validators import DataRequired, NumberRange

class ExpenseSplitForm(Form):
    user_id = SelectField("User", coerce=int, validators=[DataRequired()])
    amount = DecimalField("Amount", validators=[DataRequired(), NumberRange(min=0, message="Amount must be greater than zero.")])

class AddExpenseForm(FlaskForm):
    description = StringField("Description", validators=[DataRequired()])
    amount = DecimalField("Amount", validators=[DataRequired(), NumberRange(min=0, message="Amount must be greater than zero.")])
    payer_id = SelectField("Payer", coerce=int, validators=[DataRequired()])
    splits = FieldList(FormField(ExpenseSplitForm), min_entries=1)

    def validate_payer_id(self, field):
        valid_ids = [choice[0] for choice in field.choices]
        if field.data not in valid_ids:
            field.errors[:] = []
            raise ValidationError("Selected payer is not a member of the group.")

    def validate(self, extra_validators=None):
        rv = super().validate(extra_validators)
        if not rv:
            return False
        total = self.amount.data or 0
        split_sum = sum([split_form.amount.data or 0 for split_form in self.splits])
        if round(split_sum, 2) != round(total, 2):
            self.splits.errors.append("The sum of all splits must equal the total amount.")
            return False
        return True

