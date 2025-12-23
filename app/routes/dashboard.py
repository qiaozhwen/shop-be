from flask import Blueprint, jsonify
from flask_jwt_extended import jwt_required
from app import db
from app.models.order import Order
from app.models.product import Product
from app.models.customer import Customer
from app.models.inventory import Inventory
from app.models.finance import FinanceRecord
from datetime import datetime, date, timedelta
from sqlalchemy import func

dashboard_bp = Blueprint('dashboard', __name__)


@dashboard_bp.route('/overview', methods=['GET'])
@jwt_required()
def get_overview():
    """获取仪表盘概览数据"""
    today = date.today()
    
    # 今日订单数
    today_orders = Order.query.filter(
        func.date(Order.order_at) == today
    ).count()
    
    # 今日销售额
    today_sales = db.session.query(func.sum(Order.actual_amount)).filter(
        func.date(Order.order_at) == today,
        Order.status != 'cancelled'
    ).scalar() or 0
    
    # 商品总数
    product_count = Product.query.filter_by(is_active=1).count()
    
    # 客户总数
    customer_count = Customer.query.filter_by(status=1).count()
    
    # 库存预警数
    low_stock_count = db.session.query(Inventory).join(
        Product, Inventory.product_id == Product.id
    ).filter(
        Inventory.quantity <= Product.min_stock
    ).count()
    
    # 待处理订单数
    pending_orders = Order.query.filter_by(status='pending').count()

    return jsonify({
        'code': 200,
        'data': {
            'todayOrders': today_orders,
            'todaySales': float(today_sales),
            'productCount': product_count,
            'customerCount': customer_count,
            'lowStockCount': low_stock_count,
            'pendingOrders': pending_orders
        }
    })


@dashboard_bp.route('/sales-trend', methods=['GET'])
@jwt_required()
def get_sales_trend():
    """获取近7天销售趋势"""
    today = date.today()
    result = []
    
    for i in range(6, -1, -1):
        day = today - timedelta(days=i)
        sales = db.session.query(func.sum(Order.actual_amount)).filter(
            func.date(Order.order_at) == day,
            Order.status != 'cancelled'
        ).scalar() or 0
        
        orders = Order.query.filter(
            func.date(Order.order_at) == day,
            Order.status != 'cancelled'
        ).count()
        
        result.append({
            'date': day.isoformat(),
            'sales': float(sales),
            'orders': orders
        })

    return jsonify({
        'code': 200,
        'data': result
    })


@dashboard_bp.route('/top-products', methods=['GET'])
@jwt_required()
def get_top_products():
    """获取热销商品TOP10"""
    from app.models.order import OrderItem
    
    today = date.today()
    start_date = today - timedelta(days=30)
    
    top_products = db.session.query(
        OrderItem.product_id,
        OrderItem.product_name,
        func.sum(OrderItem.quantity).label('total_quantity'),
        func.sum(OrderItem.amount).label('total_amount')
    ).join(
        Order, OrderItem.order_id == Order.id
    ).filter(
        Order.status != 'cancelled',
        func.date(Order.order_at) >= start_date
    ).group_by(
        OrderItem.product_id, OrderItem.product_name
    ).order_by(
        func.sum(OrderItem.quantity).desc()
    ).limit(10).all()
    
    result = []
    for item in top_products:
        result.append({
            'productId': item.product_id,
            'productName': item.product_name,
            'totalQuantity': int(item.total_quantity),
            'totalAmount': float(item.total_amount)
        })

    return jsonify({
        'code': 200,
        'data': result
    })


@dashboard_bp.route('/low-stock', methods=['GET'])
@jwt_required()
def get_low_stock():
    """获取库存预警商品"""
    low_stock_items = db.session.query(Inventory, Product).join(
        Product, Inventory.product_id == Product.id
    ).filter(
        Inventory.quantity <= Product.min_stock
    ).limit(10).all()
    
    result = []
    for inv, prod in low_stock_items:
        result.append({
            'productId': prod.id,
            'productName': prod.name,
            'currentStock': inv.quantity,
            'minStock': prod.min_stock,
            'unit': prod.unit
        })

    return jsonify({
        'code': 200,
        'data': result
    })

