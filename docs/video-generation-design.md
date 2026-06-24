# 视频生成模块设计

## 1. 模块定位

视频生成模块将课程的讲稿脚本（`Course.script` / `Course.scriptByChapter`）转换为可播看的教学视频。

```
Course（课程）
    │
    ├── script：完整讲稿 / 分章节讲稿
    ├── outline.title：视频标题
    └── outline.chapters[].slides[].visualGuidance：背景画面描述
           │
           ▼
┌─────────────────────────────────────────────────────────────┐
│                  VideoGenerationService                       │
│                                                              │
│  ┌────────────┐    ┌────────────┐    ┌────────────────────┐  │
│  │ TTS 语音合成 │ → │ 视频画面生成 │ → │ 音视频合成（FFmpeg）│  │
│  │  (AIService) │    │(MiniMax API)│    │                    │  │
│  └────────────┘    └────────────┘    └────────────────────┘  │
└────────────────────────────┬────────────────────────────────┘
                             ▼
                  TeachingMaterial(type=video)
```

---

## 2. 现状分析

### 2.1 现有 VideoGenerationService

```java
// 现有方法
public TeachingMaterial generateVideoAsync(VideoGenerateRequest request,
                                            Long userId, Long tenantId);
public TeachingMaterial generateVideo(VideoGenerateRequest request,
                                       Long userId, Long tenantId);
public VideoTaskStatusVO getTaskStatus(Long taskId);
```

### 2.2 现有 VideoController

| 路径 | 说明 |
|------|------|
| `POST /api/video/generate` | 异步生成，返回 taskId |
| `POST /api/video/generate/sync` | 同步生成，直接返回文件 |

### 2.3 现有问题

| 问题 | 说明 |
|------|------|
| 视频生成依赖 PPT 背景 | 需要先有 PPT 才能生成高质量视频 |
| script 是纯文本 | 没有和幻灯片对齐，无法做"音画同步" |
| 无虚拟人配置 | 没有形象/声音定制化 |
| MiniMax 任务无回调 | 轮询压力大，响应慢 |
| 缺少字幕 | 没有自动字幕生成 |

---

## 3. 目标

| 目标 | 说明 |
|------|------|
| 音画同步 | script 分段 + slides 对齐，每段语音对应一张幻灯片 |
| 多模态背景 | 支持 PPT背景 / 虚拟人 / AI 生成画面三种模式 |
| 字幕内嵌 | 自动生成 SRT 字幕，合成到视频 |
| 画质选项 | 720p / 1080p / 4K 多档 |
| 模板化 | 提供多种视频模板（知识讲解/实验演示/习题课） |

---

## 4. 数据模型

### 4.1 VideoGenerateRequest（扩展）

```java
public class VideoGenerateRequest {
    Long courseId;              // 课程ID（必填）
    Long pptMaterialId;         // PPT材料ID（可选，用于背景模式）

    // 人物配置
    String avatarId;             // 虚拟人形象ID（可选）
    String voiceId;              // 语音ID（可选，默认女声）
    Boolean hasAvatar;           // 是否使用虚拟人（默认false）

    // 视频配置
    String videoQuality;         // 画质：720p / 1080p / 4K（默认1080p）
    String videoStyle;           // 视频风格：lecture（知识讲解）/ experiment / exercise / review

    // 背景模式
    String backgroundMode;        // ppt（PPT背景）/ avatar（虚拟人）/ ai（AI生成画面）
    String script;               // 直接传入脚本（可选，优先用Course.script）

    // 字幕
    Boolean includeSubtitle;     // 是否内嵌字幕（默认true）
    String subtitleLanguage;     // 字幕语言：zh-CN / en（默认zh-CN）
}
```

### 4.2 TeachingMaterial 扩展（video类型）

```json
{
  "id": 1,
  "courseId": 1,
  "materialType": "video",
  "title": "二次函数及其图像 - 第1章",
  "fileUrl": "https://minio:9000/xxx.mp4",
  "thumbnailUrl": "https://minio:9000/xxx.jpg",
  "duration": 845,           // 秒
  "fileSize": 52428800,       // bytes
  "status": "ready",
  "metadata": {
    "taskId": "minimax-task-123",       // MiniMax任务ID
    "avatarId": "female-teacher-01",     // 虚拟人形象
    "voiceId": "zh-female-warm",        // 语音
    "quality": "1080p",
    "style": "lecture",
    "backgroundMode": "ppt",
    "subtitles": true
  }
}
```

---

## 5. 视频生成流程

### 5.1 完整流程

```
1. 获取 Course 数据
   ↓
2. 解析 scriptByChapter（分章节讲稿）
   ↓
3. 解析 outline.chapters[].slides（幻灯片信息，用于画面）
   ↓
4. 音画对齐（将 script 切片 → 每片对应一张 slide）
   ↓
5. TTS 语音合成（每段 script → audio）
   ↓
6. 视频画面生成（每张 slide → image/video）
   ↓
7. 字幕生成（语音 → ASR → SRT）
   ↓
8. 音视频合成（FFmpeg 合并）
   ↓
9. 上传 MinIO
   ↓
10. 更新 TeachingMaterial 记录
```

### 5.2 核心接口

```java
@Service
public class VideoGenerationService {

    @Autowired private CourseMapper courseMapper;
    @Autowired private TeachingMaterialMapper materialMapper;
    @Autowired private AIService aiService;
    @Autowired private MiniMaxVideoProvider miniMaxProvider;
    @Autowired private FileStorageService fileStorageService;

    /**
     * 异步生成视频（主入口）
     * 1. 获取Course数据
     * 2. 切分script到chapter级别
     * 3. 提交MiniMax任务（每个chapter一个子任务）
     * 4. 返回taskId，前端轮询
     */
    public VideoTaskVO generateVideoAsync(VideoGenerateRequest request,
                                           Long userId, Long tenantId);

    /**
     * 同步生成视频（用于短视频/测试）
     * 全流程在同一个线程完成
     */
    public TeachingMaterial generateVideoSync(VideoGenerateRequest request,
                                               Long userId, Long tenantId);

    /**
     * 音画对齐（核心步骤）
     * 将分章节讲稿和幻灯片列表按时间顺序对齐
     * @return List<VideoSegment> 每段：{slideIndex, script, audioUrl, imageUrl}
     */
    public List<VideoSegment> alignAudioVisual(Course course,
                                               List<SlideContent> slides);

    /**
     * 合成最终视频
     * @param segments 音画对齐后的片段列表
     * @param globalScript 完整讲稿（用于字幕生成）
     * @return 合成后的MP4文件路径
     */
    public Path合成FinalVideo(List<VideoSegment> segments,
                               String globalScript,
                               VideoConfig config);
}
```

### 5.3 视频片段模型

```java
public class VideoSegment {
    Integer chapterIndex;       // 章节索引
    Integer slideIndex;         // 幻灯片索引
    String script;              // 该片段的讲稿文本
    String audioUrl;            // TTS生成的音频URL
    String imageUrl;            // 幻灯片图片URL（或AI生成画面URL）
    String videoClipUrl;        // MiniMax生成的视频片段URL
    Integer durationSeconds;    // 该片段时长
    String subtitleText;        // 该片段字幕
}
```

---

## 6. 降级策略

当 MiniMax API 不可用时，按以下顺序降级：

| 优先级 | 方案 | 说明 |
|--------|------|------|
| 1 | MiniMax 原生视频 | 质量最高，支持虚拟人 |
| 2 | PPT幻灯片切换视频 | 用FFmpeg将PPT图片+语音合成为幻灯片视频 |
| 3 | 纯音频+字幕 | 只生成TTS语音+SRT字幕，无视频画面 |

---

## 7. 与 PPT 模块的关系

```
CourseGenerateService.generateCourse()
    │
    ├─→ 生成 Course.outline（chapters + slides）
    ├─→ 生成 Course.scriptByChapter（分章节讲稿）
    │
    ├─→ PptGenerationService.generatePpt(courseId)
    │       └─→ TeachingMaterial(type=ppt, fileUrl=PPTX)
    │
    └─→ VideoGenerationService.generateVideo(courseId)
            ├─→ 消费 Course.scriptByChapter（音频源）
            ├─→ 消费 TeachingMaterial(type=ppt).fileUrl（视频背景）
            └─→ TeachingMaterial(type=video, fileUrl=MP4)
```

**关键依赖**：视频生成**强依赖** PPT——PPT提供了每张幻灯片的视觉内容，视频模块将这些静态画面转化为动态视频。

**解耦设计**：通过 `pptMaterialId` 参数松耦合。如果传了 PPT ID，用 PPT 做背景；不传则用虚拟人或 AI 生成画面。

---

## 8. 实现计划

| 阶段 | 内容 | 优先级 |
|------|------|--------|
| Phase 1 | scriptByChapter 分段 + TTS 合成 | P0 |
| Phase 2 | PPT 背景视频生成（FFmpeg 幻灯片切换） | P0 |
| Phase 3 | MiniMax 原生视频生成 + 任务队列 | P1 |
| Phase 4 | 字幕生成（ASR） + 内嵌字幕 | P1 |
| Phase 5 | 虚拟人形象定制（avatarId + voiceId） | P2 |
| Phase 6 | 多画质选项（720p/1080p/4K） | P2 |
