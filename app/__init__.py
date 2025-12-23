from flask import Flask
from flask_sqlalchemy import SQLAlchemy
from flask_jwt_extended import JWTManager
from flask_cors import CORS
from app.config import Config

db = SQLAlchemy()
jwt = JWTManager()


def create_app(config_class=Config):
    app = Flask(__name__)
    app.config.from_object(config_class)

    # 初始化扩展
    db.init_app(app)
    jwt.init_app(app)
    CORS(app)

    # 注册蓝图
    from app.routes.auth import auth_bp
    from app.routes.staff import staff_bp
    from app.routes.category import category_bp
    from app.routes.product import product_bp
    from app.routes.inventory import inventory_bp
    from app.routes.customer import customer_bp
    from app.routes.supplier import supplier_bp
    from app.routes.order import order_bp
    from app.routes.purchase import purchase_bp
    from app.routes.finance import finance_bp
    from app.routes.dashboard import dashboard_bp

    app.register_blueprint(auth_bp, url_prefix='/api/auth')
    app.register_blueprint(staff_bp, url_prefix='/api/staff')
    app.register_blueprint(category_bp, url_prefix='/api/category')
    app.register_blueprint(product_bp, url_prefix='/api/product')
    app.register_blueprint(inventory_bp, url_prefix='/api/inventory')
    app.register_blueprint(customer_bp, url_prefix='/api/customer')
    app.register_blueprint(supplier_bp, url_prefix='/api/supplier')
    app.register_blueprint(order_bp, url_prefix='/api/order')
    app.register_blueprint(purchase_bp, url_prefix='/api/purchase')
    app.register_blueprint(finance_bp, url_prefix='/api/finance')
    app.register_blueprint(dashboard_bp, url_prefix='/api/dashboard')

    return app

