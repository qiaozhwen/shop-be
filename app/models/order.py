from app import db
from datetime import datetime


class Order(db.Model):
    """订单主表"""
    __tablename__ = 'order'

    id = db.Column(db.BigInteger, primary_key=True, autoincrement=True)
    order_no = db.Column(db.String(50), unique=True, nullable=False, comment='订单号')
    customer_id = db.Column(db.BigInteger, comment='客户ID')
    customer_name = db.Column(db.String(100), comment='客户名称(冗余)')
    total_quantity = db.Column(db.Integer, nullable=False, default=0, comment='商品总数量')
    total_weight = db.Column(db.Numeric(10, 2), default=0, comment='商品总重量')
    total_amount = db.Column(db.Numeric(10, 2), nullable=False, default=0, comment='订单总金额')
    discount_amount = db.Column(db.Numeric(10, 2), default=0, comment='优惠金额')
    actual_amount = db.Column(db.Numeric(10, 2), nullable=False, default=0, comment='实付金额')
    payment_method = db.Column(db.Enum('cash', 'wechat', 'alipay', 'card', 'credit'),
                               nullable=False, default='cash', comment='支付方式')
    payment_status = db.Column(db.Enum('unpaid', 'partial', 'paid'),
                               nullable=False, default='unpaid', comment='支付状态')
    paid_amount = db.Column(db.Numeric(10, 2), default=0, comment='已付金额')
    status = db.Column(db.Enum('pending', 'processing', 'completed', 'cancelled'),
                       nullable=False, default='pending', comment='订单状态')
    remark = db.Column(db.String(500), comment='备注')
    operator_id = db.Column(db.BigInteger, nullable=False, comment='开单人ID')
    order_at = db.Column(db.DateTime, nullable=False, comment='下单时间')
    completed_at = db.Column(db.DateTime, comment='完成时间')
    created_at = db.Column(db.DateTime, nullable=False, default=datetime.now)
    updated_at = db.Column(db.DateTime, nullable=False, default=datetime.now, onupdate=datetime.now)

    def to_dict(self):
        return {
            'id': self.id,
            'orderNo': self.order_no,
            'customerId': self.customer_id,
            'customerName': self.customer_name,
            'totalQuantity': self.total_quantity,
            'totalWeight': float(self.total_weight) if self.total_weight else 0,
            'totalAmount': float(self.total_amount) if self.total_amount else 0,
            'discountAmount': float(self.discount_amount) if self.discount_amount else 0,
            'actualAmount': float(self.actual_amount) if self.actual_amount else 0,
            'paymentMethod': self.payment_method,
            'paymentStatus': self.payment_status,
            'paidAmount': float(self.paid_amount) if self.paid_amount else 0,
            'status': self.status,
            'remark': self.remark,
            'operatorId': self.operator_id,
            'orderAt': self.order_at.isoformat() if self.order_at else None,
            'completedAt': self.completed_at.isoformat() if self.completed_at else None,
            'createdAt': self.created_at.isoformat() if self.created_at else None,
        }


class OrderItem(db.Model):
    """订单明细表"""
    __tablename__ = 'order_item'

    id = db.Column(db.BigInteger, primary_key=True, autoincrement=True)
    order_id = db.Column(db.BigInteger, nullable=False, comment='订单ID')
    product_id = db.Column(db.BigInteger, nullable=False, comment='商品ID')
    product_name = db.Column(db.String(100), nullable=False, comment='商品名称(冗余)')
    unit = db.Column(db.String(20), nullable=False, default='只', comment='单位')
    quantity = db.Column(db.Integer, nullable=False, comment='数量')
    weight = db.Column(db.Numeric(10, 2), comment='重量(斤)')
    unit_price = db.Column(db.Numeric(10, 2), nullable=False, comment='单价')
    amount = db.Column(db.Numeric(10, 2), nullable=False, comment='小计金额')
    created_at = db.Column(db.DateTime, nullable=False, default=datetime.now)

    def to_dict(self):
        return {
            'id': self.id,
            'orderId': self.order_id,
            'productId': self.product_id,
            'productName': self.product_name,
            'unit': self.unit,
            'quantity': self.quantity,
            'weight': float(self.weight) if self.weight else None,
            'unitPrice': float(self.unit_price) if self.unit_price else 0,
            'amount': float(self.amount) if self.amount else 0,
        }


class OrderPayment(db.Model):
    """订单支付记录表"""
    __tablename__ = 'order_payment'

    id = db.Column(db.BigInteger, primary_key=True, autoincrement=True)
    order_id = db.Column(db.BigInteger, nullable=False, comment='订单ID')
    payment_method = db.Column(db.Enum('cash', 'wechat', 'alipay', 'card', 'credit'),
                               nullable=False, comment='支付方式')
    amount = db.Column(db.Numeric(10, 2), nullable=False, comment='支付金额')
    received_amount = db.Column(db.Numeric(10, 2), comment='实收金额(现金)')
    change_amount = db.Column(db.Numeric(10, 2), comment='找零金额(现金)')
    transaction_no = db.Column(db.String(100), comment='交易流水号')
    operator_id = db.Column(db.BigInteger, nullable=False, comment='收款人ID')
    paid_at = db.Column(db.DateTime, nullable=False, comment='支付时间')
    created_at = db.Column(db.DateTime, nullable=False, default=datetime.now)

    def to_dict(self):
        return {
            'id': self.id,
            'orderId': self.order_id,
            'paymentMethod': self.payment_method,
            'amount': float(self.amount) if self.amount else 0,
            'receivedAmount': float(self.received_amount) if self.received_amount else None,
            'changeAmount': float(self.change_amount) if self.change_amount else None,
            'transactionNo': self.transaction_no,
            'operatorId': self.operator_id,
            'paidAt': self.paid_at.isoformat() if self.paid_at else None,
        }

