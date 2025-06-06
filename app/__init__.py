from flask import Flask
from flask_sqlalchemy import SQLAlchemy
from config import Config

db = SQLAlchemy()

def create_app(config_class=Config):
    app = Flask(__name__)
    app.config.from_object(config_class)

    db.init_app(app)

    # Import all models so they are registered before create_all
    from app.models import user, group, expense, expense_share, settlement

    with app.app_context():
        db.create_all()  # Make sure tables exist

    # Register blueprints
    from app.routes.main import main_bp
    from app.routes.auth import auth_bp
    from app.routes.groups import groups_bp
    from app.routes.expenses import expenses_bp

    app.register_blueprint(main_bp)
    app.register_blueprint(auth_bp)
    app.register_blueprint(groups_bp)
    app.register_blueprint(expenses_bp)

    return app