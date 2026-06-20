#!/usr/bin/env node
/**
 * generate-ppt.js — Generates a PPTX file from a JSON config
 * Supports multiple templates: default | elegant | minimal | vibrant
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
// Template definitions
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
  },
};

const T = TEMPLATES[template] || TEMPLATES.default;

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────
function addSlide() {
  return pptx.addSlide();
}

function textRun(text, opts) {
  return pptx.addText(text, opts);
}

// ─────────────────────────────────────────────────────────────────────────────
// Title Slide
// ─────────────────────────────────────────────────────────────────────────────
function createTitleSlide() {
  const slide = addSlide();
  slide.background = { color: T.primary };

  // Top decorative bar
  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 0.5,
    fill: { color: T.secondary },
  });

  // Title
  slide.addText(config.title || '课程PPT', {
    x: 0.5, y: 1.6, w: 9, h: 1.6,
    fontSize: 44, bold: true, color: T.white,
    align: 'center', valign: 'middle',
  });

  // Subtitle
  if (config.subtitle) {
    slide.addText(config.subtitle, {
      x: 0.5, y: 3.3, w: 9, h: 0.7,
      fontSize: 20, color: T.accent,
      align: 'center', valign: 'middle',
    });
  }

  // Subject / grade tags
  if (config.subject || config.grade) {
    const tags = [config.subject, config.grade].filter(Boolean).join('  ·  ');
    slide.addText(tags, {
      x: 0.5, y: 4.1, w: 9, h: 0.5,
      fontSize: 14, color: T.accent,
      align: 'center', valign: 'middle',
    });
  }

  // Bottom bar
  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 5.1, w: 10, h: 0.525,
    fill: { color: T.secondary },
  });
  slide.addText('AI Teacher Studio', {
    x: 0.5, y: 5.1, w: 9, h: 0.525,
    fontSize: 13, color: T.light,
    align: 'center', valign: 'middle',
  });
}

// ─────────────────────────────────────────────────────────────────────────────
// Chapter Title Slide
// ─────────────────────────────────────────────────────────────────────────────
function createChapterSlide(chapter, chapterIndex) {
  const slide = addSlide();
  slide.background = { color: T.secondary };

  // Left accent bar
  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 0.3, h: 5.625,
    fill: { color: T.primary },
  });

  // Chapter number
  slide.addText(`第 ${chapterIndex} 章`, {
    x: 0.7, y: 1.4, w: 8.8, h: 0.6,
    fontSize: 18, color: T.accent, bold: false,
    align: 'left', valign: 'middle',
  });

  // Chapter title
  slide.addText(chapter.title || chapter.name || '章节', {
    x: 0.7, y: 2.0, w: 8.8, h: 1.2,
    fontSize: 38, bold: true, color: T.white,
    align: 'left', valign: 'middle',
  });

  // Chapter description / objectives
  if (chapter.description || chapter.objective) {
    slide.addText(chapter.description || chapter.objective, {
      x: 0.7, y: 3.3, w: 8.8, h: 0.8,
      fontSize: 14, color: T.light,
      align: 'left', valign: 'top',
    });
  }

  // Bottom accent line
  slide.addShape(pptx.ShapeType.rect, {
    x: 0.7, y: 4.8, w: 2, h: 0.06,
    fill: { color: T.accent },
  });
}

// ─────────────────────────────────────────────────────────────────────────────
// Content Slide
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
// Summary Slide
// ─────────────────────────────────────────────────────────────────────────────
function createSummarySlide(chapters) {
  const slide = addSlide();
  slide.background = { color: T.primary };

  slide.addText('本章小结', {
    x: 0.5, y: 0.4, w: 9, h: 0.7,
    fontSize: 28, bold: true, color: T.white,
    align: 'center', valign: 'middle',
  });

  // Accent line
  slide.addShape(pptx.ShapeType.rect, {
    x: 4, y: 1.1, w: 2, h: 0.06,
    fill: { color: T.accent },
  });

  if (chapters && chapters.length > 0) {
    const items = chapters.map((ch, i) => ({
      text: `第 ${i + 1} 章：${ch.title || ch.name || ''}`,
      options: {
        bullet: true,
        breakLine: i < chapters.length - 1,
        color: T.white,
        fontSize: 16,
      },
    }));
    slide.addText(items, {
      x: 1, y: 1.4, w: 8, h: 3.5,
      valign: 'top',
      paraSpaceAfter: 10,
    });
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// End Slide
// ─────────────────────────────────────────────────────────────────────────────
function createEndSlide() {
  const slide = addSlide();
  slide.background = { color: T.dark };

  slide.addText('谢谢观看', {
    x: 0.5, y: 2.0, w: 9, h: 1.2,
    fontSize: 48, bold: true, color: T.white,
    align: 'center', valign: 'middle',
  });

  slide.addText('AI Teacher Studio', {
    x: 0.5, y: 3.4, w: 9, h: 0.6,
    fontSize: 16, color: T.accent,
    align: 'center', valign: 'middle',
  });
}

// ─────────────────────────────────────────────────────────────────────────────
// Main
// ─────────────────────────────────────────────────────────────────────────────
createTitleSlide();

if (config.slides && Array.isArray(config.slides)) {
  config.slides.forEach((slide) => {
    switch (slide.type) {
      case 'chapter': {
        const idx = config.slides
          .slice(0, config.slides.indexOf(slide))
          .filter(s => s.type === 'chapter').length + 1;
        createChapterSlide(slide, idx);
        break;
      }
      case 'content': {
        const idx = config.slides
          .slice(0, config.slides.indexOf(slide))
          .filter(s => s.type === 'chapter').length;
        createContentSlide(slide, idx);
        break;
      }
      case 'summary':
        createSummarySlide(config.slides
          .filter(s => s.type === 'chapter')
          .map(s => ({ title: s.title })));
        break;
      case 'end':
        createEndSlide();
        break;
      default:
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