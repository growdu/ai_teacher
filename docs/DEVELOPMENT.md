# AI Teacher Studio Development Guide

## Table of Contents

- [Development Environment](#development-environment)
- [Project Structure](#project-structure)
- [Backend Development](#backend-development)
- [Frontend Development](#frontend-development)
- [Database Development](#database-development)
- [AI Provider Development](#ai-provider-development)
- [Testing](#testing)
- [Code Style](#code-style)

---

## Development Environment

### Prerequisites

| Software | Version | Purpose |
|----------|---------|---------|
| Java | 21+ | Backend runtime |
| Node.js | 20+ | Frontend build |
| Maven | 3.9+ | Backend build |
| npm | 10+ | Frontend package management |
| Docker | 24.0+ | Containerization |
| PostgreSQL | 16+ | Local database |
| Redis | 7+ | Local cache |

### IDE Configuration

#### IntelliJ IDEA

**Backend:**
1. Import as Maven project
2. Set Java 21
3. Install plugins:
   - Lombok
   - MyBatisX

**Frontend:**
1. Use VS Code or WebStorm
2. Extensions:
   - ESLint
   - Prettier
   - TypeScript Vue Plugin (Volar)

### Local Service Setup

```bash
# Start infrastructure services only
docker-compose --env-file .env up -d postgres redis minio

# Verify services
docker-compose ps
```

---

## Project Structure

### Backend Structure

```
backend/
├── src/main/java/com/aiteacher/
│   ├── AiTeacherApplication.java       # Main entry point
│   │
│   ├── common/                        # Common utilities
│   │   └── R.java                     # Unified response wrapper
│   │
│   ├── config/                        # Configuration classes
│   │   ├── SecurityConfig.java        # Spring Security config
│   │   ├── MyBatisPlusConfig.java     # MyBatis Plus config
│   │   ├── WebSocketConfig.java       # WebSocket config
│   │   ├── TenantLineHandler.java     # Multi-tenant handler
│   │   ├── TenantContext.java         # Tenant context holder
│   │   ├── OpenApiConfig.java         # Swagger/OpenAPI config
│   │   └── CorsConfig.java            # CORS configuration
│   │
│   ├── controller/                    # REST controllers
│   │   ├── AuthController.java         # Authentication
│   │   ├── CourseController.java       # Course management
│   │   ├── KnowledgePointController.java
│   │   ├── MaterialController.java     # Teaching materials
│   │   ├── TaskController.java        # Async tasks
│   │   ├── AiConfigController.java    # AI configuration
│   │   ├── QuizController.java        # Quiz generation
│   │   ├── ResourceController.java    # File resources
│   │   ├── WorkspaceController.java  # Workspaces
│   │   └── TenantController.java     # Tenant management
│   │
│   ├── service/                       # Business logic
│   │   ├── AuthService.java
│   │   ├── CourseService.java
│   │   ├── CourseGenerateService.java # AI course generation
│   │   ├── PptGenerationService.java  # PPT generation
│   │   ├── VideoGenerationService.java # Video generation
│   │   ├── KnowledgePointService.java
│   │   ├── TeachingMaterialService.java
│   │   ├── AsyncTaskService.java
│   │   ├── AIConfigService.java
│   │   ├── QuizGenerationService.java
│   │   ├── ResourceService.java
│   │   ├── WorkspaceService.java
│   │   ├── TenantService.java
│   │   └── AIService.java             # Unified AI service
│   │
│   ├── mapper/                       # MyBatis mappers
│   │   ├── UserMapper.java
│   │   ├── CourseMapper.java
│   │   ├── KnowledgePointMapper.java
│   │   ├── TeachingMaterialMapper.java
│   │   ├── AsyncTaskMapper.java
│   │   ├── AiConfigMapper.java
│   │   ├── ResourceMapper.java
│   │   ├── WorkspaceMapper.java
│   │   └── TenantMapper.java
│   │
│   ├── entity/                       # JPA/MyBatis entities
│   │   ├── User.java
│   │   ├── Course.java
│   │   ├── KnowledgePoint.java
│   │   ├── TeachingMaterial.java
│   │   ├── AsyncTask.java
│   │   ├── AiConfig.java
│   │   ├── Resource.java
│   │   ├── Workspace.java
│   │   └── Tenant.java
│   │
│   ├── dto/                          # Data transfer objects
│   │   ├── LoginRequest.java
│   │   ├── LoginResponse.java
│   │   ├── CourseGenerateRequest.java
│   │   ├── CourseGenerateResponse.java
│   │   ├── CourseOutline.java
│   │   ├── PptGenerateRequest.java
│   │   ├── VideoGenerateRequest.java
│   │   ├── QuizGenerateRequest.java
│   │   └── QuizGenerateResponse.java
│   │
│   ├── provider/                     # AI Provider abstraction
│   │   ├── AIProviderRegistry.java   # Provider registry
│   │   ├── llm/                      # LLM providers
│   │   │   ├── AbstractLLMProvider.java
│   │   │   ├── OpenAILLMProvider.java
│   │   │   ├── ClaudeLLMProvider.java
│   │   │   └── QwenLLMProvider.java
│   │   └── tts/                      # TTS providers
│   │       └── AliyunTTSProvider.java
│   │
│   ├── filter/                       # Servlet filters
│   │   └── TenantFilter.java         # Multi-tenant filter
│   │
│   ├── exception/                    # Exception handling
│   │   ├── BusinessException.java
│   │   └── GlobalExceptionHandler.java
│   │
│   └── websocket/                    # WebSocket handlers
│       └── TaskWebSocketHandler.java
│
├── src/main/resources/
│   ├── application.yml              # Main config
│   └── schema.sql                   # Database schema
│
├── scripts/                          # Node.js scripts
│   └── generate-ppt.js              # PPT generation script
│
├── pom.xml                           # Maven dependencies
└── Dockerfile                        # Docker build
```

### Frontend Structure

```
frontend/
├── src/
│   ├── main.tsx                      # Entry point
│   ├── App.tsx                       # Root component
│   │
│   ├── api/                         # API layer
│   │   ├── request.ts               # Axios wrapper
│   │   └── types.ts                 # API types
│   │
│   ├── components/                  # Shared components
│   │   └── Layout.tsx               # Main layout
│   │
│   ├── pages/                       # Page components
│   │   ├── Login.tsx               # Login page
│   │   ├── Dashboard.tsx           # Dashboard
│   │   ├── KnowledgePage.tsx      # Knowledge point management
│   │   ├── CoursePage.tsx         # Course management
│   │   ├── MaterialPage.tsx       # Teaching materials
│   │   └── SettingsPage.tsx        # Settings
│   │
│   ├── router/                      # Routing
│   │   └── index.tsx              # Route definitions
│   │
│   ├── store/                      # State management
│   │   └── userStore.ts           # User state
│   │
│   └── index.css                   # Global styles
│
├── public/                         # Static assets
│
├── package.json
├── vite.config.ts                 # Vite configuration
├── tailwind.config.js              # Tailwind CSS config
├── tsconfig.json                   # TypeScript config
└── Dockerfile                      # Docker build
```

---

## Backend Development

### Build Commands

```bash
cd backend

# Clean and compile
mvn clean compile

# Run tests
mvn test

# Package
mvn clean package -DskipTests

# Run locally (requires local PostgreSQL, Redis, MinIO)
mvn spring-boot:run
```

### Add New Dependencies

Edit `pom.xml`:

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>example-lib</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Create New Entity

```java
// src/main/java/com/aiteacher/entity/NewEntity.java
package com.aiteacher.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("new_entity")
public class NewEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String name;
    
    private Long tenantId;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    @TableLogic
    private Boolean deleted;
}
```

### Create New Mapper

```java
// src/main/java/com/aiteacher/mapper/NewEntityMapper.java
package com.aiteacher.mapper;

import com.aiteacher.entity.NewEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NewEntityMapper extends BaseMapper<NewEntity> {
}
```

### Create New Service

```java
// src/main/java/com/aiteacher/service/NewEntityService.java
package com.aiteacher.service;

import com.aiteacher.entity.NewEntity;
import com.aiteacher.mapper.NewEntityMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class NewEntityService {

    @Autowired
    private NewEntityMapper newEntityMapper;

    public List<NewEntity> list(Long tenantId) {
        LambdaQueryWrapper<NewEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NewEntity::getTenantId, tenantId);
        wrapper.orderByDesc(NewEntity::getCreatedAt);
        return newEntityMapper.selectList(wrapper);
    }
}
```

### Create New Controller

```java
// src/main/java/com/aiteacher/controller/NewEntityController.java
package com.aiteacher.controller;

import com.aiteacher.common.R;
import com.aiteacher.entity.NewEntity;
import com.aiteacher.service.NewEntityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/new-entity")
public class NewEntityController {

    @Autowired
    private NewEntityService newEntityService;

    @GetMapping("/list")
    public R<List<NewEntity>> list(@RequestParam Long tenantId) {
        return R.ok(newEntityService.list(tenantId));
    }
}
```

---

## Frontend Development

### Build Commands

```bash
cd frontend

# Install dependencies
npm install

# Development server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

### Add New Page

1. Create page component in `src/pages/`:

```tsx
// src/pages/NewPage.tsx
import { Card } from 'antd'

const NewPage = () => {
  return (
    <div>
      <h1>New Page</h1>
      <Card>Content</Card>
    </div>
  )
}

export default NewPage
```

2. Add route in `src/router/index.tsx`:

```tsx
import NewPage from '@/pages/NewPage'

// Add to router.children:
{
  path: 'new-page',
  element: <NewPage />,
}
```

3. Add menu item in `Layout.tsx`:

```tsx
{
  key: '/new-page',
  icon: <ExampleIcon />,
  label: 'New Page',
}
```

### Add New API Method

Edit `src/api/request.ts`:

```typescript
export const newApi = {
  getData: (params: Params) => request.get('/endpoint', { params }),
  createData: (data: Data) => request.post('/endpoint', data),
}
```

### Use API in Component

```tsx
import { useEffect, useState } from 'react'
import { request } from '@/api/request'

const MyComponent = () => {
  const [data, setData] = useState([])

  useEffect(() => {
    const fetchData = async () => {
      const res = await request.get('/endpoint')
      setData(res.data)
    }
    fetchData()
  }, [])

  return <div>{/* render */}</div>
}
```

---

## Database Development

### Create Migration

Edit `init.sql`:

```sql
-- New table
CREATE TABLE IF NOT EXISTS new_table (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(128),
    created_at TIMESTAMP DEFAULT NOW()
);

-- Add indexes
CREATE INDEX idx_new_table_tenant ON new_table(tenant_id);
```

### MyBatis Plus Tips

#### QueryWrapper Usage

```java
// Basic query
LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(Course::getTenantId, tenantId)
       .like(Course::getTitle, keyword)
       .orderByDesc(Course::getCreatedAt);
List<Course> list = courseMapper.selectList(wrapper);

// Paginated query
Page<Course> page = new Page<>(pageNum, pageSize);
IPage<Course> result = courseMapper.selectPage(page, wrapper);
```

#### Update Operations

```java
// Update by ID
Course course = new Course();
course.setId(id);
course.setTitle("New Title");
courseMapper.updateById(course);

// Update with conditions
LambdaUpdateWrapper<Course> wrapper = new LambdaUpdateWrapper<>();
wrapper.eq(Course::getId, id)
       .set(Course::getTitle, "New Title");
courseMapper.update(null, wrapper);
```

---

## AI Provider Development

### Add New LLM Provider

1. Create provider class:

```java
// provider/llm/NewLLMProvider.java
package com.aiteacher.provider.llm;

@Slf4j
@Service
public class NewLLMProvider extends AbstractLLMProvider {
    
    @Autowired
    private WebClient webClient;
    
    @Value("${ai.new.api-key:}")
    private String apiKey;
    
    @Value("${ai.new.base-url:https://api.new.com}")
    private String baseUrl;
    
    @Override
    public String getProviderName() {
        return "newllm";
    }
    
    @Override
    public ChatResponse chat(String prompt) {
        // Implementation
    }
    
    @Override
    public String generate(String prompt) {
        return chat(prompt).getContent();
    }
}
```

2. Register in `AIProviderRegistry`:

```java
@Autowired
private NewLLMProvider newLLMProvider;

@PostConstruct
public void init() {
    providerRegistry.register(newLLMProvider);
}
```

3. Add configuration in `application.yml`:

```yaml
ai:
  new:
    api-key: ${NEW_API_KEY:}
    base-url: https://api.new.com
```

### Add New TTS Provider

1. Create TTS provider extending base class
2. Implement `synthesize()` method
3. Register in `AIProviderRegistry`

---

## Testing

### Backend Tests

```bash
cd backend

# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=CourseGenerateServiceTest

# Run with coverage
mvn test jacoco:report
```

### Frontend Tests

```bash
cd frontend

# Run tests
npm test

# Run with coverage
npm test -- --coverage
```

### Example Test

```java
@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseMapper courseMapper;

    @InjectMocks
    private CourseService courseService;

    @Test
    void testList() {
        // Given
        Long tenantId = 1L;
        
        // When
        List<Course> result = courseService.list(tenantId);
        
        // Then
        assertNotNull(result);
    }
}
```

---

## Code Style

### Backend (Java)

Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)

Key points:
- 4 spaces indentation
- 100 character line limit
- No trailing whitespace
- LF line endings

### Frontend (TypeScript)

Follow [Airbnb JavaScript Style Guide](https://github.com/airbnb/javascript)

Key points:
- 2 spaces indentation
- Single quotes for strings
- No semicolons
- ES6+ syntax preferred

### Formatting

**Backend:**
```bash
# Format with Maven
mvn formatter:format
```

**Frontend:**
```bash
# Format with Prettier
npm run format

# Lint
npm run lint
```

---

## Common Development Tasks

### Restart Backend Service

```bash
# In container
docker-compose restart backend

# Locally (if running with Maven)
# Stop and run again
mvn spring-boot:run
```

### Clear Redis Cache

```bash
docker-compose exec redis redis-cli -a $REDIS_PASSWORD FLUSHALL
```

### Rebuild Frontend

```bash
docker-compose exec frontend npm run build
# or
cd frontend && npm run build
```

### View Database

```bash
docker-compose exec postgres psql -U postgres -d ai_teacher

# In psql:
# \dt - list tables
# SELECT * FROM users LIMIT 10;
# \q - quit
```

### Access MinIO Console

1. Open http://localhost:9001
2. Login with `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD`
3. View buckets and files
