from flask_wtf import FlaskForm
from wtforms import StringField, DecimalField, DateField, SelectField, FieldList, FormField, Form, ValidationError
from wtforms.validators import DataRequired, NumberRange, Optional
from app.forms.base_expense_form import BaseExpenseForm, ExpenseSplitForm

class EditExpenseForm(BaseExpenseForm):
    date = DateField("Date", validators=[Optional()])
