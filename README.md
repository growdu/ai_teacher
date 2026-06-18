# AI Teacher Studio

智能化教学资源生成平台 - 基于 AI 的课程、PPT、视频自动生成系统。

## 项目简介

AI Teacher Studio 是一个帮助教师快速生成教学资源的平台。用户输入知识点，系统自动生成：

- **课程大纲** - AI 生成的课程结构
- **讲稿脚本** - 详细的教师授课脚本
- **PPT 演示文稿** - 支持多种模板
- **教学视频** - 语音合成 + 视频混流

## 功能特性

| 功能 | 说明 |
|------|------|
| 课程生成 | 知识点 → AI 大纲 → 讲稿脚本 |
| PPT 生成 | 基于 PptxGenJS，支持 4 种模板 |
| 视频生成 | TTS 语音合成 + MiniMax Video-01 + FFmpeg 混流 |
| 多租户 | 租户隔离，允许多机构使用 |
| AI Provider | 支持 OpenAI/Claude/Qwen/MiniMax 动态切换 |
| 实时进度 | WebSocket 推送任务进度 |
| 资源管理 | MinIO 对象存储 |

## 技术栈

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 21 | 编程语言 |
| Spring Boot | 3.2 | Web 框架 |
| MyBatis Plus | 3.5 | ORM 框架 |
| PostgreSQL | 16 | 关系数据库 |
| Redis | 7 | 缓存 |
| MinIO | latest | 对象存储 |
| JWT | 0.12 | 身份认证 |
| springdoc | 2.3 | API 文档 |

### 前端

| 技术 | 版本 | 说明 |
|------|------|------|
| React | 18 | UI 框架 |
| TypeScript | 5.3 | 类型系统 |
| Ant Design | 5.14 | UI 组件库 |
| Tailwind CSS | 3.4 | CSS 框架 |
| React Router | 6 | 路由 |
| Zustand | 4.5 | 状态管理 |
| Axios | 1.6 | HTTP 客户端 |

### 基础设施

| 技术 | 说明 |
|------|------|
| Docker | 容器化 |
| Docker Compose | 容器编排 |
| Nginx | 反向代理 |

## 项目结构

```
ai_teacher/
├── backend/                    # Spring Boot 后端
│   ├── src/main/java/
│   │   └── com/aiteacher/
│   │       ├── controller/     # REST API 控制器
│   │       ├── service/        # 业务逻辑层
│   │       ├── mapper/         # 数据访问层
│   │       ├── entity/         # 实体类
│   │       ├── dto/            # 数据传输对象
│   │       ├── provider/       # AI Provider 抽象层
│   │       │   ├── llm/       # LLM Provider 实现
│   │       │   │   ├── AbstractLLMProvider.java
│   │       │   │   ├── OpenAILLMProvider.java
│   │       │   │   ├── ClaudeLLMProvider.java
│   │       │   │   ├── QwenLLMProvider.java
│   │       │   │   └── MiniMaxLLMProvider.java
│   │       │   ├── tts/       # TTS Provider 实现
│   │       │   │   └── AliyunTTSProvider.java
│   │       │   └── video/     # Video Provider 实现
│   │       │       └── MiniMaxVideoProvider.java
│   │       ├── config/         # 配置类
│   │       ├── filter/         # 过滤器
│   │       ├── exception/      # 异常处理
│   │       └── websocket/     # WebSocket 处理
│   ├── scripts/               # Node.js 脚本
│   │   └── generate-ppt.js   # PPT 生成脚本
│   └── pom.xml
│
├── frontend/                  # React 前端
│   ├── src/
│   │   ├── pages/            # 页面组件
│   │   ├── components/       # 公共组件
│   │   ├── api/             # API 请求封装
│   │   ├── store/           # 状态管理
│   │   └── router/          # 路由配置
│   ├── nginx.conf           # Nginx 配置
│   └── package.json
│
├── init.sql                  # 数据库初始化脚本
├── schema.sql                # 数据库 schema
├── docker-compose.yml        # Docker Compose 配置
├── .env                      # 环境变量配置
└── README.md                 # 项目文档
```

## 快速开始

### 前置要求

- Docker 24.0+
- Docker Compose 2.20+
- Git

### 1. 克隆项目

```bash
git clone <repository-url>
cd ai_teacher
```

### 2. 配置环境变量

```bash
# 创建 .env 文件
cat > .env << EOF
# Database
DATABASE_PASSWORD=ai_teacher_secure_password_2024

# Redis
REDIS_PASSWORD=ai_teacher_redis_password_2024

# MinIO
MINIO_ROOT_USER=ai_teacher
MINIO_ROOT_PASSWORD=ai_teacher_minio_password_2024

# JWT
JWT_SECRET=ai_teacher_jwt_secret_key_must_be_at_least_256_bits_long_for_hs256_algorithm

# AI API Keys (可选)
OPENAI_API_KEY=sk-your-openai-api-key
CLAUDE_API_KEY=sk-ant-your-claude-api-key
QWEN_API_KEY=sk-your-qwen-api-key
EOF
```

### 3. 启动服务

```bash
docker-compose --env-file .env up -d
```

### 4. 验证服务

```bash
# 检查服务状态
docker-compose ps

# 查看日志
docker-compose logs -f backend

# 健康检查
curl http://localhost:8080/actuator/health
```

### 5. 访问系统

| 服务 | 地址 |
|------|------|
| 前端界面 | http://localhost |
| 后端 API | http://localhost/api |
| Swagger UI | http://localhost/swagger-ui/index.html |
| MinIO Console | http://localhost:9001 |

### 默认账号

```
用户名: admin@aiteacher.com
密码: admin123
```

## 使用指南

### 创建课程流程

1. **登录系统**
   - 访问 http://localhost
   - 使用管理员账号登录

2. **添加知识点**
   - 进入「知识点」页面
   - 点击「添加知识点」
   - 输入科目、年级、知识点内容
   - 点击保存

3. **生成课程**
   - 进入「课程管理」页面
   - 点击「生成课程」按钮
   - 系统自动调用 AI 生成大纲和讲稿
   - 生成完成后可查看课程详情

4. **生成 PPT**
   - 在课程详情页面
   - 点击「生成 PPT」
   - 选择模板类型
   - 等待 PPT 生成完成

5. **生成视频**
   - 在课程详情页面
   - 点击「生成视频」
   - 系统自动进行 TTS 合成和视频混流
   - 可通过 WebSocket 实时查看进度

### AI 配置管理

进入「设置」→「AI配置」页面：

- 查看当前可用的 AI Provider
- 添加/编辑/删除 AI 配置
- 设置 Provider 优先级
- 启用/禁用特定 Provider

## API 文档

### 认证接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/auth/login` | POST | 用户登录 |

**请求示例：**
```json
POST /api/auth/login
{
  "email": "admin@aiteacher.com",
  "password": "admin123"
}
```

**响应示例：**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "user": {
      "id": 1,
      "username": "admin",
      "email": "admin@aiteacher.com"
    }
  }
}
```

### 课程管理接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/course/generate` | POST | 生成课程 |
| `/api/course/list` | GET | 课程列表 |
| `/api/course/{id}` | GET | 课程详情 |
| `/api/course/{id}` | PUT | 更新课程 |
| `/api/course/{id}` | DELETE | 删除课程 |

**生成课程请求：**
```json
POST /api/course/generate
{
  "knowledgeContent": "牛顿第二定律：F=ma",
  "subject": "物理",
  "grade": "高一"
}
```

### 知识点管理接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/knowledge-point` | POST | 创建知识点 |
| `/api/knowledge-point/list` | GET | 知识点列表 |
| `/api/knowledge-point/{id}` | GET | 知识点详情 |
| `/api/knowledge-point/{id}` | PUT | 更新知识点 |
| `/api/knowledge-point/{id}` | DELETE | 删除知识点 |

### 教材管理接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/material/ppt/generate` | POST | 生成 PPT |
| `/api/material/video/generate` | POST | 生成视频（异步） |
| `/api/material/list` | GET | 教材列表 |

**生成 PPT 请求：**
```json
POST /api/material/ppt/generate
{
  "courseId": 1,
  "template": "default"
}
```

**生成视频请求：**
```json
POST /api/material/video/generate
{
  "courseId": 1
}
```

### 任务查询接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/task/{taskId}` | GET | 查询任务状态 |

**响应示例：**
```json
{
  "code": 200,
  "data": {
    "taskId": 1,
    "status": "processing",
    "progress": 50,
    "currentStep": "正在生成讲稿..."
  }
}
```

### AI 配置接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/ai-config` | POST | 添加 AI 配置 |
| `/api/ai-config/list` | GET | 配置列表 |
| `/api/ai-config/{id}` | PUT | 更新配置 |
| `/api/ai-config/{id}` | DELETE | 删除配置 |
| `/api/ai-config/reload` | POST | 重载 Provider |
| `/api/ai-config/status` | GET | 查看可用 Provider |

### WebSocket 接口

```
ws://localhost/ws/tasks
```

**订阅任务：**
```json
{
  "type": "subscribe",
  "taskId": "123"
}
```

**接收进度更新：**
```json
{
  "type": "taskUpdate",
  "taskId": "123",
  "status": "processing",
  "progress": 75,
  "message": "正在合成语音..."
}
```

## AI Provider 架构

### Provider 列表

| Provider | 模型 | 说明 |
|----------|------|------|
| OpenAI | GPT-4o, GPT-4-turbo | OpenAI API |
| Claude | Claude 3.5 Sonnet | Anthropic Claude |
| Qwen | Qwen-Max, Qwen-Plus | 阿里云通义 |
| AliyunTTS | - | 阿里云语音合成 |

### Provider 优先级

系统按优先级选择 Provider：
- 优先级数值越大，优先级越高
- 只选择 `enabled=true` 的 Provider
- 支持环境变量快速配置

### 配置方式

**方式一：环境变量**

```bash
OPENAI_API_KEY=sk-xxx
CLAUDE_API_KEY=sk-ant-xxx
QWEN_API_KEY=sk-xxx
```

**方式二：数据库配置**

```json
POST /api/ai-config
{
  "provider": "openai",
  "model": "gpt-4o",
  "apiKey": "sk-xxx",
  "priority": 3,
  "enabled": true
}
```

## PPT 模板

系统支持 4 种 PPT 模板：

| 模板 | 说明 |
|------|------|
| default | 默认蓝色主题 |
| academic | 学术风格，简洁大方 |
| simple | 简约风格，适合低年级 |
| modern | 现代风格，色彩丰富 |

### 幻灯片类型

| 类型 | 说明 |
|------|------|
| title | 封面幻灯片 |
| chapter | 章节封面 |
| content | 内容幻灯片 |
| image | 图片幻灯片 |
| quiz | 测验幻灯片 |
| summary | 总结幻灯片 |
| end | 结束幻灯片 |

## 开发指南

### 后端开发

```bash
cd backend

# 编译项目
mvn clean compile

# 运行测试
mvn test

# 打包
mvn clean package -DskipTests

# 本地运行
mvn spring-boot:run
```

### 前端开发

```bash
cd frontend

# 安装依赖
npm install

# 开发模式运行
npm run dev

# 构建生产版本
npm run build
```

### 数据库

编辑 `init.sql` 后重启 PostgreSQL：

```bash
docker-compose restart postgres
```

### 前端代理配置

前端开发时，API 请求通过 Vite 代理到后端：

```javascript
// vite.config.ts
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

## 部署指南

### 生产环境部署

1. **准备服务器**
   - Docker 24.0+
   - Docker Compose 2.20+
   - 最低配置：2 核 CPU, 4GB 内存

2. **配置 HTTPS**
   - 使用 Nginx 配置 SSL 证书
   - 更新反向代理配置

3. **配置防火墙**
   - 开放 80 (HTTP)
   - 开放 443 (HTTPS)
   - 开放 9000/9001 (MinIO)

4. **数据备份**
   - 定期备份 PostgreSQL 数据
   - 定期备份 MinIO 文件

### Docker Compose 说明

| 服务 | 端口 | 说明 |
|------|------|------|
| postgres | 5432 | PostgreSQL 数据库 |
| redis | 6379 | Redis 缓存 |
| minio | 9000/9001 | MinIO 对象存储 |
| backend | 8080 | Spring Boot 后端 |
| frontend | 80 | Nginx 前端 |

### 健康检查

```bash
# 后端健康检查
curl http://localhost:8080/actuator/health

# PostgreSQL
docker exec ai-teacher-postgres pg_isready -U postgres

# Redis
docker exec ai-teacher-redis redis-cli -a $REDIS_PASSWORD ping
```

## 常见问题

### Q: 视频生成失败？

1. 检查 FFmpeg 是否安装：`docker-compose exec backend ffmpeg -version`
2. 检查 MinIO 连接：`docker-compose logs backend | grep MinIO`
3. 检查 TTS 配置是否正确

### Q: AI 接口调用失败？

1. 检查 API Key 是否配置正确
2. 检查网络连接是否正常
3. 查看后端日志：`docker-compose logs backend | grep ERROR`

### Q: 如何添加新的 AI Provider？

1. 在 `provider/llm/` 创建新类，继承 `AbstractLLMProvider`
2. 实现 `chat()` 和 `generate()` 方法
3. 在 `AIProviderRegistry` 中注册

### Q: PPT 生成异常？

1. 检查 Node.js 环境：`docker-compose exec backend node -v`
2. 检查 pptxgenjs 安装：`docker-compose exec backend npm list pptxgenjs`
3. 查看脚本日志

## 项目路线图

- [x] Phase 1: 基础架构
- [x] Phase 2: AI 能力层
- [x] Phase 3: 核心功能
- [x] Phase 4: 高级功能
- [x] Phase 5: 前端开发
- [x] Phase 6: 验证完善

## 在线文档

技术文档部署在 GitHub Pages：

- 文档地址: https://growdu.github.io/ai_teacher/
- 自动构建: 每次 push 到 main 分支自动部署

| 文档 | 说明 |
|------|------|
| 安装部署 | Docker Compose 生产环境部署指南 |
| 开发指南 | 本地开发、环境配置、调试方法 |
| API 文档 | REST API 完整接口参考 |
| 架构设计 | 系统架构、模块设计 |
| 技术选型 | 技术栈选择理由 |
| 高层设计 | 核心流程设计 |

## 最近更新

### v1.1 - 认证流程修复 (2024-06-18)

- **Bug Fix**: 修复前端请求未携带认证 Token 的问题
  - 原因：`request.ts` 使用 `localStorage.getItem('token')` 但 Zustand 将 token 存在 `user-storage` 中
  - 修复：改为通过 Zustand store 正确读取 token
- **优化**: 完善 CORS 配置，支持前后端分离部署
- **文档**: 更新部署指南，完善 API 接口说明

### v1.0 - 初始版本 (2024-06-17)

- 支持课程、大纲、讲稿自动生成
- 支持 PPT 生成（4 种模板）
- 支持视频生成（TTS + 视频混流）
- 多租户支持
- AI Provider 动态切换

### 计划功能

- [ ] 支持更多 AI Provider (Gemini, Groq)
- [ ] 支持更多 PPT 模板
- [ ] 支持更多视频混流格式
- [ ] 支持团队协作
- [ ] 支持课程分享

## 贡献指南

欢迎提交 Issue 和 Pull Request！

## License

MIT License