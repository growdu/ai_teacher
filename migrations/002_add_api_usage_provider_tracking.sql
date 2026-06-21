-- Migration: Add provider tracking columns to api_usage table
-- Date: 2026-06-21
-- Description: Extend api_usage to track AI provider, model and API type details

-- Add new columns to api_usage table
ALTER TABLE api_usage ADD COLUMN IF NOT EXISTS provider_name VARCHAR(32);
ALTER TABLE api_usage ADD COLUMN IF NOT EXISTS model VARCHAR(64);
ALTER TABLE api_usage ADD COLUMN IF NOT EXISTS api_type VARCHAR(16);
ALTER TABLE api_usage ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT NOW();

-- Create composite index for detailed usage queries
CREATE INDEX IF NOT EXISTS idx_api_usage_tenant_month_provider 
ON api_usage(tenant_id, year_month, provider_name);

-- Add comments for new columns
COMMENT ON COLUMN api_usage.provider_name IS 'AI provider name: openai, claude, qwen, etc.';
COMMENT ON COLUMN api_usage.model IS 'AI model: gpt-4, claude-3-5-sonnet, qwen-max, etc.';
COMMENT ON COLUMN api_usage.api_type IS 'API type: chat, generate, embedding, ppt, video, material';
