from flask import Blueprint, request, jsonify
from flask_jwt_extended import jwt_required, get_jwt_identity
from app import db
from app.models.finance import FinanceRecord, DailySettlement
from datetime import datetime, date
import uuid

finance_bp = Blueprint('finance', __name__)


@finance_bp.route('/records', methods=['GET'])
@jwt_required()
def get_finance_records():
    """获取财务流水列表"""
    page = request.args.get('page', 1, type=int)
    page_size = request.args.get('pageSize', 10, type=int)
    record_type = request.args.get('type', '')
    category = request.args.get('category', '')
    start_date = request.args.get('startDate', '')
    end_date = request.args.get('endDate', '')

    query = FinanceRecord.query

    if record_type:
        query = query.filter_by(type=record_type)
    if category:
        query = query.filter_by(category=category)
    if start_date:
        query = query.filter(FinanceRecord.record_at >= start_date)
    if end_date:
        query = query.filter(FinanceRecord.record_at <= end_date)

    total = query.count()
    records = query.order_by(FinanceRecord.created_at.desc()).offset((page - 1) * page_size).limit(page_size).all()

    return jsonify({
        'code': 200,
        'data': {
            'list': [r.to_dict() for r in records],
            'total': total,
            'page': page,
            'pageSize': page_size
        }
    })


@finance_bp.route('/records', methods=['POST'])
@jwt_required()
def create_finance_record():
    """创建财务记录"""
    current_user = get_jwt_identity()
    data = request.get_json()

    # 生成流水号
    record_no = f"FIN{datetime.now().strftime('%Y%m%d%H%M%S')}{str(uuid.uuid4())[:4].upper()}"

    record = FinanceRecord(
        record_no=record_no,
        type=data.get('type'),
        category=data.get('category'),
        amount=data.get('amount'),
        payment_method=data.get('paymentMethod'),
        related_type=data.get('relatedType'),
        related_id=data.get('relatedId'),
        description=data.get('description'),
        remark=data.get('remark'),
        operator_id=current_user['id'],
        record_at=data.get('recordAt', date.today())
    )
    db.session.add(record)
    db.session.commit()

    return jsonify({
        'code': 200,
        'message': '创建成功',
        'data': record.to_dict()
    })


@finance_bp.route('/summary', methods=['GET'])
@jwt_required()
def get_finance_summary():
    """获取财务汇总"""
    start_date = request.args.get('startDate', date.today().isoformat())
    end_date = request.args.get('endDate', date.today().isoformat())

    # 收入汇总
    income_query = db.session.query(db.func.sum(FinanceRecord.amount)).filter(
        FinanceRecord.type == 'income',
        FinanceRecord.record_at >= start_date,
        FinanceRecord.record_at <= end_date
    )
    total_income = income_query.scalar() or 0

    # 支出汇总
    expense_query = db.session.query(db.func.sum(FinanceRecord.amount)).filter(
        FinanceRecord.type == 'expense',
        FinanceRecord.record_at >= start_date,
        FinanceRecord.record_at <= end_date
    )
    total_expense = expense_query.scalar() or 0

    return jsonify({
        'code': 200,
        'data': {
            'totalIncome': float(total_income),
            'totalExpense': float(total_expense),
            'profit': float(total_income) - float(total_expense),
            'startDate': start_date,
            'endDate': end_date
        }
    })


@finance_bp.route('/settlements', methods=['GET'])
@jwt_required()
def get_settlements():
    """获取日结算列表"""
    page = request.args.get('page', 1, type=int)
    page_size = request.args.get('pageSize', 10, type=int)

    query = DailySettlement.query
    total = query.count()
    settlements = query.order_by(DailySettlement.settle_date.desc()).offset((page - 1) * page_size).limit(page_size).all()

    return jsonify({
        'code': 200,
        'data': {
            'list': [s.to_dict() for s in settlements],
            'total': total,
            'page': page,
            'pageSize': page_size
        }
    })

