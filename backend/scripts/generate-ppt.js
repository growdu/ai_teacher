#!/usr/bin/env node
/**
 * generate-ppt.js — Generates a PPTX file from a JSON config
 * Enhanced: MiniMax T2I image generation for visual slides
 * Supports: title, chapter, content, summary, end, problem, concept,
 *   derivation, exercise, diagram, case, timeline, comic, infographic,
 *   quote, activity, reflection, quiz, vote, poll, quick-fire,
 *   experiment, game, exit-ticket, result-viz
 * Usage: node generate-ppt.js <output.pptx> <config.json>
 */

const PptxGenJS = require('pptxgenjs');
const fs = require('fs');
const path = require('path');
const os = require('os');
const https = require('https');

const outputFile = process.argv[2];
const configFile = process.argv[3];

if (!outputFile || !configFile) {
  console.error('Usage: node generate-ppt.js <output.pptx> <config.json>');
  process.exit(1);
}

const config = JSON.parse(fs.readFileSync(configFile, 'utf8'));
const template = config.template || 'default';

// ─────────────────────────────────────────────────────────────────────────────
// DALL-E 3 image generation (via OpenAI API compatible endpoint)
// Falls back to shape-only visuals if no API key is available
// ─────────────────────────────────────────────────────────────────────────────

const OPENAI_API_KEY = process.env.OPENAI_API_KEY || '';
const IMAGE_CACHE_DIR = os.tmpdir() + '/ppt_images';
if (!fs.existsSync(IMAGE_CACHE_DIR)) fs.mkdirSync(IMAGE_CACHE_DIR, { recursive: true });

// Normalize any color value to 6-digit hex (handles 8-digit ARGB from AI JSON)
function normalizeColor(c) {
  if (!c || typeof c !== 'string') return null;
  const s = c.trim();
  // 8-digit ARGB (e.g. "5B9BD533") → take last 6 chars (RGB)
  if (/^[0-9A-Fa-f]{8}$/.test(s)) return s.slice(2);
  // 6-digit RGB
  if (/^[0-9A-Fa-f]{6}$/.test(s)) return s;
  return null;
}

// ─────────────────────────────────────────────────────────────────────────────
// Image generation via Pollinations.ai (free, no API key needed)
// Fallback to shape-only visuals if network fails
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Generate an image using Pollinations.ai and save to a local temp file.
 * Returns local file path or null on failure.
 * Retries up to 2 times on HTTP 429 or network errors with exponential backoff.
 */
async function generateImageWithPollinations(prompt, width = 1024, height = 1024, attempt = 1) {
  const cacheKey = `${prompt.slice(0, 80)}_${width}x${height}`.replace(/[^a-zA-Z0-9]/g, '_');
  const cachedPath = IMAGE_CACHE_DIR + '/' + cacheKey + '.png';
  if (fs.existsSync(cachedPath)) {
    console.log(`[Pollinations] Cache hit: ${cacheKey}`);
    return cachedPath;
  }

  console.log(`[Pollinations] Generating image for: "${prompt.slice(0, 60)}..." (attempt ${attempt})`);

  const encodedPrompt = encodeURIComponent(prompt);
  const url = `https://image.pollinations.ai/prompt/${encodedPrompt}?width=${width}&height=${height}&n=1&model=flux&format=png`;

  return new Promise((resolve) => {
    const req = https.get(url, { timeout: 30000 }, (res) => {
      if (res.statusCode === 429) {
        // Rate limited — wait and retry (up to 2 retries)
        if (attempt < 3) {
          const waitMs = attempt * 3000;
          console.log(`[Pollinations] Rate limited (429), retrying in ${waitMs}ms...`);
          setTimeout(() => {
            resolve(generateImageWithPollinations(prompt, width, height, attempt + 1));
          }, waitMs);
        } else {
          console.error(`[Pollinations] Rate limited after 3 attempts, skipping`);
          resolve(null);
        }
        return;
      }
      if (res.statusCode !== 200) {
        console.error(`[Pollinations] HTTP ${res.statusCode} for prompt: "${prompt.slice(0, 40)}..."`);
        resolve(null);
        return;
      }
      const file = fs.createWriteStream(cachedPath);
      res.pipe(file);
      file.on('finish', () => {
        file.close();
        const stat = fs.statSync(cachedPath);
        if (stat.size < 1000) {
          console.error(`[Pollinations] Image too small (${stat.size} bytes), likely error response`);
          fs.unlinkSync(cachedPath);
          resolve(null);
          return;
        }
        console.log(`[Pollinations] Saved to ${cachedPath} (${(stat.size / 1024).toFixed(0)}KB)`);
        resolve(cachedPath);
      });
    });
    req.on('error', (err) => {
      console.error(`[Pollinations] Network error: ${err.message}`);
      resolve(null);
    });
    req.setTimeout(30000, () => {
      req.destroy();
      console.error('[Pollinations] Request timeout');
      resolve(null);
    });
  });
}

// Alias for backward compatibility
const generateImageWithDalle = generateImageWithPollinations;

// ─────────────────────────────────────────────────────────────────────────────
// Visual Generation using pptxgenjs Shapes (no external APIs needed)
// Creates real charts, diagrams, mind maps, comics using native shapes
// ─────────────────────────────────────────────────────────────────────────────

const IMAGE_CACHE = new Map(); // cacheKey → { slide, shapeOptions }

// Create a visual diagram SHAPE (not an image file) and add it to a slide
// Returns { shapeOptions } to pass to slide.addImage(shapeOptions)
// The "image" is actually rendered as a group of pptxgenjs shapes
function generateVisualSlide(slideData, slideType, chapterIndex) {
  const templateName = slideData.artStyle || template;
  const T = TEMPLATES[templateName] || TEMPLATES.default;

  const visuals = []; // array of { type, data } descriptors

  switch (slideType) {
    case 'infographic': {
      // Bento grid / knowledge cards
      const items = slideData.bullets || slideData.contentList || [];
      const colors = [T.primary, T.secondary, T.accent, T.dark, T.cardBg];
      const layouts = [
        { x: 0.3, y: 1.6, w: 4.4, h: 2.5 },  // large left
        { x: 4.9, y: 1.6, w: 2.1, h: 2.5 },  // small right top
        { x: 7.2, y: 1.6, w: 2.5, h: 2.5 },  // small right bottom
        { x: 0.3, y: 4.3, w: 2.1, h: 1.0 },  // bottom left
        { x: 2.6, y: 4.3, w: 2.1, h: 1.0 },  // bottom middle
        { x: 4.9, y: 4.3, w: 4.8, h: 1.0 },   // bottom right (wide)
      ];
      items.slice(0, 6).forEach((item, i) => {
        const l = layouts[i];
        const color = colors[i % colors.length];
        visuals.push({ type: 'roundRect', x: l.x, y: l.y, w: l.w, h: l.h,
          fill: color + '33', line: color, lineWidth: 1.5 });
        // Number badge
        visuals.push({ type: 'ellipse', x: l.x + 0.15, y: l.y + 0.15, w: 0.4, h: 0.4,
          fill: color, line: 'none' });
        visuals.push({ type: 'text', x: l.x + 0.15, y: l.y + 0.15, w: 0.4, h: 0.4,
          text: String(i + 1), fontSize: 11, bold: true, color: 'FFFFFF',
          align: 'center', valign: 'middle' });
        // Item text
        const itemText = String(item);
        if (itemText.length > 18) {
          const mid = Math.floor(itemText.length / 2);
          visuals.push({ type: 'text', x: l.x + 0.15, y: l.y + 0.65, w: l.w - 0.3, h: l.h - 0.8,
            text: itemText, fontSize: 12, bold: i < 2, color: T.text,
            align: 'center', valign: 'middle' });
        } else {
          visuals.push({ type: 'text', x: l.x + 0.15, y: l.y + 0.15, w: l.w - 0.3, h: l.h - 0.3,
            text: itemText, fontSize: 12, bold: i < 2, color: T.text,
            align: 'center', valign: 'middle' });
        }
      });
      break;
    }

    case 'comic': {
      // Draw comic panels with speech bubbles
      const panels = slideData.comicPanels || [];
      const numPanels = Math.min(panels.length, 3);
      const panelW = numPanels === 1 ? 8.0 : (numPanels === 2 ? 4.4 : 3.0);
      const panelH = numPanels <= 2 ? 3.5 : 1.6;
      const startX = numPanels === 3 ? 0.3 : 0.8;
      const startY = 1.5;
      const gap = 0.2;

      panels.slice(0, 3).forEach((panel, i) => {
        const x = startX + i * (panelW + gap);
        const y = startY;

        // Panel border (thick outline)
        visuals.push({ type: 'rect', x, y, w: panelW, h: panelH,
          fill: 'FFFFFF', line: T.primary, lineWidth: 2 });

        // Scene description
        if (panel.scene) {
          visuals.push({ type: 'text', x: x + 0.1, y: y + 0.1, w: panelW - 0.2, h: panelH * 0.35,
            text: panel.scene.slice(0, 30), fontSize: 9, color: T.subtext,
            align: 'left', valign: 'top' });
        }

        // Speech bubble for dialogue
        if (panel.dialogue) {
          const bubbleY = y + panelH * 0.35;
          const bubbleH = Math.min(panelH * 0.4, 0.9);
          visuals.push({ type: 'roundRect', x: x + 0.15, y: bubbleY, w: panelW - 0.3, h: bubbleH,
            fill: T.accent + '33', line: T.accent, lineWidth: 1 });
          visuals.push({ type: 'text', x: x + 0.25, y: bubbleY, w: panelW - 0.5, h: bubbleH,
            text: '"' + panel.dialogue.slice(0, 25) + '"', fontSize: 10, color: T.text,
            align: 'left', valign: 'middle', italic: true });
        }

        // Caption
        if (panel.caption) {
          visuals.push({ type: 'text', x: x + 0.1, y: y + panelH - 0.35, w: panelW - 0.2, h: 0.3,
            text: '📝 ' + panel.caption.slice(0, 20), fontSize: 8, color: T.accent,
            align: 'left', valign: 'middle' });
        }
      });
      break;
    }

    case 'concept': {
      // Two-column: concept explanation + formula box
      const formula = slideData.formula;
      const bullets = slideData.bullets || [];
      const mainBody = slideData.mainBody || '';

      // Left: concept text + bullets
      if (mainBody) {
        visuals.push({ type: 'text', x: 0.5, y: 1.5, w: 4.5, h: 1.5,
          text: mainBody.slice(0, 80), fontSize: 13, color: T.text,
          align: 'left', valign: 'top' });
      }
      let bulletY = 3.1;
      for (const b of bullets.slice(0, 3)) {
        visuals.push({ type: 'text', x: 0.6, y: bulletY, w: 4.3, h: 0.4,
          text: '• ' + String(b).slice(0, 30), fontSize: 12, color: T.text,
          align: 'left', valign: 'middle' });
        bulletY += 0.45;
      }

      // Right: formula box
      if (formula) {
        visuals.push({ type: 'roundRect', x: 5.2, y: 1.3, w: 4.5, h: 2.8,
          fill: T.cardBg, line: T.accent, lineWidth: 2 });
        visuals.push({ type: 'text', x: 5.2, y: 1.4, w: 4.5, h: 0.5,
          text: '核心公式', fontSize: 11, bold: true, color: T.accent,
          align: 'center', valign: 'middle' });
        visuals.push({ type: 'text', x: 5.4, y: 1.9, w: 4.1, h: 2.0,
          text: formula, fontSize: 16, bold: true, color: T.primary,
          align: 'center', valign: 'middle' });
      }
      break;
    }

    case 'diagram': {
      // Flowchart from bullets
      const items = slideData.bullets || slideData.contentList || [];
      const boxH = 0.7;
      const startY = 1.5;
      const boxW = 8.0;
      const startX = 0.8;

      items.slice(0, 6).forEach((item, i) => {
        const y = startY + i * (boxH + 0.25);
        const color = i === 0 ? T.primary : (i === items.length - 1 ? T.accent : T.secondary);

        visuals.push({ type: 'roundRect', x: startX, y, w: boxW, h: boxH,
          fill: color + '22', line: color, lineWidth: 1.5 });

        // Number
        visuals.push({ type: 'ellipse', x: startX + 0.15, y: y + boxH/2 - 0.18, w: 0.36, h: 0.36,
          fill: color, line: 'none' });
        visuals.push({ type: 'text', x: startX + 0.15, y: y + boxH/2 - 0.18, w: 0.36, h: 0.36,
          text: String(i + 1), fontSize: 10, bold: true, color: 'FFFFFF',
          align: 'center', valign: 'middle' });

        // Text
        visuals.push({ type: 'text', x: startX + 0.6, y, w: boxW - 0.8, h: boxH,
          text: String(item), fontSize: 13, color: T.text,
          align: 'left', valign: 'middle' });

        // Arrow down
        if (i < items.length - 1) {
          visuals.push({ type: 'arrow', x: startX + boxW/2, y: y + boxH + 0.02,
            w: 0, h: 0.22 });
        }
      });
      break;
    }

    case 'timeline': {
      // Horizontal timeline with dots
      const items = slideData.bullets || slideData.contentList || [];
      if (items.length === 0) break;

      const n = Math.min(items.length, 6);
      const spacing = 8.5 / n;
      const lineY = 3.2;

      // Main line
      visuals.push({ type: 'line', x: 0.5, y: lineY, w: 8.5, h: 0 });

      const dotColors = [T.primary, T.accent, T.secondary, T.primary, T.accent, T.secondary];

      items.slice(0, n).forEach((item, i) => {
        const x = 0.5 + i * spacing + spacing / 2;
        const dotColor = dotColors[i % dotColors.length];

        // Dot
        visuals.push({ type: 'ellipse', x: x - 0.2, y: lineY - 0.2, w: 0.4, h: 0.4,
          fill: dotColor, line: 'none' });
        visuals.push({ type: 'text', x: x - 0.2, y: lineY - 0.2, w: 0.4, h: 0.4,
          text: String(i + 1), fontSize: 10, bold: true, color: 'FFFFFF',
          align: 'center', valign: 'middle' });

        // Label
        const label = typeof item === 'string' ? item.slice(0, 15) : (item.label || item.year || ('S' + (i+1)));
        visuals.push({ type: 'text', x: x - 0.8, y: lineY - 0.85, w: 1.6, h: 0.5,
          text: String(label), fontSize: 9, bold: true, color: T.primary,
          align: 'center', valign: 'middle' });

        // Description
        const desc = typeof item === 'string' ? '' : (item.event || item.description || '');
        if (desc) {
          visuals.push({ type: 'text', x: x - 0.9, y: lineY + 0.35, w: 1.8, h: 0.8,
            text: String(desc).slice(0, 20), fontSize: 8, color: T.subtext,
            align: 'center', valign: 'top' });
        }
      });
      break;
    }

    case 'problem': {
      // Scene illustration with emoji icons
      const scenario = slideData.scenario || slideData.visualGuidance || '';
      const question = slideData.mainBody || '';

      // Large question mark icon
      visuals.push({ type: 'ellipse', x: 0.4, y: 1.2, w: 2.8, h: 2.8,
        fill: T.accent + '33', line: T.accent, lineWidth: 2 });
      visuals.push({ type: 'text', x: 0.4, y: 1.2, w: 2.8, h: 2.8,
        text: '?', fontSize: 80, bold: true, color: T.accent,
        align: 'center', valign: 'middle' });

      // Question text
      visuals.push({ type: 'text', x: 3.5, y: 1.5, w: 6.2, h: 1.5,
        text: question.slice(0, 50), fontSize: 20, bold: true, color: T.white,
        align: 'left', valign: 'middle' });

      // Scenario description
      if (scenario) {
        visuals.push({ type: 'roundRect', x: 3.5, y: 3.1, w: 6.2, h: 1.0,
          fill: T.cardBg, line: T.accent, lineWidth: 1 });
        visuals.push({ type: 'text', x: 3.7, y: 3.1, w: 5.8, h: 1.0,
          text: scenario.slice(0, 60), fontSize: 12, color: T.text,
          align: 'left', valign: 'middle' });
      }
      break;
    }

    case 'exercise': {
      // Problem solving steps with highlight
      const problem = slideData.exampleProblem || slideData.mainBody || '';
      const solution = slideData.solution || (slideData.bullets || []).join('\n');

      // Problem box
      visuals.push({ type: 'roundRect', x: 0.4, y: 1.2, w: 5.0, h: 1.5,
        fill: T.primary + '22', line: T.primary, lineWidth: 1.5 });
      visuals.push({ type: 'text', x: 0.5, y: 1.2, w: 4.8, h: 0.4,
        text: '例题', fontSize: 11, bold: true, color: T.primary,
        align: 'left', valign: 'middle' });
      visuals.push({ type: 'text', x: 0.5, y: 1.6, w: 4.8, h: 1.0,
        text: problem.slice(0, 80), fontSize: 12, color: T.text,
        align: 'left', valign: 'top' });

      // Solution steps
      if (solution) {
        visuals.push({ type: 'roundRect', x: 0.4, y: 2.85, w: 9.2, h: 2.2,
          fill: T.cardBg, line: T.accent, lineWidth: 1 });
        visuals.push({ type: 'text', x: 0.5, y: 2.9, w: 9.0, h: 0.4,
          text: '解题步骤', fontSize: 11, bold: true, color: T.accent,
          align: 'left', valign: 'middle' });
        visuals.push({ type: 'text', x: 0.6, y: 3.3, w: 8.8, h: 1.6,
          text: solution.slice(0, 300), fontSize: 12, color: T.text,
          align: 'left', valign: 'top' });
      }

      // Variants
      const variants = slideData.variants || [];
      if (variants.length > 0) {
        visuals.push({ type: 'roundRect', x: 5.6, y: 1.2, w: 4.1, h: 1.5,
          fill: T.secondary + '22', line: T.secondary, lineWidth: 1 });
        visuals.push({ type: 'text', x: 5.7, y: 1.2, w: 3.9, h: 0.4,
          text: '变式训练', fontSize: 11, bold: true, color: T.secondary,
          align: 'left', valign: 'middle' });
        visuals.push({ type: 'text', x: 5.7, y: 1.6, w: 3.9, h: 1.0,
          text: variants.slice(0, 2).map((v, i) => (i+1) + '. ' + String(v).slice(0, 40)).join('\n'),
          fontSize: 11, color: T.text, align: 'left', valign: 'top' });
      }
      break;
    }

    case 'vote':
    case 'poll': {
      // Vote/quiz with option cards
      const question = slideData.question || slideData.mainBody || '';
      const options = slideData.options || slideData.choices || [];
      const optColors = [T.primary, T.accent, T.secondary, T.warm1];

      // Question
      visuals.push({ type: 'text', x: 0.5, y: 1.2, w: 9, h: 0.8,
        text: question.slice(0, 50), fontSize: 18, bold: true, color: T.white,
        align: 'center', valign: 'middle' });

      // Option cards
      const labels = ['A', 'B', 'C', 'D'];
      options.slice(0, 4).forEach((opt, i) => {
        const y = 2.2 + i * 0.75;
        const color = optColors[i % optColors.length];
        visuals.push({ type: 'roundRect', x: 1.0, y, w: 8.0, h: 0.65,
          fill: color + '22', line: color, lineWidth: 1.5 });
        visuals.push({ type: 'ellipse', x: 1.2, y: y + 0.125, w: 0.4, h: 0.4,
          fill: color, line: 'none' });
        visuals.push({ type: 'text', x: 1.2, y: y + 0.125, w: 0.4, h: 0.4,
          text: labels[i], fontSize: 12, bold: true, color: 'FFFFFF',
          align: 'center', valign: 'middle' });
        visuals.push({ type: 'text', x: 1.8, y, w: 7.0, h: 0.65,
          text: String(opt).slice(0, 35), fontSize: 14, color: T.text,
          align: 'left', valign: 'middle' });
      });
      break;
    }

    case 'case': {
      // Case study card
      const scenario = slideData.scenario || slideData.mainBody || '';
      const bullets = slideData.bullets || slideData.analysis || [];

      visuals.push({ type: 'roundRect', x: 0.4, y: 1.2, w: 9.2, h: 4.1,
        fill: T.light, line: T.secondary, lineWidth: 2 });

      // Scenario
      if (scenario) {
        visuals.push({ type: 'roundRect', x: 0.6, y: 1.4, w: 8.8, h: 1.2,
          fill: 'FFFFFF', line: T.accent, lineWidth: 1 });
        visuals.push({ type: 'text', x: 0.7, y: 1.4, w: 8.6, h: 0.4,
          text: '案例情境', fontSize: 10, bold: true, color: T.accent,
          align: 'left', valign: 'middle' });
        visuals.push({ type: 'text', x: 0.7, y: 1.8, w: 8.6, h: 0.7,
          text: scenario.slice(0, 60), fontSize: 12, color: T.text,
          align: 'left', valign: 'top' });
      }

      // Analysis points
      let y = 2.8;
      for (const b of bullets.slice(0, 3)) {
        const text = typeof b === 'string' ? b : (b.text || '');
        visuals.push({ type: 'text', x: 0.8, y, w: 8.6, h: 0.5,
          text: '  ' + String(text).slice(0, 50), fontSize: 12, color: T.text,
          align: 'left', valign: 'middle' });
        y += 0.55;
      }
      break;
    }

    case 'chapter': {
      // Chapter with decorative elements
      const title = slideData.title || '';
      const desc = slideData.description || '';

      // Large chapter number
      visuals.push({ type: 'ellipse', x: 6.5, y: 0.8, w: 3.2, h: 3.2,
        fill: T.primary + '33', line: T.primary, lineWidth: 2 });
      visuals.push({ type: 'text', x: 6.5, y: 0.8, w: 3.2, h: 3.2,
        text: '#', fontSize: 120, bold: true, color: T.primary + '44',
        align: 'center', valign: 'middle' });

      if (desc) {
        visuals.push({ type: 'text', x: 0.5, y: 4.0, w: 9.0, h: 0.8,
          text: desc.slice(0, 60), fontSize: 13, color: T.light,
          align: 'left', valign: 'top' });
      }
      break;
    }

    case 'title': {
      // Title with decorative circles
      const titleText = slideData.title || config.title || '';
      const subtitle = slideData.mainBody || config.subtitle || '';

      // Background circles
      visuals.push({ type: 'ellipse', x: -1.0, y: -1.0, w: 4.0, h: 4.0,
        fill: T.accent + '22', line: 'none' });
      visuals.push({ type: 'ellipse', x: 7.5, y: 3.5, w: 3.5, h: 3.5,
        fill: T.secondary + '22', line: 'none' });
      visuals.push({ type: 'ellipse', x: 8.0, y: -0.5, w: 2.0, h: 2.0,
        fill: T.primary + '22', line: 'none' });

      if (subtitle) {
        visuals.push({ type: 'text', x: 0.5, y: 3.5, w: 9.0, h: 0.6,
          text: subtitle.slice(0, 60), fontSize: 16, color: T.accent,
          align: 'center', valign: 'middle' });
      }
      break;
    }

    case 'quote': {
      // Large decorative quotation mark
      visuals.push({ type: 'ellipse', x: 7.8, y: 0.8, w: 2.5, h: 2.5,
        fill: T.accent + '22', line: 'none' });
      break;
    }

    case 'activity': {
      // Decorative top-right circles
      visuals.push({ type: 'ellipse', x: 8.2, y: -0.8, w: 2.5, h: 2.5,
        fill: T.secondary + '22', line: 'none' });
      visuals.push({ type: 'ellipse', x: 7.0, y: 0.2, w: 1.5, h: 1.5,
        fill: T.accent + '22', line: 'none' });
      break;
    }

    case 'reflection': {
      // Subtle background decoration
      visuals.push({ type: 'ellipse', x: 8.5, y: 3.0, w: 2.0, h: 2.0,
        fill: T.primary + '22', line: 'none' });
      break;
    }

    case 'exit-ticket': {
      // Exit ticket: question + dashed answer box
      const note = slideData.teacherNote || '';
      visuals.push({ type: 'roundRect', x: 1.0, y: 2.4, w: 8.0, h: 2.0,
        fill: T.light, line: T.accent, lineWidth: 1.5 });
      visuals.push({ type: 'text', x: 1.0, y: 2.4, w: 8.0, h: 2.0,
        text: '在此处写下你的答案...', fontSize: 14, color: T.subtext,
        align: 'center', valign: 'middle', italic: true });
      if (note) {
        visuals.push({ type: 'text', x: 0.5, y: 4.6, w: 9.0, h: 0.4,
          text: '📝 ' + note, fontSize: 10, color: T.accent,
          align: 'left', valign: 'middle' });
      }
      break;
    }

    case 'quiz': {
      // Top-right decorative circle
      visuals.push({ type: 'ellipse', x: 8.0, y: -0.5, w: 2.2, h: 2.2,
        fill: T.secondary + '22', line: 'none' });
      break;
    }

    case 'game': {
      // Fun decorative elements
      visuals.push({ type: 'ellipse', x: -0.5, y: 3.5, w: 2.5, h: 2.5,
        fill: T.accent + '22', line: 'none' });
      visuals.push({ type: 'ellipse', x: 8.5, y: 4.0, w: 2.0, h: 2.0,
        fill: T.secondary + '22', line: 'none' });
      break;
    }

    case 'experiment': {
      // Lab flask decorative
      visuals.push({ type: 'ellipse', x: 8.0, y: -0.5, w: 2.5, h: 2.5,
        fill: T.secondary + '22', line: 'none' });
      break;
    }

    case 'quickfire': {
      // Energetic decoration
      visuals.push({ type: 'ellipse', x: -1.0, y: -1.0, w: 3.0, h: 3.0,
        fill: T.accent + '22', line: 'none' });
      break;
    }

    default:
      // No special visual for this slide type
      break;
  }

  return visuals;
}

// Render visual elements onto a slide
function renderVisuals(slide, visuals) {
  for (const v of visuals) {
    switch (v.type) {
      case 'rect':
        slide.addShape(pptx.ShapeType.rect, {
          x: v.x, y: v.y, w: v.w, h: v.h,
          fill: v.fill || { color: v.fill === 'none' ? 'FFFFFF' : v.fill },
          line: v.line && v.line !== 'none' ? { color: v.line, width: v.lineWidth || 1 } : { color: 'FFFFFF', width: 0 },
        });
        break;
      case 'roundRect':
        slide.addShape(pptx.ShapeType.roundRect, {
          x: v.x, y: v.y, w: v.w, h: v.h,
          fill: { color: v.fill },
          line: v.line && v.line !== 'none' ? { color: v.line, width: v.lineWidth || 1 } : { color: 'FFFFFF', width: 0 },
        });
        break;
      case 'ellipse':
        slide.addShape(pptx.ShapeType.ellipse, {
          x: v.x, y: v.y, w: v.w, h: v.h,
          fill: { color: v.fill },
          line: v.line && v.line !== 'none' ? { color: v.line, width: v.lineWidth || 1 } : { color: 'FFFFFF', width: 0 },
        });
        break;
      case 'text':
        slide.addText(v.text, {
          x: v.x, y: v.y, w: v.w, h: v.h,
          fontSize: v.fontSize || 12,
          bold: v.bold || false,
          italic: v.italic || false,
          color: v.color || '#000000',
          align: v.align || 'left',
          valign: v.valign || 'top',
        });
        break;
      case 'line':
        slide.addShape(pptx.ShapeType.rect, {
          x: v.x, y: v.y, w: v.w, h: 0.04,
          fill: { color: v.line || '#5B9BD5' },
          line: { color: v.line || '#5B9BD5', width: 0 },
        });
        break;
      case 'arrow':
        // Draw a down arrow using triangle
        slide.addText('▼', {
          x: v.x - 0.2, y: v.y, w: 0.4, h: v.h,
          fontSize: 10, color: '#5B9BD5',
          align: 'center', valign: 'middle',
        });
        break;
    }
  }
}

// Synchronous wrapper that generates visuals for a slide type
function createVisualsForSlide(slideData, slideType, chapterIndex) {
  const visuals = generateVisualSlide(slideData, slideType, chapterIndex);
  return visuals || [];
}

// ─────────────────────────────────────────────────────────────────────────────
// pptxgenjs setup
// ─────────────────────────────────────────────────────────────────────────────
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
  },
};

const T = TEMPLATES[template] || TEMPLATES.default;

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────
function addSlide() {
  return pptx.addSlide();
}

// ─────────────────────────────────────────────────────────────────────────────
// Title Slide
// ─────────────────────────────────────────────────────────────────────────────
async function createTitleSlideAsync() {
  const slide = addSlide();
  slide.background = { color: T.primary };

  // Add visual elements (shapes-based)
  const visuals = createVisualsForSlide({ title: config.title, mainBody: config.subtitle, artStyle: config.template }, 'title', 0);
  renderVisuals(slide, visuals);

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
async function createChapterSlideAsync(chapter, chapterIndex) {
  const slide = addSlide();
  slide.background = { color: T.secondary };

// Add visual elements (shapes-based)
  const visuals = createVisualsForSlide(chapter, 'chapter', chapterIndex);
  renderVisuals(slide, visuals);

  // Left accent bar
  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 0.3, h: 5.625,
    fill: { color: T.primary },
  });

  // Chapter number
  slide.addText(`第 ${chapterIndex} 章`, {
    x: 0.7, y: 1.4, w: 5, h: 0.6,
    fontSize: 18, color: T.accent, bold: false,
    align: 'left', valign: 'middle',
  });

  // Chapter title
  slide.addText(chapter.title || chapter.name || '章节', {
    x: 0.7, y: 2.0, w: 5, h: 1.2,
    fontSize: 38, bold: true, color: T.white,
    align: 'left', valign: 'middle',
  });

  // Chapter description
  const descText = chapter.description || chapter.objective || null;
  if (descText) {
    slide.addText(descText, {
      x: 0.7, y: 3.3, w: 5, h: 0.8,
      fontSize: 14, color: T.light,
      align: 'left', valign: 'top',
    });
  }

  // learningObjectives as bullet list
  const objectives = chapter.learningObjectives;
  if (objectives && Array.isArray(objectives) && objectives.length > 0) {
    const bulletItems = objectives.map((item, i) => ({
      text: item,
      options: { bullet: true, breakLine: i < objectives.length - 1, color: T.light, fontSize: 13 },
    }));
    slide.addText(bulletItems, {
      x: 0.7, y: 4.1, w: 4.8, h: 1.2,
      valign: 'top',
    });
  }

  // Bottom bar
  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 5.1, w: 10, h: 0.525,
    fill: { color: T.primary },
  });
  slide.addText(chapter.title || chapter.name || `第 ${chapterIndex} 章`, {
    x: 0.5, y: 5.1, w: 9, h: 0.525,
    fontSize: 13, color: T.light,
    align: 'center', valign: 'middle',
  });

  // Duration tag
  if (chapter.duration) {
    slide.addText(`${chapter.duration}分钟`, {
      x: 0.7, y: 4.5, w: 2, h: 0.4,
      fontSize: 12, color: T.accent,
      align: 'left', valign: 'middle',
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

  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 0.85,
    fill: { color: T.primary },
  });

  slide.addText(`第 ${chapterIndex} 章  ${chapter.title || ''}`, {
    x: 0.4, y: 0, w: 9.2, h: 0.85,
    fontSize: 16, bold: true, color: T.white,
    align: 'left', valign: 'middle',
  });

  const slideTitle = chapter.slideTitle || chapter.title || '';
  if (slideTitle) {
    slide.addText(slideTitle, {
      x: 0.5, y: 1.05, w: 9, h: 0.55,
      fontSize: 22, bold: true, color: T.primary,
      align: 'left', valign: 'middle',
    });
  }

  const contentItems = chapter.contentList || chapter.content || [];
  if (contentItems.length > 0) {
    const bulletItems = contentItems.map((item, i) => ({
      text: item,
      options: { bullet: { type: 'number' }, breakLine: i < contentItems.length - 1, color: T.text, fontSize: 15 },
    }));
    slide.addText(bulletItems, {
      x: 0.5, y: 1.7, w: 9, h: 3.2,
      valign: 'top', paraSpaceAfter: 8,
    });
  }

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
    x: 0.5, y: 0.35, w: 9, h: 0.7,
    fontSize: 28, bold: true, color: T.white,
    align: 'center', valign: 'middle',
  });

  // Decorative underline
  slide.addShape(pptx.ShapeType.rect, {
    x: 4, y: 1.05, w: 2, h: 0.06,
    fill: { color: T.accent },
    line: { color: T.accent, width: 0 },
  });

  // Chapter summary cards (2-column grid)
  if (chapters && chapters.length > 0) {
    const maxVisible = Math.min(chapters.length, 6);
    const cols = 2;
    const cardW = 4.3;
    const cardH = 1.4;
    const startX = 0.55;
    const startY = 1.35;
    const gapX = 0.3;
    const gapY = 0.2;
    const cardColors = [T.primary, T.secondary, T.accent, T.secondary, T.accent, T.primary];

    chapters.slice(0, maxVisible).forEach((ch, i) => {
      const col = i % cols;
      const row = Math.floor(i / cols);
      const x = startX + col * (cardW + gapX);
      const y = startY + row * (cardH + gapY);
      const color = cardColors[i % cardColors.length];

      // Card background
      slide.addShape(pptx.ShapeType.roundRect, {
        x, y, w: cardW, h: cardH,
        fill: { color: color + '33' },
        line: { color, width: 1.5 },
      });
      // Chapter number badge
      slide.addShape(pptx.ShapeType.ellipse, {
        x: x + 0.15, y: y + 0.15, w: 0.45, h: 0.45,
        fill: { color },
        line: { color, width: 0 },
      });
      slide.addText(String(i + 1), {
        x: x + 0.15, y: y + 0.15, w: 0.45, h: 0.45,
        fontSize: 14, bold: true, color: T.white,
        align: 'center', valign: 'middle',
      });
      // Chapter title
      const title = ch.title || ch.name || `第 ${i + 1} 章`;
      slide.addText(title, {
        x: x + 0.7, y: y + 0.15, w: cardW - 0.9, h: 0.45,
        fontSize: 14, bold: true, color: T.white,
        align: 'left', valign: 'middle',
      });
      // Chapter description (if any)
      if (ch.description) {
        slide.addText(ch.description.slice(0, 40), {
          x: x + 0.15, y: y + 0.7, w: cardW - 0.3, h: 0.55,
          fontSize: 11, color: T.light,
          align: 'left', valign: 'top',
        });
      }
    });
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// End Slide
// ─────────────────────────────────────────────────────────────────────────────
function createEndSlide() {
  const slide = addSlide();
  slide.background = { color: T.dark };

  // Decorative background circles
  slide.addShape(pptx.ShapeType.ellipse, {
    x: -1.5, y: -1.5, w: 5, h: 5,
    fill: { color: T.primary, transparency: 85 },
    line: { color: T.primary, width: 0 },
  });
  slide.addShape(pptx.ShapeType.ellipse, {
    x: 7, y: 3, w: 4, h: 4,
    fill: { color: T.accent, transparency: 85 },
    line: { color: T.accent, width: 0 },
  });
  slide.addShape(pptx.ShapeType.ellipse, {
    x: 5, y: -2, w: 3, h: 3,
    fill: { color: T.secondary, transparency: 85 },
    line: { color: T.secondary, width: 0 },
  });

  slide.addText('谢谢观看', {
    x: 0.5, y: 1.8, w: 9, h: 1.4,
    fontSize: 52, bold: true, color: T.white,
    align: 'center', valign: 'middle',
  });

  // Decorative divider
  slide.addShape(pptx.ShapeType.rect, {
    x: 3.5, y: 3.3, w: 3, h: 0.06,
    fill: { color: T.accent },
    line: { color: T.accent, width: 0 },
  });

  slide.addText('AI Teacher Studio', {
    x: 0.5, y: 3.55, w: 9, h: 0.6,
    fontSize: 18, color: T.accent,
    align: 'center', valign: 'middle',
  });

  slide.addText('让学习更有趣', {
    x: 0.5, y: 4.2, w: 9, h: 0.5,
    fontSize: 14, color: T.subtext,
    align: 'center', valign: 'middle',
  });
}

// ─────────────────────────────────────────────────────────────────────────────
// Problem Slide (理科问题引入页) — Enhanced with image
// ─────────────────────────────────────────────────────────────────────────────
async function createProblemSlideAsync(slideData, chapterIndex) {
  const slide = addSlide();
  slide.background = { color: T.primary };

  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 0.85,
    fill: { color: T.secondary },
  });
  slide.addText(`第 ${chapterIndex} 章  问题引入`, {
    x: 0.4, y: 0, w: 9.2, h: 0.85,
    fontSize: 16, bold: true, color: T.white,
    align: 'left', valign: 'middle',
  });

  // Add visual elements (shapes-based)
  const problemVisuals = createVisualsForSlide(slideData, 'problem', chapterIndex);
  renderVisuals(slide, problemVisuals);

  // Attempt DALL-E image generation if imagePrompt is provided
  // Also use scenario/visualGuidance as fallback prompts
  const dallePrompt = slideData.imagePrompt || slideData.scenario || slideData.visualGuidance;
  if (dallePrompt) {
    const imgPath = await generateImageWithDalle(dallePrompt, 1024, 1024);
    if (imgPath) {
      slide.addImage({ path: imgPath }, {
        x: 0.4, y: 1.0, w: 2.8, h: 2.8,
        rotate: 3,
      });
    }
  }

  // Problem question (overlaid on top of visuals if needed)
  const question = slideData.mainBody || slideData.content || slideData.question || '生活中的问题：...';
  slide.addText(question, {
    x: 3.5, y: 1.5, w: 6.2, h: 1.5,
    fontSize: 20, bold: true, color: T.white,
    align: 'left', valign: 'middle',
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
// Concept Slide (理科概念讲解页) — Enhanced with diagram image
// ─────────────────────────────────────────────────────────────────────────────
async function createConceptSlideAsync(slideData, chapterIndex) {
  const slide = addSlide();
  slide.background = { color: T.light };

  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 0.85,
    fill: { color: T.primary },
  });
  slide.addText(`第 ${chapterIndex} 章  概念讲解`, {
    x: 0.4, y: 0, w: 9.2, h: 0.85,
    fontSize: 16, bold: true, color: T.white,
    align: 'left', valign: 'middle',
  });

  // Add visual elements (shapes-based)
  const conceptVisuals = createVisualsForSlide(slideData, 'concept', chapterIndex);
  renderVisuals(slide, conceptVisuals);

  // Attempt DALL-E image generation if imagePrompt is provided
  if (slideData.imagePrompt) {
    const imgPath = await generateImageWithDalle(slideData.imagePrompt, 1024, 1024);
    if (imgPath) {
      slide.addImage({ path: imgPath }, {
        x: 5.5, y: 1.0, w: 4.0, h: 4.0,
      });
    }
  }

  // Slide title
  if (slideData.title) {
    slide.addText(slideData.title, {
      x: 0.5, y: 1.0, w: 4.6, h: 0.6,
      fontSize: 24, bold: true, color: T.primary,
      align: 'left', valign: 'middle',
    });
  }

  // Formula box
  if (slideData.formula) {
    slide.addShape(pptx.ShapeType.rect, {
      x: 0.5, y: 1.7, w: 4.6, h: 0.9,
      fill: { color: T.cardBg },
      line: { color: T.accent, width: 2 },
    });
    slide.addText(slideData.formula, {
      x: 0.5, y: 1.7, w: 4.6, h: 0.9,
      fontSize: 20, bold: true, color: T.primary,
      align: 'center', valign: 'middle',
    });
  }

  // Main body text
  if (slideData.mainBody) {
    slide.addText(slideData.mainBody, {
      x: 0.5, y: 2.7, w: 4.6, h: 1.0,
      fontSize: 14, color: T.text,
      align: 'left', valign: 'top',
    });
  }

  // Key points bullets
  const keyPoints = slideData.bullets || slideData.keyPoints || [];
  if (keyPoints.length > 0) {
    const bulletItems = keyPoints.map((item, i) => ({
      text: item,
      options: { bullet: true, breakLine: i < keyPoints.length - 1, color: T.text, fontSize: 14 },
    }));
    slide.addText(bulletItems, {
      x: 0.5, y: 3.7, w: 4.6, h: 1.6,
      valign: 'top', paraSpaceAfter: 6,
    });
  }

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
// Derivation Slide
// ─────────────────────────────────────────────────────────────────────────────
function createDerivationSlide(slideData, chapterIndex) {
  const slide = addSlide();
  slide.background = { color: T.light };

  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 0.85,
    fill: { color: T.primary },
  });
  slide.addText(`第 ${chapterIndex} 章  推导证明`, {
    x: 0.4, y: 0, w: 9.2, h: 0.85,
    fontSize: 16, bold: true, color: T.white,
    align: 'left', valign: 'middle',
  });

  const derivVisuals = createVisualsForSlide(slideData, 'derivation', chapterIndex);
  renderVisuals(slide, derivVisuals);

  // DALL-E image if prompt available (fire-and-forget, doesn't block slide rendering)
  const dallePrompt = slideData.imagePrompt || slideData.scenario || slideData.visualGuidance;
  if (dallePrompt) {
    generateImageWithDalle(dallePrompt, 768, 768).then(imgPath => {
      if (imgPath) {
        slide.addImage({ path: imgPath }, {
          x: 7.5, y: 1.2, w: 2.2, h: 2.2,
        });
      }
    });
  }

  if (slideData.title) {
    slide.addText(slideData.title, {
      x: 0.5, y: 1.0, w: 9, h: 0.5,
      fontSize: 22, bold: true, color: T.primary,
      align: 'left', valign: 'middle',
    });
  }

  const steps = slideData.steps || slideData.bullets || [slideData.mainBody];
  if (steps.length > 0) {
    steps.slice(0, 6).forEach((step, i) => {
      const stepText = typeof step === 'string' ? step : (step.text || '');
      const y = 1.6 + i * 0.68;
      const isLast = i === steps.length - 1;
      const color = isLast ? T.accent : T.secondary;

      // Step card
      slide.addShape(pptx.ShapeType.roundRect, {
        x: 0.5, y, w: 9, h: 0.58,
        fill: { color: color + '18' },
        line: { color, width: 1 },
      });
      // Step number
      slide.addShape(pptx.ShapeType.ellipse, {
        x: 0.65, y: y + 0.09, w: 0.4, h: 0.4,
        fill: { color },
        line: { color, width: 0 },
      });
      slide.addText(String(i + 1), {
        x: 0.65, y: y + 0.09, w: 0.4, h: 0.4,
        fontSize: 12, bold: true, color: T.white,
        align: 'center', valign: 'middle',
      });
      // Step text
      slide.addText(stepText.slice(0, 70), {
        x: 1.2, y, w: 8.1, h: 0.58,
        fontSize: 13, color: T.text,
        align: 'left', valign: 'middle',
      });
      // Arrow connector
      if (i < steps.length - 1) {
        slide.addText('▼', {
          x: 4.7, y: y + 0.56, w: 0.6, h: 0.2,
          fontSize: 8, color: T.subtext,
          align: 'center', valign: 'middle',
        });
      }
    });
  }

  if (slideData.formula || slideData.conclusion) {
    slide.addShape(pptx.ShapeType.roundRect, {
      x: 0.5, y: 4.6, w: 9, h: 0.7,
      fill: { color: T.cardBg },
      line: { color: T.accent, width: 2 },
    });
    slide.addText('得证：' + (slideData.formula || slideData.conclusion), {
      x: 0.7, y: 4.6, w: 8.6, h: 0.7,
      fontSize: 16, bold: true, color: T.primary,
      align: 'left', valign: 'middle',
    });
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Exercise Slide — Enhanced with problem-solving illustration
// ─────────────────────────────────────────────────────────────────────────────
async function createExerciseSlideAsync(slideData, chapterIndex) {
  const slide = addSlide();
  slide.background = { color: T.light };

  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 0.85,
    fill: { color: T.primary },
  });
  slide.addText(`第 ${chapterIndex} 章  例题演练`, {
    x: 0.4, y: 0, w: 9.2, h: 0.85,
    fontSize: 16, bold: true, color: T.white,
    align: 'left', valign: 'middle',
  });

  // Add visual elements (shapes-based)
  const exerciseVisuals = createVisualsForSlide(slideData, 'exercise', chapterIndex);
  renderVisuals(slide, exerciseVisuals);

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

  const rawProblem = slideData.exampleProblem || slideData.mainBody
    || (slideData.bullets && slideData.bullets[0])
    || (slideData.contentList && slideData.contentList[0])
    || slideData.content
    || '请解答以下问题...';

  slide.addText(rawProblem, {
    x: 0.5, y: 1.5, w: 4.6, h: 0.9,
    fontSize: 16, color: T.text,
    align: 'left', valign: 'top',
  });

  const bulletsUsedAsProblem = !slideData.exampleProblem && !slideData.mainBody && slideData.bullets && slideData.bullets[0];
  const bulletsForSolution = bulletsUsedAsProblem
    ? (slideData.bullets ? slideData.bullets.slice(1) : []) : (slideData.bullets || []);
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
      x: 0.5, y: 2.9, w: 4.6, h: 1.2,
      fontSize: 14, color: T.text,
      align: 'left', valign: 'top',
    });
  }

  const allBullets = slideData.bullets || slideData.points || [];
  const variants = slideData.variants
    || (allBullets.length > 1 ? allBullets.slice(1) : []) || [];
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
      options: { breakLine: i < variants.length - 1, color: T.text, fontSize: 13 },
    }));
    slide.addText(variantItems, {
      x: 0.5, y: 4.6, w: 9, h: 1.0,
      valign: 'top', paraSpaceAfter: 4,
    });
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Diagram Slide
// ─────────────────────────────────────────────────────────────────────────────
async function createDiagramSlideAsync(slideData, chapterIndex) {
  const slide = addSlide();
  slide.background = { color: T.light };

  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 0.85,
    fill: { color: T.primary },
  });
  slide.addText(`第 ${chapterIndex} 章  ${slideData.diagramType || '图示'}`, {
    x: 0.4, y: 0, w: 9.2, h: 0.85,
    fontSize: 16, bold: true, color: T.white,
    align: 'left', valign: 'middle',
  });

  if (slideData.title) {
    slide.addText(slideData.title, {
      x: 0.5, y: 1.0, w: 9, h: 0.5,
      fontSize: 22, bold: true, color: T.primary,
      align: 'left', valign: 'middle',
    });
  }

  // Add visual elements (shapes-based)
  const diagramVisuals = createVisualsForSlide(slideData, 'diagram', chapterIndex);
  renderVisuals(slide, diagramVisuals);

  // Diagram type badge
  const diagramType = slideData.diagramType || 'flowchart';
  slide.addText(`[${diagramType}]`, {
    x: 8.0, y: 1.0, w: 1.7, h: 0.4,
    fontSize: 11, color: T.accent,
    align: 'right', valign: 'middle',
  });

  const items = slideData.bullets || slideData.contentList || [];
  if (items.length > 0) {
    const startY = 1.7;
    const boxHeight = 0.65;
    const gap = 0.15;
    items.forEach((item, i) => {
      const y = startY + i * (boxHeight + gap);
      slide.addShape(pptx.ShapeType.rect, {
        x: 0.8, y, w: 7.5, h: boxHeight,
        fill: { color: i === 0 ? T.primary : (i === items.length - 1 ? T.accent : T.secondary) },
        line: { color: T.primary, width: 1 },
      });
      slide.addText(`${i + 1}`, {
        x: 0.8, y, w: 0.6, h: boxHeight,
        fontSize: 14, bold: true, color: T.white,
        align: 'center', valign: 'middle',
      });
      slide.addText(item, {
        x: 1.5, y, w: 6.7, h: boxHeight,
        fontSize: 14, color: T.white,
        align: 'left', valign: 'middle',
      });
      if (i < items.length - 1) {
        slide.addText('▼', {
          x: 4.0, y: y + boxHeight - 0.05, w: 1, h: 0.3,
          fontSize: 12, color: T.subtext,
          align: 'center', valign: 'middle',
        });
      }
    });
  }

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
// Case Slide
// ─────────────────────────────────────────────────────────────────────────────
async function createCaseSlideAsync(slideData, chapterIndex) {
  const slide = addSlide();
  slide.background = { color: T.light };

  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 0.85,
    fill: { color: T.primary },
  });
  slide.addText(`第 ${chapterIndex} 章  案例分析`, {
    x: 0.4, y: 0, w: 9.2, h: 0.85,
    fontSize: 16, bold: true, color: T.white,
    align: 'left', valign: 'middle',
  });

  // Add visual elements (shapes-based)
  const caseVisuals = createVisualsForSlide(slideData, 'case', chapterIndex);
  renderVisuals(slide, caseVisuals);

  if (slideData.title) {
    slide.addText(slideData.title, {
      x: 0.5, y: 1.0, w: 9, h: 0.5,
      fontSize: 22, bold: true, color: T.primary,
      align: 'left', valign: 'middle',
    });
  }

  const scenario = slideData.scenario || slideData.mainBody || slideData.content || '';
  if (scenario) {
    slide.addShape(pptx.ShapeType.rect, {
      x: 0.5, y: 1.6, w: 9, h: 1.4,
      fill: { color: T.cardBg },
      line: { color: T.accent, width: 1 },
    });
    slide.addText('📋 案例情境', {
      x: 0.7, y: 1.65, w: 2, h: 0.35,
      fontSize: 11, bold: true, color: T.accent,
      align: 'left', valign: 'middle',
    });
    slide.addText(scenario, {
      x: 0.7, y: 2.0, w: 8.6, h: 0.9,
      fontSize: 13, color: T.text,
      align: 'left', valign: 'top',
    });
  }

  const points = slideData.bullets || slideData.analysis || [];
  if (points.length > 0) {
    slide.addText('💡 分析要点', {
      x: 0.5, y: 3.1, w: 2, h: 0.35,
      fontSize: 12, bold: true, color: T.primary,
      align: 'left', valign: 'middle',
    });
    const pointItems = points.map((point, i) => ({
      text: typeof point === 'string' ? point : (point.text || ''),
      options: { bullet: true, breakLine: i < points.length - 1, color: T.text, fontSize: 13 },
    }));
    slide.addText(pointItems, {
      x: 0.5, y: 3.5, w: 9, h: 1.3,
      valign: 'top', paraSpaceAfter: 5,
    });
  }

  if (slideData.teacherNote) {
    slide.addText('📝 ' + slideData.teacherNote, {
      x: 0.5, y: 5.1, w: 9, h: 0.4,
      fontSize: 11, color: T.subtext,
      align: 'left', valign: 'middle',
    });
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Timeline Slide
// ─────────────────────────────────────────────────────────────────────────────
async function createTimelineSlideAsync(slideData, chapterIndex) {
  const slide = addSlide();
  slide.background = { color: T.light };

  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 0.85,
    fill: { color: T.primary },
  });
  slide.addText(`第 ${chapterIndex} 章  时间线`, {
    x: 0.4, y: 0, w: 9.2, h: 0.85,
    fontSize: 16, bold: true, color: T.white,
    align: 'left', valign: 'middle',
  });

  if (slideData.title) {
    slide.addText(slideData.title, {
      x: 0.5, y: 1.0, w: 9, h: 0.5,
      fontSize: 22, bold: true, color: T.primary,
      align: 'left', valign: 'middle',
    });
  }

  // Add visual elements (shapes-based)
  const timelineVisuals = createVisualsForSlide(slideData, 'timeline', chapterIndex);
  renderVisuals(slide, timelineVisuals);

  slide.addShape(pptx.ShapeType.rect, {
    x: 0.5, y: 2.8, w: 9, h: 0.05,
    fill: { color: T.secondary },
  });

  let items = slideData.bullets || slideData.contentList || [];
  if (items.length > 0 && typeof items[0] === 'string') {
    items = items.map((item, i) => ({ label: `Step ${i + 1}`, event: item }));
  }
  const spacing = 9 / Math.max(items.length, 1);

  items.forEach((item, i) => {
    const x = 0.5 + i * spacing + spacing / 2;
    slide.addShape(pptx.ShapeType.ellipse, {
      x: x - 0.15, y: 2.68, w: 0.3, h: 0.3,
      fill: { color: T.accent },
    });
    const label = item.year || item.label || `Step ${i + 1}`;
    slide.addText(label, {
      x: x - 0.8, y: 2.2, w: 1.6, h: 0.4,
      fontSize: 11, bold: true, color: T.primary,
      align: 'center', valign: 'middle',
    });
    const desc = item.event || item.description || String(item);
    slide.addText(desc, {
      x: x - 1, y: 3.1, w: 2, h: 1.2,
      fontSize: 10, color: T.text,
      align: 'center', valign: 'top',
    });
  });

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
// Comic Slide — Enhanced with AI-generated panels
// ─────────────────────────────────────────────────────────────────────────────
async function createComicSlideAsync(slideData, chapterIndex) {
  const slide = addSlide();
  slide.background = { color: T.light };

  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 0.85,
    fill: { color: T.primary },
  });
  slide.addText(`第 ${chapterIndex} 章  知识漫画`, {
    x: 0.4, y: 0, w: 9.2, h: 0.85,
    fontSize: 16, bold: true, color: T.white,
    align: 'left', valign: 'middle',
  });

  if (slideData.title) {
    slide.addText(slideData.title, {
      x: 0.5, y: 1.0, w: 9, h: 0.5,
      fontSize: 20, bold: true, color: T.primary,
      align: 'left', valign: 'middle',
    });
  }

  // Add visual elements (shapes-based)
  const comicVisuals = createVisualsForSlide(slideData, 'comic', chapterIndex);
  renderVisuals(slide, comicVisuals);

  if (slideData.title) {
    slide.addText(slideData.title, {
      x: 0.5, y: 1.0, w: 9, h: 0.5,
      fontSize: 20, bold: true, color: T.primary,
      align: 'left', valign: 'middle',
    });
  }

  const comicPanels = slideData.comicPanels || [];

  if (comicPanels.length > 0) {
    const numPanels = Math.min(comicPanels.length, 3);
    const panelW = numPanels === 1 ? 8 : (numPanels === 2 ? 4.3 : 3.0);
    const panelH = numPanels <= 2 ? 3.2 : 1.6;
    const gap = 0.15;
    const startX = 0.3;
    const startY = 1.6;

    for (let i = 0; i < numPanels; i++) {
      const p = comicPanels[i];
      const row = Math.floor(i / 2);
      const col = i % 2;
      const x = startX + col * (panelW + gap);
      const y = startY + row * (panelH + gap);

      slide.addShape(pptx.ShapeType.rect, {
        x, y, w: panelW, h: panelH,
        fill: { color: T.white },
        line: { color: T.primary, width: 2 },
      });

      const sceneText = p.scene || '';
      const dialogueText = p.dialogue || '';
      const captionText = p.caption ? `📝 ${p.caption}` : '';
      const fullText = [sceneText, dialogueText, captionText].filter(Boolean).join('\n');
      slide.addText(fullText, {
        x: x + 0.1, y: y + 0.1, w: panelW - 0.2, h: panelH - 0.2,
        fontSize: numPanels <= 2 ? 12 : 9, color: T.text,
        align: 'left', valign: 'top',
      });
    }
  }

  if (slideData.duration) {
    slide.addText(`${slideData.duration}分钟`, {
      x: 8.3, y: 5.0, w: 1.4, h: 0.4,
      fontSize: 11, color: T.white,
      align: 'center', valign: 'middle',
      fill: { color: T.accent },
    });
  }

  if (slideData.teacherNote) {
    slide.addText('📝 ' + slideData.teacherNote, {
      x: 0.5, y: 5.1, w: 7.5, h: 0.4,
      fontSize: 11, color: T.subtext,
      align: 'left', valign: 'middle',
    });
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Infographic Slide — Enhanced with AI-generated knowledge graph
// ─────────────────────────────────────────────────────────────────────────────
async function createInfographicSlideAsync(slideData, chapterIndex) {
  const slide = addSlide();
  slide.background = { color: T.light };

  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 0.85,
    fill: { color: T.primary },
  });
  slide.addText(`第 ${chapterIndex} 章  知识结构图`, {
    x: 0.4, y: 0, w: 9.2, h: 0.85,
    fontSize: 16, bold: true, color: T.white,
    align: 'left', valign: 'middle',
  });

  if (slideData.title) {
    slide.addText(slideData.title, {
      x: 0.5, y: 1.0, w: 9, h: 0.5,
      fontSize: 22, bold: true, color: T.primary,
      align: 'left', valign: 'middle',
    });
  }

  // Add visual elements (shapes-based)
  const infoVisuals = createVisualsForSlide(slideData, 'infographic', chapterIndex);
  renderVisuals(slide, infoVisuals);

  // Draw bento-style knowledge cards
  const items = slideData.bullets || slideData.contentList || [];
  if (items.length > 0) {
    const cols = Math.min(items.length, 3);
    const cardW = (9 - (cols - 1) * 0.2) / cols;
    const cardH = 1.4;
    const startX = 0.3;
    const colors = [T.primary, T.secondary, T.accent, T.dark, T.cardBg];

    items.forEach((item, i) => {
      const col = i % cols;
      const row = Math.floor(i / cols);
      const x = startX + col * (cardW + 0.2);
      const y = 1.6 + row * (cardH + 0.2);
      const color = colors[i % colors.length];

      slide.addShape(pptx.ShapeType.rect, {
        x, y, w: cardW, h: cardH,
        fill: { color },
        line: { color: T.primary, width: 1 },
      });
      slide.addText(String(item), {
        x: x + 0.1, y, w: cardW - 0.2, h: cardH,
        fontSize: 13, bold: i < 2, color: i < 2 ? T.white : T.text,
        align: 'center', valign: 'middle',
      });
    });
  }

  if (slideData.mainBody) {
    slide.addText(slideData.mainBody, {
      x: 0.5, y: 4.8, w: 9, h: 0.5,
      fontSize: 12, color: T.subtext,
      align: 'center', valign: 'middle',
    });
  }

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
// Quote Slide
// ─────────────────────────────────────────────────────────────────────────────
function createQuoteSlide(slideData, chapterIndex) {
  const slide = addSlide();
  slide.background = { color: T.light };

  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 0.85,
    fill: { color: T.primary },
  });
  slide.addText(`第 ${chapterIndex} 章  名言`, {
    x: 0.4, y: 0, w: 9.2, h: 0.85,
    fontSize: 16, bold: true, color: T.white,
    align: 'left', valign: 'middle',
  });

  const quoteVisuals = createVisualsForSlide(slideData, 'quote', chapterIndex);
  renderVisuals(slide, quoteVisuals);

  const quote = slideData.content || slideData.mainBody || slideData.quote || '"名言内容..."';
  const author = slideData.author || '';

  // Decorative left bar
  slide.addShape(pptx.ShapeType.rect, {
    x: 0.5, y: 1.5, w: 0.08, h: 2.8,
    fill: { color: T.accent },
    line: { color: T.accent, width: 0 },
  });

  slide.addText('"', {
    x: 0.7, y: 1.2, w: 1, h: 1,
    fontSize: 96, bold: true, color: T.accent,
    align: 'left', valign: 'top',
  });
  slide.addText(quote, {
    x: 1.5, y: 1.8, w: 7.7, h: 2,
    fontSize: 22, italic: true, color: T.text,
    align: 'left', valign: 'top',
  });
  if (author) {
    slide.addText('— ' + author, {
      x: 1.5, y: 3.9, w: 7.7, h: 0.5,
      fontSize: 16, bold: true, color: T.primary,
      align: 'right', valign: 'middle',
    });
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Activity Slide
// ─────────────────────────────────────────────────────────────────────────────
function createActivitySlide(slideData, chapterIndex) {
  const slide = addSlide();
  slide.background = { color: T.light };

  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 0.85,
    fill: { color: T.secondary },
  });
  slide.addText(`第 ${chapterIndex} 章  课堂活动`, {
    x: 0.4, y: 0, w: 9.2, h: 0.85,
    fontSize: 16, bold: true, color: T.white,
    align: 'left', valign: 'middle',
  });

  const activityVisuals = createVisualsForSlide(slideData, 'activity', chapterIndex);
  renderVisuals(slide, activityVisuals);

  if (slideData.title) {
    slide.addText(slideData.title, {
      x: 0.5, y: 1.0, w: 9, h: 0.5,
      fontSize: 22, bold: true, color: T.primary,
      align: 'left', valign: 'middle',
    });
  }

  const instructions = slideData.instructions || slideData.steps || slideData.bullets || [];
  if (instructions.length > 0) {
    const stepColors = [T.primary, T.accent, T.secondary];
    const stepIcons = ['1', '2', '3', '4', '5'];
    instructions.slice(0, 5).forEach((item, i) => {
      const text = typeof item === 'string' ? item : (item.text || '');
      const color = stepColors[i % stepColors.length];
      const y = 1.65 + i * 0.72;
      // Step card
      slide.addShape(pptx.ShapeType.roundRect, {
        x: 0.5, y, w: 9, h: 0.62,
        fill: { color: color + '18' },
        line: { color, width: 1 },
      });
      // Step number badge
      slide.addShape(pptx.ShapeType.ellipse, {
        x: 0.65, y: y + 0.11, w: 0.4, h: 0.4,
        fill: { color },
        line: { color, width: 0 },
      });
      slide.addText(stepIcons[i], {
        x: 0.65, y: y + 0.11, w: 0.4, h: 0.4,
        fontSize: 12, bold: true, color: T.white,
        align: 'center', valign: 'middle',
      });
      // Step text
      slide.addText(text.slice(0, 60), {
        x: 1.2, y, w: 8.1, h: 0.62,
        fontSize: 13, color: T.text,
        align: 'left', valign: 'middle',
      });
    });
  }

  // Duration
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
// Reflection Slide
// ─────────────────────────────────────────────────────────────────────────────
function createReflectionSlide(slideData, chapterIndex) {
  const slide = addSlide();
  slide.background = { color: T.light };

  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 0.85,
    fill: { color: T.primary },
  });
  slide.addText(`第 ${chapterIndex} 章  思考讨论`, {
    x: 0.4, y: 0, w: 9.2, h: 0.85,
    fontSize: 16, bold: true, color: T.white,
    align: 'left', valign: 'middle',
  });

  const reflectionVisuals = createVisualsForSlide(slideData, 'reflection', chapterIndex);
  renderVisuals(slide, reflectionVisuals);

  if (slideData.title) {
    slide.addText(slideData.title, {
      x: 0.5, y: 1.0, w: 9, h: 0.5,
      fontSize: 22, bold: true, color: T.primary,
      align: 'left', valign: 'middle',
    });
  }

  const question = slideData.question || slideData.mainBody || '';
  if (question) {
    slide.addShape(pptx.ShapeType.roundRect, {
      x: 0.5, y: 1.7, w: 9, h: 1.5,
      fill: { color: T.cardBg },
      line: { color: T.primary, width: 2 },
    });
    // Left accent bar
    slide.addShape(pptx.ShapeType.rect, {
      x: 0.5, y: 1.7, w: 0.1, h: 1.5,
      fill: { color: T.accent },
      line: { color: T.accent, width: 0 },
    });
    slide.addText('💭 ' + question, {
      x: 0.8, y: 1.7, w: 8.5, h: 1.5,
      fontSize: 16, color: T.text,
      align: 'left', valign: 'middle',
    });
  }

  const points = slideData.bullets || [];
  if (points.length > 0) {
    slide.addText('思考要点：', {
      x: 0.5, y: 3.4, w: 2, h: 0.4,
      fontSize: 12, bold: true, color: T.primary,
      align: 'left', valign: 'middle',
    });
    const stepColors = [T.primary, T.accent, T.secondary];
    points.slice(0, 3).forEach((p, i) => {
      const text = typeof p === 'string' ? p : (p.text || '');
      const color = stepColors[i % stepColors.length];
      const y = 3.85 + i * 0.45;
      slide.addShape(pptx.ShapeType.ellipse, {
        x: 0.5, y, w: 0.28, h: 0.28,
        fill: { color },
        line: { color, width: 0 },
      });
      slide.addText(text.slice(0, 70), {
        x: 0.9, y: y - 0.05, w: 8.6, h: 0.4,
        fontSize: 12, color: T.text,
        align: 'left', valign: 'middle',
      });
    });
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Quiz Slide
// ─────────────────────────────────────────────────────────────────────────────
function createQuizSlide(slideData, chapterIndex) {
  const slide = addSlide();
  slide.background = { color: T.light };

  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 0.85,
    fill: { color: T.primary },
  });
  slide.addText(`第 ${chapterIndex} 章  随堂测验`, {
    x: 0.4, y: 0, w: 9.2, h: 0.85,
    fontSize: 16, bold: true, color: T.white,
    align: 'left', valign: 'middle',
  });

  const quizVisuals = createVisualsForSlide(slideData, 'quiz', chapterIndex);
  renderVisuals(slide, quizVisuals);

  if (slideData.title) {
    slide.addText(slideData.title, {
      x: 0.5, y: 1.0, w: 9, h: 0.5,
      fontSize: 20, bold: true, color: T.primary,
      align: 'left', valign: 'middle',
    });
  }

  const questions = slideData.quizQuestions || slideData.questions || [];
  if (questions.length > 0) {
    let y = 1.55;
    questions.slice(0, 3).forEach((q, qi) => {
      const qText = typeof q === 'string' ? q : (q.question || q.text || '');
      const hasOptions = q.options && q.options.length > 0;
      const cardH = hasOptions ? 2.1 : 0.65;
      const cardColor = [T.primary, T.secondary, T.accent][qi % 3];

      // Question card background
      slide.addShape(pptx.ShapeType.roundRect, {
        x: 0.5, y, w: 9, h: cardH,
        fill: { color: cardColor + '15' },
        line: { color: cardColor, width: 1 },
      });
      // Left accent bar
      slide.addShape(pptx.ShapeType.rect, {
        x: 0.5, y, w: 0.08, h: cardH,
        fill: { color: cardColor },
        line: { color: cardColor, width: 0 },
      });
      // Question number badge
      slide.addShape(pptx.ShapeType.ellipse, {
        x: 0.72, y: y + 0.12, w: 0.4, h: 0.4,
        fill: { color: cardColor },
        line: { color: cardColor, width: 0 },
      });
      slide.addText(String(qi + 1), {
        x: 0.72, y: y + 0.12, w: 0.4, h: 0.4,
        fontSize: 12, bold: true, color: T.white,
        align: 'center', valign: 'middle',
      });
      // Question text
      slide.addText(qText.slice(0, 60), {
        x: 1.25, y: y + 0.05, w: 8.0, h: 0.55,
        fontSize: 13, bold: true, color: T.text,
        align: 'left', valign: 'middle',
      });

      if (hasOptions) {
        const optLabels = ['A', 'B', 'C', 'D'];
        q.options.slice(0, 4).forEach((opt, oi) => {
          const optY = y + 0.65 + oi * 0.36;
          slide.addText(`${optLabels[oi]}. ${String(opt).slice(0, 35)}`, {
            x: 1.3, y: optY, w: 8.0, h: 0.34,
            fontSize: 11, color: T.subtext,
            align: 'left', valign: 'middle',
          });
        });
      }
      y += cardH + 0.15;
    });
  } else {
    const content = slideData.content || slideData.mainBody || '测验内容...';
    slide.addText(content, {
      x: 0.5, y: 1.6, w: 9, h: 2.0,
      fontSize: 14, color: T.text,
      align: 'left', valign: 'top',
    });
  }

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
// Vote/Poll Slide
// ─────────────────────────────────────────────────────────────────────────────
async function createVoteSlideAsync(slideData, chapterIndex) {
  const slide = addSlide();
  slide.background = { color: T.primary };

  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 0.85,
    fill: { color: T.dark },
  });
  slide.addText(`第 ${chapterIndex} 章  投票互动`, {
    x: 0.4, y: 0, w: 9.2, h: 0.85,
    fontSize: 16, bold: true, color: T.white,
    align: 'left', valign: 'middle',
  });

  // Add visual elements (shapes-based)
  const voteVisuals = createVisualsForSlide(slideData, 'vote', chapterIndex);
  renderVisuals(slide, voteVisuals);

  const question = slideData.question || slideData.mainBody || '请投票：';
  slide.addText(question, {
    x: 0.5, y: 1.5, w: 9, h: 1.5,
    fontSize: 24, bold: true, color: T.white,
    align: 'center', valign: 'middle',
  });

  const options = slideData.options || slideData.choices || [];
  if (options.length > 0) {
    const colors = [T.accent, T.secondary, T.primary, T.accent];
    options.slice(0, 4).forEach((opt, i) => {
      const labels = ['A', 'B', 'C', 'D'];
      slide.addShape(pptx.ShapeType.rect, {
        x: 0.5, y: 3.2 + i * 0.55, w: 9, h: 0.5,
        fill: { color: colors[i % colors.length] },
      });
      slide.addText(`${labels[i]}. ${opt}`, {
        x: 0.5, y: 3.2 + i * 0.55, w: 9, h: 0.5,
        fontSize: 14, bold: true, color: T.white,
        align: 'center', valign: 'middle',
      });
    });
  }

  if (slideData.teacherNote) {
    slide.addText('📝 ' + slideData.teacherNote, {
      x: 0.5, y: 5.1, w: 9, h: 0.4,
      fontSize: 11, color: T.accent,
      align: 'left', valign: 'middle',
    });
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Quick Fire Slide
// ─────────────────────────────────────────────────────────────────────────────
function createQuickFireSlide(slideData, chapterIndex) {
  const slide = addSlide();
  slide.background = { color: T.primary };

  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 0.85,
    fill: { color: T.accent },
  });
  slide.addText(`第 ${chapterIndex} 章  快速问答`, {
    x: 0.4, y: 0, w: 9.2, h: 0.85,
    fontSize: 16, bold: true, color: T.white,
    align: 'left', valign: 'middle',
  });

  slide.addShape(pptx.ShapeType.ellipse, {
    x: 4.0, y: 1.2, w: 2, h: 2,
    fill: { color: T.accent },
  });
  slide.addText('⏱', {
    x: 4.0, y: 1.2, w: 2, h: 2,
    fontSize: 48, color: T.white,
    align: 'center', valign: 'middle',
  });

  const question = slideData.mainBody || slideData.question || '快速回答...';
  slide.addText(question, {
    x: 0.5, y: 3.4, w: 9, h: 1.0,
    fontSize: 24, bold: true, color: T.white,
    align: 'center', valign: 'middle',
  });

  // QuickFire: add visuals
  const qfVisuals = createVisualsForSlide(slideData, 'quickfire', chapterIndex);
  renderVisuals(slide, qfVisuals);

  if (slideData.answer) {
    slide.addText('答案：' + slideData.answer, {
      x: 0.5, y: 4.5, w: 9, h: 0.5,
      fontSize: 14, color: T.light,
      align: 'center', valign: 'middle',
    });
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Experiment Slide
// ─────────────────────────────────────────────────────────────────────────────
function createExperimentSlide(slideData, chapterIndex) {
  const slide = addSlide();
  slide.background = { color: T.light };

  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 0.85,
    fill: { color: T.secondary },
  });
  slide.addText(`第 ${chapterIndex} 章  分组实验`, {
    x: 0.4, y: 0, w: 9.2, h: 0.85,
    fontSize: 16, bold: true, color: T.white,
    align: 'left', valign: 'middle',
  });

  const expVisuals = createVisualsForSlide(slideData, 'experiment', chapterIndex);
  renderVisuals(slide, expVisuals);

  if (slideData.title) {
    slide.addText(slideData.title, {
      x: 0.5, y: 1.0, w: 9, h: 0.5,
      fontSize: 22, bold: true, color: T.primary,
      align: 'left', valign: 'middle',
    });
  }

  if (slideData.mainBody || slideData.description) {
    slide.addText(slideData.mainBody || slideData.description, {
      x: 0.5, y: 1.6, w: 9, h: 0.6,
      fontSize: 14, color: T.text,
      align: 'left', valign: 'top',
    });
  }

  const steps = slideData.steps || slideData.bullets || [];
  if (steps.length > 0) {
    steps.slice(0, 5).forEach((step, i) => {
      const stepText = typeof step === 'string' ? step : (step.text || '');
      const color = [T.primary, T.secondary, T.accent][i % 3];
      const y = 2.35 + i * 0.62;
      // Step card
      slide.addShape(pptx.ShapeType.roundRect, {
        x: 0.5, y, w: 9, h: 0.55,
        fill: { color: color + '18' },
        line: { color, width: 1 },
      });
      // Step badge
      slide.addShape(pptx.ShapeType.ellipse, {
        x: 0.65, y: y + 0.08, w: 0.38, h: 0.38,
        fill: { color },
        line: { color, width: 0 },
      });
      slide.addText(String(i + 1), {
        x: 0.65, y: y + 0.08, w: 0.38, h: 0.38,
        fontSize: 11, bold: true, color: T.white,
        align: 'center', valign: 'middle',
      });
      slide.addText(stepText.slice(0, 70), {
        x: 1.15, y, w: 8.2, h: 0.55,
        fontSize: 12, color: T.text,
        align: 'left', valign: 'middle',
      });
    });
  }

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
// Game Slide
// ─────────────────────────────────────────────────────────────────────────────
function createGameSlide(slideData, chapterIndex) {
  const slide = addSlide();
  slide.background = { color: T.light };

  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 0.85,
    fill: { color: 'E74C3C' },
  });
  slide.addText(`第 ${chapterIndex} 章  知识游戏`, {
    x: 0.4, y: 0, w: 9.2, h: 0.85,
    fontSize: 16, bold: true, color: T.white,
    align: 'left', valign: 'middle',
  });

  const gameVisuals = createVisualsForSlide(slideData, 'game', chapterIndex);
  renderVisuals(slide, gameVisuals);

  if (slideData.title) {
    slide.addText(slideData.title, {
      x: 0.5, y: 1.0, w: 9, h: 0.6,
      fontSize: 24, bold: true, color: T.primary,
      align: 'center', valign: 'middle',
    });
  }

  if (slideData.mainBody || slideData.description) {
    slide.addText(slideData.mainBody || slideData.description, {
      x: 0.5, y: 1.7, w: 9, h: 0.6,
      fontSize: 14, color: T.text,
      align: 'left', valign: 'top',
    });
  }

  // Team score cards
  slide.addShape(pptx.ShapeType.roundRect, {
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

  slide.addShape(pptx.ShapeType.roundRect, {
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
// Exit Ticket Slide — Enhanced with illustration
// ─────────────────────────────────────────────────────────────────────────────
async function createExitTicketSlideAsync(slideData, chapterIndex) {
  const slide = addSlide();
  slide.background = { color: T.primary };

  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 0.85,
    fill: { color: T.dark },
  });
  slide.addText(`第 ${chapterIndex} 章  出门票`, {
    x: 0.4, y: 0, w: 9.2, h: 0.85,
    fontSize: 16, bold: true, color: T.white,
    align: 'left', valign: 'middle',
  });

  // Add visual elements (shapes-based)
  const exitVisuals = createVisualsForSlide(slideData, 'exit-ticket', chapterIndex);
  renderVisuals(slide, exitVisuals);

  slide.addText('出门票 - 离开前回答这个问题', {
    x: 0.5, y: 1.0, w: 9, h: 0.5,
    fontSize: 18, color: T.accent,
    align: 'center', valign: 'middle',
  });

  const question = slideData.mainBody || slideData.question || '今天学习的主要内容是什么？';
  slide.addText(question, {
    x: 0.5, y: 1.7, w: 9, h: 1.2,
    fontSize: 24, bold: true, color: T.white,
    align: 'center', valign: 'middle',
  });

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

  if (slideData.teacherNote) {
    slide.addText('📝 ' + slideData.teacherNote, {
      x: 0.5, y: 5.1, w: 9, h: 0.4,
      fontSize: 11, color: T.accent,
      align: 'left', valign: 'middle',
    });
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Result Viz Slide
// ─────────────────────────────────────────────────────────────────────────────
function createResultVizSlide(slideData, chapterIndex) {
  const slide = addSlide();
  slide.background = { color: T.light };

  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 0.85,
    fill: { color: T.secondary },
  });
  slide.addText(`第 ${chapterIndex} 章  互动结果`, {
    x: 0.4, y: 0, w: 9.2, h: 0.85,
    fontSize: 16, bold: true, color: T.white,
    align: 'left', valign: 'middle',
  });

  if (slideData.title) {
    slide.addText(slideData.title, {
      x: 0.5, y: 1.0, w: 9, h: 0.5,
      fontSize: 22, bold: true, color: T.primary,
      align: 'left', valign: 'middle',
    });
  }

  // Draw simulated bar chart if data available
  const data = slideData.data || slideData.chartData || null;
  if (data && data.labels && data.datasets) {
    const chartX = 0.8, chartY = 1.6, chartW = 8.4, chartH = 3.0;
    const barCount = data.labels.length;
    const barGroupW = chartW / barCount;
    const maxVal = Math.max(...data.datasets.flatMap(ds => ds.data), 1);

    // Chart background
    slide.addShape(pptx.ShapeType.rect, {
      x: chartX, y: chartY, w: chartW, h: chartH,
      fill: { color: 'F8F9FA' },
      line: { color: T.subtext, width: 0.5 },
    });
    // Y-axis grid lines
    for (let g = 0; g <= 4; g++) {
      const gy = chartY + (chartH / 4) * g;
      slide.addShape(pptx.ShapeType.rect, {
        x: chartX, y: gy, w: chartW, h: 0.01,
        fill: { color: 'DEE2E6' },
        line: { color: 'DEE2E6', width: 0 },
      });
    }
    // Bars
    data.datasets.forEach((ds, dsi) => {
      const rawColor = ds.color || [T.primary, T.secondary, T.accent][dsi % 3];
      const color = normalizeColor(rawColor) || T.primary;
      ds.data.forEach((val, i) => {
        const barH = Math.max((val / maxVal) * (chartH - 0.4), 0.1);
        const barW = Math.min(barGroupW / data.datasets.length - 0.15, 1.0);
        const bx = chartX + i * barGroupW + dsi * (barW + 0.15) + barGroupW * 0.15;
        const by = chartY + chartH - barH - 0.1;
        slide.addShape(pptx.ShapeType.rect, {
          x: bx, y: by, w: barW, h: barH,
          fill: { color },
          line: { color, width: 0 },
        });
        // Value label
        slide.addText(String(val), {
          x: bx - 0.1, y: by - 0.3, w: barW + 0.2, h: 0.3,
          fontSize: 9, bold: true, color: T.text,
          align: 'center', valign: 'middle',
        });
      });
    });
    // X-axis labels
    data.labels.forEach((label, i) => {
      slide.addText(String(label), {
        x: chartX + i * barGroupW, y: chartY + chartH + 0.05, w: barGroupW, h: 0.35,
        fontSize: 10, color: T.subtext,
        align: 'center', valign: 'top',
      });
    });
  } else {
    // Placeholder
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
  }

  if (slideData.summary) {
    slide.addText(slideData.summary, {
      x: 0.5, y: 5.2, w: 9, h: 0.4,
      fontSize: 12, color: T.subtext,
      align: 'center', valign: 'middle',
    });
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Main — async entry point
// ─────────────────────────────────────────────────────────────────────────────
async function main() {
  // Title slide (async image generation)
  await createTitleSlideAsync();

  let chapterIndex = 0;

  if (config.slides && Array.isArray(config.slides)) {
    for (const slide of config.slides) {
      const slideType = slide.slideType || slide.type || 'content';

      if (slideType === 'title') {
        // Already created
        continue;
      }

      if (slideType === 'chapter') {
        chapterIndex = config.slides
          .slice(0, config.slides.indexOf(slide))
          .filter(s => (s.slideType || s.type) === 'chapter').length + 1;
        await createChapterSlideAsync(slide, chapterIndex);
        continue;
      }

      // All other slide types — async versions where image generation is available
      switch (slideType) {
        case 'content':
          createContentSlide(slide, chapterIndex);
          break;
        case 'summary':
          createSummarySlide(config.slides
            .filter(s => (s.slideType || s.type) === 'chapter')
            .map(s => ({ title: s.title })));
          break;
        case 'end':
          // Created after loop
          break;
        case 'problem':
          await createProblemSlideAsync(slide, chapterIndex);
          break;
        case 'concept':
          await createConceptSlideAsync(slide, chapterIndex);
          break;
        case 'derivation':
          createDerivationSlide(slide, chapterIndex);
          break;
        case 'exercise':
          await createExerciseSlideAsync(slide, chapterIndex);
          break;
        case 'diagram':
          await createDiagramSlideAsync(slide, chapterIndex);
          break;
        case 'case':
          await createCaseSlideAsync(slide, chapterIndex);
          break;
        case 'timeline':
          await createTimelineSlideAsync(slide, chapterIndex);
          break;
        case 'comic':
          await createComicSlideAsync(slide, chapterIndex);
          break;
        case 'infographic':
          await createInfographicSlideAsync(slide, chapterIndex);
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
          await createVoteSlideAsync(slide, chapterIndex);
          break;
        case 'poll':
          await createVoteSlideAsync(slide, chapterIndex);
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
          await createExitTicketSlideAsync(slide, chapterIndex);
          break;
        case 'result-viz':
          createResultVizSlide(slide, chapterIndex);
          break;
        case 'text':
          createContentSlide(slide, chapterIndex);
          break;
        default:
          // Unknown type: render as simple content slide
          createContentSlide({ ...slide, title: slide.title, contentList: slide.bullets || slide.contentList || [] }, chapterIndex);
          break;
      }
    }
  } else if (config.chapters && Array.isArray(config.chapters)) {
    for (let idx = 0; idx < config.chapters.length; idx++) {
      await createChapterSlideAsync(config.chapters[idx], idx + 1);
      createContentSlide(config.chapters[idx], idx + 1);
    }
    createSummarySlide(config.chapters);
  }

  // End slide
  createEndSlide();

  // Write file
  await pptx.writeFile({ fileName: outputFile });
  console.log(`[PPT] Written to ${outputFile}`);

  // Cleanup cached images
  for (const [cacheKey, filePath] of IMAGE_CACHE) {
    try {
      if (fs.existsSync(filePath)) {
        fs.unlinkSync(filePath);
      }
    } catch (err) {
      // ignore cleanup errors
    }
  }
  IMAGE_CACHE.clear();
}

main().catch(err => {
  console.error('[PPT Generation Error]', err);
  process.exit(1);
});
