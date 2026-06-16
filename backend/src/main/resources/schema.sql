-- AI Teacher Studio Database Schema

-- Drop tables if exist (in reverse order of dependencies)
DROP TABLE IF EXISTS async_task CASCADE;
DROP TABLE IF EXISTS teaching_material CASCADE;
DROP TABLE IF EXISTS course CASCADE;
DROP TABLE IF EXISTS knowledge_point CASCADE;
DROP TABLE IF EXISTS workspace CASCADE;
DROP TABLE IF EXISTS ai_config CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS tenant CASCADE;

-- 租户表
CREATE TABLE tenant (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    code VARCHAR(64) UNIQUE NOT NULL,
    plan VARCHAR(32) DEFAULT 'free',
    quota JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted INTEGER DEFAULT 0
);

-- 用户表
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT,
    username VARCHAR(64) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(128),
    role VARCHAR(32) DEFAULT 'teacher',
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted INTEGER DEFAULT 0,
    CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id)
);

-- 工作空间表
CREATE TABLE workspace (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    settings JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT NOW(),
    deleted INTEGER DEFAULT 0,
    CONSTRAINT fk_workspace_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id)
);

-- 知识点表
CREATE TABLE knowledge_point (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    workspace_id BIGINT,
    subject VARCHAR(64),
    grade VARCHAR(32),
    content TEXT NOT NULL,
    tags VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted INTEGER DEFAULT 0,
    CONSTRAINT fk_knowledge_point_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT fk_knowledge_point_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id)
);

-- 课程表
CREATE TABLE course (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    workspace_id BIGINT,
    knowledge_point_id BIGINT,
    title VARCHAR(255),
    outline JSONB,
    script TEXT,
    status VARCHAR(32) DEFAULT 'draft',
    creator_id BIGINT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted INTEGER DEFAULT 0,
    CONSTRAINT fk_course_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT fk_course_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id),
    CONSTRAINT fk_course_knowledge_point FOREIGN KEY (knowledge_point_id) REFERENCES knowledge_point(id),
    CONSTRAINT fk_course_creator FOREIGN KEY (creator_id) REFERENCES users(id)
);

-- 教学材料表
CREATE TABLE teaching_material (
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
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted INTEGER DEFAULT 0,
    CONSTRAINT fk_teaching_material_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT fk_teaching_material_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id),
    CONSTRAINT fk_teaching_material_course FOREIGN KEY (course_id) REFERENCES course(id)
);

-- AI配置表
CREATE TABLE ai_config (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    provider VARCHAR(64) NOT NULL,
    api_key_encrypted VARCHAR(512),
    api_secret VARCHAR(255),
    base_url VARCHAR(255),
    model VARCHAR(128),
    enabled BOOLEAN DEFAULT TRUE,
    priority INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted INTEGER DEFAULT 0,
    CONSTRAINT fk_ai_config_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id)
);

-- 异步任务表
CREATE TABLE async_task (
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
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    completed_at TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_users_tenant_id ON users(tenant_id);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_workspace_tenant_id ON workspace(tenant_id);
CREATE INDEX idx_knowledge_point_tenant_id ON knowledge_point(tenant_id);
CREATE INDEX idx_knowledge_point_workspace_id ON knowledge_point(workspace_id);
CREATE INDEX idx_course_tenant_id ON course(tenant_id);
CREATE INDEX idx_course_workspace_id ON course(workspace_id);
CREATE INDEX idx_course_knowledge_point_id ON course(knowledge_point_id);
CREATE INDEX idx_teaching_material_tenant_id ON teaching_material(tenant_id);
CREATE INDEX idx_teaching_material_course_id ON teaching_material(course_id);
CREATE INDEX idx_ai_config_tenant_id ON ai_config(tenant_id);
CREATE INDEX idx_async_task_user_id ON async_task(user_id);
CREATE INDEX idx_async_task_status ON async_task(status);