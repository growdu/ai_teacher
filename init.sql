-- AI Teacher Studio Database Initialization Script

-- Create database (run as superuser if needed)
-- CREATE DATABASE ai_teacher;

--租户表
CREATE TABLE IF NOT EXISTS tenant (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    code VARCHAR(64) UNIQUE NOT NULL,
    plan VARCHAR(32) DEFAULT 'free',
    quota JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT,
    username VARCHAR(64) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(128),
    role VARCHAR(32) DEFAULT 'teacher',
    enabled BOOLEAN DEFAULT TRUE,
    deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 工作空间表
CREATE TABLE IF NOT EXISTS workspace (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    settings JSONB DEFAULT '{}',
    deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 知识点表
CREATE TABLE IF NOT EXISTS knowledge_point (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    workspace_id BIGINT,
    subject VARCHAR(64),
    grade VARCHAR(32),
    content TEXT NOT NULL,
    tags VARCHAR(255),
    deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 课程表
CREATE TABLE IF NOT EXISTS course (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    workspace_id BIGINT,
    knowledge_point_id BIGINT,
    title VARCHAR(255),
    outline JSONB,
    script TEXT,
    status VARCHAR(32) DEFAULT 'draft',
    creator_id BIGINT,
    deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 教学材料表
CREATE TABLE IF NOT EXISTS teaching_material (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    workspace_id BIGINT,
    course_id BIGINT,
    material_type VARCHAR(32) NOT NULL,
    title VARCHAR(255),
    file_url VARCHAR(512),
    thumbnail_url VARCHAR(512),
    duration INT,
    file_size BIGINT,
    status VARCHAR(32) DEFAULT 'generated',
    deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- AI配置表
CREATE TABLE IF NOT EXISTS ai_config (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    provider VARCHAR(64) NOT NULL,
    api_key_encrypted VARCHAR(512),
    api_secret VARCHAR(255),
    base_url VARCHAR(255),
    model VARCHAR(128),
    enabled BOOLEAN DEFAULT TRUE,
    priority INT DEFAULT 0,
    deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 异步任务表
CREATE TABLE IF NOT EXISTS async_task (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT,
    user_id BIGINT,
    task_type VARCHAR(32),
    task_name VARCHAR(128),
    status VARCHAR(32) DEFAULT 'pending',
    progress INT DEFAULT 0,
    current_step VARCHAR(128),
    result TEXT,
    error_message TEXT,
    context TEXT,
    deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    completed_at TIMESTAMP
);

-- 资源表
CREATE TABLE IF NOT EXISTS resource (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    workspace_id BIGINT,
    user_id BIGINT,
    resource_name VARCHAR(255) NOT NULL,
    resource_type VARCHAR(32),
    file_url VARCHAR(512),
    file_size BIGINT,
    mime_type VARCHAR(128),
    deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 创建默认租户
INSERT INTO tenant (name, code, plan) VALUES ('默认租户', 'default', 'pro');

-- 创建默认管理员用户 (密码: admin123)
INSERT INTO users (tenant_id, username, password, email, role) 
VALUES (1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'admin@example.com', 'admin');

-- =====================================================
-- 订阅/付费功能 Schema
-- =====================================================

-- Plan 表 - 订阅套餐表
CREATE TABLE IF NOT EXISTS plan (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    billing_period VARCHAR(16) NOT NULL DEFAULT 'monthly',
    features JSONB DEFAULT '{}',
    api_quota INT NOT NULL DEFAULT 0,
    storage_quota INT NOT NULL DEFAULT 1,
    status VARCHAR(16) NOT NULL DEFAULT 'active',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Subscription 表 - 租户订阅记录表
CREATE TABLE IF NOT EXISTS subscription (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'active',
    started_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    auto_renew BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT fk_subscription_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT fk_subscription_plan FOREIGN KEY (plan_id) REFERENCES plan(id)
);

-- API 使用量表
CREATE TABLE IF NOT EXISTS api_usage (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    year_month VARCHAR(7) NOT NULL,
    call_count INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP DEFAULT NOW(),
    created_at TIMESTAMP DEFAULT NOW(),
    provider_name VARCHAR(50) DEFAULT 'default',
    model VARCHAR(100) DEFAULT 'default',
    api_type VARCHAR(50) DEFAULT 'chat',
    CONSTRAINT fk_api_usage_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT uk_api_usage_tenant_month UNIQUE (tenant_id, year_month)
);

-- 预置套餐数据
INSERT INTO plan (name, description, price, billing_period, features, api_quota, storage_quota, status)
VALUES (
    'Free',
    '免费套餐，适合个人试用',
    0.00,
    'monthly',
    '["知识点管理", "课程管理", "基础AI生成"]',
    100,
    1,
    'active'
) ON CONFLICT DO NOTHING;

INSERT INTO plan (name, description, price, billing_period, features, api_quota, storage_quota, status)
VALUES (
    'Pro',
    '专业套餐，适合教师和小型机构',
    99.00,
    'monthly',
    '["知识点管理", "课程管理", "教材中心", "测验管理", "高级AI生成", "优先客服支持"]',
    1000,
    10,
    'active'
) ON CONFLICT DO NOTHING;

INSERT INTO plan (name, description, price, billing_period, features, api_quota, storage_quota, status)
VALUES (
    'Enterprise',
    '企业套餐，适合教育机构',
    299.00,
    'monthly',
    '["全功能开放", "无限AI调用", "100GB存储", "专属客服", "API访问", "定制服务"]',
    -1,
    100,
    'active'
) ON CONFLICT DO NOTHING;

-- 为现有租户创建默认订阅
INSERT INTO subscription (tenant_id, plan_id, status, started_at, expires_at, auto_renew)
SELECT t.id, p.id, 'active', NOW(), NOW() + INTERVAL '1 month', TRUE
FROM tenant t
CROSS JOIN plan p
WHERE p.name = 'Free'
AND NOT EXISTS (
    SELECT 1 FROM subscription s WHERE s.tenant_id = t.id
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_users_tenant ON users(tenant_id);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_workspace_tenant ON workspace(tenant_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_point_tenant ON knowledge_point(tenant_id);
CREATE INDEX IF NOT EXISTS idx_course_tenant ON course(tenant_id);
CREATE INDEX IF NOT EXISTS idx_teaching_material_tenant ON teaching_material(tenant_id);
CREATE INDEX IF NOT EXISTS idx_ai_config_tenant ON ai_config(tenant_id);
CREATE INDEX IF NOT EXISTS idx_subscription_tenant ON subscription(tenant_id);
CREATE INDEX IF NOT EXISTS idx_subscription_plan ON subscription(plan_id);
CREATE INDEX IF NOT EXISTS idx_subscription_status ON subscription(status);
CREATE INDEX IF NOT EXISTS idx_api_usage_tenant ON api_usage(tenant_id);
CREATE INDEX IF NOT EXISTS idx_api_usage_month ON api_usage(year_month);