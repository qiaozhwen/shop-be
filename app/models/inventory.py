from app import db
from datetime import datetime


class Inventory(db.Model):
    """库存表"""
    __tablename__ = 'inventory'

    id = db.Column(db.BigInteger, primary_key=True, autoincrement=True)
    product_id = db.Column(db.BigInteger, nullable=False, unique=True, comment='商品ID')
    quantity = db.Column(db.Integer, nullable=False, default=0, comment='当前库存数量')
    total_weight = db.Column(db.Numeric(10, 2), default=0, comment='总重量(斤)')
    min_quantity = db.Column(db.Integer, default=0, comment='最低库存')
    max_quantity = db.Column(db.Integer, comment='最高库存')
    low_stock_alert = db.Column(db.SmallInteger, default=0, comment='低库存预警')
    notes = db.Column(db.Text, comment='备注')
    created_at = db.Column(db.DateTime, nullable=False, default=datetime.now)
    updated_at = db.Column(db.DateTime, nullable=False, default=datetime.now, onupdate=datetime.now)

    def to_dict(self):
        return {
            'id': self.id,
            'productId': self.product_id,
            'quantity': self.quantity,
            'totalWeight': float(self.total_weight) if self.total_weight else 0,
            'minQuantity': self.min_quantity,
            'maxQuantity': self.max_quantity,
            'lowStockAlert': self.low_stock_alert,
            'notes': self.notes,
        }


class InventoryInbound(db.Model):
    """入库记录表"""
    __tablename__ = 'inventory_inbound'

    id = db.Column(db.BigInteger, primary_key=True, autoincrement=True)
    inbound_no = db.Column(db.String(50), unique=True, nullable=False, comment='入库单号')
    supplier_id = db.Column(db.BigInteger, comment='供应商ID')
    product_id = db.Column(db.BigInteger, nullable=False, comment='商品ID')
    quantity = db.Column(db.Integer, nullable=False, comment='入库数量')
    weight = db.Column(db.Numeric(10, 2), comment='入库重量(斤)')
    unit_price = db.Column(db.Numeric(10, 2), comment='采购单价')
    total_amount = db.Column(db.Numeric(10, 2), comment='采购总额')
    batch_no = db.Column(db.String(50), comment='批次号')
    type = db.Column(db.Enum('purchase', 'return', 'adjust', 'other'), 
                     nullable=False, default='purchase', comment='入库类型')
    remark = db.Column(db.String(500), comment='备注')
    operator_id = db.Column(db.BigInteger, nullable=False, comment='操作人ID')
    inbound_at = db.Column(db.DateTime, nullable=False, comment='入库时间')
    created_at = db.Column(db.DateTime, nullable=False, default=datetime.now)

    def to_dict(self):
        return {
            'id': self.id,
            'inboundNo': self.inbound_no,
            'supplierId': self.supplier_id,
            'productId': self.product_id,
            'quantity': self.quantity,
            'weight': float(self.weight) if self.weight else None,
            'unitPrice': float(self.unit_price) if self.unit_price else None,
            'totalAmount': float(self.total_amount) if self.total_amount else None,
            'batchNo': self.batch_no,
            'type': self.type,
            'remark': self.remark,
            'operatorId': self.operator_id,
            'inboundAt': self.inbound_at.isoformat() if self.inbound_at else None,
        }


class InventoryOutbound(db.Model):
    """出库记录表"""
    __tablename__ = 'inventory_outbound'

    id = db.Column(db.BigInteger, primary_key=True, autoincrement=True)
    outbound_no = db.Column(db.String(50), unique=True, nullable=False, comment='出库单号')
    type = db.Column(db.Enum('sale', 'damage', 'adjust', 'other'), 
                     nullable=False, default='sale', comment='出库类型')
    order_id = db.Column(db.BigInteger, comment='关联订单ID')
    product_id = db.Column(db.BigInteger, nullable=False, comment='商品ID')
    quantity = db.Column(db.Integer, nullable=False, comment='出库数量')
    weight = db.Column(db.Numeric(10, 2), comment='出库重量(斤)')
    reason = db.Column(db.String(500), comment='出库原因')
    operator_id = db.Column(db.BigInteger, nullable=False, comment='操作人ID')
    outbound_at = db.Column(db.DateTime, nullable=False, comment='出库时间')
    created_at = db.Column(db.DateTime, nullable=False, default=datetime.now)

    def to_dict(self):
        return {
            'id': self.id,
            'outboundNo': self.outbound_no,
            'type': self.type,
            'orderId': self.order_id,
            'productId': self.product_id,
            'quantity': self.quantity,
            'weight': float(self.weight) if self.weight else None,
            'reason': self.reason,
            'operatorId': self.operator_id,
            'outboundAt': self.outbound_at.isoformat() if self.outbound_at else None,
        }


class InventoryAlert(db.Model):
    """库存预警表"""
    __tablename__ = 'inventory_alert'

    id = db.Column(db.BigInteger, primary_key=True, autoincrement=True)
    product_id = db.Column(db.BigInteger, nullable=False, comment='商品ID')
    current_stock = db.Column(db.Integer, nullable=False, comment='当前库存')
    min_stock = db.Column(db.Integer, nullable=False, comment='最低库存')
    alert_level = db.Column(db.Enum('warning', 'critical'), 
                            nullable=False, default='warning', comment='预警级别')
    handled = db.Column(db.SmallInteger, nullable=False, default=0, comment='处理状态')
    handled_by = db.Column(db.BigInteger, comment='处理人ID')
    handled_at = db.Column(db.DateTime, comment='处理时间')
    created_at = db.Column(db.DateTime, nullable=False, default=datetime.now)

    def to_dict(self):
        return {
            'id': self.id,
            'productId': self.product_id,
            'currentStock': self.current_stock,
            'minStock': self.min_stock,
            'alertLevel': self.alert_level,
            'handled': self.handled,
            'handledBy': self.handled_by,
            'handledAt': self.handled_at.isoformat() if self.handled_at else None,
            'createdAt': self.created_at.isoformat() if self.created_at else None,
        }

