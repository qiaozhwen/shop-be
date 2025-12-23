import os
from datetime import timedelta
from urllib.parse import quote_plus
from dotenv import load_dotenv

load_dotenv()


class Config:
    # 数据库配置
    DB_HOST = os.getenv('DB_HOST', '106.14.227.122')
    DB_PORT = os.getenv('DB_PORT', '3306')
    DB_USER = os.getenv('DB_USERNAME', 'root')
    DB_PASSWORD = os.getenv('DB_PASSWORD', '13524155957Qz@1')
    DB_NAME = os.getenv('DB_DATABASE', 'freshbird')

    # URL 编码密码中的特殊字符
    SQLALCHEMY_DATABASE_URI = f'mysql+pymysql://{DB_USER}:{quote_plus(DB_PASSWORD)}@{DB_HOST}:{DB_PORT}/{DB_NAME}?charset=utf8mb4'
    SQLALCHEMY_TRACK_MODIFICATIONS = False

    # JWT 配置
    JWT_SECRET_KEY = os.getenv('JWT_SECRET', 'freshbird-secret-key-2024')
    JWT_ACCESS_TOKEN_EXPIRES = timedelta(days=7)

    # 应用配置
    SECRET_KEY = os.getenv('SECRET_KEY', 'freshbird-app-secret')

