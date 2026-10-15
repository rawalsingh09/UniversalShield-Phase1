// Scan result view: score gauge, level, evidence, indicators, recommendations, method, export.
import { api } from '../api.js';
import { h, mount, loading, confirmDialog, toast } from '../dom.js';
import { riskGauge } from '../charts.js';
import { categoryLabel, evidenceDescription, formatDateTime, levelDescription, SOURCE_LABELS } from '../format.js';

function indicatorCard(i) {
  return h('div', { class: 'indicator' },
    h('div', { class: 'head' },
      h('span', { class: 'title' }, i.title),
      h('span', { class: 'row small muted' }, h('span', { class: 'badge source plain' }, SOURCE_LABELS[i.source] || i.source),
        `weight ${i.weight.toFixed(2)}`)),
    h('div', { class: 'weight-bar', 'aria-hidden': 'true' }, h('span', { style: `width:${Math.round(i.weight * 100)}%` })),
    h('p', { class: 'small muted', style: 'margin:0' }, i.explanation),
    i.evidence ? h('p', { class: 'small', style: 'margin:.4rem 0 0' }, 'Matched: ', h('span', { class: 'evidence' }, i.evidence)) : null,
    h('p', { class: 'small muted', style: 'margin:.3rem 0 0' }, `Group ${i.group} · ${i.code} · ${i.detectorId}`));
}

export async function render(container, [id], { navigate }) {
  mount(container, loading('Loading result…'));
  const r = await api.getScan(id);
  const del = async () => {
    if (!(await confirmDialog('Delete this scan?', 'The result and its indicators will be permanently removed from your history.', 'Delete'))) return;
    await api.deleteScan(r.id);
    toast('Scan deleted.', 'success');
    navigate('/history');
  };
  mount(container,
    h('div', { class: 'page-head' },
      h('div', {}, h('h1', {}, 'Scan result'), h('p', {}, `${categoryLabel(r.category)} · ${formatDateTime(r.createdAt)}`)),
      h('div', { class: 'row' },
        h('a', { class: 'btn btn-secondary btn-sm', href: `/api/reports/${r.id}?format=html`, download: '' }, 'Export HTML report'),
        h('a', { class: 'btn btn-ghost btn-sm', href: `/api/reports/${r.id}?format=json`, download: '' }, 'Export JSON'),
        h('button', { class: 'btn btn-danger btn-sm', type: 'button', onclick: del }, 'Delete'))),
    h('section', { class: 'card result-top', 'aria-label': 'Summary' },
      riskGauge(r.riskScore, r.riskLevel),
      h('div', { class: 'stack' },
        h('div', { class: 'row' },
          h('span', { class: `badge level-${r.riskLevel}` }, r.riskLevel),
          h('span', { class: 'badge source plain' }, `Evidence: ${r.evidenceStrength}`),
          r.sources.map((s) => h('span', { class: 'badge source plain' }, SOURCE_LABELS[s] || s)),
          r.demo ? h('span', { class: 'badge demo plain' }, 'DEMO · synthetic data') : null),
        h('p', { style: 'font-size:1.1rem;margin:0' }, levelDescription(r.riskLevel)),
        h('p', { class: 'muted small', style: 'margin:0' }, evidenceDescription(r.evidenceStrength)),
        r.inputExcerpt ? h('p', { class: 'small', style: 'margin:0' }, h('span', { class: 'muted' }, 'Stored excerpt (sanitised): '), r.inputExcerpt)
          : h('p', { class: 'small muted', style: 'margin:0' }, 'No input excerpt stored (your privacy setting).'))),
    h('div', { class: 'grid grid-2', style: 'margin-top:1rem' },
      h('section', { class: 'card' }, h('h2', {}, 'Recommended actions'),
        h('ol', { class: 'recs' }, r.recommendations.map((x) => h('li', {}, h('strong', {}, x.action), h('br'), h('span', { class: 'muted small' }, x.reason))))),
      h('section', { class: 'card' }, h('h2', {}, 'Why this result'),
        h('ul', {}, r.reasons.map((x) => h('li', { class: 'small' }, x))))),
    h('section', { class: 'card', style: 'margin-top:1rem' },
      h('h2', {}, `Matched indicators (${r.indicators.length})`),
      r.indicators.length ? r.indicators.map(indicatorCard)
        : h('p', { class: 'muted' }, 'No known warning signs matched. That is not a guarantee of safety.')),
    h('div', { class: 'grid grid-2', style: 'margin-top:1rem' },
      h('section', { class: 'card' }, h('h2', {}, 'Scan method'),
        h('dl', { class: 'meta' },
          h('dt', {}, 'Result ID'), h('dd', {}, h('code', {}, r.id)),
          h('dt', {}, 'Timestamp'), h('dd', {}, formatDateTime(r.createdAt)),
          h('dt', {}, 'Ruleset'), h('dd', {}, r.rulesetVersion),
          h('dt', {}, 'Detectors'), h('dd', {}, Object.entries(r.detectorVersions).map(([k, v]) => `${k} ${v}`).join(', ')),
          h('dt', {}, 'Scoring'), h('dd', { class: 'small' }, r.scoringMethod)),
        r.notes.length ? [h('h3', { style: 'margin-top:1rem' }, 'Notes'), h('ul', {}, r.notes.map((n) => h('li', { class: 'small muted' }, n)))] : null),
      h('section', { class: 'card' }, h('h2', {}, 'Important'),
        r.disclaimers.map((d) => h('p', { class: 'note small' }, d)))),
    h('div', { class: 'row', style: 'margin-top:1.5rem' },
      h('a', { class: 'btn btn-primary', href: '#/scan/' + r.category }, 'Scan another'),
      h('a', { class: 'btn btn-secondary', href: '#/history' }, 'View history')));
}
