
from flask_wtf import FlaskForm
from wtforms import StringField, DecimalField, DateField, SelectField, FieldList, FormField, Form, ValidationError
from wtforms.validators import DataRequired, NumberRange, Optional

class ExpenseSplitForm(Form):
    user_id = SelectField("User", coerce=int, validators=[DataRequired()])
    amount = DecimalField("Amount", validators=[DataRequired(), NumberRange(min=0, message="Amount must be greater than zero.")])

class EditExpenseForm(FlaskForm):
    description = StringField("Description", validators=[DataRequired()])
    amount = DecimalField("Amount", validators=[DataRequired(), NumberRange(min=0, message="Amount must be greater than zero.")])
    date = DateField("Date", validators=[Optional()])
    payer_id = SelectField("Payer", coerce=int, validators=[DataRequired()])
    splits = FieldList(FormField(ExpenseSplitForm), min_entries=1)

    def validate_payer_id(self, field):
        valid_ids = [choice[0] for choice in field.choices]
        if field.data not in valid_ids:
            # Remove the default error if present
            field.errors[:] = []
            raise ValidationError("Selected payer is not a member of the group.")
