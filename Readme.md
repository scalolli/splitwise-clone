# Splitwise Clone

A simple expense sharing application built with Flask and SQLAlchemy, inspired by Splitwise.

## Features

- User management
- Group creation and management
- Expense tracking
- Expense sharing among group members

## Technologies Used

- Flask: Web framework
- SQLAlchemy: ORM for database interactions
- SQLite: Database

## Setup and Installation

1. Clone the repository:
   ```
   git clone https://github.com/yourusername/splitwise-clone.git
   cd splitwise-clone
   ```

2. Create and activate a virtual environment:
   ```
   python -m venv venv
   source venv/bin/activate  # On Windows: venv\Scripts\activate
   ```

3. Install dependencies:
   ```
   pip install -r requirements.txt
   ```

4. Run the application:
   ```
   python app.py
   ```

5. Open your browser and navigate to:
   ```
   http://127.0.0.1:5000/
   ```

## Current Status

This is a basic implementation with the following functionality:
- Database models for users, groups, expenses, and settlements
- Sample data generation for testing
- Basic views to display users, groups, and group details

## Future Enhancements

- User authentication
- Forms for creating groups and adding expenses
- Balance calculations
- Settlement tracking
- Improved UI/UX

## License

MIT
