from flask import Blueprint, request, jsonify
from flask_jwt_extended import create_access_token, jwt_required, get_jwt_identity
from app import db
from app.models.staff import Staff
from app.models.system_log import SystemLog
import bcrypt
from datetime import datetime

auth_bp = Blueprint('auth', __name__)


@auth_bp.route('/login', methods=['POST'])
def login():
    """用户登录"""
    data = request.get_json()
    username = data.get('username')
    password = data.get('password')

    if not username or not password:
        return jsonify({'code': 400, 'message': '用户名和密码不能为空'}), 400

    # 查找用户
    print(f'1111111: {data}')
    staff = Staff.query.filter_by(username=username).first()
    print(f'2222222: {staff}')
    if not staff:
        return jsonify({'code': 401, 'message': '用户名或密码错误'}), 401

    # 验证密码
    if not bcrypt.checkpw(password.encode('utf-8'), staff.password.encode('utf-8')):
        return jsonify({'code': 401, 'message': '用户名或密码错误'}), 401

    # 检查状态
    if staff.status != 1:
        return jsonify({'code': 403, 'message': '账号已被禁用'}), 403
    # 更新最后登录时间
    staff.last_login_at = datetime.now()
    db.session.commit()

    # 记录登录日志
    log = SystemLog(
        staff_id=staff.id,
        staff_name=staff.name,
        module='auth',
        action='login',
        content=f'用户 {staff.username} 登录成功',
        ip=request.remote_addr,
        user_agent=request.headers.get('User-Agent', '')[:500]
    )
    db.session.add(log)
    db.session.commit()

    # 生成 JWT token
    access_token = create_access_token(identity={'id': staff.id, 'username': staff.username})

    return jsonify({
        'code': 200,
        'message': '登录成功',
        'data': {
            'access_token': access_token,
            'user': {
                'id': staff.id,
                'username': staff.username,
                'name': staff.name,
                'role': staff.role,
                'avatar': staff.avatar,
            }
        }
    })


@auth_bp.route('/profile', methods=['GET'])
@jwt_required()
def get_profile():
    """获取当前用户信息"""
    current_user = get_jwt_identity()
    staff = Staff.query.get(current_user['id'])
    
    if not staff:
        return jsonify({'code': 404, 'message': '用户不存在'}), 404

    return jsonify({
        'code': 200,
        'data': staff.to_dict()
    })


@auth_bp.route('/logout', methods=['POST'])
@jwt_required()
def logout():
    """用户登出"""
    current_user = get_jwt_identity()
    
    # 记录登出日志
    log = SystemLog(
        staff_id=current_user['id'],
        staff_name=current_user['username'],
        module='auth',
        action='logout',
        content=f'用户 {current_user["username"]} 登出',
        ip=request.remote_addr
    )
    db.session.add(log)
    db.session.commit()

    return jsonify({
        'code': 200,
        'message': '登出成功'
    })


@auth_bp.route('/change-password', methods=['POST'])
@jwt_required()
def change_password():
    """修改密码"""
    current_user = get_jwt_identity()
    data = request.get_json()
    
    old_password = data.get('oldPassword')
    new_password = data.get('newPassword')

    if not old_password or not new_password:
        return jsonify({'code': 400, 'message': '请输入旧密码和新密码'}), 400

    staff = Staff.query.get(current_user['id'])
    if not staff:
        return jsonify({'code': 404, 'message': '用户不存在'}), 404

    # 验证旧密码
    if not bcrypt.checkpw(old_password.encode('utf-8'), staff.password.encode('utf-8')):
        return jsonify({'code': 400, 'message': '旧密码错误'}), 400

    # 加密新密码
    hashed = bcrypt.hashpw(new_password.encode('utf-8'), bcrypt.gensalt())
    staff.password = hashed.decode('utf-8')
    db.session.commit()

    return jsonify({
        'code': 200,
        'message': '密码修改成功'
    })

