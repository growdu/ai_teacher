#!/usr/bin/env node

/**
 * PPT Generation Script using PptxGenJS
 * Usage: node generate-ppt.js <output-path> <json-config>
 */

const PptxGenJS = require('pptxgenjs');
const fs = require('fs');
const path = require('path');

const outputPath = process.argv[2];
const configPath = process.argv[3];

if (!outputPath || !configPath) {
    console.error('Usage: node generate-ppt.js <output-path> <json-config>');
    process.exit(1);
}

const config = JSON.parse(fs.readFileSync(configPath, 'utf-8'));

const pptx = new PptxGenJS();

// Set presentation properties
pptx.author = 'AI Teacher Studio';
pptx.title = config.title || '教学PPT';
pptx.subject = config.subject || '';
pptx.company = '';

// Define colors based on template
const templates = {
    default: {
        primary: '1E3A8A',      // Blue
        secondary: '3B82F6',
        text: '1F2937',
        lightBg: 'F3F4F6',
        accent: 'EF4444'
    },
    academic: {
        primary: '1E3A5F',      // Dark blue
        secondary: '4A90A4',
        text: '2D3748',
        lightBg: 'EDF2F7',
        accent: '38A169'
    },
    simple: {
        primary: '374151',      // Gray
        secondary: '6B7280',
        text: '111827',
        lightBg: 'F9FAFB',
        accent: 'F59E0B'
    },
    modern: {
        primary: '7C3AED',      // Purple
        secondary: 'A78BFA',
        text: '1F2937',
        lightBg: 'F5F3FF',
        accent: 'EC4899'
    }
};

const theme = templates[config.template] || templates.default;

// Generate slides based on outline
const slides = config.slides || [];

slides.forEach((slideData, index) => {
    const slide = pptx.addSlide();

    // Set background
    if (slideData.type === 'title') {
        slide.background = { color: theme.primary };
    } else {
        slide.background = { color: 'FFFFFF' };
    }

    switch (slideData.type) {
        case 'title':
            // Title slide
            slide.addText(config.title || '教学PPT', {
                x: 0.5,
                y: 2.5,
                w: '90%',
                h: 1.5,
                fontSize: 44,
                bold: true,
                color: 'FFFFFF',
                align: 'center'
            });
            slide.addText(config.subtitle || '', {
                x: 0.5,
                y: 4,
                w: '90%',
                h: 0.6,
                fontSize: 20,
                color: 'FFFFFF',
                align: 'center'
            });
            break;

        case 'chapter':
            // Chapter header slide
            slide.addShape(pptx.ShapeType.rect, {
                x: 0,
                y: 0,
                w: '100%',
                h: '100%',
                fill: { color: theme.primary }
            });
            slide.addText(`第${index}章`, {
                x: 0.5,
                y: 1.5,
                w: '90%',
                h: 0.6,
                fontSize: 18,
                color: 'FFFFFF',
                align: 'center'
            });
            slide.addText(slideData.title, {
                x: 0.5,
                y: 2.2,
                w: '90%',
                h: 1.2,
                fontSize: 36,
                bold: true,
                color: 'FFFFFF',
                align: 'center'
            });
            if (slideData.duration) {
                slide.addText(`时长: ${slideData.duration}分钟`, {
                    x: 0.5,
                    y: 3.5,
                    w: '90%',
                    h: 0.5,
                    fontSize: 14,
                    color: 'FFFFFF',
                    align: 'center'
                });
            }
            break;

        case 'content':
            // Content slide
            // Header bar
            slide.addShape(pptx.ShapeType.rect, {
                x: 0,
                y: 0,
                w: '100%',
                h: 0.8,
                fill: { color: theme.primary }
            });
            slide.addText(slideData.title || config.title, {
                x: 0.5,
                y: 0.15,
                w: '90%',
                h: 0.5,
                fontSize: 20,
                bold: true,
                color: 'FFFFFF'
            });

            // Content area
            if (slideData.content) {
                if (Array.isArray(slideData.content)) {
                    slideData.content.forEach((item, i) => {
                        slide.addText(item, {
                            x: 0.8,
                            y: 1.2 + i * 0.5,
                            w: '85%',
                            h: 0.5,
                            fontSize: 16,
                            color: theme.text,
                            bullet: true
                        });
                    });
                } else {
                    slide.addText(slideData.content, {
                        x: 0.8,
                        y: 1.2,
                        w: '85%',
                        h: 4,
                        fontSize: 18,
                        color: theme.text,
                        valign: 'top'
                    });
                }
            }

            // Key points highlight
            if (slideData.keyPoints && slideData.keyPoints.length > 0) {
                slide.addShape(pptx.ShapeType.rect, {
                    x: 0.5,
                    y: 4.5,
                    w: '90%',
                    h: 0.8,
                    fill: { color: theme.lightBg }
                });
                slide.addText('重点: ' + slideData.keyPoints.join(', '), {
                    x: 0.7,
                    y: 4.6,
                    w: '85%',
                    h: 0.6,
                    fontSize: 12,
                    color: theme.accent,
                    valign: 'middle'
                });
            }
            break;

        case 'image':
            // Image slide
            slide.addShape(pptx.ShapeType.rect, {
                x: 0,
                y: 0,
                w: '100%',
                h: 0.8,
                fill: { color: theme.primary }
            });
            slide.addText(slideData.title || '图示', {
                x: 0.5,
                y: 0.15,
                w: '90%',
                h: 0.5,
                fontSize: 20,
                bold: true,
                color: 'FFFFFF'
            });

            if (slideData.imagePath && fs.existsSync(slideData.imagePath)) {
                slide.addImage({
                    x: 1,
                    y: 1.5,
                    w: 8,
                    h: 4,
                    path: slideData.imagePath
                });
            } else {
                // Placeholder
                slide.addShape(pptx.ShapeType.rect, {
                    x: 1,
                    y: 1.5,
                    w: 8,
                    h: 4,
                    fill: { color: theme.lightBg },
                    line: { color: theme.secondary, width: 1 }
                });
                slide.addText('[图片]', {
                    x: 1,
                    y: 3,
                    w: 8,
                    h: 1,
                    fontSize: 24,
                    color: theme.secondary,
                    align: 'center'
                });
            }
            break;

        case 'quiz':
            // Quiz slide
            slide.addShape(pptx.ShapeType.rect, {
                x: 0,
                y: 0,
                w: '100%',
                h: 0.8,
                fill: { color: theme.accent }
            });
            slide.addText('随堂练习', {
                x: 0.5,
                y: 0.15,
                w: '90%',
                h: 0.5,
                fontSize: 20,
                bold: true,
                color: 'FFFFFF'
            });

            if (slideData.questions) {
                slideData.questions.forEach((q, i) => {
                    slide.addText(`${i + 1}. ${q}`, {
                        x: 0.8,
                        y: 1.2 + i * 0.7,
                        w: '85%',
                        h: 0.6,
                        fontSize: 16,
                        color: theme.text
                    });
                });
            }
            break;

        case 'summary':
            // Summary slide
            slide.addShape(pptx.ShapeType.rect, {
                x: 0,
                y: 0,
                w: '100%',
                h: 0.8,
                fill: { color: theme.primary }
            });
            slide.addText('课堂小结', {
                x: 0.5,
                y: 0.15,
                w: '90%',
                h: 0.5,
                fontSize: 20,
                bold: true,
                color: 'FFFFFF'
            });

            if (slideData.points) {
                slideData.points.forEach((point, i) => {
                    slide.addText(point, {
                        x: 0.8,
                        y: 1.2 + i * 0.6,
                        w: '85%',
                        h: 0.5,
                        fontSize: 16,
                        color: theme.text,
                        bullet: true
                    });
                });
            }
            break;

        case 'end':
            // End slide
            slide.addShape(pptx.ShapeType.rect, {
                x: 0,
                y: 0,
                w: '100%',
                h: '100%',
                fill: { color: theme.primary }
            });
            slide.addText('谢谢观看', {
                x: 0.5,
                y: 2.5,
                w: '90%',
                h: 1,
                fontSize: 48,
                bold: true,
                color: 'FFFFFF',
                align: 'center'
            });
            slide.addText(config.title || '', {
                x: 0.5,
                y: 3.5,
                w: '90%',
                h: 0.6,
                fontSize: 18,
                color: 'FFFFFF',
                align: 'center'
            });
            break;
    }
});

// Save the presentation
pptx.writeFile({ fileName: outputPath })
    .then(() => {
        console.log('PPT generated successfully: ' + outputPath);
        process.exit(0);
    })
    .catch(err => {
        console.error('Failed to generate PPT:', err);
        process.exit(1);
    });