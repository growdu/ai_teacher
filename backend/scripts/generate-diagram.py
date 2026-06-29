#!/usr/bin/env python3
"""
generate-diagram.py — Generate visual diagrams for PPT slides
Uses matplotlib and PIL to create real educational visuals.
Types: flowchart, mindmap, formula, timeline, bento, concept, comic, case, vote
Usage: python3 generate-diagram.py <type> <output.png> <json_config>
"""

import sys
import json
import os
import math
import tempfile
from pathlib import Path

# ─── Imports ──────────────────────────────────────────────────────────────────
MATPLOTLIB_OK = False
try:
    matplotlib_imported = True
    import matplotlib
    matplotlib.use('Agg')
    import matplotlib.pyplot as plt
    import matplotlib.patches as mpatches
    from matplotlib.patches import FancyBboxPatch, FancyArrowPatch, Circle
    MATPLOTLIB_OK = True
except ImportError:
    pass

PIL_OK = False
try:
    from PIL import Image, ImageDraw, ImageFont
    PIL_OK = True
except ImportError:
    pass


# ─── Color Palette ─────────────────────────────────────────────────────────────
COLORS = {
    'primary':   '#1F4E79',
    'secondary': '#2E75B6',
    'accent':    '#5B9BD5',
    'light':     '#F5F5F5',
    'dark':      '#1C2833',
    'text':      '#2C3E50',
    'white':     '#FFFFFF',
    'card_bg':   '#E8F0F8',
    'warm1':     '#F4A460',
    'warm2':     '#E67E22',
    'green':     '#27AE60',
    'red':       '#E74C3C',
    'purple':    '#8E44AD',
}


# ─── Flowchart ─────────────────────────────────────────────────────────────────
def generate_flowchart(config, output):
    if not MATPLOTLIB_OK:
        return None
    items = config.get('bullets', config.get('contentList', []))
    title = config.get('title', '')
    n = len(items)
    fig_h = max(3, n * 0.9 + 1.5)
    fig, ax = plt.subplots(figsize=(10, fig_h))
    ax.set_xlim(0, 10)
    ax.set_ylim(0, fig_h)
    ax.axis('off')
    if title:
        ax.text(5, fig_h - 0.3, title, fontsize=16, fontweight='bold',
                ha='center', va='center', color=COLORS['primary'])
    box_h = 0.65
    box_w = 7.5
    start_x = 1.25
    start_y = fig_h - 1.5
    for i, item in enumerate(items):
        y = start_y - i * 0.85
        if i == 0:
            color = COLORS['primary']
        elif i == n - 1:
            color = COLORS['accent']
        else:
            color = COLORS['secondary']
        fancy = FancyBboxPatch((start_x, y - box_h / 2), box_w, box_h,
                               boxstyle="round,pad=0.05", linewidth=1.5,
                               edgecolor=color, facecolor=color + '22')
        ax.add_patch(fancy)
        circle = Circle((start_x + 0.25, y), 0.22, color=color, zorder=2)
        ax.add_patch(circle)
        ax.text(start_x + 0.25, y, str(i + 1), fontsize=10, ha='center', va='center',
                color='white', fontweight='bold', zorder=3)
        ax.text(start_x + 0.6, y, str(item), fontsize=13, ha='left', va='center',
                color=COLORS['text'], zorder=3)
        if i < n - 1:
            ax.annotate('', xy=(5, y - 0.5), xytext=(5, y - 0.35),
                        arrowprops=dict(arrowstyle='->', color=COLORS['accent'], lw=1.5))
    plt.tight_layout()
    plt.savefig(output, dpi=150, bbox_inches='tight', facecolor='white')
    plt.close()
    return output


# ─── Mind Map ─────────────────────────────────────────────────────────────────
def generate_mindmap(config, output):
    if not MATPLOTLIB_OK:
        return None
    items = config.get('bullets', [])
    title = config.get('title', 'Mind Map')
    center_text = config.get('mainBody', title)
    fig, ax = plt.subplots(figsize=(10, 8))
    ax.set_xlim(-5, 5)
    ax.set_ylim(-4, 4)
    ax.axis('off')
    center = Circle((0, 0), 1.0, color=COLORS['primary'], zorder=2)
    ax.add_patch(center)
    # Split center text into lines
    c1 = center_text[:12] if len(center_text) > 12 else center_text
    c2 = center_text[12:24] if len(center_text) > 12 else ''
    ax.text(0, 0.2, c1, fontsize=10, ha='center', va='center',
            color='white', fontweight='bold', zorder=3)
    if c2:
        ax.text(0, -0.4, c2, fontsize=8, ha='center', va='center',
                color='white', zorder=3)
    n = len(items)
    if n == 0:
        return None
    angles = [2 * math.pi * i / n - math.pi / 2 for i in range(n)]
    radius = 3.0
    spoke_colors = [COLORS['accent'], COLORS['secondary'], COLORS['warm1'],
                    COLORS['green'], COLORS['purple'], COLORS['red']]
    for i, item in enumerate(items):
        angle = angles[i]
        x = radius * math.cos(angle)
        y = radius * math.sin(angle)
        ax.plot([0, x * 0.85], [0, y * 0.85], color=COLORS['accent'],
                linewidth=2, zorder=1)
        node_color = spoke_colors[i % len(spoke_colors)]
        node = Circle((x, y), 0.55, color=node_color, zorder=2)
        ax.add_patch(node)
        text = str(item)
        if len(text) > 8:
            mid = len(text) // 2
            t1 = text[:mid]
            t2 = text[mid:]
            ax.text(x, y + 0.15, t1, fontsize=8, ha='center', va='center',
                    color='white', fontweight='bold', zorder=3)
            ax.text(x, y - 0.15, t2, fontsize=8, ha='center', va='center',
                    color='white', fontweight='bold', zorder=3)
        else:
            ax.text(x, y, text, fontsize=9, ha='center', va='center',
                    color='white', fontweight='bold', zorder=3)
    ax.text(0, 3.6, title, fontsize=14, fontweight='bold', ha='center', va='center',
            color=COLORS['primary'])
    plt.tight_layout()
    plt.savefig(output, dpi=150, bbox_inches='tight', facecolor='white')
    plt.close()
    return output


# ─── Formula Display ──────────────────────────────────────────────────────────
def generate_formula(config, output):
    if not MATPLOTLIB_OK:
        return None
    formula = config.get('formula', '')
    title = config.get('title', '')
    bullets = config.get('bullets', [])
    fig, ax = plt.subplots(figsize=(9, 4))
    ax.set_xlim(0, 9)
    ax.set_ylim(0, 4)
    ax.axis('off')
    bg = FancyBboxPatch((0.2, 0.2), 8.6, 3.6, boxstyle="round,pad=0.1",
                        linewidth=2, edgecolor=COLORS['accent'], facecolor=COLORS['card_bg'])
    ax.add_patch(bg)
    if title:
        ax.text(4.5, 3.5, title, fontsize=13, fontweight='bold', ha='center', va='center',
                color=COLORS['primary'])
    ax.text(4.5, 2.4, formula, fontsize=18, ha='center', va='center',
            color=COLORS['dark'], fontweight='bold',
            bbox=dict(boxstyle='round,pad=0.3', facecolor='white', edgecolor=COLORS['primary']))
    y_start = 1.7
    for i, b in enumerate(bullets[:3]):
        ax.text(0.8, y_start - i * 0.5, '- ' + str(b), fontsize=11, ha='left', va='center',
                color=COLORS['text'])
    plt.tight_layout()
    plt.savefig(output, dpi=150, bbox_inches='tight', facecolor='white')
    plt.close()
    return output


# ─── Timeline ─────────────────────────────────────────────────────────────────
def generate_timeline(config, output):
    if not MATPLOTLIB_OK:
        return None
    items = config.get('bullets', config.get('contentList', []))
    title = config.get('title', '')
    if not items:
        return None
    n = len(items)
    total_w = max(9, n * 2.5)
    fig, ax = plt.subplots(figsize=(total_w / 1.5, 5))
    ax.set_xlim(-0.5, total_w + 0.5)
    ax.set_ylim(-2, 2)
    ax.axis('off')
    ax.plot([0, total_w], [0, 0], color=COLORS['secondary'], linewidth=3, zorder=1)
    x_positions = [i * total_w / max(n - 1, 1) for i in range(n)]
    node_colors = [COLORS['primary'], COLORS['accent'], COLORS['warm1'],
                   COLORS['green'], COLORS['purple']]
    for i, (x, item) in enumerate(zip(x_positions, items)):
        node = Circle((x, 0), 0.35, color=node_colors[i % len(node_colors)], zorder=3)
        ax.add_patch(node)
        ax.text(x, 0, str(i + 1), fontsize=11, ha='center', va='center',
                color='white', fontweight='bold', zorder=4)
        if isinstance(item, dict):
            label = item.get('label', item.get('year', 'Step ' + str(i + 1)))
            desc = item.get('event', item.get('description', str(item)))
        else:
            label = 'Step ' + str(i + 1)
            desc = str(item)
        ax.text(x, 0.7, str(label), fontsize=9, ha='center', va='bottom',
                color=COLORS['primary'], fontweight='bold')
        ax.text(x, -0.4, str(desc)[:15], fontsize=8, ha='center', va='top',
                color=COLORS['text'])
    if title:
        ax.text(total_w / 2, 1.6, title, fontsize=14, fontweight='bold',
                ha='center', va='center', color=COLORS['primary'])
    plt.tight_layout()
    plt.savefig(output, dpi=150, bbox_inches='tight', facecolor='white')
    plt.close()
    return output


# ─── Bento Grid ───────────────────────────────────────────────────────────────
def generate_bento(config, output):
    if not MATPLOTLIB_OK:
        return None
    items = config.get('bullets', config.get('contentList', []))
    title = config.get('title', '')
    if not items:
        return None
    n = min(len(items), 6)
    fig, ax = plt.subplots(figsize=(10, 6))
    ax.set_xlim(0, 10)
    ax.set_ylim(0, 6)
    ax.axis('off')
    if title:
        ax.text(5, 5.6, title, fontsize=15, fontweight='bold', ha='center', va='center',
                color=COLORS['primary'])
    col_colors = [COLORS['primary'], COLORS['secondary'], COLORS['accent'],
                 COLORS['warm1'], COLORS['green'], COLORS['purple']]
    layouts = [
        (0.3, 2.8, 4.5, 2.6),
        (5.0, 2.8, 2.2, 2.6),
        (7.4, 2.8, 2.3, 2.6),
        (0.3, 0.3, 2.2, 2.3),
        (2.7, 0.3, 2.2, 2.3),
        (5.0, 0.3, 4.7, 2.3),
    ]
    for i in range(n):
        x, y, w, h = layouts[i]
        color = col_colors[i % len(col_colors)]
        rect = FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.08",
                              linewidth=1.5, edgecolor=color, facecolor=color + '33')
        ax.add_patch(rect)
        badge = Circle((x + 0.35, y + h - 0.35), 0.25, color=color, zorder=2)
        ax.add_patch(badge)
        ax.text(x + 0.35, y + h - 0.35, str(i + 1), fontsize=9, ha='center', va='center',
                color='white', fontweight='bold', zorder=3)
        text = str(items[i])
        if len(text) > 20:
            mid = len(text) // 2
            t1 = text[:mid]
            t2 = text[mid:]
            ax.text(x + 0.2, y + h / 2 + 0.15, t1, fontsize=10, ha='left', va='center',
                    color=COLORS['text'], fontweight='bold')
            ax.text(x + 0.2, y + h / 2 - 0.2, t2, fontsize=10, ha='left', va='center',
                    color=COLORS['text'], fontweight='bold')
        else:
            ax.text(x + 0.2, y + h / 2, text, fontsize=11, ha='left', va='center',
                    color=COLORS['text'], fontweight='bold')
    plt.tight_layout()
    plt.savefig(output, dpi=150, bbox_inches='tight', facecolor='white')
    plt.close()
    return output


# ─── Concept Illustration ──────────────────────────────────────────────────────
def generate_concept(config, output):
    if not MATPLOTLIB_OK:
        return None
    title = config.get('title', '')
    formula = config.get('formula', '')
    bullets = config.get('bullets', [])
    main_body = config.get('mainBody', '')
    fig, axes = plt.subplots(1, 2, figsize=(10, 5), gridspec_kw={'width_ratios': [1, 1]})
    ax_left = axes[0]
    ax_right = axes[1]
    for ax in [ax_left, ax_right]:
        ax.set_xlim(0, 5)
        ax.set_ylim(0, 5)
        ax.axis('off')
    ax_left.text(2.5, 4.5, title, fontsize=14, fontweight='bold', ha='center', va='center',
                 color=COLORS['primary'])
    if main_body:
        ax_left.text(2.5, 3.7, main_body[:80], fontsize=10, ha='center', va='center',
                     color=COLORS['text'])
    for i, b in enumerate(bullets[:3]):
        ax_left.text(0.3, 3.0 - i * 0.6, '- ' + str(b), fontsize=10, ha='left', va='center',
                     color=COLORS['text'])
    if formula:
        bg = FancyBboxPatch((0.3, 1.5), 4.4, 2.5, boxstyle="round,pad=0.1",
                             linewidth=2, edgecolor=COLORS['accent'], facecolor=COLORS['card_bg'])
        ax_right.add_patch(bg)
        ax_right.text(2.5, 3.0, 'Formula', fontsize=10, ha='center', va='center',
                      color=COLORS['accent'], fontweight='bold')
        ax_right.text(2.5, 2.2, formula, fontsize=12, ha='center', va='center',
                      color=COLORS['dark'], fontweight='bold')
    plt.tight_layout()
    plt.savefig(output, dpi=150, bbox_inches='tight', facecolor='white')
    plt.close()
    return output


# ─── Comic Strip ─────────────────────────────────────────────────────────────
def generate_comic(config, output):
    if not PIL_OK:
        return None
    panels = config.get('comicPanels', [])
    title = config.get('title', '')
    if not panels:
        return None
    n = min(len(panels), 3)
    panel_w = 400
    panel_h = 300
    gap = 20
    img_w = n * panel_w + (n - 1) * gap + 40
    img_h = panel_h + 80
    img = Image.new('RGB', (img_w, img_h), color='white')
    draw = ImageDraw.Draw(img)
    # Simple font
    try:
        fnt = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 14)
    except:
        fnt = ImageFont.load_default()
    # Title
    draw.text((img_w // 2, 20), title, fill=COLORS['primary'])
    for i, panel in enumerate(panels[:n]):
        x = 20 + i * (panel_w + gap)
        y = 50
        draw.rectangle([x, y, x + panel_w, y + panel_h],
                       outline=COLORS['primary'], width=3)
        scene = panel.get('scene', '')
        dialogue = panel.get('dialogue', '')
        caption = panel.get('caption', '')
        draw.text((x + 10, y + 10), scene[:30], fill=COLORS['text'])
        draw.text((x + 10, y + panel_h // 2), 'D: ' + dialogue[:25], fill=COLORS['dark'])
        draw.text((x + 10, y + panel_h - 30), 'N: ' + caption[:25], fill=COLORS['accent'])
    img.save(output)
    return output


# ─── Case Study ──────────────────────────────────────────────────────────────
def generate_case(config, output):
    if not MATPLOTLIB_OK:
        return None
    title = config.get('title', '')
    scenario = config.get('scenario', config.get('mainBody', ''))
    bullets = config.get('bullets', config.get('analysis', []))
    fig, ax = plt.subplots(figsize=(9, 5))
    ax.set_xlim(0, 9)
    ax.set_ylim(0, 5)
    ax.axis('off')
    bg = FancyBboxPatch((0.2, 0.2), 8.6, 4.6, boxstyle="round,pad=0.1",
                        linewidth=2, edgecolor=COLORS['secondary'], facecolor=COLORS['light'])
    ax.add_patch(bg)
    ax.text(4.5, 4.4, title, fontsize=14, fontweight='bold', ha='center', va='center',
            color=COLORS['primary'])
    if scenario:
        scenario_bg = FancyBboxPatch((0.5, 2.8), 8.0, 1.3, boxstyle="round,pad=0.05",
                                      linewidth=1, edgecolor=COLORS['accent'], facecolor='white')
        ax.add_patch(scenario_bg)
        ax.text(4.5, 3.5, scenario[:60], fontsize=11, ha='center', va='center',
                color=COLORS['text'])
    y = 2.5
    for b in bullets[:3]:
        text = str(b.get('text', b)) if isinstance(b, dict) else str(b)
        ax.text(0.8, y, text[:40], fontsize=10, ha='left', va='center',
                color=COLORS['text'])
        y -= 0.7
    plt.tight_layout()
    plt.savefig(output, dpi=150, bbox_inches='tight', facecolor='white')
    plt.close()
    return output


# ─── Vote/Quiz ────────────────────────────────────────────────────────────────
def generate_vote(config, output):
    if not MATPLOTLIB_OK:
        return None
    question = config.get('question', config.get('mainBody', ''))
    options = config.get('options', config.get('choices', []))
    title = config.get('title', '')
    n_opts = min(len(options), 4)
    fig, ax = plt.subplots(figsize=(9, 5))
    ax.set_xlim(0, 9)
    ax.set_ylim(0, 5)
    ax.axis('off')
    if title:
        ax.text(4.5, 4.6, title, fontsize=13, fontweight='bold', ha='center', va='center',
                color=COLORS['primary'])
    ax.text(4.5, 3.8, question[:50], fontsize=14, ha='center', va='center',
            color=COLORS['dark'], fontweight='bold')
    opt_colors = [COLORS['primary'], COLORS['accent'], COLORS['secondary'], COLORS['warm1']]
    labels = ['A', 'B', 'C', 'D']
    for i in range(n_opts):
        color = opt_colors[i % len(opt_colors)]
        y = 2.8 - i * 0.7
        rect = FancyBboxPatch((1.0, y - 0.25), 7.0, 0.55, boxstyle="round,pad=0.05",
                              linewidth=1.5, edgecolor=color, facecolor=color + '22')
        ax.add_patch(rect)
        label_circle = Circle((1.4, y + 0.025), 0.22, color=color, zorder=2)
        ax.add_patch(label_circle)
        ax.text(1.4, y + 0.025, labels[i], fontsize=10, ha='center', va='center',
                color='white', fontweight='bold', zorder=3)
        ax.text(1.9, y + 0.025, str(options[i])[:30], fontsize=12, ha='left', va='center',
                color=COLORS['text'])
    plt.tight_layout()
    plt.savefig(output, dpi=150, bbox_inches='tight', facecolor='white')
    plt.close()
    return output


# ─── Diagram Types Registry ────────────────────────────────────────────────────
DIAGRAM_TYPES = {
    'flowchart': generate_flowchart,
    'mindmap':   generate_mindmap,
    'formula':   generate_formula,
    'timeline':  generate_timeline,
    'bento':     generate_bento,
    'concept':   generate_concept,
    'comic':     generate_comic,
    'case':      generate_case,
    'vote':      generate_vote,
}


# ─── Main ─────────────────────────────────────────────────────────────────────
def main():
    if len(sys.argv) < 4:
        print("Usage: python3 generate-diagram.py <type> <output.png> <json_config>", file=sys.stderr)
        sys.exit(1)
    diagram_type = sys.argv[1]
    output_path = sys.argv[2]
    config_json = sys.argv[3]
    try:
        config = json.loads(config_json)
    except (json.JSONDecodeError, TypeError):
        try:
            with open(config_json) as f:
                config = json.load(f)
        except Exception:
            config = {}
    generator = DIAGRAM_TYPES.get(diagram_type)
    if not generator:
        print("Unknown diagram type: " + diagram_type, file=sys.stderr)
        print("Available: " + ', '.join(DIAGRAM_TYPES.keys()), file=sys.stderr)
        sys.exit(1)
    if not MATPLOTLIB_OK and diagram_type == 'comic':
        print("matplotlib and PIL not available", file=sys.stderr)
        sys.exit(1)
    result = generator(config, output_path)
    if result and os.path.exists(result):
        print(result)
        sys.exit(0)
    else:
        print("Failed to generate " + diagram_type, file=sys.stderr)
        sys.exit(1)


if __name__ == '__main__':
    main()
