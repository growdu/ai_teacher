-- Migration: Add payment table for tracking paid transactions
-- Run this AFTER schema-subscription.sql on existing deployments
-- Can be safely re-run (IF NOT EXISTS)

CREATE TABLE IF NOT EXISTS payment (
    id BIGSERIAL PRIMARY KEY,
    trade_no VARCHAR(64) NOT NULL UNIQUE,         -- AITS-{tenantId}-yyyyMMddHHmmss-{uuid}
    tenant_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    channel VARCHAR(16) NOT NULL,                 -- alipay / wechat
    status VARCHAR(16) NOT NULL DEFAULT 'pending', -- pending / paid / failed / expired
    paid_at VARCHAR(32),                          -- ISO timestamp when paid
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT fk_payment_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT fk_payment_plan FOREIGN KEY (plan_id) REFERENCES plan(id)
);

CREATE INDEX IF NOT EXISTS idx_payment_trade_no ON payment(trade_no);
CREATE INDEX IF NOT EXISTS idx_payment_tenant ON payment(tenant_id);
CREATE INDEX IF NOT EXISTS idx_payment_status ON payment(status);

-- Grant application user permissions if needed
-- GRANT ALL PRIVILEGES ON TABLE payment TO YOUR_APP_USER;
