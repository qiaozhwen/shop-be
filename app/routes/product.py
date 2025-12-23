from flask import Blueprint, request, jsonify
from flask_jwt_extended import jwt_required
from app import db
from app.models.product import Product
from app.models.category import Category

product_bp = Blueprint('product', __name__)


@product_bp.route('', methods=['GET'])
@jwt_required()
def get_product_list():
    """获取商品列表"""
    page = request.args.get('page', 1, type=int)
    page_size = request.args.get('pageSize', 10, type=int)
    keyword = request.args.get('keyword', '')
    category_id = request.args.get('categoryId', type=int)
    is_active = request.args.get('isActive', type=int)

    query = Product.query

    if keyword:
        query = query.filter(
            (Product.name.like(f'%{keyword}%')) | 
            (Product.code.like(f'%{keyword}%'))
        )
    if category_id:
        query = query.filter_by(category_id=category_id)
    if is_active is not None:
        query = query.filter_by(is_active=is_active)

    total = query.count()
    products = query.order_by(Product.created_at.desc()).offset((page - 1) * page_size).limit(page_size).all()

    # 获取分类信息
    result = []
    for p in products:
        data = p.to_dict()
        if p.category_id:
            category = Category.query.get(p.category_id)
            data['categoryName'] = category.name if category else None
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


@product_bp.route('/<int:id>', methods=['GET'])
@jwt_required()
def get_product(id):
    """获取商品详情"""
    product = Product.query.get(id)
    if not product:
        return jsonify({'code': 404, 'message': '商品不存在'}), 404

    data = product.to_dict()
    if product.category_id:
        category = Category.query.get(product.category_id)
        data['categoryName'] = category.name if category else None

    return jsonify({
        'code': 200,
        'data': data
    })


@product_bp.route('', methods=['POST'])
@jwt_required()
def create_product():
    """创建商品"""
    data = request.get_json()

    # 检查编码是否已存在
    if data.get('code') and Product.query.filter_by(code=data['code']).first():
        return jsonify({'code': 400, 'message': '商品编码已存在'}), 400

    product = Product(
        category_id=data.get('categoryId'),
        code=data.get('code'),
        name=data.get('name'),
        unit=data.get('unit', '只'),
        price=data.get('price'),
        cost_price=data.get('costPrice'),
        weight_avg=data.get('weightAvg'),
        image_url=data.get('imageUrl'),
        description=data.get('description'),
        min_stock=data.get('minStock', 0),
        is_active=data.get('isActive', 1),
        sku=data.get('sku')
    )
    db.session.add(product)
    db.session.commit()

    return jsonify({
        'code': 200,
        'message': '创建成功',
        'data': product.to_dict()
    })


@product_bp.route('/<int:id>', methods=['PUT'])
@jwt_required()
def update_product(id):
    """更新商品"""
    product = Product.query.get(id)
    if not product:
        return jsonify({'code': 404, 'message': '商品不存在'}), 404

    data = request.get_json()

    # 检查编码是否重复
    if data.get('code') and data['code'] != product.code:
        if Product.query.filter_by(code=data['code']).first():
            return jsonify({'code': 400, 'message': '商品编码已存在'}), 400
        product.code = data['code']

    if data.get('categoryId') is not None:
        product.category_id = data['categoryId']
    if data.get('name'):
        product.name = data['name']
    if data.get('unit'):
        product.unit = data['unit']
    if data.get('price') is not None:
        product.price = data['price']
    if data.get('costPrice') is not None:
        product.cost_price = data['costPrice']
    if data.get('weightAvg') is not None:
        product.weight_avg = data['weightAvg']
    if data.get('imageUrl') is not None:
        product.image_url = data['imageUrl']
    if data.get('description') is not None:
        product.description = data['description']
    if data.get('minStock') is not None:
        product.min_stock = data['minStock']
    if data.get('isActive') is not None:
        product.is_active = data['isActive']
    if data.get('sku') is not None:
        product.sku = data['sku']

    db.session.commit()

    return jsonify({
        'code': 200,
        'message': '更新成功',
        'data': product.to_dict()
    })


@product_bp.route('/<int:id>', methods=['DELETE'])
@jwt_required()
def delete_product(id):
    """删除商品"""
    product = Product.query.get(id)
    if not product:
        return jsonify({'code': 404, 'message': '商品不存在'}), 404

    db.session.delete(product)
    db.session.commit()

    return jsonify({
        'code': 200,
        'message': '删除成功'
    })

