# 技术选型文档

## 1. 技术架构

```
┌─────────────────────────────────────────────────────────┐
│                    前端 (Web管理后台)                     │
│        React 18 + Ant Design + 视频预览播放器            │
└────────────────────────┬────────────────────────────────┘
                         │ HTTP REST
┌────────────────────────▼────────────────────────────────┐
│                    后端服务                              │
│  Spring Boot 3 + MyBatis-Plus + PostgreSQL              │
└────────────────────────┬────────────────────────────────┘
                         │
        ┌────────────────┼────────────────┐
        ▼                ▼                ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│  LLM服务集群  │ │  媒体服务集群 │ │  文件存储集群 │
└──────┬───────┘ └──────┬───────┘ └──────┬───────┘
       │                │                │
       ▼                ▼                ▼
  OpenAI/Claude    FFmpeg/数字人     MinIO/S3
  Qwen/DeepSeek    云厂商API         视频/PPT文件
```

---

## 2. 前端技术选型

### 2.1 核心框架
| 技术 | 版本 | 用途 |
|------|------|------|
| React | 18.x | UI框架 |
| TypeScript | 5.x | 类型安全 |
| Ant Design | 5.x | 组件库 |
| TailwindCSS | 3.x | 样式方案 |
| Axios | 1.x | HTTP请求 |
| React Player | 0.x | 视频播放 |
| xlsx | 0.x | PPT预览 |

### 2.2 选型理由
- **React 18**：生态成熟，组件化开发
- **Ant Design**：企业级UI，快速搭建后台
- **React Player**：支持MP4在线预览
- **xlsx**：前端预览PPT文件内容

---

## 3. 后端技术选型

### 3.1 核心框架
| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 21 | LTS版本 |
| Spring Boot | 3.2.x | 应用框架 |
| MyBatis-Plus | 3.5.x | ORM框架 |
| PostgreSQL | 16.x | 主数据库 |
| Redis | 7.x | 缓存 |
| MinIO | latest | S3兼容存储 |

### 3.2 工作流编排
| 技术 | 说明 |
|------|------|
| 自研编排引擎 | 轻量级流程控制，适合串行生成场景 |
| Spring Async | 异步任务执行 |
| Redis Queue | 任务队列 |

### 3.3 任务处理
```java
// 异步任务执行
@Async
public CompletableFuture<VideoResult> generateVideo(VideoRequest request) {
    // 1. 生成音频
    Audio audio = ttsService.synthesize(request.getScript());
    
    // 2. 合成视频
    Video video = videoService合成(audio, request.getAvatar());
    
    return CompletableFuture.completedFuture(video);
}
```

---

## 4. AI能力接入

### 4.1 LLM 服务
| 厂商 | 模型 | 用途 |
|------|------|------|
| OpenAI | GPT-4o | 课程大纲生成、讲稿生成 |
| Anthropic | Claude 3.5 Sonnet | 内容优化 |
| 阿里云 | Qwen-Max | 国内主力LLM |
| 深度求索 | DeepSeek-V3 | 成本优化 |

### 4.2 TTS 服务
| 厂商 | 特点 |
|------|------|
| 阿里云TTS | 中文效果好，支持多音色 |
| 火山引擎TTS | 性价比高 |
| Azure TTS | 多语言支持 |

### 4.3 数字人服务
| 厂商 | 说明 |
|------|------|
| 腾讯云数字人 | 2D数字人，口型合成 |
| 阿里云数字人 | 2D/3D数字人 |
| 华为云数字人 | 数字人视频合成 |

### 4.4 视频生成
| 方案 | 说明 |
|------|------|
| FFmpeg | 视频拼接、转码、裁剪 |
| 数字人厂商API | 数字人口播视频 |
| 混流服务 | 音频+画面合成 |

---

## 5. PPT生成技术方案

### 5.1 方案对比
| 方案 | 优点 | 缺点 |
|------|------|------|
| PptxGenJS | 编程控制，灵活度高 | 样式有限 |
| Apache POI | Java原生 | 复杂 |
| LibreOffice | 渲染模板 | 性能差 |
| 厂商API | 质量高 | 成本高 |

### 5.2 推荐方案：PptxGenJS
```javascript
// 示例：生成简单PPT
const pptxgen = require('pptxgenjs');
let pptx = new pptxgen();
let slide = pptx.addSlide();
slide.addText('牛顿第二定律', { x: 1, y: 1, fontSize: 32 });
slide.addText('F = ma', { x: 1, y: 2, fontSize: 24 });
pptx.writeFile({ fileName: 'output.pptx' });
```

### 5.3 PPT模板设计
```
templates/
├── default.pptx        # 默认模板
├── academic.pptx      # 学术风格
├── simple.pptx        # 简洁风格
└── modern.pptx        # 现代风格
```

---

## 6. 视频生成技术方案

### 6.1 视频生成流程
```
音频文件 ───┐
            ├──▶ 视频合成服务 ──▶ 输出MP4
PPT图片 ────┘
```

### 6.2 视频处理：FFmpeg
```bash
# 音频+图片混流
ffmpeg -i audio.mp3 -loop 1 -i slide.png \
  -c:v libx264 -c:a aac -shortest output.mp4

# 多图轮播
ffmpeg -i audio.mp3 -framerate 1 \
  -i "slide%d.png" -c:v libx264 -pix_fmt yuv420p output.mp4
```

### 6.3 数字人视频
```java
// 数字人服务调用
DigitalHumanVideo video = digitalHumanService.generateVideo(
    DigitalHumanRequest.builder()
        .script(request.getScript())
        .avatarId(request.getAvatarId())
        .voiceId(request.getVoiceId())
        .backgroundVideo(request.getPptVideo())
        .build()
);
```

---

## 7. 数据库设计

### 7.1 核心表结构
```sql
-- 租户表
CREATE TABLE tenant (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(128),
    created_at TIMESTAMP DEFAULT NOW()
);

-- 知识点表
CREATE TABLE knowledge_point (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT REFERENCES tenant(id),
    subject VARCHAR(64),
    grade VARCHAR(32),
    content TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- 课程表
CREATE TABLE course (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT REFERENCES tenant(id),
    knowledge_point_id BIGINT REFERENCES knowledge_point(id),
    outline JSONB,        -- 课程大纲
    script TEXT,          -- 讲稿脚本
    status VARCHAR(32),
    created_at TIMESTAMP DEFAULT NOW()
);

-- 教学材料表
CREATE TABLE teaching_material (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT REFERENCES tenant(id),
    course_id BIGINT REFERENCES course(id),
    material_type VARCHAR(32),  -- ppt/video
    file_url VARCHAR(512),
    duration INT,               -- 视频时长(秒)
    status VARCHAR(32),
    created_at TIMESTAMP DEFAULT NOW()
);

-- AI配置表
CREATE TABLE ai_config (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT REFERENCES tenant(id),
    provider VARCHAR(64),       -- openai/qwen/deepseek
    api_key_encrypted VARCHAR(512),
    model VARCHAR(128),
    enabled BOOLEAN DEFAULT TRUE
);
```

### 7.2 JSONB应用
```sql
-- 课程大纲结构示例
{
  "chapters": [
    {
      "title": "定律引入",
      "duration": "5min",
      "key_points": ["生活实例", "问题提出"]
    }
  ]
}
```

---

## 8. 文件存储

### 8.1 存储架构
```
MinIO (S3兼容)
├── bucket: teaching-materials
│   ├── /ppt/          # PPT文件
│   ├── /video/        # 视频文件
│   ├── /audio/        # 音频文件
│   └── /thumbnail/    # 缩略图
```

### 8.2 文件清理策略
| 类型 | 保留时间 | 说明 |
|------|----------|------|
| 视频文件 | 30天 | 生成后保留30天 |
| PPT文件 | 永久 | 用户可下载 |
| 音频文件 | 7天 | 中间产物 |
| 缩略图 | 永久 | 列表展示用 |

---

## 9. 部署架构

### 9.1 Docker Compose 开发环境
```yaml
version: '3.8'
services:
  api:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/ai_teacher
      REDIS_URL: redis://redis:6379
    depends_on:
      - postgres
      - redis

  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: ai_teacher
      POSTGRES_USER: ai_teacher
      POSTGRES_PASSWORD: secret
    volumes:
      - pgdata:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    volumes:
      - redisdata:/data

  minio:
    image: minio/minio
    command: server /data
    ports:
      - "9000:9000"
    volumes:
      - miniodata:/data

volumes:
  pgdata:
  redisdata:
  miniodata:
```

### 9.2 生产环境推荐
| 组件 | 推荐配置 |
|------|----------|
| API服务器 | 2核4G × 2台 |
| 数据库 | PostgreSQL 16, 4核8G |
| Redis | 2核4G |
| MinIO | 分布式部署 |

---

## 10. 技术选型总结

### 10.1 核心技术决策
| 决策点 | 选择 | 理由 |
|--------|------|------|
| 前端框架 | React 18 + Ant Design | 快速开发企业级后台 |
| 后端框架 | Spring Boot 3 | 企业级，稳定 |
| 数据库 | PostgreSQL | JSONB支持灵活配置 |
| PPT生成 | PptxGenJS | 编程控制，灵活 |
| 视频合成 | FFmpeg + 数字人API | 混流+数字人口播 |
| 文件存储 | MinIO | S3兼容，自托管 |
| LLM | OpenAI/Claude/Qwen | 多厂商备份 |

### 10.2 关键技术路径
```
知识点输入
    │
    ▼
LLM生成课程大纲 ──▶ 讲稿脚本
    │                   │
    ▼                   ▼
PPT生成 ◀────────┘   音频合成 (TTS)
    │                   │
    ▼                   ▼
视频混流 ◀────────┘   数字人视频
    │
    ▼
最终输出：PPT + 视频
```