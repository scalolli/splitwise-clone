from app import create_app, db
from app.models.user import User
from app.models.group import Group
from app.models.expense import Expense
from app.models.expense_share import ExpenseShare
from app.models.settlement import Settlement

app = create_app()

@app.shell_context_processor
def make_shell_context():
    return {
        'db': db, 
        'User': User, 
        'Group': Group, 
        'Expense': Expense, 
        'ExpenseShare': ExpenseShare, 
        'Settlement': Settlement
    }

if __name__ == '__main__':
    app.run(debug=True)