INSERT INTO fault_category (category_code, category_name, description, created_at)
VALUES
    ('WATER_ELECTRIC', '水电故障', '漏水、跳闸、没电等问题', NOW()),
    ('NETWORK', '网络故障', '断网、掉线、网速慢等问题', NOW()),
    ('FURNITURE', '家具故障', '桌椅柜体等家具损坏', NOW()),
    ('AIR_CONDITIONER', '空调故障', '不制冷、不制热、漏水等空调问题', NOW()),
    ('DOOR_WINDOW', '门窗故障', '门锁、门把手、窗户损坏等', NOW()),
    ('LIGHTING', '照明故障', '灯管损坏、照明异常等', NOW()),
    ('OTHER', '其他故障', '无法归类的其他报修问题', NOW())
ON DUPLICATE KEY UPDATE
    category_name = VALUES(category_name),
    description = VALUES(description);

INSERT INTO sys_user (username, password_hash, real_name, phone, role, status, created_at, updated_at)
VALUES
    ('admin001', '{noop}123456', '系统管理员', '13900000001', 'ADMIN', 1, NOW(), NOW()),
    ('worker001', '{noop}123456', '张师傅', '13900000002', 'WORKER', 1, NOW(), NOW()),
    ('worker002', '{noop}123456', '李师傅', '13900000003', 'WORKER', 1, NOW(), NOW()),
    ('reporter001', '{noop}123456', '王同学', '13900000004', 'REPORTER', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    real_name = VALUES(real_name),
    phone = VALUES(phone),
    role = VALUES(role),
    status = VALUES(status),
    updated_at = NOW();

INSERT INTO maintenance_worker (user_id, skill_tags, service_area, current_load, is_available, created_at, updated_at)
SELECT u.id, 'AIR_CONDITIONER,LIGHTING', '图书馆片区,教学楼片区', 1, 1, NOW(), NOW()
FROM sys_user u
WHERE u.username = 'worker001'
ON DUPLICATE KEY UPDATE
    skill_tags = VALUES(skill_tags),
    service_area = VALUES(service_area),
    current_load = VALUES(current_load),
    is_available = VALUES(is_available),
    updated_at = NOW();

INSERT INTO maintenance_worker (user_id, skill_tags, service_area, current_load, is_available, created_at, updated_at)
SELECT u.id, 'NETWORK,WATER_ELECTRIC', '宿舍片区,教学楼片区', 0, 1, NOW(), NOW()
FROM sys_user u
WHERE u.username = 'worker002'
ON DUPLICATE KEY UPDATE
    skill_tags = VALUES(skill_tags),
    service_area = VALUES(service_area),
    current_load = VALUES(current_load),
    is_available = VALUES(is_available),
    updated_at = NOW();
