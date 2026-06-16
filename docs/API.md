# AI Teacher Studio API Documentation

## Overview

Base URL: `http://localhost/api`

## Authentication

All authenticated endpoints require a JWT token in the Authorization header:

```
Authorization: Bearer <token>
```

## Response Format

All responses follow this format:

```json
{
  "code": 200,
  "msg": "success",
  "data": {}
}
```

| Field | Type | Description |
|-------|------|-------------|
| code | int | Status code (200 = success) |
| msg | string | Message |
| data | object | Response data |

## Error Codes

| Code | Description |
|------|-------------|
| 200 | Success |
| 400 | Bad Request |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |
| 500 | Internal Server Error |

---

## Authentication APIs

### Login

**POST** `/api/auth/login`

Login and get JWT token.

**Request:**
```json
{
  "email": "admin@aiteacher.com",
  "password": "admin123"
}
```

**Response:**
```json
{
  "code": 200,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": 1,
      "username": "admin",
      "email": "admin@aiteacher.com",
      "role": "admin"
    }
  }
}
```

---

## Tenant APIs

### Create Tenant

**POST** `/api/tenant`

**Request:**
```json
{
  "name": "学校A",
  "code": "school_a",
  "plan": "pro"
}
```

### List Tenants

**GET** `/api/tenant/list`

### Get Tenant

**GET** `/api/tenant/{id}`

### Update Tenant

**PUT** `/api/tenant/{id}`

### Tenant Page

**GET** `/api/tenant/page?pageNum=1&pageSize=10`

---

## Workspace APIs

### Create Workspace

**POST** `/api/workspace`

**Request:**
```json
{
  "name": "物理教研组",
  "settings": {}
}
```

### List Workspaces

**GET** `/api/workspace/list`

### Get Workspace

**GET** `/api/workspace/{id}`

### Update Workspace

**PUT** `/api/workspace/{id}`

### Delete Workspace

**DELETE** `/api/workspace/{id}`

---

## Knowledge Point APIs

### Create Knowledge Point

**POST** `/api/knowledge-point`

**Request:**
```json
{
  "subject": "物理",
  "grade": "高一",
  "content": "牛顿第二定律：F=ma",
  "tags": "力学,动力学"
}
```

### List Knowledge Points

**GET** `/api/knowledge-point/list?subject=物理&grade=高一`

### Get Knowledge Point

**GET** `/api/knowledge-point/{id}`

### Update Knowledge Point

**PUT** `/api/knowledge-point/{id}`

**Request:**
```json
{
  "subject": "物理",
  "grade": "高一",
  "content": "更新的内容",
  "tags": "力学"
}
```

### Delete Knowledge Point

**DELETE** `/api/knowledge-point/{id}`

---

## Course APIs

### Generate Course

**POST** `/api/course/generate`

Generate course outline and script from knowledge point.

**Request:**
```json
{
  "knowledgeContent": "牛顿第二定律：F=ma",
  "subject": "物理",
  "grade": "高一"
}
```

**Response:**
```json
{
  "code": 200,
  "data": {
    "courseId": 1,
    "title": "牛顿第二定律",
    "outline": {
      "chapters": [...]
    },
    "script": "各位同学..."
  }
}
```

### List Courses

**GET** `/api/course/list?pageNum=1&pageSize=10`

### Get Course

**GET** `/api/course/{id}`

### Update Course

**PUT** `/api/course/{id}`

**Request:**
```json
{
  "title": "更新的标题",
  "outline": {},
  "script": "更新的讲稿"
}
```

### Delete Course

**DELETE** `/api/course/{id}`

---

## Teaching Material APIs

### Generate PPT

**POST** `/api/material/ppt/generate`

**Request:**
```json
{
  "courseId": 1,
  "template": "default"
}
```

| Field | Type | Required | Description |
|-------|------|---------|-------------|
| courseId | long | Yes | Course ID |
| template | string | No | Template type: default/academic/simple/modern |

**Response:**
```json
{
  "code": 200,
  "data": {
    "materialId": 1,
    "status": "generated",
    "fileUrl": "http://minio:9000/ai-teacher/ppt/xxx.pptx"
  }
}
```

### Generate Video

**POST** `/api/material/video/generate`

Generate video from course script (async operation).

**Request:**
```json
{
  "courseId": 1
}
```

**Response:**
```json
{
  "code": 200,
  "data": {
    "taskId": 123,
    "status": "pending"
  }
}
```

### List Materials

**GET** `/api/material/list?courseId=1&type=ppt`

| Parameter | Type | Description |
|-----------|------|-------------|
| courseId | long | Filter by course |
| type | string | Filter by type: ppt/video |

### Get Material

**GET** `/api/material/{id}`

### Delete Material

**DELETE** `/api/material/{id}`

---

## Task APIs

### Get Task Status

**GET** `/api/task/{taskId}`

**Response:**
```json
{
  "code": 200,
  "data": {
    "taskId": 123,
    "taskType": "video_generation",
    "status": "processing",
    "progress": 50,
    "currentStep": "正在合成语音",
    "result": null,
    "errorMessage": null
  }
}
```

**Status Values:**
- `pending` - Task is waiting
- `processing` - Task is running
- `completed` - Task completed successfully
- `failed` - Task failed

### List Tasks

**GET** `/api/task/list?userId=1&status=processing`

---

## AI Config APIs

### Create AI Config

**POST** `/api/ai-config`

**Request:**
```json
{
  "provider": "openai",
  "model": "gpt-4o",
  "apiKey": "sk-xxx",
  "baseUrl": "https://api.openai.com",
  "priority": 3,
  "enabled": true
}
```

### List AI Configs

**GET** `/api/ai-config/list`

### Update AI Config

**PUT** `/api/ai-config/{id}`

### Delete AI Config

**DELETE** `/api/ai-config/{id}`

### Reload AI Provider

**POST** `/api/ai-config/reload?id=1`

Reload provider configuration.

### Get Provider Status

**GET** `/api/ai-config/status`

**Response:**
```json
{
  "code": 200,
  "data": {
    "availableProviders": ["openai", "claude", "qwen"],
    "activeProvider": "openai",
    "models": {
      "openai": "gpt-4o",
      "claude": "claude-3-5-sonnet-20241022"
    }
  }
}
```

---

## Quiz APIs

### Generate Quiz

**POST** `/api/quiz/generate`

Generate quiz questions from course.

**Request:**
```json
{
  "courseId": 1,
  "questionTypes": ["choice", "fill", "essay"],
  "difficulty": "medium",
  "count": 10
}
```

**Response:**
```json
{
  "code": 200,
  "data": {
    "quizId": 1,
    "questions": [
      {
        "type": "choice",
        "question": "牛顿第二定律描述了什么？",
        "options": ["A. 作用力与反作用力", "B. 加速度与力成正比", "C. 能量守恒", "D. 动量守恒"],
        "answer": "B"
      }
    ]
  }
}
```

---

## Resource APIs

### Upload Resource

**POST** `/api/resource/upload`

Multipart form data upload.

| Field | Type | Description |
|-------|------|-------------|
| file | file | File to upload |
| workspaceId | long | Optional workspace ID |

### List Resources

**GET** `/api/resource/list?workspaceId=1&resourceType=image`

### Get Resource

**GET** `/api/resource/{id}`

### Delete Resource

**DELETE** `/api/resource/{id}`

### Resource Page

**GET** `/api/resource/page?pageNum=1&pageSize=10&resourceType=video`

---

## WebSocket API

### Connection

```
ws://localhost/ws/tasks
```

### Subscribe to Task

Send:
```json
{
  "type": "subscribe",
  "taskId": "123"
}
```

Receive:
```json
{
  "type": "subscribed",
  "taskId": "123"
}
```

### Receive Task Updates

```json
{
  "type": "taskUpdate",
  "taskId": "123",
  "status": "processing",
  "progress": 75,
  "message": "正在合成语音..."
}
```

### Unsubscribe

Send:
```json
{
  "type": "unsubscribe"
}
```

---

## Environment Variables

| Variable | Description | Required |
|----------|-------------|----------|
| DATABASE_PASSWORD | PostgreSQL password | Yes |
| REDIS_PASSWORD | Redis password | Yes |
| MINIO_ROOT_USER | MinIO access key | Yes |
| MINIO_ROOT_PASSWORD | MinIO secret key | Yes |
| JWT_SECRET | JWT signing secret (256+ bits) | Yes |
| OPENAI_API_KEY | OpenAI API key | No |
| CLAUDE_API_KEY | Claude API key | No |
| QWEN_API_KEY | Qwen API key | No |

---

## Rate Limiting

Currently no rate limiting is enforced.

---

## Versioning

API is version 1.0. Future versions will be available at `/api/v2/`.
