#!/usr/bin/env node
/**
 * extract-pptx-text.js — Extract plain text from each slide in a PPTX file.
 * Input:  <pptxPath>  (path to a .pptx file)
 * Output: JSON array of { slideNumber, texts[] }
 *
 * Usage: node extract-pptx-text.js /tmp/my.pptx
 */

'use strict';

const path = require('path');
const fs   = require('fs');

const JSZip = require('/usr/local/lib/node_modules/pptxgenjs/node_modules/jszip');

if (process.argv.length < 3) {
  console.error('Usage: node extract-pptx-text.js <pptxPath>');
  process.exit(1);
}

const pptxPath = process.argv[2];

async function extractText() {
  const data = fs.readFileSync(pptxPath);
  const zip  = await JSZip.loadAsync(data);

  // PPTX stores slides as ppt/slides/slideN.xml
  const slideFiles = Object.keys(zip.files)
    .filter(n => /^ppt\/slides\/slide\d+\.xml$/.test(n))
    .sort((a, b) => {
      const na = parseInt(a.match(/slide(\d+)/)[1]);
      const nb = parseInt(b.match(/slide(\d+)/)[1]);
      return na - nb;
    });

  const slides = [];

  for (const slidePath of slideFiles) {
    const xmlString = await zip.files[slidePath].async('string');
    const slideNum  = parseInt(slidePath.match(/slide(\d+)/)[1]);

    // Extract all <a:t> text elements (Office Open XML standard)
    const texts = [];
    const regex = /<a:t[^>]*>([^<]*)<\/a:t>/g;
    let m;
    while ((m = regex.exec(xmlString)) !== null) {
      const t = m[1].trim();
      if (t) texts.push(t);
    }

    // Also grab <p:sp> shape text blocks in one go if needed
    slides.push({ slideNumber: slideNum, texts });
  }

  // Output JSON to stdout
  process.stdout.write(JSON.stringify(slides, null, 2));
}

extractText().catch(err => {
  console.error('Error:', err.message);
  process.exit(1);
});
