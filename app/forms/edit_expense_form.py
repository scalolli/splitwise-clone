
from flask_wtf import FlaskForm
from wtforms import StringField, DecimalField, DateField, SelectField, FieldList, FormField, Form
from wtforms.validators import DataRequired, NumberRange, Optional

class ExpenseSplitForm(Form):
    user_id = SelectField("User", coerce=int, validators=[DataRequired()])
    amount = DecimalField("Amount", validators=[DataRequired(), NumberRange(min=0)])

class EditExpenseForm(FlaskForm):
    description = StringField("Description", validators=[DataRequired()])
    amount = DecimalField("Amount", validators=[DataRequired(), NumberRange(min=0)])
    date = DateField("Date", validators=[Optional()])
    payer_id = SelectField("Payer", coerce=int, validators=[DataRequired()])
    splits = FieldList(FormField(ExpenseSplitForm), min_entries=1)
