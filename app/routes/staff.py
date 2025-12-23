from flask import Blueprint, request, jsonify
from flask_jwt_extended import jwt_required, get_jwt_identity
from app import db
from app.models.staff import Staff
import bcrypt

staff_bp = Blueprint('staff', __name__)


@staff_bp.route('', methods=['GET'])
@jwt_required()
def get_staff_list():
    """获取员工列表"""
    page = request.args.get('page', 1, type=int)
    page_size = request.args.get('pageSize', 10, type=int)
    keyword = request.args.get('keyword', '')
    role = request.args.get('role', '')
    status = request.args.get('status', type=int)

    query = Staff.query

    if keyword:
        query = query.filter(
            (Staff.username.like(f'%{keyword}%')) | 
            (Staff.name.like(f'%{keyword}%')) |
            (Staff.phone.like(f'%{keyword}%'))
        )
    if role:
        query = query.filter_by(role=role)
    if status is not None:
        query = query.filter_by(status=status)

    total = query.count()
    staff_list = query.order_by(Staff.created_at.desc()).offset((page - 1) * page_size).limit(page_size).all()

    return jsonify({
        'code': 200,
        'data': {
            'list': [s.to_dict() for s in staff_list],
            'total': total,
            'page': page,
            'pageSize': page_size
        }
    })


@staff_bp.route('/<int:id>', methods=['GET'])
@jwt_required()
def get_staff(id):
    """获取员工详情"""
    staff = Staff.query.get(id)
    if not staff:
        return jsonify({'code': 404, 'message': '员工不存在'}), 404

    return jsonify({
        'code': 200,
        'data': staff.to_dict()
    })


@staff_bp.route('', methods=['POST'])
@jwt_required()
def create_staff():
    """创建员工"""
    data = request.get_json()

    # 检查用户名是否已存在
    if Staff.query.filter_by(username=data.get('username')).first():
        return jsonify({'code': 400, 'message': '用户名已存在'}), 400

    # 加密密码
    password = data.get('password', '123456')
    hashed = bcrypt.hashpw(password.encode('utf-8'), bcrypt.gensalt())

    staff = Staff(
        username=data.get('username'),
        password=hashed.decode('utf-8'),
        name=data.get('name'),
        phone=data.get('phone'),
        avatar=data.get('avatar'),
        role=data.get('role', 'cashier'),
        status=data.get('status', 1)
    )
    db.session.add(staff)
    db.session.commit()

    return jsonify({
        'code': 200,
        'message': '创建成功',
        'data': staff.to_dict()
    })


@staff_bp.route('/<int:id>', methods=['PUT'])
@jwt_required()
def update_staff(id):
    """更新员工"""
    staff = Staff.query.get(id)
    if not staff:
        return jsonify({'code': 404, 'message': '员工不存在'}), 404

    data = request.get_json()

    # 检查用户名是否重复
    if data.get('username') and data['username'] != staff.username:
        if Staff.query.filter_by(username=data['username']).first():
            return jsonify({'code': 400, 'message': '用户名已存在'}), 400
        staff.username = data['username']

    if data.get('name'):
        staff.name = data['name']
    if data.get('phone') is not None:
        staff.phone = data['phone']
    if data.get('avatar') is not None:
        staff.avatar = data['avatar']
    if data.get('role'):
        staff.role = data['role']
    if data.get('status') is not None:
        staff.status = data['status']
    if data.get('password'):
        hashed = bcrypt.hashpw(data['password'].encode('utf-8'), bcrypt.gensalt())
        staff.password = hashed.decode('utf-8')

    db.session.commit()

    return jsonify({
        'code': 200,
        'message': '更新成功',
        'data': staff.to_dict()
    })


@staff_bp.route('/<int:id>', methods=['DELETE'])
@jwt_required()
def delete_staff(id):
    """删除员工"""
    staff = Staff.query.get(id)
    if not staff:
        return jsonify({'code': 404, 'message': '员工不存在'}), 404

    db.session.delete(staff)
    db.session.commit()

    return jsonify({
        'code': 200,
        'message': '删除成功'
    })

