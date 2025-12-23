from app import db
from datetime import datetime


class FinanceRecord(db.Model):
    """财务流水表"""
    __tablename__ = 'finance_record'

    id = db.Column(db.BigInteger, primary_key=True, autoincrement=True)
    record_no = db.Column(db.String(50), unique=True, nullable=False, comment='流水号')
    type = db.Column(db.Enum('income', 'expense'), nullable=False, comment='类型')
    category = db.Column(db.Enum('sale', 'purchase', 'customer_repay', 'supplier_pay',
                                 'salary', 'rent', 'utility', 'other'),
                         nullable=False, comment='分类')
    amount = db.Column(db.Numeric(10, 2), nullable=False, comment='金额')
    payment_method = db.Column(db.String(20), comment='支付方式')
    related_type = db.Column(db.String(50), comment='关联类型')
    related_id = db.Column(db.BigInteger, comment='关联ID')
    description = db.Column(db.String(255), comment='描述')
    remark = db.Column(db.String(500), comment='备注')
    operator_id = db.Column(db.BigInteger, nullable=False, comment='操作人ID')
    record_at = db.Column(db.Date, nullable=False, comment='记账日期')
    created_at = db.Column(db.DateTime, nullable=False, default=datetime.now)

    def to_dict(self):
        return {
            'id': self.id,
            'recordNo': self.record_no,
            'type': self.type,
            'category': self.category,
            'amount': float(self.amount) if self.amount else 0,
            'paymentMethod': self.payment_method,
            'relatedType': self.related_type,
            'relatedId': self.related_id,
            'description': self.description,
            'remark': self.remark,
            'operatorId': self.operator_id,
            'recordAt': self.record_at.isoformat() if self.record_at else None,
            'createdAt': self.created_at.isoformat() if self.created_at else None,
        }


class DailySettlement(db.Model):
    """日结算表"""
    __tablename__ = 'daily_settlement'

    id = db.Column(db.BigInteger, primary_key=True, autoincrement=True)
    settle_date = db.Column(db.Date, unique=True, nullable=False, comment='结算日期')
    total_orders = db.Column(db.Integer, default=0, comment='订单数')
    total_sales = db.Column(db.Numeric(12, 2), default=0, comment='销售总额')
    cash_amount = db.Column(db.Numeric(10, 2), default=0, comment='现金收入')
    wechat_amount = db.Column(db.Numeric(10, 2), default=0, comment='微信收入')
    alipay_amount = db.Column(db.Numeric(10, 2), default=0, comment='支付宝收入')
    card_amount = db.Column(db.Numeric(10, 2), default=0, comment='刷卡收入')
    credit_amount = db.Column(db.Numeric(10, 2), default=0, comment='赊账金额')
    total_expense = db.Column(db.Numeric(10, 2), default=0, comment='支出总额')
    profit = db.Column(db.Numeric(12, 2), default=0, comment='利润')
    operator_id = db.Column(db.BigInteger, comment='结算人ID')
    settled_at = db.Column(db.DateTime, comment='结算时间')
    created_at = db.Column(db.DateTime, nullable=False, default=datetime.now)

    def to_dict(self):
        return {
            'id': self.id,
            'settleDate': self.settle_date.isoformat() if self.settle_date else None,
            'totalOrders': self.total_orders,
            'totalSales': float(self.total_sales) if self.total_sales else 0,
            'cashAmount': float(self.cash_amount) if self.cash_amount else 0,
            'wechatAmount': float(self.wechat_amount) if self.wechat_amount else 0,
            'alipayAmount': float(self.alipay_amount) if self.alipay_amount else 0,
            'cardAmount': float(self.card_amount) if self.card_amount else 0,
            'creditAmount': float(self.credit_amount) if self.credit_amount else 0,
            'totalExpense': float(self.total_expense) if self.total_expense else 0,
            'profit': float(self.profit) if self.profit else 0,
            'operatorId': self.operator_id,
            'settledAt': self.settled_at.isoformat() if self.settled_at else None,
        }

