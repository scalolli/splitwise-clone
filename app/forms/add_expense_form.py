from flask_wtf import FlaskForm
from wtforms import StringField, DecimalField, SelectField, FieldList, FormField, Form, ValidationError
from wtforms.validators import DataRequired, NumberRange
from app.forms.base_expense_form import BaseExpenseForm, ExpenseSplitForm

class AddExpenseForm(BaseExpenseForm):
    pass
