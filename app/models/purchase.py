from app import db
from datetime import datetime


class PurchaseOrder(db.Model):
    """采购订单表"""
    __tablename__ = 'purchase_order'

    id = db.Column(db.BigInteger, primary_key=True, autoincrement=True)
    purchase_no = db.Column(db.String(50), unique=True, nullable=False, comment='采购单号')
    supplier_id = db.Column(db.BigInteger, nullable=False, comment='供应商ID')
    total_quantity = db.Column(db.Integer, nullable=False, default=0, comment='采购总数量')
    total_weight = db.Column(db.Numeric(10, 2), default=0, comment='采购总重量')
    total_amount = db.Column(db.Numeric(10, 2), nullable=False, default=0, comment='采购总金额')
    paid_amount = db.Column(db.Numeric(10, 2), default=0, comment='已付金额')
    payment_status = db.Column(db.Enum('unpaid', 'partial', 'paid'),
                               nullable=False, default='unpaid', comment='付款状态')
    status = db.Column(db.Enum('pending', 'confirmed', 'received', 'cancelled'),
                       nullable=False, default='pending', comment='采购状态')
    expected_at = db.Column(db.Date, comment='预计到货日期')
    received_at = db.Column(db.DateTime, comment='实际到货时间')
    remark = db.Column(db.String(500), comment='备注')
    operator_id = db.Column(db.BigInteger, nullable=False, comment='采购员ID')
    created_at = db.Column(db.DateTime, nullable=False, default=datetime.now)
    updated_at = db.Column(db.DateTime, nullable=False, default=datetime.now, onupdate=datetime.now)

    def to_dict(self):
        return {
            'id': self.id,
            'purchaseNo': self.purchase_no,
            'supplierId': self.supplier_id,
            'totalQuantity': self.total_quantity,
            'totalWeight': float(self.total_weight) if self.total_weight else 0,
            'totalAmount': float(self.total_amount) if self.total_amount else 0,
            'paidAmount': float(self.paid_amount) if self.paid_amount else 0,
            'paymentStatus': self.payment_status,
            'status': self.status,
            'expectedAt': self.expected_at.isoformat() if self.expected_at else None,
            'receivedAt': self.received_at.isoformat() if self.received_at else None,
            'remark': self.remark,
            'operatorId': self.operator_id,
            'createdAt': self.created_at.isoformat() if self.created_at else None,
        }


class PurchaseOrderItem(db.Model):
    """采购明细表"""
    __tablename__ = 'purchase_order_item'

    id = db.Column(db.BigInteger, primary_key=True, autoincrement=True)
    purchase_id = db.Column(db.BigInteger, nullable=False, comment='采购单ID')
    product_id = db.Column(db.BigInteger, nullable=False, comment='商品ID')
    product_name = db.Column(db.String(100), nullable=False, comment='商品名称(冗余)')
    quantity = db.Column(db.Integer, nullable=False, comment='采购数量')
    weight = db.Column(db.Numeric(10, 2), comment='采购重量(斤)')
    unit_price = db.Column(db.Numeric(10, 2), nullable=False, comment='采购单价')
    amount = db.Column(db.Numeric(10, 2), nullable=False, comment='小计金额')
    received_quantity = db.Column(db.Integer, default=0, comment='已收货数量')
    created_at = db.Column(db.DateTime, nullable=False, default=datetime.now)

    def to_dict(self):
        return {
            'id': self.id,
            'purchaseId': self.purchase_id,
            'productId': self.product_id,
            'productName': self.product_name,
            'quantity': self.quantity,
            'weight': float(self.weight) if self.weight else None,
            'unitPrice': float(self.unit_price) if self.unit_price else 0,
            'amount': float(self.amount) if self.amount else 0,
            'receivedQuantity': self.received_quantity,
        }

