#!/usr/bin/env node
/**
 * generate-ppt.js — Generates a PPTX file from a JSON config
 * Supports multiple templates: default | elegant | minimal | vibrant
 * Supports extended slide types: title, chapter, content, summary, end,
 *   diagram, timeline, comic, infographic, quote, activity, reflection,
 *   problem, concept, derivation, exercise,
 *   vote, poll, quick-fire, experiment, game, exit-ticket, result-viz
 * Usage: node generate-ppt.js <output.pptx> <config.json>
 */

const PptxGenJS = require('pptxgenjs');
const fs = require('fs');

const outputFile = process.argv[2];
const configFile = process.argv[3];

if (!outputFile || !configFile) {
  console.error('Usage: node generate-ppt.js <output.pptx> <config.json>');
  process.exit(1);
}

const config = JSON.parse(fs.readFileSync(configFile, 'utf8'));
const template = config.template || 'default';

const pptx = new PptxGenJS();
pptx.author = 'AI Teacher Studio';
pptx.title = config.title || '课程PPT';
pptx.subject = config.subtitle || '';

// ─────────────────────────────────────────────────────────────────────────────
// Template definitions (9 templates per design doc)
// ─────────────────────────────────────────────────────────────────────────────
const TEMPLATES = {
  default: {
    name: '学院蓝',
    primary:   '1F4E79',
    secondary: '2E75B6',
    accent:    '5B9BD5',
    dark:      '1C2833',
    light:     'F5F5F5',
    white:     'FFFFFF',
    text:      '2C3E50',
    subtext:   '7F8C8D',
    headerBg:  '2E75B6',
    cardBg:    'E8F0F8',
    // 渐变色（用于标题/章节页背景）
    gradientStart: '1F4E79',
    gradientEnd:   '2E75B6',
    gradientAngle: 270, // 从上到下
    // 装饰色
    decorColor: '5B9BD5',
  },
  elegant: {
    name: '典雅绿',
    primary:   '1A4731',
    secondary: '2D6A4F',
    accent:    '52B788',
    dark:      '1B2A1B',
    light:     'F0F7F2',
    white:     'FFFFFF',
    text:      '1C2833',
    subtext:   '52796F',
    headerBg:  '2D6A4F',
    cardBg:    'D8F0E4',
    gradientStart: '1A4731',
    gradientEnd:   '2D6A4F',
    gradientAngle: 270,
    decorColor: '52B788',
  },
  minimal: {
    name: '简约白',
    primary:   '2C3E50',
    secondary: '34495E',
    accent:    '7F8C8D',
    dark:      '1C2833',
    light:     'FAFAFA',
    white:     'FFFFFF',
    text:      '2C3E50',
    subtext:   '95A5A6',
    headerBg:  '34495E',
    cardBg:    'ECF0F1',
    gradientStart: '2C3E50',
    gradientEnd:   '34495E',
    gradientAngle: 270,
    decorColor: '7F8C8D',
  },
  vibrant: {
    name: '活力橙',
    primary:   'C0392B',
    secondary: 'E67E22',
    accent:    'F39C12',
    dark:      '1C2833',
    light:     'FEF9E7',
    white:     'FFFFFF',
    text:      '2C3E50',
    subtext:   '7F8C8D',
    headerBg:  'E67E22',
    cardBg:    'FDEBD0',
    gradientStart: 'C0392B',
    gradientEnd:   'E67E22',
    gradientAngle: 270,
    decorColor: 'F39C12',
  },
  academic: {
    name: '学术风',
    primary:   '1A3A5C',
    secondary: '2E6B8A',
    accent:    '5BA4C4',
    dark:      '1A2A3A',
    light:     'F0F5F8',
    white:     'FFFFFF',
    text:      '2C3E50',
    subtext:   '5D7A8C',
    headerBg:  '2E6B8A',
    cardBg:    'D5E8F0',
    gradientStart: '1A3A5C',
    gradientEnd:   '2E6B8A',
    gradientAngle: 270,
    decorColor: '5BA4C4',
  },
  'hand-drawn-edu': {
    name: '手绘教育风',
    primary:   '5D4E37',
    secondary: '8B7355',
    accent:    'F4A460',
    dark:      '3E2F1C',
    light:     'FDF5E6',
    white:     'FFFEF9',
    text:      '3E2F1C',
    subtext:   '7A6855',
    headerBg:  '8B7355',
    cardBg:    'F4E8D4',
    gradientStart: '5D4E37',
    gradientEnd:   '8B7355',
    gradientAngle: 270,
    decorColor: 'F4A460',
  },
  chalkboard: {
    name: '黑板风',
    primary:   '1A1A1A',
    secondary: '2D4A2D',
    accent:    '7CB342',
    dark:      '0D1A0D',
    light:     '1A2E1A',
    white:     'F5F5F5',
    text:      'E8F5E9',
    subtext:   'A5D6A7',
    headerBg:  '2D4A2D',
    cardBg:    '2E5D2E',
    gradientStart: '2D4A2D',
    gradientEnd:   '1A1A1A',
    gradientAngle: 270,
    decorColor: '7CB342',
  },
  kawaii: {
    name: '可爱风',
    primary:   'FF9ECD',
    secondary: 'FFE4EC',
    accent:    'FF6B9D',
    dark:      'D16B8E',
    light:     'FFF0F5',
    white:     'FFFFFF',
    text:      '6B4E5C',
    subtext:   'B48E9F',
    headerBg:  'FF9ECD',
    cardBg:    'FFE4EC',
    gradientStart: 'FF9ECD',
    gradientEnd:   'FF6B9D',
    gradientAngle: 270,
    decorColor: 'FF6B9D',
  },
  'corporate-memphis': {
    name: '孟菲斯风',
    primary:   '3B82F6',
    secondary: 'F472B6',
    accent:    'FBBF24',
    dark:      '1E3A5F',
    light:     'FEFBF6',
    white:     'FFFFFF',
    text:      '1E3A5F',
    subtext:   '6B7280',
    headerBg:  '3B82F6',
    cardBg:    'FEF3C7',
    gradientStart: '3B82F6',
    gradientEnd:   'F472B6',
    gradientAngle: 270,
    decorColor: 'FBBF24',
  },
};

const T = TEMPLATES[template] || TEMPLATES.default;

// 中文字体优先列表
const CHINESE_FONT = 'Microsoft YaHei';
const FALLBACK_FONT = 'Arial';

// 为文字元素添加中文字体支持
function makeFontOpts(base) {
  return { ...base, fontFace: CHINESE_FONT };
}

// ─────────────────────────────────────────────────────────────────────────────
// 渐变背景 helper（标题页/章节页使用）
// ─────────────────────────────────────────────────────────────────────────────
function addGradientBackground(slide, options = {}) {
  const angle = options.angle || T.gradientAngle || 270;
  const startColor = options.start || T.gradientStart || T.primary;
  const endColor   = options.end   || T.gradientEnd   || T.secondary;

  // PptxGenJS 渐变：angle 为 0-360 的角度，0 = 向上，90 = 向右，180 = 向下，270 = 向左
  // 转换为 PptxGenJS 的 format.GradientAngle（效果：颜色从浅到深沿角度方向变化）
  slide.background = {
    type: 'solid',
    color: startColor,
  };

  // 在幻灯片上叠加半透明渐变矩形，模拟渐变效果
  // 由于 PptxGenJS 的 solid background 不支持直接渐变，我们用多层透明矩形模拟
  const numLayers = 8;
  for (let i = 0; i < numLayers; i++) {
    const alpha = 1 - (i / numLayers);
    const blended = blendColor(startColor, endColor, i / numLayers);
    slide.addShape(pptx.ShapeType.rect, {
      x: 0, y: 0, w: 10, h: 5.625,
      fill: { color: blended, transparency: 100 - Math.round(alpha * 40) },
      line: { color: blended, width: 0, transparency: 100 },
    });
  }
}

// 颜色混合函数：hex 颜色混合
function blendColor(hex1, hex2, ratio) {
  const r1 = parseInt(hex1.slice(0, 2), 16);
  const g1 = parseInt(hex1.slice(2, 4), 16);
  const b1 = parseInt(hex1.slice(4, 6), 16);
  const r2 = parseInt(hex2.slice(0, 2), 16);
  const g2 = parseInt(hex2.slice(2, 4), 16);
  const b2 = parseInt(hex2.slice(4, 6), 16);
  const r = Math.round(r1 + (r2 - r1) * ratio);
  const g = Math.round(g1 + (g2 - g1) * ratio);
  const b = Math.round(b1 + (b2 - b1) * ratio);
  return ((r << 16) | (g << 8) | b).toString(16).padStart(6, '0');
}

// 添加装饰圆形
function addDecorCircles(slide, count = 3, options = {}) {
  const size = options.size || 0.8;
  const color = options.color || T.decorColor || T.accent;
  const opacity = options.opacity || 20; // 0-100，透明度百分比
  const positions = options.positions || [
    { x: -0.3, y: -0.3 },
    { x: 9.2, y: 4.8 },
    { x: 0.3, y: 4.5 },
  ];

  positions.slice(0, count).forEach(pos => {
    slide.addShape(pptx.ShapeType.ellipse, {
      x: pos.x, y: pos.y, w: size, h: size,
      fill: { color: color, transparency: opacity },
      line: { color: color, width: 0, transparency: 100 },
    });
  });
}

// 添加装饰线条
function addDecorLines(slide, options = {}) {
  const color = options.color || T.decorColor || T.accent;
  const thickness = options.thickness || 1.5;
  const opacity = options.opacity || 60;

  // 右上角对角线装饰
  slide.addShape(pptx.ShapeType.rect, {
    x: 8.5, y: 0, w: 1.5, h: thickness,
    fill: { color: color, transparency: opacity },
    line: { width: 0 },
  });
  slide.addShape(pptx.ShapeType.rect, {
    x: 9.85, y: 0, w: thickness, h: 1.2,
    fill: { color: color, transparency: opacity },
    line: { width: 0 },
  });

  // 左下角对角线装饰
  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 5.425, w: 1.5, h: thickness,
    fill: { color: color, transparency: opacity },
    line: { width: 0 },
  });
  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 4.425, w: thickness, h: 1.2,
    fill: { color: color, transparency: opacity },
    line: { width: 0 },
  });
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────
function addSlide() {
  return pptx.addSlide();
}

// ─────────────────────────────────────────────────────────────────────────────
// Title Slide (enhanced with gradient + decor + Chinese font)
// ─────────────────────────────────────────────────────────────────────────────
function createTitleSlide() {
  const slide = addSlide();

  // 渐变背景
  addGradientBackground(slide);

  // 装饰圆形
  addDecorCircles(slide, 3, {
    size: 1.2,
    color: T.decorColor,
    opacity: 15,
    positions: [
      { x: -0.5, y: -0.5 },
      { x: 8.8, y: 4.2 },
      { x: 0.5, y: 4.8 },
    ],
  });

  // 装饰线条
  addDecorLines(slide, { color: T.white, opacity: 30, thickness: 2 });

  // 顶部装饰条（渐变感）
  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 0.08,
    fill: { color: T.accent },
    line: { width: 0 },
  });

  // 左侧竖条装饰
  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 0.12, h: 5.625,
    fill: { color: T.accent },
    line: { width: 0 },
  });

  // 主标题
  slide.addText(config.title || '课程PPT', {
    x: 0.5, y: 1.5, w: 9, h: 1.4,
    fontSize: 42, bold: true, color: T.white,
    align: 'center', valign: 'middle',
    fontFace: CHINESE_FONT,
  });

  // 标题下划线装饰
  slide.addShape(pptx.ShapeType.rect, {
    x: 3.5, y: 2.95, w: 3, h: 0.06,
    fill: { color: T.accent },
    line: { width: 0 },
  });

  // 副标题
  if (config.subtitle) {
    slide.addText(config.subtitle, {
      x: 0.5, y: 3.15, w: 9, h: 0.7,
      fontSize: 20, color: T.accent,
      align: 'center', valign: 'middle',
      fontFace: CHINESE_FONT,
    });
  }

  // Subject / grade tags
  if (config.subject || config.grade) {
    const tags = [config.subject, config.grade].filter(Boolean).join('  ·  ');
    slide.addText(tags, {
      x: 0.5, y: 3.95, w: 9, h: 0.5,
      fontSize: 14, color: T.white,
      align: 'center', valign: 'middle',
      fontFace: CHINESE_FONT,
      transparency: 20,
    });
  }

  // 底部装饰条
  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 5.0, w: 10, h: 0.625,
    fill: { color: T.secondary },
    line: { width: 0 },
  });

  // 底部装饰条上的渐变叠加
  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 5.0, w: 10, h: 0.08,
    fill: { color: T.accent },
    line: { width: 0 },
  });

  slide.addText('AI Teacher Studio', {
    x: 0.5, y: 5.05, w: 9, h: 0.55,
    fontSize: 13, color: T.light,
    align: 'center', valign: 'middle',
    fontFace: CHINESE_FONT,
  });
}

// ─────────────────────────────────────────────────────────────────────────────
// Chapter Title Slide (enhanced with gradient + decor + Chinese font)
// ─────────────────────────────────────────────────────────────────────────────
function createChapterSlide(chapter, chapterIndex) {
  const slide = addSlide();

  // 渐变背景
  addGradientBackground(slide, { start: T.secondary, end: T.primary });

  // 装饰圆形
  addDecorCircles(slide, 2, {
    size: 1.5,
    color: T.accent,
    opacity: 12,
    positions: [
      { x: -0.7, y: 3.8 },
      { x: 8.5, y: -0.7 },
    ],
  });

  // 顶部装饰线
  slide.addShape(pptx.ShapeType.rect, {
    x: 0.7, y: 0.5, w: 2, h: 0.05,
    fill: { color: T.accent },
    line: { width: 0 },
  });

  // Left accent bar
  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 0.18, h: 5.625,
    fill: { color: T.accent },
    line: { width: 0 },
  });

  // Chapter number
  slide.addText(`第 ${chapterIndex} 章`, {
    x: 0.7, y: 0.7, w: 8.8, h: 0.6,
    fontSize: 18, color: T.accent, bold: false,
    align: 'left', valign: 'middle',
    fontFace: CHINESE_FONT,
  });

  // Chapter title
  slide.addText(chapter.title || chapter.name || '章节', {
    x: 0.7, y: 1.4, w: 8.8, h: 1.2,
    fontSize: 38, bold: true, color: T.white,
    align: 'left', valign: 'middle',
    fontFace: CHINESE_FONT,
  });

  // 标题下划装饰
  slide.addShape(pptx.ShapeType.rect, {
    x: 0.7, y: 2.65, w: 2.5, h: 0.06,
    fill: { color: T.accent },
    line: { width: 0 },
  });

  // Chapter description / objectives
  const descText = chapter.description || chapter.objective || null;
  if (descText) {
    slide.addText(descText, {
      x: 0.7, y: 2.85, w: 8.8, h: 0.8,
      fontSize: 14, color: T.light,
      align: 'left', valign: 'top',
      fontFace: CHINESE_FONT,
    });
  }

  // learningObjectives as bullet list
  const objectives = chapter.learningObjectives;
  if (objectives && Array.isArray(objectives) && objectives.length > 0) {
    const bulletItems = objectives.map((item, i) => ({
      text: item,
      options: {
        bullet: true,
        breakLine: i < objectives.length - 1,
        color: T.light,
        fontSize: 13,
        fontFace: CHINESE_FONT,
      },
    }));
    slide.addText(bulletItems, {
      x: 0.7, y: 2.85, w: 8.8, h: 1.0,
      valign: 'top',
      paraSpaceAfter: 4,
    });
  }

  // Duration tag
  if (chapter.duration) {
    slide.addShape(pptx.ShapeType.rect, {
      x: 0.7, y: 4.4, w: 1.5, h: 0.4,
      fill: { color: T.accent },
      line: { width: 0 },
    });
    slide.addText(`${chapter.duration}分钟`, {
      x: 0.7, y: 4.4, w: 1.5, h: 0.4,
      fontSize: 12, color: T.white,
      align: 'center', valign: 'middle',
      fontFace: CHINESE_FONT,
    });
  }

  // Bottom accent line
  slide.addShape(pptx.ShapeType.rect, {
    x: 0.7, y: 4.95, w: 2, h: 0.06,
    fill: { color: T.accent },
    line: { width: 0 },
  });
}

// ─────────────────────────────────────────────────────────────────────────────
// Content Slide (existing)
// ─────────────────────────────────────────────────────────────────────────────
function createContentSlide(chapter, chapterIndex) {
  const slide = addSlide();
  slide.background = { color: T.light };

  // Header bar
  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 0.85,
    fill: { color: T.primary },
  });

  // Chapter label in header
  slide.addText(`第 ${chapterIndex} 章  ${chapter.title || ''}`, {
    x: 0.4, y: 0, w: 9.2, h: 0.85,
    fontSize: 16, bold: true, color: T.white,
    align: 'left', valign: 'middle',
  });

  // Slide title
  const slideTitle = chapter.slideTitle || chapter.title || '';
  if (slideTitle) {
    slide.addText(slideTitle, {
      x: 0.5, y: 1.05, w: 9, h: 0.55,
      fontSize: 22, bold: true, color: T.primary,
      align: 'left', valign: 'middle',
    });
  }

  // Content bullets
  const contentItems = chapter.contentList || chapter.content || [];
  if (contentItems.length > 0) {
    const bulletItems = contentItems.map((item, i) => ({
      text: item,
      options: {
        bullet: { type: 'number' },
        breakLine: i < contentItems.length - 1,
        color: T.text,
        fontSize: 15,
      },
    }));
    slide.addText(bulletItems, {
      x: 0.5, y: 1.7, w: 9, h: 3.2,
      valign: 'top',
      paraSpaceAfter: 8,
    });
  }

  // Key points highlight box
  const keyPoints = chapter.keyPoints || [];
  if (keyPoints.length > 0) {
    slide.addShape(pptx.ShapeType.rect, {
      x: 0.5, y: 4.6, w: 9, h: 0.8,
      fill: { color: T.cardBg },
      line: { color: T.accent, width: 1 },
    });
    slide.addText('重点：' + keyPoints.join('  |  '), {
      x: 0.7, y: 4.6, w: 8.6, h: 0.8,
      fontSize: 12, color: T.subtext,
      align: 'left', valign: 'middle',
    });
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Summary Slide (enhanced)
// ─────────────────────────────────────────────────────────────────────────────
function createSummarySlide(chapters) {
  const slide = addSlide();
  slide.background = { color: T.primary };

  // 装饰圆形
  addDecorCircles(slide, 4, {
    size: 1.0,
    color: T.accent,
    opacity: 10,
    positions: [
      { x: -0.4, y: -0.4 },
      { x: 9.0, y: -0.3 },
      { x: 8.8, y: 4.5 },
      { x: -0.3, y: 4.6 },
    ],
  });

  // 顶部装饰线
  slide.addShape(pptx.ShapeType.rect, {
    x: 4, y: 0.4, w: 2, h: 0.06,
    fill: { color: T.accent },
    line: { width: 0 },
  });

  slide.addText('本章小结', {
    x: 0.5, y: 0.5, w: 9, h: 0.7,
    fontSize: 28, bold: true, color: T.white,
    align: 'center', valign: 'middle',
    fontFace: CHINESE_FONT,
  });

  // Accent line under title
  slide.addShape(pptx.ShapeType.rect, {
    x: 4, y: 1.2, w: 2, h: 0.05,
    fill: { color: T.accent },
    line: { width: 0 },
  });

  // 左侧装饰条
  slide.addShape(pptx.ShapeType.rect, {
    x: 0.8, y: 1.5, w: 0.08, h: 3.5,
    fill: { color: T.accent },
    line: { width: 0 },
  });

  if (chapters && chapters.length > 0) {
    const items = chapters.map((ch, i) => ({
      text: `第 ${i + 1} 章：${ch.title || ch.name || ''}`,
      options: {
        bullet: true,
        breakLine: i < chapters.length - 1,
        color: T.white,
        fontSize: 16,
        fontFace: CHINESE_FONT,
      },
    }));
    slide.addText(items, {
      x: 1.1, y: 1.5, w: 8, h: 3.5,
      valign: 'top',
      paraSpaceAfter: 12,
    });
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// End Slide (enhanced with gradient + decor)
// ─────────────────────────────────────────────────────────────────────────────
function createEndSlide() {
  const slide = addSlide();

  // 渐变背景（从 dark 到 primary）
  addGradientBackground(slide, { start: T.dark, end: T.primary });

  // 装饰圆形
  addDecorCircles(slide, 3, {
    size: 1.8,
    color: T.accent,
    opacity: 10,
    positions: [
      { x: -0.8, y: -0.8 },
      { x: 8.2, y: 3.8 },
      { x: 0.5, y: 4.5 },
    ],
  });

  // 装饰线条
  addDecorLines(slide, { color: T.accent, opacity: 40, thickness: 2 });

  // 顶部装饰条
  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 0.08,
    fill: { color: T.accent },
    line: { width: 0 },
  });

  // 中央装饰圆
  slide.addShape(pptx.ShapeType.ellipse, {
    x: 4.25, y: 1.2, w: 1.5, h: 1.5,
    fill: { color: T.accent, transparency: 80 },
    line: { color: T.accent, width: 2 },
  });

  slide.addText('谢谢观看', {
    x: 0.5, y: 2.0, w: 9, h: 1.2,
    fontSize: 48, bold: true, color: T.white,
    align: 'center', valign: 'middle',
    fontFace: CHINESE_FONT,
  });

  // 下划装饰
  slide.addShape(pptx.ShapeType.rect, {
    x: 3.5, y: 3.3, w: 3, h: 0.06,
    fill: { color: T.accent },
    line: { width: 0 },
  });

  slide.addText('AI Teacher Studio', {
    x: 0.5, y: 3.5, w: 9, h: 0.6,
    fontSize: 16, color: T.accent,
    align: 'center', valign: 'middle',
    fontFace: CHINESE_FONT,
  });

  // 底部装饰条
  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 5.2, w: 10, h: 0.425,
    fill: { color: T.secondary },
    line: { width: 0 },
  });
}

// ─────────────────────────────────────────────────────────────────────────────
// NEW: Problem Slide (理科问题引入页)
// ─────────────────────────────────────────────────────────────────────────────
function createProblemSlide(slideData, chapterIndex) {
  const slide = addSlide();
  slide.background = { color: T.primary };

  // Header bar
  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 0.85,
    fill: { color: T.secondary },
  });
  slide.addText(`第 ${chapterIndex} 章  问题引入`, {
    x: 0.4, y: 0, w: 9.2, h: 0.85,
    fontSize: 16, bold: true, color: T.white,
    align: 'left', valign: 'middle',
  });

  // Problem question - large centered text
  const question = slideData.mainBody || slideData.content || slideData.question || '生活中的问题：...';
  slide.addText(question, {
    x: 0.5, y: 1.5, w: 9, h: 2.5,
    fontSize: 28, bold: true, color: T.white,
    align: 'center', valign: 'middle',
  });

  // Scenario hint
  if (slideData.scenario || slideData.visualGuidance) {
    slide.addText(slideData.scenario || slideData.visualGuidance, {
      x: 0.5, y: 4.2, w: 9, h: 0.6,
      fontSize: 14, color: T.accent,
      align: 'center', valign: 'middle',
    });
  }

  // Duration badge
  if (slideData.duration) {
    slide.addText(`${slideData.duration}分钟`, {
      x: 8.3, y: 5.0, w: 1.4, h: 0.4,
      fontSize: 11, color: T.light,
      align: 'center', valign: 'middle',
      fill: { color: T.accent },
    });
  }

  // Teacher note
  if (slideData.teacherNote) {
    slide.addText('📝 ' + slideData.teacherNote, {
      x: 0.5, y: 4.9, w: 7.5, h: 0.5,
      fontSize: 11, color: T.subtext,
      align: 'left', valign: 'middle',
    });
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// NEW: Concept Slide (理科概念讲解页)
// ─────────────────────────────────────────────────────────────────────────────
function createConceptSlide(slideData, chapterIndex) {
  const slide = addSlide();
  slide.background = { color: T.light };

  // Header bar
  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 0.85,
    fill: { color: T.primary },
  });
  slide.addText(`第 ${chapterIndex} 章  概念讲解`, {
    x: 0.4, y: 0, w: 9.2, h: 0.85,
    fontSize: 16, bold: true, color: T.white,
    align: 'left', valign: 'middle',
  });

  // Slide title
  if (slideData.title) {
    slide.addText(slideData.title, {
      x: 0.5, y: 1.0, w: 9, h: 0.6,
      fontSize: 24, bold: true, color: T.primary,
      align: 'left', valign: 'middle',
    });
  }

  // Formula box (if present) — enhanced with border and shadow effect
  if (slideData.formula) {
    // 外框（深色描边效果）
    slide.addShape(pptx.ShapeType.rect, {
      x: 0.45, y: 1.65, w: 9.1, h: 0.95,
      fill: { color: T.primary },
      line: { width: 0 },
    });
    // 内框（公式背景）
    slide.addShape(pptx.ShapeType.rect, {
      x: 0.5, y: 1.7, w: 9, h: 0.9,
      fill: { color: T.cardBg },
      line: { color: T.accent, width: 2 },
    });
    // 公式标签
    slide.addShape(pptx.ShapeType.rect, {
      x: 0.5, y: 1.7, w: 0.8, h: 0.35,
      fill: { color: T.accent },
      line: { width: 0 },
    });
    slide.addText('公式', {
      x: 0.5, y: 1.7, w: 0.8, h: 0.35,
      fontSize: 10, bold: true, color: T.white,
      align: 'center', valign: 'middle',
      fontFace: CHINESE_FONT,
    });
    slide.addText(slideData.formula, {
      x: 0.5, y: 1.7, w: 9, h: 0.9,
      fontSize: 20, bold: true, color: T.primary,
      align: 'center', valign: 'middle',
      fontFace: CHINESE_FONT,
    });
  }

  // Main body text
  if (slideData.mainBody) {
    slide.addText(slideData.mainBody, {
      x: 0.5, y: 2.7, w: 9, h: 1.0,
      fontSize: 14, color: T.text,
      align: 'left', valign: 'top',
      fontFace: CHINESE_FONT,
    });
  }

  // Key points bullets
  const keyPoints = slideData.bullets || slideData.keyPoints || [];
  if (keyPoints.length > 0) {
    const bulletItems = keyPoints.map((item, i) => ({
      text: item,
      options: {
        bullet: true,
        breakLine: i < keyPoints.length - 1,
        color: T.text,
        fontSize: 14,
        fontFace: CHINESE_FONT,
      },
    }));
    slide.addText(bulletItems, {
      x: 0.5, y: 3.7, w: 9, h: 1.6,
      valign: 'top',
      paraSpaceAfter: 6,
    });
  }

  // Duration badge
  if (slideData.duration) {
    slide.addText(`${slideData.duration}分钟`, {
      x: 8.3, y: 5.0, w: 1.4, h: 0.4,
      fontSize: 11, color: T.white,
      align: 'center', valign: 'middle',
      fill: { color: T.accent },
    });
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// NEW: Derivation Slide (理科推导/证明页)
// ─────────────────────────────────────────────────────────────────────────────
function createDerivationSlide(slideData, chapterIndex) {
  const slide = addSlide();
  slide.background = { color: T.light };

  // Header bar
  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 0.85,
    fill: { color: T.primary },
  });
  slide.addText(`第 ${chapterIndex} 章  推导证明`, {
    x: 0.4, y: 0, w: 9.2, h: 0.85,
    fontSize: 16, bold: true, color: T.white,
    align: 'left', valign: 'middle',
  });

  // Title
  if (slideData.title) {
    slide.addText(slideData.title, {
      x: 0.5, y: 1.0, w: 9, h: 0.5,
      fontSize: 22, bold: true, color: T.primary,
      align: 'left', valign: 'middle',
    });
  }

  // Step-by-step derivation
  const steps = slideData.steps || slideData.bullets || [slideData.mainBody];
  if (steps.length > 0) {
    const stepItems = steps.map((step, i) => ({
      text: `${i + 1}. ${step}`,
      options: {
        breakLine: i < steps.length - 1,
        color: T.text,
        fontSize: 15,
      },
    }));
    slide.addText(stepItems, {
      x: 0.5, y: 1.6, w: 9, h: 3.2,
      valign: 'top',
      paraSpaceAfter: 12,
    });
  }

  // Conclusion box
  if (slideData.formula || slideData.conclusion) {
    slide.addShape(pptx.ShapeType.rect, {
      x: 0.5, y: 4.6, w: 9, h: 0.7,
      fill: { color: T.cardBg },
      line: { color: T.accent, width: 1 },
    });
    slide.addText('得证：' + (slideData.formula || slideData.conclusion), {
      x: 0.7, y: 4.6, w: 8.6, h: 0.7,
      fontSize: 16, bold: true, color: T.primary,
      align: 'left', valign: 'middle',
    });
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// NEW: Exercise Slide (例题演练页)
// ─────────────────────────────────────────────────────────────────────────────
function createExerciseSlide(slideData, chapterIndex) {
  const slide = addSlide();
  slide.background = { color: T.light };

  // Header bar
  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 0.85,
    fill: { color: T.primary },
  });
  slide.addText(`第 ${chapterIndex} 章  例题演练`, {
    x: 0.4, y: 0, w: 9.2, h: 0.85,
    fontSize: 16, bold: true, color: T.white,
    align: 'left', valign: 'middle',
  });

  // Example problem label
  slide.addShape(pptx.ShapeType.rect, {
    x: 0.5, y: 1.0, w: 1.2, h: 0.4,
    fill: { color: T.accent },
  });
  slide.addText('例题', {
    x: 0.5, y: 1.0, w: 1.2, h: 0.4,
    fontSize: 12, bold: true, color: T.white,
    align: 'center', valign: 'middle',
  });

  // Problem statement — 扩大fallback链：exampleProblem → mainBody → bullets[0] → contentList[0] → content
  const rawProblem = slideData.exampleProblem || slideData.mainBody
    || (slideData.bullets && slideData.bullets[0])
    || (slideData.contentList && slideData.contentList[0])
    || slideData.content
    || '请解答以下问题...';

  // 判断bullets[0]是否被用作了problem（用于solution fallback去重）
  const bulletsUsedAsProblem = !slideData.exampleProblem && !slideData.mainBody && slideData.bullets && slideData.bullets[0];

  slide.addText(rawProblem, {
    x: 0.5, y: 1.5, w: 9, h: 0.9,
    fontSize: 16, color: T.text,
    align: 'left', valign: 'top',
  });

  // Solution section — 扩大fallback链：
  // solution → bullets(排除已用作problem的) → steps → contentList(当无其他时)
  // 注意：content 不作为 solution，避免与 problem 重复
  const bulletsForSolution = bulletsUsedAsProblem
    ? (slideData.bullets ? slideData.bullets.slice(1) : [])
    : (slideData.bullets || []);
  const rawSolution = slideData.solution
    || (bulletsForSolution.length > 0 ? '【解题步骤】\n' + bulletsForSolution.join('\n') : null)
    || (slideData.steps && slideData.steps.length > 0 ? '【推导过程】\n' + slideData.steps.join('\n') : null)
    || (slideData.contentList && slideData.contentList.length > 0 && !slideData.bullets ? '【内容要点】\n' + slideData.contentList.join('\n') : null);
  if (rawSolution) {
    slide.addShape(pptx.ShapeType.rect, {
      x: 0.5, y: 2.45, w: 9, h: 0.35,
      fill: { color: T.cardBg },
    });
    slide.addText('解题步骤', {
      x: 0.7, y: 2.45, w: 2, h: 0.35,
      fontSize: 12, bold: true, color: T.subtext,
      align: 'left', valign: 'middle',
    });

    slide.addText(rawSolution, {
      x: 0.5, y: 2.9, w: 9, h: 1.2,
      fontSize: 14, color: T.text,
      align: 'left', valign: 'top',
    });
  }

  // Variants/变式题 — 扩大fallback链：variants → bullets(排除首个) → points
  // 注意：contentList/steps 不作为 variants（语义不符）
  const allBullets = slideData.bullets || slideData.points || [];
  const variants = slideData.variants
    || (allBullets.length > 1 ? allBullets.slice(1) : [])
    || [];
  if (variants.length > 0) {
    slide.addShape(pptx.ShapeType.rect, {
      x: 0.5, y: 4.15, w: 9, h: 0.35,
      fill: { color: T.secondary },
    });
    slide.addText('变式训练题', {
      x: 0.7, y: 4.15, w: 2, h: 0.35,
      fontSize: 12, bold: true, color: T.white,
      align: 'left', valign: 'middle',
    });

    const variantItems = variants.map((v, i) => ({
      text: `${i + 1}. ${v}`,
      options: {
        breakLine: i < variants.length - 1,
        color: T.text,
        fontSize: 13,
      },
    }));
    slide.addText(variantItems, {
      x: 0.5, y: 4.6, w: 9, h: 1.0,
      valign: 'top',
      paraSpaceAfter: 4,
    });
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// NEW: Diagram Slide (流程图/对比图/时间线)
// ─────────────────────────────────────────────────────────────────────────────
function createDiagramSlide(slideData, chapterIndex) {
  const slide = addSlide();
  slide.background = { color: T.light };

  // Header bar
  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 0.85,
    fill: { color: T.primary },
  });
  slide.addText(`第 ${chapterIndex} 章  ${slideData.diagramType || '图示'}`, {
    x: 0.4, y: 0, w: 9.2, h: 0.85,
    fontSize: 16, bold: true, color: T.white,
    align: 'left', valign: 'middle',
  });

  // Slide title
  if (slideData.title) {
    slide.addText(slideData.title, {
      x: 0.5, y: 1.0, w: 9, h: 0.5,
      fontSize: 22, bold: true, color: T.primary,
      align: 'left', valign: 'middle',
    });
  }

  // Diagram type indicator
  const diagramType = slideData.diagramType || 'flowchart';
  slide.addText(`[${diagramType}]`, {
    x: 8.0, y: 1.0, w: 1.7, h: 0.4,
    fontSize: 11, color: T.accent,
    align: 'right', valign: 'middle',
  });

  // Diagram content - bullets arranged as steps
  const items = slideData.bullets || slideData.contentList || [];
  if (items.length > 0) {
    const startY = 1.7;
    const boxHeight = 0.65;
    const gap = 0.15;

    items.forEach((item, i) => {
      const y = startY + i * (boxHeight + gap);

      // Box
      slide.addShape(pptx.ShapeType.rect, {
        x: 0.8, y: y, w: 7.5, h: boxHeight,
        fill: { color: i === 0 ? T.primary : (i === items.length - 1 ? T.accent : T.secondary) },
        line: { color: T.primary, width: 1 },
      });

      // Step number
      slide.addText(`${i + 1}`, {
        x: 0.8, y: y, w: 0.6, h: boxHeight,
        fontSize: 14, bold: true, color: T.white,
        align: 'center', valign: 'middle',
      });

      // Text
      slide.addText(item, {
        x: 1.5, y: y, w: 6.7, h: boxHeight,
        fontSize: 14, color: T.white,
        align: 'left', valign: 'middle',
      });

      // Arrow connector (except last)
      if (i < items.length - 1) {
        slide.addText('▼', {
          x: 4.0, y: y + boxHeight - 0.05, w: 1, h: 0.3,
          fontSize: 12, color: T.subtext,
          align: 'center', valign: 'middle',
        });
      }
    });
  }

  // Duration badge
  if (slideData.duration) {
    slide.addText(`${slideData.duration}分钟`, {
      x: 8.3, y: 5.0, w: 1.4, h: 0.4,
      fontSize: 11, color: T.white,
      align: 'center', valign: 'middle',
      fill: { color: T.accent },
    });
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// NEW: Timeline Slide (历史/发展时间线)
// ─────────────────────────────────────────────────────────────────────────────
function createTimelineSlide(slideData, chapterIndex) {
  const slide = addSlide();
  slide.background = { color: T.light };

  // Header bar
  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 0.85,
    fill: { color: T.primary },
  });
  slide.addText(`第 ${chapterIndex} 章  时间线`, {
    x: 0.4, y: 0, w: 9.2, h: 0.85,
    fontSize: 16, bold: true, color: T.white,
    align: 'left', valign: 'middle',
  });

  // Slide title
  if (slideData.title) {
    slide.addText(slideData.title, {
      x: 0.5, y: 1.0, w: 9, h: 0.5,
      fontSize: 22, bold: true, color: T.primary,
      align: 'left', valign: 'middle',
    });
  }

  // Timeline line
  slide.addShape(pptx.ShapeType.rect, {
    x: 0.5, y: 2.8, w: 9, h: 0.05,
    fill: { color: T.secondary },
  });

  // Timeline items
  const items = slideData.bullets || slideData.contentList || [];
  const spacing = 9 / Math.max(items.length, 1);

  items.forEach((item, i) => {
    const x = 0.5 + i * spacing + spacing / 2;

    // Dot on timeline
    slide.addShape(pptx.ShapeType.ellipse, {
      x: x - 0.15, y: 2.68, w: 0.3, h: 0.3,
      fill: { color: T.accent },
    });

    // Label above
    slide.addText(item.year || item.label || `Step ${i + 1}`, {
      x: x - 0.8, y: 2.2, w: 1.6, h: 0.4,
      fontSize: 11, bold: true, color: T.primary,
      align: 'center', valign: 'middle',
    });

    // Description below
    slide.addText(item.event || item.description || item, {
      x: x - 1, y: 3.1, w: 2, h: 1.2,
      fontSize: 10, color: T.text,
      align: 'center', valign: 'top',
    });
  });

  // Duration badge
  if (slideData.duration) {
    slide.addText(`${slideData.duration}分钟`, {
      x: 8.3, y: 5.0, w: 1.4, h: 0.4,
      fontSize: 11, color: T.white,
      align: 'center', valign: 'middle',
      fill: { color: T.accent },
    });
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// NEW: Comic Slide (知识漫画页 - uses assetUrl)
// ─────────────────────────────────────────────────────────────────────────────
function createComicSlide(slideData, chapterIndex) {
  const slide = addSlide();
  slide.background = { color: T.light };

  // Header bar
  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 0.85,
    fill: { color: T.primary },
  });
  slide.addText(`第 ${chapterIndex} 章  知识漫画`, {
    x: 0.4, y: 0, w: 9.2, h: 0.85,
    fontSize: 16, bold: true, color: T.white,
    align: 'left', valign: 'middle',
  });

  // Slide title
  if (slideData.title) {
    slide.addText(slideData.title, {
      x: 0.5, y: 1.0, w: 9, h: 0.5,
      fontSize: 20, bold: true, color: T.primary,
      align: 'left', valign: 'middle',
    });
  }

  // Comic panels (comicPanels array or assetUrl for generated image)
  if (slideData.assetUrl) {
    // Use generated image
    slide.addImage({ data: slideData.assetUrl, x: 0.5, y: 1.6, w: 9, h: 3.5 });
  } else if (slideData.comicPanels && slideData.comicPanels.length > 0) {
    // Render from comicPanels description (fallback when no image)
    const panels = slideData.comicPanels;
    const panelW = 9 / Math.min(panels.length, 3);
    const panelH = 3.2;
    const startY = 1.6;

    panels.slice(0, 4).forEach((panel, i) => {
      const x = 0.5 + i * panelW;

      // Panel border
      slide.addShape(pptx.ShapeType.rect, {
        x: x, y: startY, w: panelW - 0.1, h: panelH,
        fill: { color: T.white },
        line: { color: T.secondary, width: 2 },
      });

      // Scene description
      if (panel.scene) {
        slide.addText(panel.scene, {
          x: x + 0.1, y: startY + 0.1, w: panelW - 0.3, h: panelH * 0.5,
          fontSize: 10, color: T.subtext,
          align: 'left', valign: 'top',
        });
      }

      // Dialogue/caption
      if (panel.dialogue || panel.caption) {
        slide.addText(panel.dialogue || panel.caption, {
          x: x + 0.1, y: startY + panelH * 0.5, w: panelW - 0.3, h: panelH * 0.4,
          fontSize: 11, bold: true, color: T.primary,
          align: 'left', valign: 'middle',
        });
      }
    });
  } else {
    // Placeholder
    slide.addShape(pptx.ShapeType.rect, {
      x: 0.5, y: 1.6, w: 9, h: 3.5,
      fill: { color: T.cardBg },
      line: { color: T.accent, width: 1, dashType: 'dash' },
    });
    slide.addText('[漫画图片占位]', {
      x: 0.5, y: 1.6, w: 9, h: 3.5,
      fontSize: 16, color: T.subtext,
      align: 'center', valign: 'middle',
    });
  }

  // Teacher note
  if (slideData.teacherNote) {
    slide.addText('📝 ' + slideData.teacherNote, {
      x: 0.5, y: 5.1, w: 9, h: 0.4,
      fontSize: 11, color: T.subtext,
      align: 'left', valign: 'middle',
    });
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// NEW: Infographic Slide (信息图页 - uses assetUrl)
// ─────────────────────────────────────────────────────────────────────────────
function createInfographicSlide(slideData, chapterIndex) {
  const slide = addSlide();
  slide.background = { color: T.light };

  // Header bar
  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 0.85,
    fill: { color: T.primary },
  });
  slide.addText(`第 ${chapterIndex} 章  信息图`, {
    x: 0.4, y: 0, w: 9.2, h: 0.85,
    fontSize: 16, bold: true, color: T.white,
    align: 'left', valign: 'middle',
  });

  // Slide title
  if (slideData.title) {
    slide.addText(slideData.title, {
      x: 0.5, y: 1.0, w: 9, h: 0.5,
      fontSize: 22, bold: true, color: T.primary,
      align: 'left', valign: 'middle',
    });
  }

  // Layout hint
  if (slideData.layout) {
    slide.addText(`[${slideData.layout}]`, {
      x: 8.0, y: 1.0, w: 1.7, h: 0.4,
      fontSize: 11, color: T.accent,
      align: 'right', valign: 'middle',
    });
  }

  if (slideData.assetUrl) {
    // Use generated infographic image
    slide.addImage({ data: slideData.assetUrl, x: 0.5, y: 1.6, w: 9, h: 3.5 });
  } else {
    // Fallback: render key points as cards in a grid
    const items = slideData.bullets || slideData.keyPoints || [];
    const cols = Math.min(items.length, 3);
    const rows = Math.ceil(items.length / cols);
    const cardW = 8.5 / cols;
    const cardH = 3.0 / rows;
    const startX = 0.7;
    const startY = 1.6;

    items.forEach((item, i) => {
      const col = i % cols;
      const row = Math.floor(i / cols);
      const x = startX + col * cardW;
      const y = startY + row * cardH;

      slide.addShape(pptx.ShapeType.rect, {
        x: x, y: y, w: cardW - 0.1, h: cardH - 0.1,
        fill: { color: T.cardBg },
        line: { color: T.accent, width: 1 },
      });

      slide.addText(item, {
        x: x + 0.1, y: y + 0.1, w: cardW - 0.3, h: cardH - 0.2,
        fontSize: 12, color: T.text,
        align: 'center', valign: 'middle',
      });
    });
  }

  // Duration badge
  if (slideData.duration) {
    slide.addText(`${slideData.duration}分钟`, {
      x: 8.3, y: 5.0, w: 1.4, h: 0.4,
      fontSize: 11, color: T.white,
      align: 'center', valign: 'middle',
      fill: { color: T.accent },
    });
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// NEW: Quote Slide (名人名言页)
// ─────────────────────────────────────────────────────────────────────────────
function createQuoteSlide(slideData, chapterIndex) {
  const slide = addSlide();
  slide.background = { color: T.secondary };

  // Large opening quote mark
  slide.addText('"', {
    x: 0.5, y: 0.8, w: 1.5, h: 1.5,
    fontSize: 120, bold: true, color: T.accent,
    align: 'left', valign: 'top',
  });

  // Quote text
  const quoteText = slideData.quoteText || slideData.mainBody || '名言内容...';
  slide.addText(quoteText, {
    x: 1.0, y: 1.8, w: 8, h: 2.0,
    fontSize: 26, italic: true, color: T.white,
    align: 'center', valign: 'middle',
  });

  // Closing quote mark
  slide.addText('"', {
    x: 8.0, y: 3.3, w: 1.5, h: 1.5,
    fontSize: 120, bold: true, color: T.accent,
    align: 'right', valign: 'bottom',
  });

  // Author
  if (slideData.quoteAuthor) {
    slide.addText('—— ' + slideData.quoteAuthor, {
      x: 1.0, y: 4.2, w: 8, h: 0.5,
      fontSize: 16, color: T.light,
      align: 'right', valign: 'middle',
    });
  }

  // Duration badge
  if (slideData.duration) {
    slide.addText(`${slideData.duration}分钟`, {
      x: 8.3, y: 5.0, w: 1.4, h: 0.4,
      fontSize: 11, color: T.white,
      align: 'center', valign: 'middle',
      fill: { color: T.accent },
    });
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// NEW: Activity Slide (课堂互动活动页)
// ─────────────────────────────────────────────────────────────────────────────
function createActivitySlide(slideData, chapterIndex) {
  const slide = addSlide();
  slide.background = { color: T.light };

  // Header bar
  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 0.85,
    fill: { color: T.accent },
  });
  slide.addText(`第 ${chapterIndex} 章  课堂活动`, {
    x: 0.4, y: 0, w: 9.2, h: 0.85,
    fontSize: 16, bold: true, color: T.white,
    align: 'left', valign: 'middle',
  });

  // Activity title
  if (slideData.title) {
    slide.addText(slideData.title, {
      x: 0.5, y: 1.0, w: 9, h: 0.6,
      fontSize: 24, bold: true, color: T.primary,
      align: 'left', valign: 'middle',
    });
  }

  // Activity description
  if (slideData.mainBody || slideData.activityDesc) {
    slide.addText(slideData.mainBody || slideData.activityDesc, {
      x: 0.5, y: 1.7, w: 9, h: 0.8,
      fontSize: 14, color: T.text,
      align: 'left', valign: 'top',
    });
  }

  // Steps
  const steps = slideData.bullets || slideData.steps || [];
  if (steps.length > 0) {
    const stepItems = steps.map((step, i) => ({
      text: `${i + 1}. ${step}`,
      options: {
        breakLine: i < steps.length - 1,
        color: T.text,
        fontSize: 14,
      },
    }));
    slide.addText(stepItems, {
      x: 0.5, y: 2.6, w: 9, h: 2.2,
      valign: 'top',
      paraSpaceAfter: 8,
    });
  }

  // Duration badge
  if (slideData.duration) {
    slide.addText(`${slideData.duration}分钟`, {
      x: 8.3, y: 5.0, w: 1.4, h: 0.4,
      fontSize: 11, color: T.white,
      align: 'center', valign: 'middle',
      fill: { color: T.accent },
    });
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// NEW: Reflection Slide (思考与讨论页)
// ─────────────────────────────────────────────────────────────────────────────
function createReflectionSlide(slideData, chapterIndex) {
  const slide = addSlide();
  slide.background = { color: T.primary };

  // Header bar
  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 0.85,
    fill: { color: T.dark },
  });
  slide.addText(`第 ${chapterIndex} 章  思考与讨论`, {
    x: 0.4, y: 0, w: 9.2, h: 0.85,
    fontSize: 16, bold: true, color: T.white,
    align: 'left', valign: 'middle',
  });

  // Question mark icon
  slide.addText('?', {
    x: 0.5, y: 1.2, w: 1.5, h: 1.5,
    fontSize: 80, bold: true, color: T.accent,
    align: 'center', valign: 'middle',
  });

  // Reflection question
  const question = slideData.mainBody || slideData.question || '思考题内容...';
  slide.addText(question, {
    x: 2.0, y: 1.5, w: 7.5, h: 1.5,
    fontSize: 22, bold: true, color: T.white,
    align: 'left', valign: 'middle',
  });

  // Guidance text
  if (slideData.visualGuidance || slideData.teacherNote) {
    slide.addText(slideData.visualGuidance || slideData.teacherNote, {
      x: 0.5, y: 3.3, w: 9, h: 0.6,
      fontSize: 13, color: T.accent,
      align: 'left', valign: 'middle',
    });
  }

  // Discussion prompts
  const prompts = slideData.bullets || [];
  if (prompts.length > 0) {
    slide.addText('讨论提示：', {
      x: 0.5, y: 4.0, w: 2, h: 0.4,
      fontSize: 12, bold: true, color: T.light,
      align: 'left', valign: 'middle',
    });

    const promptItems = prompts.map((p, i) => ({
      text: p,
      options: {
        bullet: true,
        breakLine: i < prompts.length - 1,
        color: T.light,
        fontSize: 12,
      },
    }));
    slide.addText(promptItems, {
      x: 0.5, y: 4.4, w: 9, h: 1.0,
      valign: 'top',
      paraSpaceAfter: 4,
    });
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// NEW: Vote Slide (课堂投票页)
// ─────────────────────────────────────────────────────────────────────────────
function createVoteSlide(slideData, chapterIndex) {
  const slide = addSlide();
  slide.background = { color: T.light };

  // Header bar
  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 0.85,
    fill: { color: T.accent },
  });
  slide.addText(`第 ${chapterIndex} 章  课堂投票`, {
    x: 0.4, y: 0, w: 9.2, h: 0.85,
    fontSize: 16, bold: true, color: T.white,
    align: 'left', valign: 'middle',
  });

  // Vote question
  if (slideData.title) {
    slide.addText(slideData.title, {
      x: 0.5, y: 1.0, w: 9, h: 0.6,
      fontSize: 24, bold: true, color: T.primary,
      align: 'center', valign: 'middle',
    });
  }

  // Options
  const options = slideData.bullets || slideData.options || [];
  const optionColors = [T.primary, T.secondary, T.accent, 'E74C3C'];

  if (options.length > 0) {
    options.forEach((option, i) => {
      const y = 1.8 + i * 0.85;

      // Option card
      slide.addShape(pptx.ShapeType.rect, {
        x: 1.5, y: y, w: 7, h: 0.7,
        fill: { color: optionColors[i % optionColors.length] },
      });

      // Option label (A, B, C, D)
      const labels = ['A', 'B', 'C', 'D'];
      slide.addText(labels[i], {
        x: 1.5, y: y, w: 0.7, h: 0.7,
        fontSize: 18, bold: true, color: T.white,
        align: 'center', valign: 'middle',
      });

      // Option text
      slide.addText(option, {
        x: 2.3, y: y, w: 6, h: 0.7,
        fontSize: 16, color: T.white,
        align: 'left', valign: 'middle',
      });
    });
  }

  // Teacher note
  if (slideData.teacherNote) {
    slide.addText('📝 ' + slideData.teacherNote, {
      x: 0.5, y: 5.1, w: 9, h: 0.4,
      fontSize: 11, color: T.subtext,
      align: 'left', valign: 'middle',
    });
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// NEW: Quick-fire Slide (快速问答页)
// ─────────────────────────────────────────────────────────────────────────────
function createQuickFireSlide(slideData, chapterIndex) {
  const slide = addSlide();
  slide.background = { color: T.primary };

  // Header bar
  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 0.85,
    fill: { color: T.accent },
  });
  slide.addText(`第 ${chapterIndex} 章  快速问答`, {
    x: 0.4, y: 0, w: 9.2, h: 0.85,
    fontSize: 16, bold: true, color: T.white,
    align: 'left', valign: 'middle',
  });

  // Timer placeholder
  slide.addShape(pptx.ShapeType.ellipse, {
    x: 4.0, y: 1.2, w: 2, h: 2,
    fill: { color: T.accent },
  });
  slide.addText('⏱', {
    x: 4.0, y: 1.2, w: 2, h: 2,
    fontSize: 48, color: T.white,
    align: 'center', valign: 'middle',
  });

  // Question
  const question = slideData.mainBody || slideData.question || '快速回答...';
  slide.addText(question, {
    x: 0.5, y: 3.4, w: 9, h: 1.0,
    fontSize: 24, bold: true, color: T.white,
    align: 'center', valign: 'middle',
  });

  // Answer hint
  if (slideData.answer) {
    slide.addText('答案：' + slideData.answer, {
      x: 0.5, y: 4.5, w: 9, h: 0.5,
      fontSize: 14, color: T.light,
      align: 'center', valign: 'middle',
    });
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// NEW: Experiment Slide (分组实验页)
// ─────────────────────────────────────────────────────────────────────────────
function createExperimentSlide(slideData, chapterIndex) {
  const slide = addSlide();
  slide.background = { color: T.light };

  // Header bar
  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 0.85,
    fill: { color: T.secondary },
  });
  slide.addText(`第 ${chapterIndex} 章  分组实验`, {
    x: 0.4, y: 0, w: 9.2, h: 0.85,
    fontSize: 16, bold: true, color: T.white,
    align: 'left', valign: 'middle',
  });

  // Slide title
  if (slideData.title) {
    slide.addText(slideData.title, {
      x: 0.5, y: 1.0, w: 9, h: 0.5,
      fontSize: 22, bold: true, color: T.primary,
      align: 'left', valign: 'middle',
    });
  }

  // Experiment description
  if (slideData.mainBody || slideData.description) {
    slide.addText(slideData.mainBody || slideData.description, {
      x: 0.5, y: 1.6, w: 9, h: 0.6,
      fontSize: 14, color: T.text,
      align: 'left', valign: 'top',
    });
  }

  // Steps
  const steps = slideData.steps || slideData.bullets || [];
  if (steps.length > 0) {
    const stepItems = steps.map((step, i) => ({
      text: `${i + 1}. ${step}`,
      options: {
        breakLine: i < steps.length - 1,
        color: T.text,
        fontSize: 13,
      },
    }));
    slide.addText(stepItems, {
      x: 0.5, y: 2.3, w: 9, h: 2.0,
      valign: 'top',
      paraSpaceAfter: 6,
    });
  }

  // Recording table placeholder
  slide.addShape(pptx.ShapeType.rect, {
    x: 0.5, y: 4.4, w: 9, h: 0.9,
    fill: { color: T.cardBg },
    line: { color: T.accent, width: 1 },
  });
  slide.addText('📊 实验记录表（请学生填写观察数据）', {
    x: 0.5, y: 4.4, w: 9, h: 0.9,
    fontSize: 13, color: T.subtext,
    align: 'center', valign: 'middle',
  });
}

// ─────────────────────────────────────────────────────────────────────────────
// NEW: Game Slide (知识游戏页)
// ─────────────────────────────────────────────────────────────────────────────
function createGameSlide(slideData, chapterIndex) {
  const slide = addSlide();
  slide.background = { color: T.light };

  // Header bar
  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 0.85,
    fill: { color: 'E74C3C' },
  });
  slide.addText(`第 ${chapterIndex} 章  知识游戏`, {
    x: 0.4, y: 0, w: 9.2, h: 0.85,
    fontSize: 16, bold: true, color: T.white,
    align: 'left', valign: 'middle',
  });

  // Game title
  if (slideData.title) {
    slide.addText(slideData.title, {
      x: 0.5, y: 1.0, w: 9, h: 0.6,
      fontSize: 24, bold: true, color: T.primary,
      align: 'center', valign: 'middle',
    });
  }

  // Game description
  if (slideData.mainBody || slideData.description) {
    slide.addText(slideData.mainBody || slideData.description, {
      x: 0.5, y: 1.7, w: 9, h: 0.6,
      fontSize: 14, color: T.text,
      align: 'left', valign: 'top',
    });
  }

  // Scoreboard area
  slide.addShape(pptx.ShapeType.rect, {
    x: 1.0, y: 2.5, w: 3.5, h: 1.5,
    fill: { color: T.cardBg },
    line: { color: T.primary, width: 2 },
  });
  slide.addText('队伍 A', {
    x: 1.0, y: 2.5, w: 3.5, h: 0.5,
    fontSize: 14, bold: true, color: T.primary,
    align: 'center', valign: 'middle',
  });
  slide.addText('0', {
    x: 1.0, y: 3.0, w: 3.5, h: 1.0,
    fontSize: 48, bold: true, color: T.primary,
    align: 'center', valign: 'middle',
  });

  slide.addShape(pptx.ShapeType.rect, {
    x: 5.5, y: 2.5, w: 3.5, h: 1.5,
    fill: { color: T.cardBg },
    line: { color: T.accent, width: 2 },
  });
  slide.addText('队伍 B', {
    x: 5.5, y: 2.5, w: 3.5, h: 0.5,
    fontSize: 14, bold: true, color: T.accent,
    align: 'center', valign: 'middle',
  });
  slide.addText('0', {
    x: 5.5, y: 3.0, w: 3.5, h: 1.0,
    fontSize: 48, bold: true, color: T.accent,
    align: 'center', valign: 'middle',
  });

  // Rules
  if (slideData.rules || slideData.bullets) {
    const rules = slideData.rules || slideData.bullets;
    slide.addText('游戏规则：', {
      x: 0.5, y: 4.2, w: 2, h: 0.4,
      fontSize: 12, bold: true, color: T.subtext,
      align: 'left', valign: 'middle',
    });
    slide.addText(rules.join(' | '), {
      x: 2.2, y: 4.2, w: 7.3, h: 0.4,
      fontSize: 11, color: T.subtext,
      align: 'left', valign: 'middle',
    });
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// NEW: Exit Ticket Slide (出门票页)
// ─────────────────────────────────────────────────────────────────────────────
function createExitTicketSlide(slideData, chapterIndex) {
  const slide = addSlide();
  slide.background = { color: T.primary };

  // Header bar
  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 0.85,
    fill: { color: T.dark },
  });
  slide.addText(`第 ${chapterIndex} 章  出门票`, {
    x: 0.4, y: 0, w: 9.2, h: 0.85,
    fontSize: 16, bold: true, color: T.white,
    align: 'left', valign: 'middle',
  });

  // Title
  slide.addText('出门票 - 离开前回答这个问题', {
    x: 0.5, y: 1.0, w: 9, h: 0.5,
    fontSize: 18, color: T.accent,
    align: 'center', valign: 'middle',
  });

  // Question
  const question = slideData.mainBody || slideData.question || '今天学习的主要内容是什么？';
  slide.addText(question, {
    x: 0.5, y: 1.7, w: 9, h: 1.2,
    fontSize: 24, bold: true, color: T.white,
    align: 'center', valign: 'middle',
  });

  // Answer area
  slide.addShape(pptx.ShapeType.rect, {
    x: 1.0, y: 3.2, w: 8, h: 1.8,
    fill: { color: T.light },
    line: { color: T.accent, width: 1, dashType: 'dash' },
  });
  slide.addText('在此处写下你的答案...', {
    x: 1.0, y: 3.2, w: 8, h: 1.8,
    fontSize: 14, color: T.subtext,
    align: 'center', valign: 'middle',
  });

  // Teacher note
  if (slideData.teacherNote) {
    slide.addText('📝 ' + slideData.teacherNote, {
      x: 0.5, y: 5.1, w: 9, h: 0.4,
      fontSize: 11, color: T.accent,
      align: 'left', valign: 'middle',
    });
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// NEW: Result Viz Slide (结果可视化页)
// ─────────────────────────────────────────────────────────────────────────────
function createResultVizSlide(slideData, chapterIndex) {
  const slide = addSlide();
  slide.background = { color: T.light };

  // Header bar
  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 0.85,
    fill: { color: T.secondary },
  });
  slide.addText(`第 ${chapterIndex} 章  互动结果`, {
    x: 0.4, y: 0, w: 9.2, h: 0.85,
    fontSize: 16, bold: true, color: T.white,
    align: 'left', valign: 'middle',
  });

  // Slide title
  if (slideData.title) {
    slide.addText(slideData.title, {
      x: 0.5, y: 1.0, w: 9, h: 0.5,
      fontSize: 22, bold: true, color: T.primary,
      align: 'left', valign: 'middle',
    });
  }

  // Placeholder for chart visualization
  slide.addShape(pptx.ShapeType.rect, {
    x: 0.5, y: 1.6, w: 9, h: 3.5,
    fill: { color: T.cardBg },
    line: { color: T.accent, width: 1 },
  });

  slide.addText('📊 实时统计图表占位', {
    x: 0.5, y: 1.6, w: 9, h: 3.5,
    fontSize: 18, color: T.subtext,
    align: 'center', valign: 'middle',
  });

  // Data summary if provided
  if (slideData.summary) {
    slide.addText(slideData.summary, {
      x: 0.5, y: 5.2, w: 9, h: 0.4,
      fontSize: 12, color: T.subtext,
      align: 'center', valign: 'middle',
    });
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Quiz Slide (随堂测验 - existing enhanced)
// ─────────────────────────────────────────────────────────────────────────────
function createQuizSlide(slideData, chapterIndex) {
  const slide = addSlide();
  slide.background = { color: T.light };

  // Header bar
  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 0.85,
    fill: { color: T.primary },
  });
  slide.addText(`第 ${chapterIndex} 章  随堂测验`, {
    x: 0.4, y: 0, w: 9.2, h: 0.85,
    fontSize: 16, bold: true, color: T.white,
    align: 'left', valign: 'middle',
  });

  // Slide title
  if (slideData.title) {
    slide.addText(slideData.title, {
      x: 0.5, y: 1.0, w: 9, h: 0.5,
      fontSize: 20, bold: true, color: T.primary,
      align: 'left', valign: 'middle',
    });
  }

  // Quiz questions
  const questions = slideData.quizQuestions || slideData.questions || [];
  if (questions.length > 0) {
    let y = 1.6;
    questions.slice(0, 3).forEach((q, qi) => {
      // Question number badge
      slide.addShape(pptx.ShapeType.rect, {
        x: 0.5, y: y, w: 0.5, h: 0.5,
        fill: { color: T.accent },
      });
      slide.addText(`${qi + 1}`, {
        x: 0.5, y: y, w: 0.5, h: 0.5,
        fontSize: 14, bold: true, color: T.white,
        align: 'center', valign: 'middle',
      });

      // Question text
      slide.addText(typeof q === 'string' ? q : (q.question || q.text || ''), {
        x: 1.1, y: y, w: 8.4, h: 0.5,
        fontSize: 14, color: T.text,
        align: 'left', valign: 'middle',
      });

      // Options if present
      if (q.options) {
        q.options.slice(0, 4).forEach((opt, oi) => {
          const labels = ['A', 'B', 'C', 'D'];
          slide.addText(`${labels[oi]}. ${opt}`, {
            x: 1.3, y: y + 0.5 + oi * 0.35, w: 8.2, h: 0.35,
            fontSize: 12, color: T.subtext,
            align: 'left', valign: 'middle',
          });
        });
      }

      y += 1.3 + (q.options ? 1.4 : 0);
    });
  } else {
    // Fallback content
    const content = slideData.content || slideData.mainBody || '测验内容...';
    slide.addText(content, {
      x: 0.5, y: 1.6, w: 9, h: 2.0,
      fontSize: 14, color: T.text,
      align: 'left', valign: 'top',
    });
  }

  // Duration badge
  if (slideData.duration) {
    slide.addText(`${slideData.duration}分钟`, {
      x: 8.3, y: 5.0, w: 1.4, h: 0.4,
      fontSize: 11, color: T.white,
      align: 'center', valign: 'middle',
      fill: { color: T.accent },
    });
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Main
// ─────────────────────────────────────────────────────────────────────────────
createTitleSlide();

let chapterIndex = 0;

if (config.slides && Array.isArray(config.slides)) {
  config.slides.forEach((slide) => {
    // Normalize: AI outputs slideType, Java objects use type
    const slideType = slide.slideType || slide.type || 'content';

    // Track chapter index for section slides
    if (slideType === 'chapter') {
      chapterIndex = config.slides
        .slice(0, config.slides.indexOf(slide))
        .filter(s => (s.slideType || s.type) === 'chapter').length + 1;
    }

    switch (slideType) {
      case 'title':
        // First title slide already created by createTitleSlide(), skip duplicate
        break;
      case 'chapter':
        createChapterSlide(slide, chapterIndex);
        break;
      case 'content':
        createContentSlide(slide, chapterIndex);
        break;
      case 'summary':
        createSummarySlide(config.slides
          .filter(s => (s.slideType || s.type) === 'chapter')
          .map(s => ({ title: s.title })));
        break;
      case 'end':
        // End slide already created after loop, skip duplicate
        break;

      // New slide types
      case 'problem':
        createProblemSlide(slide, chapterIndex);
        break;
      case 'concept':
        createConceptSlide(slide, chapterIndex);
        break;
      case 'derivation':
        createDerivationSlide(slide, chapterIndex);
        break;
      case 'exercise':
        createExerciseSlide(slide, chapterIndex);
        break;
      case 'diagram':
        createDiagramSlide(slide, chapterIndex);
        break;
      case 'timeline':
        createTimelineSlide(slide, chapterIndex);
        break;
      case 'comic':
        createComicSlide(slide, chapterIndex);
        break;
      case 'infographic':
        createInfographicSlide(slide, chapterIndex);
        break;
      case 'quote':
        createQuoteSlide(slide, chapterIndex);
        break;
      case 'activity':
        createActivitySlide(slide, chapterIndex);
        break;
      case 'reflection':
        createReflectionSlide(slide, chapterIndex);
        break;
      case 'quiz':
        createQuizSlide(slide, chapterIndex);
        break;
      case 'vote':
        createVoteSlide(slide, chapterIndex);
        break;
      case 'poll':
        createVoteSlide(slide, chapterIndex); // poll reuses vote layout
        break;
      case 'quick-fire':
        createQuickFireSlide(slide, chapterIndex);
        break;
      case 'experiment':
        createExperimentSlide(slide, chapterIndex);
        break;
      case 'game':
        createGameSlide(slide, chapterIndex);
        break;
      case 'exit-ticket':
        createExitTicketSlide(slide, chapterIndex);
        break;
      case 'result-viz':
        createResultVizSlide(slide, chapterIndex);
        break;
      case 'text':
        // text slide falls back to content slide
        createContentSlide(slide, chapterIndex);
        break;
      default:
        // Skip unknown types silently
        break;
    }
  });
} else if (config.chapters && Array.isArray(config.chapters)) {
  config.chapters.forEach((ch, idx) => {
    createChapterSlide(ch, idx + 1);
    createContentSlide(ch, idx + 1);
  });
  createSummarySlide(config.chapters);
  createEndSlide();
}

pptx.writeFile({ fileName: outputFile })
  .then(() => {
    console.log('PPT generated: ' + outputFile + ' [template: ' + template + ']');
  })
  .catch((err) => {
    console.error('Error generating PPT:', err);
    process.exit(1);
  });
