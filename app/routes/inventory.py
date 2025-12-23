from flask import Blueprint, request, jsonify
from flask_jwt_extended import jwt_required, get_jwt_identity
from app import db
from app.models.inventory import Inventory, InventoryInbound, InventoryOutbound
from app.models.product import Product
from datetime import datetime
import uuid

inventory_bp = Blueprint('inventory', __name__)


@inventory_bp.route('', methods=['GET'])
@jwt_required()
def get_inventory_list():
    """获取库存列表"""
    page = request.args.get('page', 1, type=int)
    page_size = request.args.get('pageSize', 10, type=int)
    keyword = request.args.get('keyword', '')
    low_stock = request.args.get('lowStock', type=int)

    query = db.session.query(Inventory, Product).join(
        Product, Inventory.product_id == Product.id
    )

    if keyword:
        query = query.filter(Product.name.like(f'%{keyword}%'))
    if low_stock == 1:
        query = query.filter(Inventory.quantity <= Inventory.min_quantity)

    total = query.count()
    items = query.offset((page - 1) * page_size).limit(page_size).all()

    result = []
    for inv, prod in items:
        data = inv.to_dict()
        data['productName'] = prod.name
        data['productCode'] = prod.code
        data['unit'] = prod.unit
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


@inventory_bp.route('/inbound', methods=['POST'])
@jwt_required()
def create_inbound():
    """创建入库记录"""
    current_user = get_jwt_identity()
    data = request.get_json()

    # 生成入库单号
    inbound_no = f"IN{datetime.now().strftime('%Y%m%d%H%M%S')}{str(uuid.uuid4())[:4].upper()}"

    inbound = InventoryInbound(
        inbound_no=inbound_no,
        supplier_id=data.get('supplierId'),
        product_id=data.get('productId'),
        quantity=data.get('quantity'),
        weight=data.get('weight'),
        unit_price=data.get('unitPrice'),
        total_amount=data.get('totalAmount'),
        batch_no=data.get('batchNo'),
        type=data.get('type', 'purchase'),
        remark=data.get('remark'),
        operator_id=current_user['id'],
        inbound_at=datetime.now()
    )
    db.session.add(inbound)

    # 更新库存
    inventory = Inventory.query.filter_by(product_id=data.get('productId')).first()
    if inventory:
        inventory.quantity += data.get('quantity', 0)
        if data.get('weight'):
            inventory.total_weight = float(inventory.total_weight or 0) + float(data.get('weight', 0))
    else:
        inventory = Inventory(
            product_id=data.get('productId'),
            quantity=data.get('quantity', 0),
            total_weight=data.get('weight', 0)
        )
        db.session.add(inventory)

    db.session.commit()

    return jsonify({
        'code': 200,
        'message': '入库成功',
        'data': inbound.to_dict()
    })


@inventory_bp.route('/outbound', methods=['POST'])
@jwt_required()
def create_outbound():
    """创建出库记录"""
    current_user = get_jwt_identity()
    data = request.get_json()

    # 检查库存
    inventory = Inventory.query.filter_by(product_id=data.get('productId')).first()
    if not inventory or inventory.quantity < data.get('quantity', 0):
        return jsonify({'code': 400, 'message': '库存不足'}), 400

    # 生成出库单号
    outbound_no = f"OUT{datetime.now().strftime('%Y%m%d%H%M%S')}{str(uuid.uuid4())[:4].upper()}"

    outbound = InventoryOutbound(
        outbound_no=outbound_no,
        type=data.get('type', 'sale'),
        order_id=data.get('orderId'),
        product_id=data.get('productId'),
        quantity=data.get('quantity'),
        weight=data.get('weight'),
        reason=data.get('reason'),
        operator_id=current_user['id'],
        outbound_at=datetime.now()
    )
    db.session.add(outbound)

    # 更新库存
    inventory.quantity -= data.get('quantity', 0)
    if data.get('weight'):
        inventory.total_weight = max(0, float(inventory.total_weight or 0) - float(data.get('weight', 0)))

    db.session.commit()

    return jsonify({
        'code': 200,
        'message': '出库成功',
        'data': outbound.to_dict()
    })


@inventory_bp.route('/inbound', methods=['GET'])
@jwt_required()
def get_inbound_list():
    """获取入库记录列表"""
    page = request.args.get('page', 1, type=int)
    page_size = request.args.get('pageSize', 10, type=int)

    query = InventoryInbound.query
    total = query.count()
    items = query.order_by(InventoryInbound.created_at.desc()).offset((page - 1) * page_size).limit(page_size).all()

    return jsonify({
        'code': 200,
        'data': {
            'list': [i.to_dict() for i in items],
            'total': total,
            'page': page,
            'pageSize': page_size
        }
    })


@inventory_bp.route('/outbound', methods=['GET'])
@jwt_required()
def get_outbound_list():
    """获取出库记录列表"""
    page = request.args.get('page', 1, type=int)
    page_size = request.args.get('pageSize', 10, type=int)

    query = InventoryOutbound.query
    total = query.count()
    items = query.order_by(InventoryOutbound.created_at.desc()).offset((page - 1) * page_size).limit(page_size).all()

    return jsonify({
        'code': 200,
        'data': {
            'list': [i.to_dict() for i in items],
            'total': total,
            'page': page,
            'pageSize': page_size
        }
    })

