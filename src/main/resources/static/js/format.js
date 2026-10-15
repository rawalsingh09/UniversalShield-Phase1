// Pure formatting helpers (no DOM), unit-tested with Node.

export const CATEGORY_LABELS = Object.freeze({
  MESSAGE: 'Message',
  URL: 'URL / Website',
  TRANSACTION: 'Transaction',
  CALL: 'Call',
  APP_PERMISSION: 'App permissions',
});

export const LEVELS = Object.freeze(['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']);

export const SOURCE_LABELS = Object.freeze({
  LOCAL_HEURISTIC: 'Local heuristics',
  TRAINED_MODEL: 'Trained model',
  VERIFIED_EXTERNAL: 'Verified external data',
});

export function categoryLabel(c) {
  return CATEGORY_LABELS[c] || c;
}

export function formatDateTime(iso) {
  if (!iso) return '—';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '—';
  return d.toLocaleString(undefined, { year: 'numeric', month: 'short', day: '2-digit', hour: '2-digit', minute: '2-digit' });
}

export function truncate(text, max) {
  if (!text) return '';
  return text.length > max ? text.slice(0, max - 1) + '…' : text;
}

export function levelDescription(level) {
  switch (level) {
    case 'CRITICAL': return 'Several strong warning signs. Stop and do not act on this.';
    case 'HIGH': return 'Strong warning signs. Verify independently before doing anything.';
    case 'MEDIUM': return 'Some warning signs. Check through an official channel.';
    case 'LOW': return 'Few or no warning signs found. This does not guarantee safety.';
    default: return '';
  }
}

export function evidenceDescription(strength) {
  switch (strength) {
    case 'STRONG': return 'Three or more independent groups of signs support this result.';
    case 'MODERATE': return 'Two independent groups, or one strong sign, support this result.';
    default: return 'Limited evidence: short input or only weak signs. Treat with caution.';
  }
}

/** Width percentages for a distribution bar; returns [] when total is 0. */
export function distribution(counts, keys) {
  const total = keys.reduce((s, k) => s + (counts[k] || 0), 0);
  if (total === 0) return [];
  return keys.filter((k) => counts[k] > 0).map((k) => ({ key: k, count: counts[k], pct: (counts[k] / total) * 100 }));
}

/** Builds query params for the history API, dropping empty values. */
export function historyParams(filters) {
  const out = {};
  for (const [k, v] of Object.entries(filters)) {
    if (v !== undefined && v !== null && String(v).trim() !== '') out[k] = String(v).trim();
  }
  return out;
}
