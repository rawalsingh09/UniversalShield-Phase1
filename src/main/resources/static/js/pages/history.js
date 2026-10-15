// History page: search, category/level/date filters, pagination, delete one or all.
import { api } from '../api.js';
import { h, mount, loading, emptyState, errorState, confirmDialog, toast } from '../dom.js';
import { CATEGORY_LABELS, LEVELS, categoryLabel, formatDateTime, historyParams } from '../format.js';

const state = { q: '', category: '', riskLevel: '', from: '', to: '', page: 0, size: 10 };

export async function render(container, _params, { navigate }) {
  const results = h('div', {});
  const filters = h('form', { class: 'filters', role: 'search', 'aria-label': 'Filter history' },
    h('div', { class: 'field' }, h('label', { for: 'h-q' }, 'Search'), h('input', { id: 'h-q', name: 'q', type: 'search', maxlength: 100, value: state.q, placeholder: 'Text or indicator…' })),
    h('div', { class: 'field' }, h('label', { for: 'h-cat' }, 'Category'),
      h('select', { id: 'h-cat', name: 'category' }, h('option', { value: '' }, 'All'),
        Object.entries(CATEGORY_LABELS).map(([k, l]) => h('option', { value: k, selected: state.category === k }, l)))),
    h('div', { class: 'field' }, h('label', { for: 'h-lvl' }, 'Risk level'),
      h('select', { id: 'h-lvl', name: 'riskLevel' }, h('option', { value: '' }, 'All'),
        LEVELS.map((l) => h('option', { value: l, selected: state.riskLevel === l }, l)))),
    h('div', { class: 'field' }, h('label', { for: 'h-from' }, 'From'), h('input', { id: 'h-from', name: 'from', type: 'date', value: state.from })),
    h('div', { class: 'field' }, h('label', { for: 'h-to' }, 'To'), h('input', { id: 'h-to', name: 'to', type: 'date', value: state.to })),
    h('div', { class: 'row' }, h('button', { class: 'btn btn-primary', type: 'submit' }, 'Apply'),
      h('button', { class: 'btn btn-ghost', type: 'button', onclick: () => { Object.assign(state, { q: '', category: '', riskLevel: '', from: '', to: '', page: 0 }); render(container, [], { navigate }); } }, 'Reset')));

  filters.addEventListener('submit', (e) => {
    e.preventDefault();
    const f = Object.fromEntries(new FormData(filters).entries());
    if (f.from && f.to && f.from > f.to) { toast("'From' must be on or before 'To'.", 'error'); return; }
    Object.assign(state, f, { page: 0 });
    load();
  });

  const deleteAll = async () => {
    if (!(await confirmDialog('Delete all history?', 'Every scan in your history will be permanently deleted. Your account stays.', 'Delete everything'))) return;
    const r = await api.deleteAllScans();
    toast(`Deleted ${r.deleted} scan(s).`, 'success');
    state.page = 0;
    load();
  };

  async function load() {
    mount(results, loading('Loading history…'));
    let data;
    try {
      data = await api.listScans(historyParams({ ...state }));
    } catch (e) {
      mount(results, errorState(e, load));
      return;
    }
    if (data.totalItems === 0) {
      const filtered = state.q || state.category || state.riskLevel || state.from || state.to;
      mount(results, emptyState(filtered ? 'No scans match these filters' : 'No scans yet',
        filtered ? 'Try different filters or reset them.' : 'Your scan results will appear here.',
        filtered ? null : h('a', { class: 'btn btn-primary', href: '#/scan' }, 'Run your first scan')));
      return;
    }
    const rows = data.items.map((s) => h('tr', {},
      h('td', {}, formatDateTime(s.createdAt)),
      h('td', {}, categoryLabel(s.category)),
      h('td', {}, h('span', { class: `badge level-${s.riskLevel}` }, `${s.riskLevel} · ${s.riskScore}`)),
      h('td', { class: 'small' }, s.excerpt || h('span', { class: 'muted' }, '(no excerpt stored)'),
        s.topIndicator ? h('div', { class: 'muted' }, 'Top sign: ' + s.topIndicator) : null,
        s.demo ? h('span', { class: 'badge demo plain', style: 'margin-top:.25rem' }, 'DEMO') : null),
      h('td', {}, h('div', { class: 'row' },
        h('a', { class: 'btn btn-secondary btn-sm', href: '#/result/' + s.id }, 'View'),
        h('button', { class: 'btn btn-danger btn-sm', type: 'button', 'aria-label': `Delete scan from ${formatDateTime(s.createdAt)}`,
          onclick: async () => {
            if (!(await confirmDialog('Delete this scan?', 'This cannot be undone.', 'Delete'))) return;
            await api.deleteScan(s.id);
            toast('Scan deleted.', 'success');
            if (data.items.length === 1 && state.page > 0) state.page -= 1;
            load();
          } }, 'Delete')))));
    const pageSize = h('select', { 'aria-label': 'Results per page', onchange: (e) => { state.size = Number(e.target.value); state.page = 0; load(); } },
      [10, 20, 50].map((n) => h('option', { value: n, selected: state.size === n }, `${n} per page`)));
    mount(results,
      h('div', { class: 'table-wrap' }, h('table', {},
        h('caption', { class: 'visually-hidden' }, 'Your scan history'),
        h('thead', {}, h('tr', {}, ['Date', 'Category', 'Risk', 'Summary', 'Actions'].map((t) => h('th', { scope: 'col' }, t)))),
        h('tbody', {}, rows))),
      h('div', { class: 'pager' },
        h('span', { class: 'muted small' }, `${data.totalItems} scan(s) · page ${data.page + 1} of ${data.totalPages}`),
        pageSize,
        h('button', { class: 'btn btn-ghost btn-sm', type: 'button', disabled: data.page === 0, onclick: () => { state.page -= 1; load(); } }, 'Previous'),
        h('button', { class: 'btn btn-ghost btn-sm', type: 'button', disabled: data.page + 1 >= data.totalPages, onclick: () => { state.page += 1; load(); } }, 'Next')));
  }

  mount(container,
    h('div', { class: 'page-head' },
      h('div', {}, h('h1', {}, 'Scan history'), h('p', {}, 'Only you can see your history. Scans are deleted automatically after your retention period.')),
      h('button', { class: 'btn btn-danger', type: 'button', onclick: deleteAll }, 'Delete all history')),
    h('div', { class: 'card' }, filters),
    h('div', { class: 'card', style: 'margin-top:1rem' }, results));
  load();
}
