from flask import Blueprint, request, jsonify
from flask_jwt_extended import jwt_required, get_jwt_identity
from app import db
from app.models.order import Order, OrderItem, OrderPayment
from app.models.product import Product
from app.models.customer import Customer
from app.models.inventory import Inventory
from datetime import datetime
import uuid

order_bp = Blueprint('order', __name__)


@order_bp.route('', methods=['GET'])
@jwt_required()
def get_order_list():
    """获取订单列表"""
    page = request.args.get('page', 1, type=int)
    page_size = request.args.get('pageSize', 10, type=int)
    keyword = request.args.get('keyword', '')
    status = request.args.get('status', '')
    payment_status = request.args.get('paymentStatus', '')

    query = Order.query

    if keyword:
        query = query.filter(
            (Order.order_no.like(f'%{keyword}%')) | 
            (Order.customer_name.like(f'%{keyword}%'))
        )
    if status:
        query = query.filter_by(status=status)
    if payment_status:
        query = query.filter_by(payment_status=payment_status)

    total = query.count()
    orders = query.order_by(Order.created_at.desc()).offset((page - 1) * page_size).limit(page_size).all()

    return jsonify({
        'code': 200,
        'data': {
            'list': [o.to_dict() for o in orders],
            'total': total,
            'page': page,
            'pageSize': page_size
        }
    })


@order_bp.route('/<int:id>', methods=['GET'])
@jwt_required()
def get_order(id):
    """获取订单详情"""
    order = Order.query.get(id)
    if not order:
        return jsonify({'code': 404, 'message': '订单不存在'}), 404

    # 获取订单明细
    items = OrderItem.query.filter_by(order_id=id).all()
    # 获取支付记录
    payments = OrderPayment.query.filter_by(order_id=id).all()

    data = order.to_dict()
    data['items'] = [i.to_dict() for i in items]
    data['payments'] = [p.to_dict() for p in payments]

    return jsonify({
        'code': 200,
        'data': data
    })


@order_bp.route('', methods=['POST'])
@jwt_required()
def create_order():
    """创建订单"""
    current_user = get_jwt_identity()
    data = request.get_json()

    # 生成订单号
    order_no = f"ORD{datetime.now().strftime('%Y%m%d%H%M%S')}{str(uuid.uuid4())[:4].upper()}"

    # 获取客户信息
    customer_name = None
    if data.get('customerId'):
        customer = Customer.query.get(data['customerId'])
        customer_name = customer.name if customer else None

    # 创建订单
    order = Order(
        order_no=order_no,
        customer_id=data.get('customerId'),
        customer_name=customer_name,
        total_quantity=0,
        total_weight=0,
        total_amount=0,
        discount_amount=data.get('discountAmount', 0),
        actual_amount=0,
        payment_method=data.get('paymentMethod', 'cash'),
        payment_status='unpaid',
        status='pending',
        remark=data.get('remark'),
        operator_id=current_user['id'],
        order_at=datetime.now()
    )
    db.session.add(order)
    db.session.flush()

    # 创建订单明细
    total_quantity = 0
    total_weight = 0
    total_amount = 0

    for item_data in data.get('items', []):
        product = Product.query.get(item_data.get('productId'))
        if not product:
            continue

        quantity = item_data.get('quantity', 0)
        weight = item_data.get('weight', 0)
        unit_price = item_data.get('unitPrice', float(product.price))
        amount = quantity * unit_price if product.unit == '只' else weight * unit_price

        item = OrderItem(
            order_id=order.id,
            product_id=product.id,
            product_name=product.name,
            unit=product.unit,
            quantity=quantity,
            weight=weight,
            unit_price=unit_price,
            amount=amount
        )
        db.session.add(item)

        total_quantity += quantity
        total_weight += weight or 0
        total_amount += amount

        # 扣减库存
        inventory = Inventory.query.filter_by(product_id=product.id).first()
        if inventory:
            inventory.quantity = max(0, inventory.quantity - quantity)

    # 更新订单汇总
    order.total_quantity = total_quantity
    order.total_weight = total_weight
    order.total_amount = total_amount
    order.actual_amount = total_amount - (data.get('discountAmount') or 0)

    db.session.commit()

    return jsonify({
        'code': 200,
        'message': '创建成功',
        'data': order.to_dict()
    })


@order_bp.route('/<int:id>/pay', methods=['POST'])
@jwt_required()
def pay_order(id):
    """订单支付"""
    current_user = get_jwt_identity()
    order = Order.query.get(id)
    if not order:
        return jsonify({'code': 404, 'message': '订单不存在'}), 404

    data = request.get_json()
    amount = data.get('amount', 0)

    # 创建支付记录
    payment = OrderPayment(
        order_id=id,
        payment_method=data.get('paymentMethod', 'cash'),
        amount=amount,
        received_amount=data.get('receivedAmount'),
        change_amount=data.get('changeAmount'),
        transaction_no=data.get('transactionNo'),
        operator_id=current_user['id'],
        paid_at=datetime.now()
    )
    db.session.add(payment)

    # 更新订单支付状态
    order.paid_amount = float(order.paid_amount or 0) + amount
    if order.paid_amount >= float(order.actual_amount):
        order.payment_status = 'paid'
        order.status = 'completed'
        order.completed_at = datetime.now()
    else:
        order.payment_status = 'partial'

    db.session.commit()

    return jsonify({
        'code': 200,
        'message': '支付成功',
        'data': order.to_dict()
    })


@order_bp.route('/<int:id>/cancel', methods=['POST'])
@jwt_required()
def cancel_order(id):
    """取消订单"""
    order = Order.query.get(id)
    if not order:
        return jsonify({'code': 404, 'message': '订单不存在'}), 404

    if order.status == 'completed':
        return jsonify({'code': 400, 'message': '已完成的订单无法取消'}), 400

    # 恢复库存
    items = OrderItem.query.filter_by(order_id=id).all()
    for item in items:
        inventory = Inventory.query.filter_by(product_id=item.product_id).first()
        if inventory:
            inventory.quantity += item.quantity

    order.status = 'cancelled'
    db.session.commit()

    return jsonify({
        'code': 200,
        'message': '订单已取消'
    })

