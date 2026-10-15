import { test } from 'node:test';
import assert from 'node:assert/strict';
import { categoryLabel, distribution, historyParams, levelDescription, truncate, formatDateTime }
  from '../../main/resources/static/js/format.js';

test('labels and descriptions', () => {
  assert.equal(categoryLabel('APP_PERMISSION'), 'App permissions');
  assert.equal(categoryLabel('UNKNOWN'), 'UNKNOWN');
  assert.match(levelDescription('LOW'), /does not guarantee safety/);
  assert.match(levelDescription('CRITICAL'), /Stop/);
});

test('distribution computes percentages from real counts only', () => {
  assert.deepEqual(distribution({ LOW: 0, HIGH: 0 }, ['LOW', 'HIGH']), []);
  const d = distribution({ LOW: 1, MEDIUM: 0, HIGH: 3 }, ['LOW', 'MEDIUM', 'HIGH']);
  assert.deepEqual(d.map((x) => [x.key, x.pct]), [['LOW', 25], ['HIGH', 75]]);
});

test('history params drop empty filters', () => {
  assert.deepEqual(historyParams({ q: ' otp ', category: '', riskLevel: null, page: 0, size: 10 }), { q: 'otp', page: '0', size: '10' });
});

test('truncate and dates', () => {
  assert.equal(truncate('abcdef', 4), 'abc…');
  assert.equal(truncate('', 4), '');
  assert.equal(formatDateTime(null), '—');
  assert.equal(formatDateTime('nonsense'), '—');
});
