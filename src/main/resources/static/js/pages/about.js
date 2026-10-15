// About & methodology: architecture, detectors (live), scoring, ML status, limitations.
import { api } from '../api.js';
import { h, mount, loading } from '../dom.js';

function mlSection(models) {
  return Object.entries(models).map(([task, m]) => {
    const meta = m.metadata;
    const available = m.status.startsWith('AVAILABLE');
    return h('div', { class: 'indicator' },
      h('div', { class: 'head' }, h('span', { class: 'title' }, `${task} model`),
        h('span', { class: 'badge ' + (available ? 'ok' : 'off') }, available ? 'Available' : 'Unavailable')),
      h('p', { class: 'small muted', style: 'margin:.3rem 0' }, m.status),
      meta ? h('div', { class: 'small' },
        h('p', { style: 'margin:.2rem 0' }, `Version ${meta.modelVersion} · ${meta.algorithm}`),
        h('p', { style: 'margin:.2rem 0' }, `Dataset: ${meta.datasetDescription} · train ${meta.trainSize} / test ${meta.testSize} · seed ${meta.splitSeed}`),
        h('p', { style: 'margin:.2rem 0' }, `Test precision ${meta.modelMetrics.precision}, recall ${meta.modelMetrics.recall}, F1 ${meta.modelMetrics.f1}, false-positive rate ${meta.modelMetrics.falsePositiveRate}`),
        h('p', { class: 'muted', style: 'margin:.2rem 0' }, 'These figures were computed by the training pipeline on held-out rows of that dataset only. They are not real-world accuracy.'))
        : h('p', { class: 'small muted', style: 'margin:0' }, 'Train a model with the command in docs/ML.md. Until then the heuristic detectors work on their own.'));
  });
}

export async function render(container) {
  mount(container, loading('Loading methodology…'));
  const [detectors, method] = await Promise.all([api.detectors(), api.methodology()]);
  const t = method.thresholds;
  mount(container,
    h('div', { class: 'page-head' }, h('div', {}, h('h1', {}, 'About & methodology'),
      h('p', {}, 'How UniversalShield works, how scores are calculated, and what it cannot do.'))),
    h('section', { class: 'card' }, h('h2', {}, 'Architecture overview'),
      h('div', { class: 'arch', 'aria-label': 'Request flow' },
        h('span', {}, 'Browser (HTML/CSS/JS)'), h('b', {}, '→'), h('span', {}, 'Spring Security (session + CSRF)'), h('b', {}, '→'),
        h('span', {}, 'REST controllers'), h('b', {}, '→'), h('span', {}, 'ScanService'), h('b', {}, '→'),
        h('span', {}, 'RiskEngine'), h('b', {}, '→'), h('span', {}, '5 detectors + optional ML'), h('b', {}, '→'),
        h('span', {}, 'PostgreSQL / H2 via JPA + Flyway')),
      h('p', { class: 'small muted', style: 'margin-top:1rem' }, 'Java 17+ and Spring Boot on the server; vanilla JavaScript in the browser. Detectors implement one DetectionModule interface, so new ones plug in without changing the engine. Full documentation is in the docs/ folder.')),
    h('section', { class: 'card', style: 'margin-top:1rem' }, h('h2', {}, 'Detection modules'),
      detectors.map((d) => h('div', { class: 'indicator' },
        h('div', { class: 'head' }, h('span', { class: 'title' }, d.displayName),
          h('span', { class: 'row' }, h('span', { class: 'badge source plain' }, d.sourceType), h('span', { class: 'badge ' + (d.available ? 'ok' : 'off') }, d.available ? 'Available' : 'Unavailable'))),
        h('p', { class: 'small muted', style: 'margin:.3rem 0 0' }, d.description),
        h('p', { class: 'small muted', style: 'margin:.2rem 0 0' }, `${d.id} · version ${d.version} · ${d.categories.join(', ')}`)))),
    h('div', { class: 'grid grid-2', style: 'margin-top:1rem' },
      h('section', { class: 'card' }, h('h2', {}, 'Scoring methodology'),
        h('ol', { class: 'small' },
          h('li', {}, 'Each detector reports indicators with a documented weight between 0 and 1 and a correlation group.'),
          h('li', {}, 'Correlated signs share a group (for example several urgency phrases). Only the strongest weight in each group counts, so the same tactic is never double-counted.'),
          h('li', {}, 'Groups are combined with a noisy-OR: ', h('code', {}, method.formula), '. Each independent group raises the score with diminishing returns.'),
          h('li', {}, 'Without verified external data the score is capped at 95, because local rules can never establish certainty.'),
          h('li', {}, `Levels: LOW < ${t.medium} ≤ MEDIUM < ${t.high} ≤ HIGH < ${t.critical} ≤ CRITICAL. Thresholds are configurable.`),
          h('li', {}, 'Evidence strength (LIMITED, MODERATE, STRONG) is reported separately: it counts independent groups and flags short or incomplete input.')),
        h('p', { class: 'small muted' }, `Ruleset ${method.rulesetVersion}. Weights are expert-assigned starting values, not statistically calibrated. docs/SCORING.md explains how to calibrate them with labeled data.`)),
      h('section', { class: 'card' }, h('h2', {}, 'Dataset & model'), mlSection(method.mlModels))),
    h('section', { class: 'card', style: 'margin-top:1rem' }, h('h2', {}, 'Limitations'),
      h('ul', { class: 'small' },
        h('li', {}, 'Heuristics match known wording and structures. New scam scripts, other languages and image-based messages can be missed.'),
        h('li', {}, 'A HIGH score is not proof of fraud, and a LOW score is never a guarantee of safety.'),
        h('li', {}, 'URL analysis is structural only. HTTPS shows that a connection is encrypted, not who runs the site. No verified reputation provider is configured by default.'),
        h('li', {}, 'Call screening is based on your description only. Caller ID can be spoofed, and UniversalShield has no live caller-ID data.'),
        h('li', {}, 'Permission analysis cannot prove an app is malware. "Justified" is judged from the category you choose.'),
        h('li', {}, 'Transaction checks compare only with scans you submitted yourself; there is no access to bank data.'),
        h('li', {}, 'The bundled datasets are small and synthetic. Any model metrics computed on them are optimistic and do not describe real-world performance.'))));
}
