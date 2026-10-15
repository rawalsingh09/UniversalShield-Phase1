// Lightweight charts drawn from real API data only (no chart library, no placeholder numbers).
import { h, svg } from './dom.js';
import { categoryLabel, distribution, LEVELS } from './format.js';

/** Semicircle-free ring gauge showing a 0-100 risk score with its level. */
export function riskGauge(score, level) {
  const r = 88;
  const c = 2 * Math.PI * r;
  const color = { LOW: 'var(--low)', MEDIUM: 'var(--medium)', HIGH: 'var(--high)', CRITICAL: 'var(--critical)' }[level];
  const arc = svg('circle', { class: 'arc', cx: 110, cy: 110, r, fill: 'none', stroke: color, 'stroke-width': 16,
    'stroke-linecap': 'round', 'stroke-dasharray': c, 'stroke-dashoffset': c, transform: 'rotate(-90 110 110)' });
  const el = svg('svg', { class: 'gauge', viewBox: '0 0 220 220', role: 'img', 'aria-label': `Risk score ${score} out of 100, level ${level}` },
    svg('circle', { class: 'track', cx: 110, cy: 110, r, fill: 'none', 'stroke-width': 16 }),
    arc,
    svg('text', { class: 'value-text', x: 110, y: 118, 'text-anchor': 'middle' }, String(score)),
    svg('text', { class: 'sub-text', x: 110, y: 145, 'text-anchor': 'middle' }, 'RISK SCORE / 100'));
  requestAnimationFrame(() => requestAnimationFrame(() => arc.setAttribute('stroke-dashoffset', String(c * (1 - score / 100)))));
  return el;
}

/** Horizontal bars for scans per category (one hue: this encodes magnitude, not identity). */
export function categoryBars(byCategory) {
  const entries = Object.entries(byCategory);
  const max = Math.max(1, ...entries.map(([, v]) => v));
  return h('div', { class: 'bars', role: 'table', 'aria-label': 'Scans per category' },
    entries.map(([k, v]) => h('div', { class: 'bar-row', role: 'row', title: `${categoryLabel(k)}: ${v} scan(s)` },
      h('span', { role: 'rowheader' }, categoryLabel(k)),
      h('span', { class: 'bar-track', 'aria-hidden': 'true' },
        h('span', { class: 'bar-fill', style: `width:${(v / max) * 100}%` })),
      h('span', { class: 'count', role: 'cell' }, String(v)))));
}

/** Stacked distribution bar of risk levels with a labeled legend (never color alone). */
export function levelDistribution(byLevel) {
  const parts = distribution(byLevel, LEVELS);
  const bar = parts.length === 0
    ? h('p', { class: 'muted small' }, 'No scans yet.')
    : h('div', { class: 'dist', role: 'img', 'aria-label': parts.map((p) => `${p.key}: ${p.count}`).join(', ') },
      parts.map((p) => h('span', { class: 'bg-' + p.key, style: `width:${p.pct}%`, title: `${p.key}: ${p.count} (${p.pct.toFixed(0)}%)` })));
  const legend = h('div', { class: 'legend' },
    LEVELS.map((l) => h('span', {}, h('i', { class: 'bg-' + l, 'aria-hidden': 'true' }), `${l} ${byLevel[l] || 0}`)));
  return h('div', {}, bar, legend);
}
