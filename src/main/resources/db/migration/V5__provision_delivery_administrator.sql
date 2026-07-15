UPDATE staff
SET password = '$2b$12$IK3e9t.lDUbaQU.USXYPr.bG7YuPX6SLOvyR3tvUopoo7B903uZde',
    role = 'ADMIN',
    status = 'ACTIVE',
    failed_login_count = 0,
    locked_until = NULL
WHERE phone = '13524155957';

INSERT INTO staff (store_id, name, phone, nickname, role, password, status, failed_login_count, created_at)
SELECT 1,
       '系统管理员',
       '13524155957',
       '系统管理员',
       'ADMIN',
       '$2b$12$IK3e9t.lDUbaQU.USXYPr.bG7YuPX6SLOvyR3tvUopoo7B903uZde',
       'ACTIVE',
       0,
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM staff WHERE phone = '13524155957');
