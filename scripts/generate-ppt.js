#!/usr/bin/env node
/**
 * generate-ppt.js — Generates a PPTX file from a JSON config
 * Usage: node generate-ppt.js <output.pptx> <config.json>
 */

const PptxGenJS = require('pptxgenjs');
const fs = require('fs');
const path = require('path');

const outputFile = process.argv[2];
const configFile = process.argv[3];

if (!outputFile || !configFile) {
  console.error('Usage: node generate-ppt.js <output.pptx> <config.json>');
  process.exit(1);
}

const config = JSON.parse(fs.readFileSync(configFile, 'utf8'));
const pptx = new PptxGenJS();

// Set presentation properties
pptx.author = 'AI Teacher Studio';
pptx.title = config.title || '课程PPT';
pptx.subject = config.subtitle || '';

// Helper: add a text run
function addText(text, opts) {
  return pptx.addText(text, opts);
}

// Theme colours
const THEME = {
  primary: '1F4E79',    // deep blue
  secondary: '2E75B6', // mid blue
  accent: '5B9BD5',    // light blue
  dark: '1C2833',
  light: 'F5F5F5',
  white: 'FFFFFF',
  text: '2C3E50',
  subtext: '7F8C8D',
};

function setBackground(color) {
  pptx.addSlide().background = { color };
}

function createTitleSlide() {
  const slide = pptx.addSlide();
  slide.background = { color: THEME.primary };

  // Decorative shape top
  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 0.6,
    fill: { color: THEME.secondary },
  });

  // Title
  slide.addText(config.title || '课程PPT', {
    x: 0.5, y: 1.8, w: 9, h: 1.5,
    fontSize: 44, bold: true, color: THEME.white,
    align: 'center', valign: 'middle',
  });

  // Subtitle
  if (config.subtitle) {
    slide.addText(config.subtitle, {
      x: 0.5, y: 3.4, w: 9, h: 0.8,
      fontSize: 20, color: 'B8D4F0',
      align: 'center', valign: 'middle',
    });
  }

  // Bottom bar
  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 5.0, w: 10, h: 0.625,
    fill: { color: THEME.secondary },
  });

  slide.addText('AI Teacher Studio', {
    x: 0.5, y: 5.0, w: 9, h: 0.625,
    fontSize: 14, color: 'B8D4F0',
    align: 'center', valign: 'middle',
  });
}

function createChapterSlide(chapter, chapterIndex) {
  const slide = pptx.addSlide();
  slide.background = { color: THEME.secondary };

  // Left accent bar
  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 0.25, h: 5.625,
    fill: { color: THEME.primary },
  });

  // Chapter label
  slide.addText(`第 ${chapterIndex} 章`, {
    x: 0.6, y: 1.5, w: 8.8, h: 0.6,
    fontSize: 18, color: 'B8D4F0', bold: false,
  });

  // Chapter title
  slide.addText(chapter.title || '', {
    x: 0.6, y: 2.1, w: 8.8, h: 1.4,
    fontSize: 36, bold: true, color: THEME.white,
  });

  // Duration if available
  if (chapter.duration) {
    slide.addText(`${chapter.duration} 分钟`, {
      x: 0.6, y: 3.6, w: 8.8, h: 0.5,
      fontSize: 16, color: 'B8D4F0',
    });
  }
}

function createContentSlide(chapter, chapterIndex) {
  const slide = pptx.addSlide();
  slide.background = { color: THEME.light };

  // Header bar
  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 0.9,
    fill: { color: THEME.primary },
  });

  // Chapter + title in header
  slide.addText(`第 ${chapterIndex} 章  |  ${chapter.title || ''}`, {
    x: 0.4, y: 0, w: 9.2, h: 0.9,
    fontSize: 18, bold: true, color: THEME.white,
    valign: 'middle',
  });

  // Key points section
  let y = 1.2;
  if (chapter.keyPoints && chapter.keyPoints.length > 0) {
    slide.addText('重点', {
      x: 0.5, y: y, w: 9, h: 0.45,
      fontSize: 16, bold: true, color: THEME.primary,
    });
    y += 0.45;

    chapter.keyPoints.forEach((kp) => {
      slide.addText('●  ' + kp, {
        x: 0.7, y: y, w: 8.6, h: 0.42,
        fontSize: 14, color: THEME.text,
      });
      y += 0.42;
    });
  }

  // Teaching notes
  if (chapter.teachingNotes) {
    y += 0.2;
    slide.addText('教学备注', {
      x: 0.5, y: y, w: 9, h: 0.45,
      fontSize: 16, bold: true, color: THEME.primary,
    });
    y += 0.45;
    slide.addText(chapter.teachingNotes, {
      x: 0.7, y: y, w: 8.6, h: 0.8,
      fontSize: 13, color: THEME.subtext,
      italic: true,
    });
  }
}

function createSummarySlide(chapters) {
  const slide = pptx.addSlide();
  slide.background = { color: THEME.light };

  // Header
  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 0.9,
    fill: { color: THEME.primary },
  });
  slide.addText('课堂小结', {
    x: 0.4, y: 0, w: 9.2, h: 0.9,
    fontSize: 22, bold: true, color: THEME.white,
    valign: 'middle',
  });

  if (chapters && chapters.length > 0) {
    let y = 1.2;
    chapters.forEach((ch, idx) => {
      slide.addText(`${idx + 1}. ${ch.title || ''}`, {
        x: 0.6, y: y, w: 8.8, h: 0.45,
        fontSize: 16, bold: true, color: THEME.text,
      });
      y += 0.5;
    });
  }
}

function createEndSlide() {
  const slide = pptx.addSlide();
  slide.background = { color: THEME.primary };

  slide.addText('谢谢观看', {
    x: 0.5, y: 2.0, w: 9, h: 1.2,
    fontSize: 48, bold: true, color: THEME.white,
    align: 'center', valign: 'middle',
  });

  slide.addText('AI Teacher Studio', {
    x: 0.5, y: 3.4, w: 9, h: 0.6,
    fontSize: 18, color: 'B8D4F0',
    align: 'center',
  });
}

// --- Main ---
// Title slide
createTitleSlide();

// Slides
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
      case 'title':
      default:
        // Already handled above
        break;
    }
  });
} else if (config.chapters && Array.isArray(config.chapters)) {
  // Fallback: chapters array directly
  config.chapters.forEach((ch, idx) => {
    createChapterSlide(ch, idx + 1);
    createContentSlide(ch, idx + 1);
  });
  createSummarySlide(config.chapters);
  createEndSlide();
}

// Save
pptx.writeFile({ fileName: outputFile })
  .then(() => {
    console.log('PPT generated: ' + outputFile);
  })
  .catch((err) => {
    console.error('Error generating PPT:', err);
    process.exit(1);
  });
