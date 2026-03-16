# Splitwise Clone

[![Test Suite](https://github.com/scalolli/splitwise-clone/actions/workflows/test.yml/badge.svg)](https://github.com/scalolli/splitwise-clone/actions/workflows/test.yml)
An expense sharing application inspired by Splitwise. The active project is now the Kotlin/http4k rewrite; the legacy Flask app remains in the repository as a behavioral reference and is no longer the main delivery track.

## Current Phase

- Active work is the ground-up Kotlin/http4k rewrite in `docs/http4k-rewrite/`
- The next implementation step is `SLICE-001` in `docs/http4k-rewrite/07-handoff.md`
- The Flask app in `app/` is effectively frozen and treated as a reference, not the forward path
- New architecture, backlog, and handoff notes live under `docs/http4k-rewrite/`

## Legacy Flask App Features

- **User Authentication**: Secure login and registration system
- **Group Management**: Create and manage expense sharing groups
- **Expense Tracking**: Add, edit, and track shared expenses
- **Smart Splitting**: Equal and custom expense splitting options
- **Balance Calculation**: Automatic balance tracking and settlement suggestions
- **Comprehensive Validation**: Form validation with detailed error handling

## Legacy Flask Stack

- **Backend**: Flask 2.0.1, SQLAlchemy 1.4.46
- **Database**: SQLite with Flask-SQLAlchemy 2.5.1
- **Testing**: Pytest with comprehensive test coverage
- **CI/CD**: GitHub Actions for automated testing
- **Frontend**: Jinja2 templates with responsive CSS

## Legacy Flask Status

- ✅ **65 tests passing, 1 skipped**
- ✅ **100% success rate on active tests**
- ✅ **Complete expense editing functionality**
- ✅ **Comprehensive form validation**
- ✅ **Automated CI/CD pipeline**

This legacy implementation is retained for behavioral comparison while the Kotlin/http4k rewrite is built.

## Legacy Flask Setup

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

## Legacy Flask Tests

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

## Repository Structure

```
splitwise-fresh/
├── docs/http4k-rewrite/ # Rewrite plan, architecture, backlog, handoff
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

## Rewrite Docs

Start here if you are continuing the rewrite work:

- `docs/http4k-rewrite/README.md` - documentation index
- `docs/http4k-rewrite/07-handoff.md` - current state and exact next action
- `docs/http4k-rewrite/04-iteration-backlog.md` - slice-by-slice delivery order
- `docs/http4k-rewrite/06-decisions.md` - locked architectural decisions

The current next phase is to start `SLICE-001: Gradle project scaffold` and create the first failing test for `GET /health` in the new Kotlin app.

## Legacy Flask Database Schema

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

For active delivery, apply that workflow to the Kotlin/http4k rewrite first. The legacy Flask codebase should only be consulted as a reference unless a rewrite document explicitly says otherwise.

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

## Kotlin Rewrite

This application is being rewritten from scratch in Kotlin using http4k. The Python/Flask implementation is no longer the active product direction; it remains in the repository as a legacy reference while the rewrite becomes the primary codebase.

The rewrite fixes known issues in the Flask app, including missing auth on expense edit, settlements ignored in balance calculation, and Float-based money storage.

See [`docs/http4k-rewrite/`](docs/http4k-rewrite/) for the full plan:
- Charter and scope — `00-charter.md`
- Target architecture — `01-target-architecture.md`
- Behavior spec (routes, auth matrix, validation rules) — `02-behavior-spec.md`
- Slice-by-slice backlog — `04-iteration-backlog.md`
- Architectural decisions — `06-decisions.md`
- Current handoff state (where to pick up) — `07-handoff.md`

## License

This project is developed for educational purposes. Feel free to use and modify as needed.

## Support

If you encounter any issues or have questions, please [open an issue](https://github.com/basavarajkalloli/splitwise-fresh/issues) on GitHub.
