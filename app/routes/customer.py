from flask import Blueprint, request, jsonify
from flask_jwt_extended import jwt_required
from app import db
from app.models.customer import Customer, CustomerCreditLog

customer_bp = Blueprint('customer', __name__)


@customer_bp.route('', methods=['GET'])
@jwt_required()
def get_customer_list():
    """获取客户列表"""
    page = request.args.get('page', 1, type=int)
    page_size = request.args.get('pageSize', 10, type=int)
    keyword = request.args.get('keyword', '')
    customer_type = request.args.get('type', '')
    level = request.args.get('level', '')
    status = request.args.get('status', type=int)

    query = Customer.query

    if keyword:
        query = query.filter(
            (Customer.name.like(f'%{keyword}%')) | 
            (Customer.phone.like(f'%{keyword}%'))
        )
    if customer_type:
        query = query.filter_by(type=customer_type)
    if level:
        query = query.filter_by(level=level)
    if status is not None:
        query = query.filter_by(status=status)

    total = query.count()
    customers = query.order_by(Customer.created_at.desc()).offset((page - 1) * page_size).limit(page_size).all()

    return jsonify({
        'code': 200,
        'data': {
            'list': [c.to_dict() for c in customers],
            'total': total,
            'page': page,
            'pageSize': page_size
        }
    })


@customer_bp.route('/<int:id>', methods=['GET'])
@jwt_required()
def get_customer(id):
    """获取客户详情"""
    customer = Customer.query.get(id)
    if not customer:
        return jsonify({'code': 404, 'message': '客户不存在'}), 404

    return jsonify({
        'code': 200,
        'data': customer.to_dict()
    })


@customer_bp.route('', methods=['POST'])
@jwt_required()
def create_customer():
    """创建客户"""
    data = request.get_json()

    customer = Customer(
        name=data.get('name'),
        type=data.get('type', 'restaurant'),
        level=data.get('level', 'normal'),
        contact_name=data.get('contactName'),
        phone=data.get('phone'),
        address=data.get('address'),
        credit_limit=data.get('creditLimit', 0),
        remark=data.get('remark'),
        status=data.get('status', 1)
    )
    db.session.add(customer)
    db.session.commit()

    return jsonify({
        'code': 200,
        'message': '创建成功',
        'data': customer.to_dict()
    })


@customer_bp.route('/<int:id>', methods=['PUT'])
@jwt_required()
def update_customer(id):
    """更新客户"""
    customer = Customer.query.get(id)
    if not customer:
        return jsonify({'code': 404, 'message': '客户不存在'}), 404

    data = request.get_json()

    if data.get('name'):
        customer.name = data['name']
    if data.get('type'):
        customer.type = data['type']
    if data.get('level'):
        customer.level = data['level']
    if data.get('contactName') is not None:
        customer.contact_name = data['contactName']
    if data.get('phone'):
        customer.phone = data['phone']
    if data.get('address') is not None:
        customer.address = data['address']
    if data.get('creditLimit') is not None:
        customer.credit_limit = data['creditLimit']
    if data.get('remark') is not None:
        customer.remark = data['remark']
    if data.get('status') is not None:
        customer.status = data['status']

    db.session.commit()

    return jsonify({
        'code': 200,
        'message': '更新成功',
        'data': customer.to_dict()
    })


@customer_bp.route('/<int:id>', methods=['DELETE'])
@jwt_required()
def delete_customer(id):
    """删除客户"""
    customer = Customer.query.get(id)
    if not customer:
        return jsonify({'code': 404, 'message': '客户不存在'}), 404

    db.session.delete(customer)
    db.session.commit()

    return jsonify({
        'code': 200,
        'message': '删除成功'
    })


@customer_bp.route('/<int:id>/credit-logs', methods=['GET'])
@jwt_required()
def get_credit_logs(id):
    """获取客户欠款记录"""
    page = request.args.get('page', 1, type=int)
    page_size = request.args.get('pageSize', 10, type=int)

    query = CustomerCreditLog.query.filter_by(customer_id=id)
    total = query.count()
    logs = query.order_by(CustomerCreditLog.created_at.desc()).offset((page - 1) * page_size).limit(page_size).all()

    return jsonify({
        'code': 200,
        'data': {
            'list': [l.to_dict() for l in logs],
            'total': total,
            'page': page,
            'pageSize': page_size
        }
    })

