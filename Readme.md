# Splitwise Clone

![Tests](https://github.com/basavarajkalloli/splitwise-fresh/workflows/Test%20Suite/badge.svg)
[![codecov](https://codecov.io/gh/basavarajkalloli/splitwise-fresh/branch/main/graph/badge.svg)](https://codecov.io/gh/basavarajkalloli/splitwise-fresh)

A comprehensive expense sharing application built with Flask and SQLAlchemy, inspired by Splitwise.

## Features

- **User Authentication**: Secure login and registration system
- **Group Management**: Create and manage expense sharing groups
- **Expense Tracking**: Add, edit, and track shared expenses
- **Smart Splitting**: Equal and custom expense splitting options
- **Balance Calculation**: Automatic balance tracking and settlement suggestions
- **Comprehensive Validation**: Form validation with detailed error handling

## Technologies Used

- **Backend**: Flask 2.0.1, SQLAlchemy 1.4.46
- **Database**: SQLite with Flask-SQLAlchemy 2.5.1
- **Testing**: Pytest with comprehensive test coverage
- **CI/CD**: GitHub Actions for automated testing
- **Frontend**: Jinja2 templates with responsive CSS

## Project Status

- ✅ **65 tests passing, 1 skipped**
- ✅ **100% success rate on active tests**
- ✅ **Complete expense editing functionality**
- ✅ **Comprehensive form validation**
- ✅ **Automated CI/CD pipeline**

## Setup and Installation

### Prerequisites
- Python 3.9+ (tested on 3.9, 3.10, 3.11, 3.12)
- Git

### Local Development Setup

1. **Clone the repository**:
   ```bash
   git clone https://github.com/basavarajkalloli/splitwise-fresh.git
   cd splitwise-fresh
   ```

2. **Create virtual environment**:
   ```bash
   python -m venv venv
   source venv/bin/activate  # On Windows: venv\Scripts\activate
   ```

3. **Install dependencies**:
   ```bash
   pip install -r requirements.txt
   ```

4. **Initialize the database**:
   ```bash
   python run.py
   ```

5. **Run the application**:
   ```bash
   flask run
   ```

6. **Access the application**:
   Open your browser and navigate to `http://localhost:5000`

## Running Tests

### Run all tests:
```bash
python -m pytest
```

### Run with coverage:
```bash
python -m pytest --cov=app --cov-report=term-missing
```

### Run specific test categories:
```bash
# Unit tests only
python -m pytest tests/unit/

# Functional tests only  
python -m pytest tests/functional/

# Specific test file
python -m pytest tests/functional/test_expense_editing.py -v
```

## Project Structure

```
splitwise-fresh/
├── app/
│   ├── forms/           # WTForms validation classes
│   ├── models/          # SQLAlchemy database models
│   ├── routes/          # Flask route handlers
│   ├── services/        # Business logic layer
│   ├── templates/       # Jinja2 HTML templates
│   ├── static/          # CSS, JS, images
│   └── utils/           # Helper utilities
├── tests/
│   ├── functional/      # End-to-end integration tests
│   ├── unit/           # Unit tests for components
│   └── conftest.py     # Pytest configuration and fixtures
├── .github/
│   └── workflows/      # GitHub Actions CI/CD
└── instance/
    └── splitwise.db    # SQLite database file
```

## Database Schema

- **User**: User accounts with authentication
- **Group**: Expense sharing groups with members
- **Expense**: Individual expenses with payer information
- **ExpenseShare**: Split details for each expense
- **Settlement**: Payment tracking between users

## Development Workflow

This project follows **Test-Driven Development (TDD)**:

1. **Write tests first** for any new feature
2. **Implement the feature** to make tests pass
3. **Refactor** while keeping tests green
4. **All changes require tests** - no exceptions

### GitHub Actions CI/CD

The project includes automated testing on:
- Every push to `main` or `develop` branches
- Every pull request to `main` or `develop` branches
- Multiple Python versions (3.9, 3.10, 3.11, 3.12)
- Automatic code coverage reporting

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. **Write tests first** for your changes
4. Implement your feature
5. Ensure all tests pass (`pytest`)
6. Commit your changes (`git commit -m 'Add amazing feature'`)
7. Push to the branch (`git push origin feature/amazing-feature`)
8. Open a Pull Request

## Next Steps

See [`.github/instructions.md`](.github/instructions.md) for detailed development roadmap including:

- **Expense deletion functionality** (HIGH PRIORITY)
- Authentication system improvements
- Enhanced group management
- User profile management
- Settlement system enhancements

## License

This project is developed for educational purposes. Feel free to use and modify as needed.

## Support

If you encounter any issues or have questions, please [open an issue](https://github.com/basavarajkalloli/splitwise-fresh/issues) on GitHub.
