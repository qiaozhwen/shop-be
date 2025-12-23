from flask import Blueprint, request, jsonify
from flask_jwt_extended import jwt_required
from app import db
from app.models.category import Category

category_bp = Blueprint('category', __name__)


@category_bp.route('', methods=['GET'])
@jwt_required()
def get_category_list():
    """获取分类列表"""
    status = request.args.get('status', type=int)
    
    query = Category.query
    if status is not None:
        query = query.filter_by(status=status)
    
    categories = query.order_by(Category.sort.asc()).all()
    
    return jsonify({
        'code': 200,
        'data': [c.to_dict() for c in categories]
    })


@category_bp.route('/<int:id>', methods=['GET'])
@jwt_required()
def get_category(id):
    """获取分类详情"""
    category = Category.query.get(id)
    if not category:
        return jsonify({'code': 404, 'message': '分类不存在'}), 404

    return jsonify({
        'code': 200,
        'data': category.to_dict()
    })


@category_bp.route('', methods=['POST'])
@jwt_required()
def create_category():
    """创建分类"""
    data = request.get_json()

    category = Category(
        name=data.get('name'),
        icon=data.get('icon'),
        sort=data.get('sort', 0),
        status=data.get('status', 1)
    )
    db.session.add(category)
    db.session.commit()

    return jsonify({
        'code': 200,
        'message': '创建成功',
        'data': category.to_dict()
    })


@category_bp.route('/<int:id>', methods=['PUT'])
@jwt_required()
def update_category(id):
    """更新分类"""
    category = Category.query.get(id)
    if not category:
        return jsonify({'code': 404, 'message': '分类不存在'}), 404

    data = request.get_json()

    if data.get('name'):
        category.name = data['name']
    if data.get('icon') is not None:
        category.icon = data['icon']
    if data.get('sort') is not None:
        category.sort = data['sort']
    if data.get('status') is not None:
        category.status = data['status']

    db.session.commit()

    return jsonify({
        'code': 200,
        'message': '更新成功',
        'data': category.to_dict()
    })


@category_bp.route('/<int:id>', methods=['DELETE'])
@jwt_required()
def delete_category(id):
    """删除分类"""
    category = Category.query.get(id)
    if not category:
        return jsonify({'code': 404, 'message': '分类不存在'}), 404

    db.session.delete(category)
    db.session.commit()

    return jsonify({
        'code': 200,
        'message': '删除成功'
    })

