from app import db
from datetime import datetime


class SystemLog(db.Model):
    """系统日志表"""
    __tablename__ = 'system_log'

    id = db.Column(db.BigInteger, primary_key=True, autoincrement=True)
    staff_id = db.Column(db.BigInteger, comment='操作人ID')
    staff_name = db.Column(db.String(50), comment='操作人姓名')
    module = db.Column(db.Enum('auth', 'product', 'category', 'inventory', 'order', 
                               'customer', 'supplier', 'purchase', 'finance', 'system'),
                       nullable=False, comment='模块')
    action = db.Column(db.Enum('create', 'update', 'delete', 'login', 'logout', 
                               'export', 'import', 'other'),
                       nullable=False, comment='操作')
    content = db.Column(db.Text, comment='操作内容描述')
    ip = db.Column(db.String(50), comment='IP地址')
    user_agent = db.Column(db.String(500), comment='浏览器信息')
    created_at = db.Column(db.DateTime, nullable=False, default=datetime.now)

    def to_dict(self):
        return {
            'id': self.id,
            'staffId': self.staff_id,
            'staffName': self.staff_name,
            'module': self.module,
            'action': self.action,
            'content': self.content,
            'ip': self.ip,
            'createdAt': self.created_at.isoformat() if self.created_at else None,
        }

