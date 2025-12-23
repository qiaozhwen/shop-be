from flask import Blueprint, request, jsonify
from flask_jwt_extended import jwt_required
from app import db
from app.models.supplier import Supplier

supplier_bp = Blueprint('supplier', __name__)


@supplier_bp.route('', methods=['GET'])
@jwt_required()
def get_supplier_list():
    """获取供应商列表"""
    page = request.args.get('page', 1, type=int)
    page_size = request.args.get('pageSize', 10, type=int)
    keyword = request.args.get('keyword', '')
    status = request.args.get('status', type=int)

    query = Supplier.query

    if keyword:
        query = query.filter(
            (Supplier.name.like(f'%{keyword}%')) | 
            (Supplier.phone.like(f'%{keyword}%'))
        )
    if status is not None:
        query = query.filter_by(status=status)

    total = query.count()
    suppliers = query.order_by(Supplier.created_at.desc()).offset((page - 1) * page_size).limit(page_size).all()

    return jsonify({
        'code': 200,
        'data': {
            'list': [s.to_dict() for s in suppliers],
            'total': total,
            'page': page,
            'pageSize': page_size
        }
    })


@supplier_bp.route('/<int:id>', methods=['GET'])
@jwt_required()
def get_supplier(id):
    """获取供应商详情"""
    supplier = Supplier.query.get(id)
    if not supplier:
        return jsonify({'code': 404, 'message': '供应商不存在'}), 404

    return jsonify({
        'code': 200,
        'data': supplier.to_dict()
    })


@supplier_bp.route('', methods=['POST'])
@jwt_required()
def create_supplier():
    """创建供应商"""
    data = request.get_json()

    supplier = Supplier(
        name=data.get('name'),
        contact_name=data.get('contactName'),
        phone=data.get('phone'),
        address=data.get('address'),
        bank_name=data.get('bankName'),
        bank_account=data.get('bankAccount'),
        supply_products=data.get('supplyProducts'),
        rating=data.get('rating', 5),
        remark=data.get('remark'),
        status=data.get('status', 1)
    )
    db.session.add(supplier)
    db.session.commit()

    return jsonify({
        'code': 200,
        'message': '创建成功',
        'data': supplier.to_dict()
    })


@supplier_bp.route('/<int:id>', methods=['PUT'])
@jwt_required()
def update_supplier(id):
    """更新供应商"""
    supplier = Supplier.query.get(id)
    if not supplier:
        return jsonify({'code': 404, 'message': '供应商不存在'}), 404

    data = request.get_json()

    if data.get('name'):
        supplier.name = data['name']
    if data.get('contactName') is not None:
        supplier.contact_name = data['contactName']
    if data.get('phone'):
        supplier.phone = data['phone']
    if data.get('address') is not None:
        supplier.address = data['address']
    if data.get('bankName') is not None:
        supplier.bank_name = data['bankName']
    if data.get('bankAccount') is not None:
        supplier.bank_account = data['bankAccount']
    if data.get('supplyProducts') is not None:
        supplier.supply_products = data['supplyProducts']
    if data.get('rating') is not None:
        supplier.rating = data['rating']
    if data.get('remark') is not None:
        supplier.remark = data['remark']
    if data.get('status') is not None:
        supplier.status = data['status']

    db.session.commit()

    return jsonify({
        'code': 200,
        'message': '更新成功',
        'data': supplier.to_dict()
    })


@supplier_bp.route('/<int:id>', methods=['DELETE'])
@jwt_required()
def delete_supplier(id):
    """删除供应商"""
    supplier = Supplier.query.get(id)
    if not supplier:
        return jsonify({'code': 404, 'message': '供应商不存在'}), 404

    db.session.delete(supplier)
    db.session.commit()

    return jsonify({
        'code': 200,
        'message': '删除成功'
    })

