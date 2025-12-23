from flask import Blueprint, request, jsonify
from flask_jwt_extended import jwt_required, get_jwt_identity
from app import db
from app.models.purchase import PurchaseOrder, PurchaseOrderItem
from app.models.product import Product
from app.models.supplier import Supplier
from app.models.inventory import Inventory
from datetime import datetime
import uuid

purchase_bp = Blueprint('purchase', __name__)


@purchase_bp.route('', methods=['GET'])
@jwt_required()
def get_purchase_list():
    """获取采购订单列表"""
    page = request.args.get('page', 1, type=int)
    page_size = request.args.get('pageSize', 10, type=int)
    status = request.args.get('status', '')
    payment_status = request.args.get('paymentStatus', '')

    query = PurchaseOrder.query

    if status:
        query = query.filter_by(status=status)
    if payment_status:
        query = query.filter_by(payment_status=payment_status)

    total = query.count()
    purchases = query.order_by(PurchaseOrder.created_at.desc()).offset((page - 1) * page_size).limit(page_size).all()

    # 添加供应商名称
    result = []
    for p in purchases:
        data = p.to_dict()
        supplier = Supplier.query.get(p.supplier_id)
        data['supplierName'] = supplier.name if supplier else None
        result.append(data)

    return jsonify({
        'code': 200,
        'data': {
            'list': result,
            'total': total,
            'page': page,
            'pageSize': page_size
        }
    })


@purchase_bp.route('/<int:id>', methods=['GET'])
@jwt_required()
def get_purchase(id):
    """获取采购订单详情"""
    purchase = PurchaseOrder.query.get(id)
    if not purchase:
        return jsonify({'code': 404, 'message': '采购订单不存在'}), 404

    items = PurchaseOrderItem.query.filter_by(purchase_id=id).all()
    supplier = Supplier.query.get(purchase.supplier_id)

    data = purchase.to_dict()
    data['supplierName'] = supplier.name if supplier else None
    data['items'] = [i.to_dict() for i in items]

    return jsonify({
        'code': 200,
        'data': data
    })


@purchase_bp.route('', methods=['POST'])
@jwt_required()
def create_purchase():
    """创建采购订单"""
    current_user = get_jwt_identity()
    data = request.get_json()

    # 生成采购单号
    purchase_no = f"PO{datetime.now().strftime('%Y%m%d%H%M%S')}{str(uuid.uuid4())[:4].upper()}"

    purchase = PurchaseOrder(
        purchase_no=purchase_no,
        supplier_id=data.get('supplierId'),
        total_quantity=0,
        total_weight=0,
        total_amount=0,
        payment_status='unpaid',
        status='pending',
        expected_at=data.get('expectedAt'),
        remark=data.get('remark'),
        operator_id=current_user['id']
    )
    db.session.add(purchase)
    db.session.flush()

    # 创建采购明细
    total_quantity = 0
    total_weight = 0
    total_amount = 0

    for item_data in data.get('items', []):
        product = Product.query.get(item_data.get('productId'))
        if not product:
            continue

        quantity = item_data.get('quantity', 0)
        weight = item_data.get('weight', 0)
        unit_price = item_data.get('unitPrice', 0)
        amount = quantity * unit_price if product.unit == '只' else weight * unit_price

        item = PurchaseOrderItem(
            purchase_id=purchase.id,
            product_id=product.id,
            product_name=product.name,
            quantity=quantity,
            weight=weight,
            unit_price=unit_price,
            amount=amount
        )
        db.session.add(item)

        total_quantity += quantity
        total_weight += weight or 0
        total_amount += amount

    purchase.total_quantity = total_quantity
    purchase.total_weight = total_weight
    purchase.total_amount = total_amount

    db.session.commit()

    return jsonify({
        'code': 200,
        'message': '创建成功',
        'data': purchase.to_dict()
    })


@purchase_bp.route('/<int:id>/receive', methods=['POST'])
@jwt_required()
def receive_purchase(id):
    """采购收货"""
    purchase = PurchaseOrder.query.get(id)
    if not purchase:
        return jsonify({'code': 404, 'message': '采购订单不存在'}), 404

    # 更新库存
    items = PurchaseOrderItem.query.filter_by(purchase_id=id).all()
    for item in items:
        inventory = Inventory.query.filter_by(product_id=item.product_id).first()
        if inventory:
            inventory.quantity += item.quantity
            if item.weight:
                inventory.total_weight = float(inventory.total_weight or 0) + float(item.weight)
        else:
            inventory = Inventory(
                product_id=item.product_id,
                quantity=item.quantity,
                total_weight=item.weight or 0
            )
            db.session.add(inventory)

        item.received_quantity = item.quantity

    purchase.status = 'received'
    purchase.received_at = datetime.now()
    db.session.commit()

    return jsonify({
        'code': 200,
        'message': '收货成功',
        'data': purchase.to_dict()
    })


@purchase_bp.route('/<int:id>/cancel', methods=['POST'])
@jwt_required()
def cancel_purchase(id):
    """取消采购订单"""
    purchase = PurchaseOrder.query.get(id)
    if not purchase:
        return jsonify({'code': 404, 'message': '采购订单不存在'}), 404

    if purchase.status == 'received':
        return jsonify({'code': 400, 'message': '已收货的订单无法取消'}), 400

    purchase.status = 'cancelled'
    db.session.commit()

    return jsonify({
        'code': 200,
        'message': '订单已取消'
    })

