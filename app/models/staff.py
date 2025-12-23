from app import db
from datetime import datetime


class Staff(db.Model):
    """员工表"""
    __tablename__ = 'staff'

    id = db.Column(db.BigInteger, primary_key=True, autoincrement=True)
    username = db.Column(db.String(50), unique=True, nullable=False, comment='用户名')
    password = db.Column(db.String(255), nullable=False, comment='密码(加密)')
    name = db.Column(db.String(50), nullable=False, comment='姓名')
    phone = db.Column(db.String(20), comment='手机号')
    avatar = db.Column(db.String(255), comment='头像URL')
    role = db.Column(db.Enum('admin', 'manager', 'cashier', 'warehouse'), 
                     nullable=False, default='cashier', comment='角色')
    status = db.Column(db.SmallInteger, nullable=False, default=1, comment='状态: 0-禁用, 1-启用')
    last_login_at = db.Column(db.DateTime, comment='最后登录时间')
    created_at = db.Column(db.DateTime, nullable=False, default=datetime.now)
    updated_at = db.Column(db.DateTime, nullable=False, default=datetime.now, onupdate=datetime.now)

    def to_dict(self):
        return {
            'id': self.id,
            'username': self.username,
            'name': self.name,
            'phone': self.phone,
            'avatar': self.avatar,
            'role': self.role,
            'status': self.status,
            'lastLoginAt': self.last_login_at.isoformat() if self.last_login_at else None,
            'createdAt': self.created_at.isoformat() if self.created_at else None,
        }

