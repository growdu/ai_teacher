如果定位成**教学视频制作流程编排平台（Workflow Orchestration Platform）**，而不是单纯的视频生成工具，那么架构需要升级。

核心思想应变为：

> 平台本身不绑定任何 AI 模模型、TTS、数字人、视频生成厂商，而是提供统一的 AI 能力抽象层 + 工作流编排引擎，教师通过拖拽流程即可组合不同厂商能力。

这与目前的：

* [Dify](https://dify.ai?utm_source=chatgpt.com)
* [n8n](https://n8n.io?utm_source=chatgpt.com)
* [Flowise](https://flowiseai.com?utm_source=chatgpt.com)
* [Langflow](https://www.langflow.org?utm_source=chatgpt.com)

类似，但更加聚焦于**教学内容生产领域**。

---

# 一、架构升级

原架构：

```text
知识点
 ↓
课程生成
 ↓
PPT生成
 ↓
讲稿生成
 ↓
视频生成
```

升级后：

```text
知识点
 ↓
Workflow Engine
 ↓
Node Graph
 ↓
AI Provider Layer
 ↓
资源生产
```

即：

```text
平台 = 流程编排

模型 = 插件

厂商 = Provider
```

---

# 二、总体架构

```text
┌─────────────────────────────┐
│      Workflow Designer      │
└─────────────┬───────────────┘
              │
              ▼
┌─────────────────────────────┐
│      Workflow Engine        │
└─────────────┬───────────────┘
              │
 ┌────────────┼─────────────┐
 ▼            ▼             ▼

AI Layer   Media Layer   Tool Layer

 ▼            ▼             ▼

Provider   Provider      Provider
Registry   Registry      Registry

 ▼

OpenAI
Claude
Qwen
DeepSeek
Gemini
Azure OpenAI

 ▼

TTS
Avatar
Video
Image
PPT
```

---

# 三、新增核心模块

## 3.1 Provider Center

统一管理：

```text
模型
TTS
视频
图片
数字人
OCR
ASR
搜索
```

---

## Provider接口定义

```java
public interface AIProvider {

    ChatResponse chat(ChatRequest request);

    Stream<ChatChunk> stream(ChatRequest request);

    EmbeddingResponse embedding(
        EmbeddingRequest request);

}
```

---

## Provider注册

```json
{
  "provider":"openai",
  "model":"gpt-5",
  "apiKey":"xxxx",
  "baseUrl":"https://api.openai.com"
}
```

---

支持：

| Provider      | 类型   |
| ------------- | ---- |
| OpenAI        | LLM  |
| Anthropic     | LLM  |
| Google Gemini | LLM  |
| Qwen          | LLM  |
| DeepSeek      | LLM  |
| Moonshot      | LLM  |
| Azure OpenAI  | LLM  |
| Ollama        | 本地模型 |
| vLLM          | 私有部署 |
| Xinference    | 私有部署 |

---

# 四、模型配置中心

这是平台核心能力。

## Model Registry

```text
模型注册中心
```

数据模型：

```sql
CREATE TABLE ai_model
(
    id bigint,

    provider_id bigint,

    model_name varchar(128),

    model_type varchar(32),

    max_tokens int,

    context_length int,

    enabled boolean
);
```

---

模型类型：

```text
LLM

Embedding

Rerank

Image

Video

TTS

ASR

Avatar
```

---

例如：

```text
GPT-5
Claude Sonnet
DeepSeek-V3
Qwen-Max
Gemini 2.5
```

全部动态配置。

无需修改代码。

---

# 五、节点编排引擎

类似：

```text
Dify Workflow
ComfyUI
n8n
```

---

## 节点体系

```text
Start

Input

Knowledge

Prompt

LLM

Condition

Loop

PPT

TTS

Avatar

Video

Publish

End
```

---

## 教学视频流程

```text
Start
 ↓

知识点输入

 ↓

课程大纲节点

 ↓

讲稿节点

 ↓

PPT节点

 ↓

数字人节点

 ↓

视频合成节点

 ↓

发布节点

 ↓

End
```

---

# 六、教学领域专用节点

相比 Dify 增加：

## Curriculum Node

输入：

```text
牛顿第二定律
```

输出：

```json
{
  "chapters":[]
}
```

---

## Lesson Plan Node

输出：

```json
{
  "目标":"",
  "重点":"",
  "难点":""
}
```

---

## PPT Generate Node

调用：

```text
PptxGenJS
Marp
Gamma
```

---

输出：

```text
pptx
```

---

## Exam Generate Node

输出：

```json
{
  "questions":[]
}
```

---

## Classroom Interaction Node

生成：

```text
课堂提问

课堂讨论

课堂练习
```

---

# 七、AI能力抽象层

统一抽象。

## Chat能力

```java
interface ChatModel
{
   ChatResponse chat(
      Prompt prompt);
}
```

---

## Image能力

```java
interface ImageModel
{
    Image generate(
       String prompt);
}
```

---

## Video能力

```java
interface VideoModel
{
    Video generate(
        VideoRequest req);
}
```

---

## TTS能力

```java
interface TTSModel
{
   Audio speak(
       String text);
}
```

---

这样：

```text
Kling
Runway
Veo
Wan
```

都能接入。

---

# 八、工作流存储设计

## workflow

```sql
CREATE TABLE workflow
(
    id bigint,

    name varchar(255),

    version varchar(64),

    creator_id bigint,

    status varchar(32)
);
```

---

## workflow_node

```sql
CREATE TABLE workflow_node
(
    id bigint,

    workflow_id bigint,

    node_type varchar(64),

    config jsonb
);
```

---

## workflow_edge

```sql
CREATE TABLE workflow_edge
(
    id bigint,

    workflow_id bigint,

    source_node bigint,

    target_node bigint
);
```

---

## workflow_execution

```sql
CREATE TABLE workflow_execution
(
    id bigint,

    workflow_id bigint,

    status varchar(32),

    started_at timestamp,

    finished_at timestamp
);
```

---

# 九、多租户设计（团队创作）

你的场景已经涉及：

* 学校
* 培训机构
* 企业大学
* 个人教师

建议直接设计 SaaS 多租户。

```text
Tenant
  ├── Workspace
  │      ├── Workflow
  │      ├── Course
  │      ├── Video
  │      └── Assets
```

角色：

```text
超级管理员

机构管理员

教研员

教师

视频制作人

审核员

学生
```

---

# 十、最终架构定位

最终建议将产品定义为：

## AI Teacher Studio

副标题：

> 面向教育行业的 AI 工作流编排与课程生产平台

定位不再是：

```text
AI视频生成工具
```

而是：

```text
教育领域 Dify

+
教育领域 Canva

+
教育领域 CapCut

+
教育领域 数字人平台
```

核心竞争力是**流程编排 + 模型自由切换 + 教学领域专用节点 + 多人协作生产流水线**。

这样未来不仅能生成教学视频，还能扩展到：

* 课程包生成
* 题库生成
* 试卷生成
* 微课生成
* 企业培训课程生成
* AI助教
* AI教研平台

形成完整的教育内容生产中台。

