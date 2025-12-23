from app import db
from datetime import datetime


class Category(db.Model):
    """商品分类表"""
    __tablename__ = 'category'

    id = db.Column(db.BigInteger, primary_key=True, autoincrement=True)
    name = db.Column(db.String(50), nullable=False, comment='分类名称')
    icon = db.Column(db.String(50), comment='图标')
    sort = db.Column(db.Integer, nullable=False, default=0, comment='排序')
    status = db.Column(db.SmallInteger, nullable=False, default=1, comment='状态: 0-禁用, 1-启用')
    created_at = db.Column(db.DateTime, nullable=False, default=datetime.now)
    updated_at = db.Column(db.DateTime, nullable=False, default=datetime.now, onupdate=datetime.now)

    def to_dict(self):
        return {
            'id': self.id,
            'name': self.name,
            'icon': self.icon,
            'sort': self.sort,
            'status': self.status,
            'createdAt': self.created_at.isoformat() if self.created_at else None,
        }

