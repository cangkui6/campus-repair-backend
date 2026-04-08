CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    real_name VARCHAR(50) NOT NULL,
    phone VARCHAR(20),
    role VARCHAR(20) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT uk_sys_user_username UNIQUE (username),
    CONSTRAINT ck_sys_user_role CHECK (role IN ('REPORTER', 'WORKER', 'ADMIN')),
    CONSTRAINT ck_sys_user_status CHECK (status IN (0, 1))
);

CREATE TABLE IF NOT EXISTS fault_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_code VARCHAR(50) NOT NULL,
    category_name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    created_at DATETIME NOT NULL,
    CONSTRAINT uk_fault_category_code UNIQUE (category_code)
);

CREATE TABLE IF NOT EXISTS maintenance_worker (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    skill_tags VARCHAR(255) NOT NULL,
    service_area VARCHAR(255) NOT NULL,
    current_load INT NOT NULL DEFAULT 0,
    is_available TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT uk_worker_user_id UNIQUE (user_id),
    CONSTRAINT fk_worker_user_id FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT ck_worker_is_available CHECK (is_available IN (0, 1)),
    CONSTRAINT ck_worker_current_load CHECK (current_load >= 0)
);

CREATE TABLE IF NOT EXISTS repair_ticket (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ticket_no VARCHAR(32) NOT NULL,
    reporter_id BIGINT NOT NULL,
    raw_text TEXT NOT NULL,
    location_text VARCHAR(255),
    device_type VARCHAR(100),
    fault_desc VARCHAR(500),
    urgency_level VARCHAR(20),
    contact_masked VARCHAR(32),
    time_preference VARCHAR(100),
    category_id BIGINT,
    status VARCHAR(20) NOT NULL,
    current_worker_id BIGINT,
    submitted_at DATETIME NOT NULL,
    completed_at DATETIME,
    closed_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT uk_repair_ticket_no UNIQUE (ticket_no),
    CONSTRAINT fk_ticket_reporter_id FOREIGN KEY (reporter_id) REFERENCES sys_user (id),
    CONSTRAINT fk_ticket_category_id FOREIGN KEY (category_id) REFERENCES fault_category (id),
    CONSTRAINT fk_ticket_worker_id FOREIGN KEY (current_worker_id) REFERENCES maintenance_worker (id),
    CONSTRAINT ck_ticket_urgency CHECK (urgency_level IS NULL OR urgency_level IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT ck_ticket_status CHECK (status IN ('待受理', '待人工确认', '已解析', '待分配', '已派单', '处理中', '已完成', '已评价', '已关闭'))
);

CREATE TABLE IF NOT EXISTS dispatch_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ticket_id BIGINT NOT NULL,
    worker_id BIGINT NOT NULL,
    score_skill DECIMAL(5,2),
    score_area DECIMAL(5,2),
    score_load DECIMAL(5,2),
    score_perf DECIMAL(5,2),
    score_urgency DECIMAL(5,2),
    total_score DECIMAL(6,2),
    score_version INT,
    dispatch_type VARCHAR(20) NOT NULL,
    dispatch_status VARCHAR(20) NOT NULL,
    remark VARCHAR(255),
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_dispatch_ticket_id FOREIGN KEY (ticket_id) REFERENCES repair_ticket (id),
    CONSTRAINT fk_dispatch_worker_id FOREIGN KEY (worker_id) REFERENCES maintenance_worker (id),
    CONSTRAINT ck_dispatch_type CHECK (dispatch_type IN ('AUTO', 'MANUAL')),
    CONSTRAINT ck_dispatch_status CHECK (dispatch_status IN ('ASSIGNED', 'ACCEPTED', 'REJECTED'))
);

CREATE TABLE IF NOT EXISTS operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ticket_id BIGINT NOT NULL,
    operator_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    from_status VARCHAR(20),
    to_status VARCHAR(20),
    detail VARCHAR(500),
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_log_ticket_id FOREIGN KEY (ticket_id) REFERENCES repair_ticket (id),
    CONSTRAINT fk_log_operator_id FOREIGN KEY (operator_id) REFERENCES sys_user (id)
);

CREATE TABLE IF NOT EXISTS notification_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    receiver_id BIGINT NOT NULL,
    ticket_id BIGINT,
    title VARCHAR(100) NOT NULL,
    content VARCHAR(500) NOT NULL,
    is_read TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_message_receiver_id FOREIGN KEY (receiver_id) REFERENCES sys_user (id),
    CONSTRAINT fk_message_ticket_id FOREIGN KEY (ticket_id) REFERENCES repair_ticket (id),
    CONSTRAINT ck_message_is_read CHECK (is_read IN (0, 1))
);

CREATE TABLE IF NOT EXISTS dispatch_feedback_snapshot (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    window_start DATETIME NOT NULL,
    window_end DATETIME NOT NULL,
    dispatch_count INT NOT NULL,
    reassign_rate DECIMAL(6,4) NOT NULL,
    reject_rate DECIMAL(6,4) NOT NULL,
    timeout_rate DECIMAL(6,4) NOT NULL,
    avg_complete_hours DECIMAL(8,2) NOT NULL,
    applied_version INT NOT NULL,
    created_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS dispatch_weight_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    version_no INT NOT NULL,
    weight_skill DECIMAL(6,4) NOT NULL,
    weight_area DECIMAL(6,4) NOT NULL,
    weight_load DECIMAL(6,4) NOT NULL,
    weight_perf DECIMAL(6,4) NOT NULL,
    weight_urgency DECIMAL(6,4) NOT NULL,
    trigger_source VARCHAR(30) NOT NULL,
    is_active TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS llm_parse_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ticket_id BIGINT,
    operator_id BIGINT,
    prompt_version VARCHAR(50) NOT NULL,
    provider_name VARCHAR(50) NOT NULL DEFAULT 'ZHIPU_GLM',
    model_name VARCHAR(100) NOT NULL,
    latency_ms BIGINT NOT NULL DEFAULT 0,
    raw_response TEXT,
    parse_status VARCHAR(30) NOT NULL,
    normalized_result TEXT,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_llm_audit_ticket_id FOREIGN KEY (ticket_id) REFERENCES repair_ticket (id),
    CONSTRAINT fk_llm_audit_operator_id FOREIGN KEY (operator_id) REFERENCES sys_user (id)
);

CREATE TABLE IF NOT EXISTS llm_parse_review_queue (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ticket_id BIGINT,
    operator_id BIGINT,
    raw_text TEXT NOT NULL,
    reason_code VARCHAR(50) NOT NULL DEFAULT 'EMPTY_FAULT',
    reason VARCHAR(255) NOT NULL,
    queue_status VARCHAR(30) NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_llm_queue_ticket_id FOREIGN KEY (ticket_id) REFERENCES repair_ticket (id),
    CONSTRAINT fk_llm_queue_operator_id FOREIGN KEY (operator_id) REFERENCES sys_user (id)
);

-- 索引可在数据库初始化后按需手动创建，避免不同MySQL版本语法兼容问题
