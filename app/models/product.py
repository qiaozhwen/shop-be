from app import db
from datetime import datetime


class Product(db.Model):
    """商品表"""
    __tablename__ = 'product'

    id = db.Column(db.BigInteger, primary_key=True, autoincrement=True)
    category_id = db.Column(db.BigInteger, comment='分类ID')
    code = db.Column(db.String(50), unique=True, comment='商品编码')
    name = db.Column(db.String(100), nullable=False, comment='商品名称')
    unit = db.Column(db.String(20), nullable=False, default='只', comment='单位: 只/斤')
    price = db.Column(db.Numeric(10, 2), nullable=False, comment='销售单价')
    cost_price = db.Column(db.Numeric(10, 2), comment='成本价')
    weight_avg = db.Column(db.Numeric(10, 2), comment='平均重量(斤)')
    image_url = db.Column(db.String(255), comment='商品图片')
    description = db.Column(db.Text, comment='商品描述')
    min_stock = db.Column(db.Integer, nullable=False, default=0, comment='最低库存预警值')
    is_active = db.Column(db.SmallInteger, nullable=False, default=1, comment='状态: 0-下架, 1-上架')
    sku = db.Column(db.String(50), comment='SKU')
    created_at = db.Column(db.DateTime, nullable=False, default=datetime.now)
    updated_at = db.Column(db.DateTime, nullable=False, default=datetime.now, onupdate=datetime.now)

    def to_dict(self):
        return {
            'id': self.id,
            'categoryId': self.category_id,
            'code': self.code,
            'name': self.name,
            'unit': self.unit,
            'price': float(self.price) if self.price else 0,
            'costPrice': float(self.cost_price) if self.cost_price else None,
            'weightAvg': float(self.weight_avg) if self.weight_avg else None,
            'imageUrl': self.image_url,
            'description': self.description,
            'minStock': self.min_stock,
            'isActive': self.is_active,
            'sku': self.sku,
            'createdAt': self.created_at.isoformat() if self.created_at else None,
        }

