#!/bin/bash
# AI Teacher Studio 数据备份脚本
# 用法: ./scripts/backup.sh
# 建议通过 cron 每日执行: 0 2 * * * /home/ubuntu/ai_teacher/scripts/backup.sh

set -euo pipefail

BACKUP_DIR="/home/ubuntu/ai_teacher/backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
LOG_FILE="${BACKUP_DIR}/backup_${TIMESTAMP}.log"

mkdir -p "${BACKUP_DIR}"

log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "${LOG_FILE}"
}

log "=== AI Teacher Studio 备份开始 ==="

# 1. PostgreSQL 数据库备份
log "备份 PostgreSQL 数据库..."
docker exec ai-teacher-postgres pg_dump -U postgres ai_teacher > "${BACKUP_DIR}/postgres_${TIMESTAMP}.sql" 2>> "${LOG_FILE}"
log "PostgreSQL 备份完成: ${BACKUP_DIR}/postgres_${TIMESTAMP}.sql"

# 2. MinIO 对象存储备份
log "备份 MinIO 数据..."
docker exec ai-teacher-minio tar czf - -C /data . > "${BACKUP_DIR}/minio_${TIMESTAMP}.tar.gz" 2>> "${LOG_FILE}"
log "MinIO 备份完成: ${BACKUP_DIR}/minio_${TIMESTAMP}.tar.gz"

# 3. 备份 .env 和敏感配置
log "备份配置文件..."
if [ -f "/home/ubuntu/ai_teacher/.env" ]; then
  cp "/home/ubuntu/ai_teacher/.env" "${BACKUP_DIR}/.env.backup_${TIMESTAMP}"
  log "配置文件备份完成: ${BACKUP_DIR}/.env.backup_${TIMESTAMP}"
else
  log "警告: .env 文件不存在，跳过"
fi

# 4. 清理超过 7 天的备份
log "清理超过 7 天的旧备份..."
find "${BACKUP_DIR}" -name "postgres_*.sql" -mtime +7 -delete
find "${BACKUP_DIR}" -name "minio_*.tar.gz" -mtime +7 -delete
find "${BACKUP_DIR}" -name "*.log" -mtime +7 -delete

log "=== AI Teacher Studio 备份完成 ==="
log "备份文件保存在: ${BACKUP_DIR}/"
