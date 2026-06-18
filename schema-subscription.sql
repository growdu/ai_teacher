-- AI Teacher Studio 订阅/付费功能 Schema

-- =====================================================
-- 1. Plan 表 - 订阅套餐表
-- =====================================================
CREATE TABLE IF NOT EXISTS plan (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    billing_period VARCHAR(16) NOT NULL DEFAULT 'monthly', -- monthly: 月付, yearly: 年付
    features JSONB DEFAULT '{}',
    api_quota INT NOT NULL DEFAULT 0,           -- 每月API调用次数, -1表示无限
    storage_quota INT NOT NULL DEFAULT 1,       -- 存储配额(GB)
    status VARCHAR(16) NOT NULL DEFAULT 'active', -- active, inactive
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- =====================================================
-- 2. Subscription 表 - 租户订阅记录表
-- =====================================================
CREATE TABLE IF NOT EXISTS subscription (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'active', -- active, cancelled, expired
    started_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    auto_renew BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT fk_subscription_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT fk_subscription_plan FOREIGN KEY (plan_id) REFERENCES plan(id)
);

-- =====================================================
-- 3. API 使用量表 - 记录每月API调用次数
-- =====================================================
CREATE TABLE IF NOT EXISTS api_usage (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    year_month VARCHAR(7) NOT NULL,              -- 格式: 2024-01
    call_count INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT fk_api_usage_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT uk_api_usage_tenant_month UNIQUE (tenant_id, year_month)
);

-- =====================================================
-- 4. 修改 tenant 表 - 注释掉 plan 字段(改用 subscription 表)
-- =====================================================
-- ALTER TABLE tenant DROP COLUMN IF EXISTS plan;
-- 为保持向后兼容,保留plan字段但标记为deprecated
COMMENT ON COLUMN tenant.plan IS 'Deprecated: Use subscription table instead';

-- =====================================================
-- 5. 预置套餐数据
-- =====================================================

-- Free 套餐: 0元, 100次/月, 1GB
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

-- Pro 套餐: 99元/月, 1000次/月, 10GB
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

-- Enterprise 套餐: 299元/月, 无限次, 100GB
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

-- =====================================================
-- 6. 为现有租户创建默认订阅(Free套餐)
-- =====================================================
INSERT INTO subscription (tenant_id, plan_id, status, started_at, expires_at, auto_renew)
SELECT t.id, p.id, 'active', NOW(), NOW() + INTERVAL '1 month', TRUE
FROM tenant t
CROSS JOIN plan p
WHERE p.name = 'Free'
AND NOT EXISTS (
    SELECT 1 FROM subscription s WHERE s.tenant_id = t.id
);

-- =====================================================
-- 7. 创建索引
-- =====================================================
CREATE INDEX IF NOT EXISTS idx_subscription_tenant ON subscription(tenant_id);
CREATE INDEX IF NOT EXISTS idx_subscription_plan ON subscription(plan_id);
CREATE INDEX IF NOT EXISTS idx_subscription_status ON subscription(status);
CREATE INDEX IF NOT EXISTS idx_api_usage_tenant ON api_usage(tenant_id);
CREATE INDEX IF NOT EXISTS idx_api_usage_month ON api_usage(year_month);
