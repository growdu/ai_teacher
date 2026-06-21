# Nginx HTTPS SSL + Let's Encrypt 配置指南

## 文件结构

```
deploy/nginx/
├── nginx.conf              # 主配置（已启用 HTTPS/TLS）
├── ssl/                    # SSL 证书目录
│   ├── fullchain.pem       # 证书 + 中间 CA（Let's Encrypt 生成）
│   └── privkey.pem         # 私钥（Let's Encrypt 生成）
├── html/                   # ACME challenge 目录（挂载到 /var/www/html）
│   └── .well-known/        # Let's Encrypt 验证文件（自动生成）
├── letsencrypt-renew.service  # 续期 systemd service
├── letsencrypt-renew.timer   # 续期 systemd 定时器（每日 3:00/15:00）
└── vite.svg
```

## 获取 Let's Encrypt 证书

### 方式一：使用脚本（推荐）

```bash
# 申请证书（将 your-domain.com 替换为你的域名）
LETSENCRYPT_EMAIL=your@email.com ./scripts/letsencrypt.sh obtain your-domain.com

# 启用自动续期
sudo cp deploy/nginx/letsencrypt-renew.service /etc/systemd/system/
sudo cp deploy/nginx/letsencrypt-renew.timer /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now letsencrypt-renew.timer
```

### 方式二：手动申请

```bash
# 确认 80 端口未被占用，然后：
sudo certbot certonly --nginx -d your-domain.com --_EMAIL your@email.com

# 手动复制到 ssl 目录：
sudo ln -sf /etc/letsencrypt/live/your-domain.com/fullchain.pem deploy/nginx/ssl/fullchain.pem
sudo ln -sf /etc/letsencrypt/live/your-domain.com/privkey.pem deploy/nginx/ssl/privkey.pem
```

## 验证 HTTPS 配置

```bash
# 测试 Nginx 配置语法
nginx -t -c /home/ubuntu/ai_teacher/deploy/nginx/nginx.conf

# SSL 实验室测试
curl -k https://localhost/nginx-health
openssl s_client -connect localhost:443 -servername your-domain.com
```

## 证书自动续期

Let's Encrypt 证书有效期 90 天，自动续期通过 systemd timer 执行：

```bash
# 查看下次执行时间
systemctl list-timers letsencrypt-renew.timer

# 手动测试续期
./scripts/letsencrypt.sh renew

# 查看续期日志
journalctl -u letsencrypt-renew.service
```

## 生产环境检查清单

- [ ] 已申请 Let's Encrypt 证书并放置于 `deploy/nginx/ssl/`
- [ ] `ssl/fullchain.pem` 和 `ssl/privkey.pem` 已正确链接
- [ ] ACME challenge 目录 `deploy/nginx/html/.well-known/acme-challenge/` 可通过 HTTP 访问
- [ ] `CORS_ALLOWED_ORIGINS` 已更新为 `https://your-domain.com`
- [ ] Nginx 配置测试通过：`nginx -t`
- [ ] 自动续期 timer 已启用并运行
- [ ] 防火墙开放 443 端口：`sudo ufw allow 443/tcp`
