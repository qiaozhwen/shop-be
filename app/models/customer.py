from app import db
from datetime import datetime


class Customer(db.Model):
    """客户表"""
    __tablename__ = 'customer'

    id = db.Column(db.BigInteger, primary_key=True, autoincrement=True)
    name = db.Column(db.String(100), nullable=False, comment='客户名称')
    type = db.Column(db.Enum('restaurant', 'retail', 'wholesale', 'personal'),
                     nullable=False, default='restaurant', comment='客户类型')
    level = db.Column(db.Enum('normal', 'vip', 'svip'),
                      nullable=False, default='normal', comment='客户等级')
    contact_name = db.Column(db.String(50), comment='联系人')
    phone = db.Column(db.String(20), nullable=False, comment='联系电话')
    address = db.Column(db.String(255), comment='地址')
    credit_limit = db.Column(db.Numeric(10, 2), default=0, comment='赊账额度')
    credit_balance = db.Column(db.Numeric(10, 2), default=0, comment='当前欠款')
    total_orders = db.Column(db.Integer, default=0, comment='累计订单数')
    total_amount = db.Column(db.Numeric(12, 2), default=0, comment='累计消费金额')
    last_order_at = db.Column(db.DateTime, comment='最后下单时间')
    remark = db.Column(db.String(500), comment='备注')
    status = db.Column(db.SmallInteger, nullable=False, default=1, comment='状态')
    created_at = db.Column(db.DateTime, nullable=False, default=datetime.now)
    updated_at = db.Column(db.DateTime, nullable=False, default=datetime.now, onupdate=datetime.now)

    def to_dict(self):
        return {
            'id': self.id,
            'name': self.name,
            'type': self.type,
            'level': self.level,
            'contactName': self.contact_name,
            'phone': self.phone,
            'address': self.address,
            'creditLimit': float(self.credit_limit) if self.credit_limit else 0,
            'creditBalance': float(self.credit_balance) if self.credit_balance else 0,
            'totalOrders': self.total_orders,
            'totalAmount': float(self.total_amount) if self.total_amount else 0,
            'lastOrderAt': self.last_order_at.isoformat() if self.last_order_at else None,
            'remark': self.remark,
            'status': self.status,
            'createdAt': self.created_at.isoformat() if self.created_at else None,
        }


class CustomerCreditLog(db.Model):
    """客户欠款记录表"""
    __tablename__ = 'customer_credit_log'

    id = db.Column(db.BigInteger, primary_key=True, autoincrement=True)
    customer_id = db.Column(db.BigInteger, nullable=False, comment='客户ID')
    type = db.Column(db.Enum('credit', 'repay'), nullable=False, comment='类型')
    amount = db.Column(db.Numeric(10, 2), nullable=False, comment='金额')
    order_id = db.Column(db.BigInteger, comment='关联订单ID')
    balance_before = db.Column(db.Numeric(10, 2), nullable=False, comment='变动前余额')
    balance_after = db.Column(db.Numeric(10, 2), nullable=False, comment='变动后余额')
    remark = db.Column(db.String(255), comment='备注')
    operator_id = db.Column(db.BigInteger, nullable=False, comment='操作人ID')
    created_at = db.Column(db.DateTime, nullable=False, default=datetime.now)

    def to_dict(self):
        return {
            'id': self.id,
            'customerId': self.customer_id,
            'type': self.type,
            'amount': float(self.amount) if self.amount else 0,
            'orderId': self.order_id,
            'balanceBefore': float(self.balance_before) if self.balance_before else 0,
            'balanceAfter': float(self.balance_after) if self.balance_after else 0,
            'remark': self.remark,
            'operatorId': self.operator_id,
            'createdAt': self.created_at.isoformat() if self.created_at else None,
        }

