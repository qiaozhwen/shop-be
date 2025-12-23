from app import db
from datetime import datetime


class Supplier(db.Model):
    """供应商表"""
    __tablename__ = 'supplier'

    id = db.Column(db.BigInteger, primary_key=True, autoincrement=True)
    name = db.Column(db.String(100), nullable=False, comment='供应商名称')
    contact_name = db.Column(db.String(50), comment='联系人')
    phone = db.Column(db.String(20), nullable=False, comment='联系电话')
    address = db.Column(db.String(255), comment='地址')
    bank_name = db.Column(db.String(100), comment='开户银行')
    bank_account = db.Column(db.String(50), comment='银行账号')
    supply_products = db.Column(db.String(255), comment='主营商品')
    total_purchase = db.Column(db.Numeric(12, 2), default=0, comment='累计采购金额')
    unpaid_amount = db.Column(db.Numeric(10, 2), default=0, comment='待付款金额')
    rating = db.Column(db.SmallInteger, default=5, comment='评分: 1-5')
    remark = db.Column(db.String(500), comment='备注')
    status = db.Column(db.SmallInteger, nullable=False, default=1, comment='状态')
    created_at = db.Column(db.DateTime, nullable=False, default=datetime.now)
    updated_at = db.Column(db.DateTime, nullable=False, default=datetime.now, onupdate=datetime.now)

    def to_dict(self):
        return {
            'id': self.id,
            'name': self.name,
            'contactName': self.contact_name,
            'phone': self.phone,
            'address': self.address,
            'bankName': self.bank_name,
            'bankAccount': self.bank_account,
            'supplyProducts': self.supply_products,
            'totalPurchase': float(self.total_purchase) if self.total_purchase else 0,
            'unpaidAmount': float(self.unpaid_amount) if self.unpaid_amount else 0,
            'rating': self.rating,
            'remark': self.remark,
            'status': self.status,
            'createdAt': self.created_at.isoformat() if self.created_at else None,
        }

