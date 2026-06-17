# AI Teacher Studio Deployment Guide

## Table of Contents

- [Prerequisites](#prerequisites)
- [Environment Configuration](#environment-configuration)
- [Local Development Deployment](#local-development-deployment)
- [Production Deployment](#production-deployment)
- [Docker Compose Services](#docker-compose-services)
- [Database Setup](#database-setup)
- [MinIO Setup](#minio-setup)
- [SSL/HTTPS Configuration](#sslhttps-configuration)
- [Backup and Recovery](#backup-and-recovery)
- [Monitoring](#monitoring)
- [Troubleshooting](#troubleshooting)

---

## Prerequisites

### System Requirements

| Resource | Minimum | Recommended |
|----------|---------|-------------|
| CPU | 2 cores | 4 cores |
| Memory | 4 GB | 8 GB |
| Disk | 20 GB | 50 GB |
| OS | Ubuntu 20.04+ / Debian 11+ | Ubuntu 22.04 LTS |

### Required Software

- Docker 24.0+
- Docker Compose 2.20+
- Git

### Optional Software

- FFmpeg (for video generation)
- Node.js 20+ (for local development)

---

## Environment Configuration

### 1. Clone Repository

```bash
git clone <repository-url>
cd ai_teacher
```

### 2. Create Environment File

```bash
cat > .env << 'EOF'
# ===================
# Database
# ===================
DATABASE_PASSWORD=your_secure_database_password

# ===================
# Redis
# ===================
REDIS_PASSWORD=your_secure_redis_password

# ===================
# MinIO Object Storage
# ===================
MINIO_ENDPOINT=http://minio:9000
MINIO_ROOT_USER=ai_teacher
MINIO_ROOT_PASSWORD=your_secure_minio_password

# ===================
# JWT Authentication
# ===================
# Must be at least 256 bits (32 characters) for HS256
JWT_SECRET=your_secure_jwt_secret_at_least_32_characters_long

# ===================
# AI API Keys (Configure at least one to enable AI features)
# ===================
AI_OPENAI_API_KEY=sk-you...-key
AI_CLAUDE_API_KEY=sk-ant...-key
AI_QWEN_API_KEY=sk-you...-key
AI_MINIMAX_API_KEY=your-minimax-api-key
AI_MINIMAX_VIDEO_API_KEY=your-minimax-video-api-key

# ===================
# Aliyun TTS (Optional)
# ===================
ALIYUN_TTS_ACCESS_KEY=your-aliyun-access-key
ALIYUN_TTS_ACCESS_SECRET=your-aliyun-access-secret
EOF
```

### 3. Modify Permissions

```bash
chmod 600 .env
```

---

## Local Development Deployment

### Quick Start

```bash
# Start all services
docker-compose --env-file .env up -d

# Check service status
docker-compose ps

# View logs
docker-compose logs -f backend
docker-compose logs -f frontend

# Stop all services
docker-compose down
```

### Service Ports

| Service | Internal Port | External Port | URL |
|---------|---------------|---------------|-----|
| Frontend | 80 | 3000 | http://localhost:3000 |
| Backend | 8080 | 8080 | http://localhost:8080 |
| PostgreSQL | 5432 | 5432 | localhost:5432 |
| Redis | 6379 | 6379 | localhost:6379 |
| MinIO API | 9000 | 9000 | http://localhost:9000 |
| MinIO Console | 9001 | 9001 | http://localhost:9001 |

### Accessing Services

- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080/api
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **MinIO Console**: http://localhost:9001 (login with MINIO_ROOT_USER/MINIO_ROOT_PASSWORD)

### Default Credentials

```
Admin Login:
Email: admin@aiteacher.com
Password: admin123
```

---

## Production Deployment

### 1. Server Preparation

```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Install Docker
curl -fsSL https://get.docker.com | sh

# Install Docker Compose
sudo apt install docker-compose -y

# Add current user to docker group
sudo usermod -aG docker $USER
newgrp docker
```

### 2. Firewall Configuration

```bash
# Allow HTTP/HTTPS
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# Allow MinIO ports (if accessing externally)
# sudo ufw allow 9000/tcp
# sudo ufw allow 9001/tcp

# Enable firewall
sudo ufw enable
```

### 3. Directory Structure

```bash
# Create deployment directory
sudo mkdir -p /opt/ai-teacher
sudo chown $USER:$USER /opt/ai-teacher

# Move files
mv /path/to/ai_teacher/* /opt/ai-teacher/
cd /opt/ai-teacher
```

### 4. Production Environment

Create `/opt/ai-teacher/.env.production`:

```bash
# Database
DATABASE_PASSWORD=production_secure_password

# Redis
REDIS_PASSWORD=production_redis_password

# MinIO
MINIO_ROOT_USER=production_user
MINIO_ROOT_PASSWORD=production_minio_password

# JWT - Use a strong, unique secret
JWT_SECRET=production_jwt_secret_at_least_256_bits_long_for_security

# AI Keys
OPENAI_API_KEY=sk-prod-openai-key
CLAUDE_API_KEY=sk-ant-prod-claude-key
```

### 5. Start Services

```bash
cd /opt/ai-teacher

# Start in detached mode
docker-compose --env-file .env.production up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f
```

### 6. Auto-Start with Systemd

Create `/etc/systemd/system/ai-teacher.service`:

```ini
[Unit]
Description=AI Teacher Studio
Requires=docker-compose.service
After=docker-compose.service

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=/opt/ai-teacher
ExecStart=/usr/local/bin/docker-compose --env-file .env.production up -d
ExecStop=/usr/local/bin/docker-compose --env-file .env.production down
TimeoutStartSec=0

[Install]
WantedBy=multi-user.target
```

Enable the service:

```bash
sudo systemctl daemon-reload
sudo systemctl enable ai-teacher.service
sudo systemctl start ai-teacher.service
```

---

## Docker Compose Services

### Service Architecture

```
                    ┌─────────────┐
                    │   Nginx     │
                    │  (Frontend) │
                    └──────┬──────┘
                           │
                           ▼
                    ┌─────────────┐
                    │   Backend   │
                    │ Spring Boot │
                    └──────┬──────┘
                           │
          ┌────────────────┼────────────────┐
          │                │                │
          ▼                ▼                ▼
    ┌──────────┐    ┌──────────┐    ┌──────────┐
    │ Postgres │    │  Redis   │    │  MinIO   │
    │    DB    │    │  Cache   │    │  Storage │
    └──────────┘    └──────────┘    └──────────┘
```

### Backend Service

The backend service:
- Builds from `backend/Dockerfile`
- Exposes port 8080
- Depends on PostgreSQL, Redis, MinIO
- Health check enabled

### Frontend Service

The frontend service:
- Builds from `frontend/Dockerfile`
- Serves static files via Nginx
- Proxies API requests to backend
- No external ports (uses Nginx on port 80)

### PostgreSQL Service

- Image: `postgresql:16`
- Persistent volume: `postgres_data`
- Initializes from `init.sql`
- Port: 5432 (exposed for development)

### Redis Service

- Image: `redis:7-alpine`
- Persistent volume: `redis_data`
- Requires password authentication
- Port: 6379 (exposed for development)

### MinIO Service

- Image: `minio/minio:latest`
- Persistent volume: `minio_data`
- Console port: 9001
- API port: 9000
- Health check enabled

---

## Database Setup

### Initial Schema

The database schema is automatically created on first startup via `init.sql`.

### Tables

| Table | Description |
|-------|-------------|
| tenant | Multi-tenant organizations |
| users | User accounts |
| workspace | User workspaces |
| knowledge_point | Knowledge points |
| course | Generated courses |
| teaching_material | PPT/Video materials |
| ai_config | AI provider configurations |
| async_task | Background task status |
| resource | Uploaded files |

### Indexes

```sql
CREATE INDEX idx_users_tenant_id ON users(tenant_id);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_course_tenant_id ON course(tenant_id);
CREATE INDEX idx_teaching_material_course_id ON teaching_material(course_id);
CREATE INDEX idx_async_task_status ON async_task(status);
```

### Connection Settings

```yaml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/ai_teacher
    username: postgres
    password: ${DATABASE_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
```

---

## MinIO Setup

### Create Bucket

1. Access MinIO Console at http://localhost:9001
2. Login with credentials from `.env`
3. Create bucket named `ai-teacher`

### Default Buckets

The application uses bucket `ai-teacher` for:
- PPT files
- Video files
- Uploaded resources

### Bucket Policy

The backend has full access to the bucket. For production, consider using IAM policies.

---

## SSL/HTTPS Configuration

### Option 1: Nginx Reverse Proxy with SSL

Create `/etc/nginx/sites-available/ai-teacher`:

```nginx
server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl;
    server_name your-domain.com;

    ssl_certificate /path/to/fullchain.pem;
    ssl_certificate_key /path/to/privkey.pem;

    # Frontend
    location / {
        proxy_pass http://localhost:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # Backend API
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # Swagger UI
    location /swagger-ui/ {
        proxy_pass http://localhost:8080/swagger-ui/;
        proxy_set_header Host $host;
    }
}
```

Enable the site:

```bash
sudo ln -s /etc/nginx/sites-available/ai-teacher /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

### Option 2: Docker with SSL

Modify `docker-compose.yml` to use a reverse proxy container like `jwilder/nginx-proxy` with Let's Encrypt.

---

## Backup and Recovery

### Database Backup

```bash
# Backup
docker-compose exec postgres pg_dump -U postgres ai_teacher > backup_$(date +%Y%m%d).sql

# Restore
docker-compose exec -T postgres psql -U postgres ai_teacher < backup_20240101.sql
```

### MinIO Data Backup

```bash
# Backup MinIO data directory
sudo tar -czf minio_backup_$(date +%Y%m%d).tar.gz /var/lib/docker/volumes/ai_teacher_minio_data/

# Restore
sudo tar -xzf minio_backup_20240101.tar.gz -C /
```

### Automated Backups

Create `/opt/ai-teacher/backup.sh`:

```bash
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR=/opt/backups

mkdir -p $BACKUP_DIR

# Database backup
docker-compose exec -T postgres pg_dump -U postgres ai_teacher | gzip > $BACKUP_DIR/db_$DATE.sql.gz

# MinIO backup
sudo tar -czf $BACKUP_DIR/minio_$DATE.tar.gz /var/lib/docker/volumes/ai_teacher_minio_data/

# Keep only last 7 days
find $BACKUP_DIR -mtime +7 -delete
```

Add to crontab:

```bash
crontab -e
# Add line:
# 0 2 * * * /opt/ai-teacher/backup.sh
```

---

## Monitoring

### Health Checks

```bash
# Backend health
curl http://localhost:8080/actuator/health

# Docker health
docker ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"
```

### Log Management

```bash
# View backend logs
docker-compose logs -f backend

# View all logs
docker-compose logs -f

# Export logs
docker-compose logs backend > backend.log
```

### Resource Usage

```bash
# Container resource usage
docker stats

# Disk usage
docker system df
```

---

## Troubleshooting

### Common Issues

#### 1. Backend Won't Start

```bash
# Check logs
docker-compose logs backend

# Common causes:
# - Database connection failure
# - Redis connection failure
# - MinIO connection failure
# - Port already in use
```

#### 2. Database Connection Error

```bash
# Check PostgreSQL status
docker-compose ps postgres

# Test connection
docker-compose exec postgres pg_isready -U postgres

# Check logs
docker-compose logs postgres
```

#### 3. MinIO Bucket Not Found

```bash
# Access MinIO Console
# Create bucket 'ai-teacher' manually
```

#### 4. Video Generation Fails

```bash
# Check FFmpeg in backend container
docker-compose exec backend ffmpeg -version

# If not installed, add to backend Dockerfile:
# RUN apt-get update && apt-get install -y ffmpeg
```

#### 5. AI API Errors

```bash
# Check API key configuration
docker-compose exec backend env | grep API_KEY

# Test API key validity
curl -H "Authorization: Bearer $OPENAI_API_KEY" https://api.openai.com/v1/models
```

#### 6. Frontend Shows Blank Page

```bash
# Check Nginx logs
docker-compose logs frontend

# Common causes:
# - Build failed
# - Static files not found
# - API proxy misconfiguration
```

### Debug Mode

Enable debug logging in `application.yml`:

```yaml
logging:
  level:
    com.aiteacher: DEBUG
    org.springframework.web: DEBUG
```

### Reset All Data

```bash
# Stop services
docker-compose down

# Remove volumes (WARNING: deletes all data)
docker-compose rm -v

# Remove images
docker-compose down --rmi all

# Restart
docker-compose --env-file .env up -d
```
