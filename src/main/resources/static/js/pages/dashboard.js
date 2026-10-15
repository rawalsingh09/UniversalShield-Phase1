// Dashboard: statistics computed from the user's own stored scans.
import { api } from '../api.js';
import { h, mount, loading, emptyState } from '../dom.js';
import { categoryBars, levelDistribution } from '../charts.js';
import { categoryLabel, formatDateTime } from '../format.js';
import { session } from '../state.js';

function scanList(items, emptyText) {
  if (!items.length) return h('p', { class: 'muted small' }, emptyText);
  return h('ul', { class: 'list' }, items.map((s) => h('li', {},
    h('div', {},
      h('div', { class: 'row' }, h('span', { class: `badge level-${s.riskLevel}` }, `${s.riskLevel} · ${s.riskScore}`),
        h('span', { class: 'small' }, categoryLabel(s.category)), s.demo ? h('span', { class: 'badge demo plain' }, 'DEMO') : null),
      h('div', { class: 'excerpt' }, s.topIndicator || s.excerpt || 'No indicators matched'),
      h('div', { class: 'small muted' }, formatDateTime(s.createdAt))),
    h('a', { class: 'btn btn-secondary btn-sm', href: '#/result/' + s.id, 'aria-label': 'View result' }, 'View'))));
}

export async function render(container) {
  mount(container, loading('Loading dashboard…'));
  const d = await api.dashboard();
  const high = (d.byRiskLevel.HIGH || 0) + (d.byRiskLevel.CRITICAL || 0);
  const head = h('div', { class: 'page-head' },
    h('div', {}, h('h1', {}, `Hello, ${session.user.displayName}`), h('p', {}, 'Your personal overview, computed from your own scans.')),
    h('a', { class: 'btn btn-primary', href: '#/scan' }, 'New scan'));
  if (d.totalScans === 0) {
    mount(container, head, h('div', { class: 'card' }, emptyState('No scans yet',
      'Run your first scan to see statistics here. Nothing on this page is pre-filled.',
      h('a', { class: 'btn btn-primary', href: '#/scan' }, 'Open the scanner'))));
    return;
  }
  const tile = (label, value, sub) => h('div', { class: 'card tile' }, h('div', { class: 'label' }, label), h('div', { class: 'value' }, String(value)),
    sub ? h('div', { class: 'small muted' }, sub) : null);
  mount(container, head,
    h('div', { class: 'grid grid-4' },
      tile('Total scans', d.totalScans, d.oldestScanAt ? 'since ' + formatDateTime(d.oldestScanAt) : null),
      tile('High or critical', high, d.totalScans ? `${Math.round((high / d.totalScans) * 100)}% of your scans` : null),
      tile('Demo scans', d.demoScans, d.demoScans ? 'synthetic examples' : 'none'),
      tile('Retention', `${d.retentionDays} d`, h('a', { href: '#/settings' }, 'change'))),
    h('div', { class: 'grid grid-2', style: 'margin-top:1rem' },
      h('section', { class: 'card' }, h('h2', {}, 'Scans by category'), categoryBars(d.byCategory)),
      h('section', { class: 'card' }, h('h2', {}, 'Risk-level distribution'), levelDistribution(d.byRiskLevel))),
    h('div', { class: 'grid grid-2', style: 'margin-top:1rem' },
      h('section', { class: 'card' }, h('div', { class: 'row spread' }, h('h2', {}, 'Recent scans'), h('a', { href: '#/history', class: 'small' }, 'All history')),
        scanList(d.recentScans, 'No scans yet.')),
      h('section', { class: 'card' }, h('h2', {}, 'Recent high-risk findings'),
        scanList(d.recentHighRisk, 'No HIGH or CRITICAL results. That does not mean nothing risky reached you.'))),
    h('p', { class: 'note small', style: 'margin-top:1rem' }, d.statisticsNote));
}
