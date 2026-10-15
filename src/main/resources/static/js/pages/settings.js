// Settings & privacy: preferences, retention info, delete history, delete account.
import { api } from '../api.js';
import { h, mount, loading, field, showFieldErrors, confirmDialog, toast } from '../dom.js';
import { CATEGORY_LABELS, formatDateTime } from '../format.js';
import { session } from '../state.js';

export async function render(container, _params, { navigate }) {
  mount(container, loading('Loading settings…'));
  const [profile, dash] = await Promise.all([api.profile(), api.dashboard()]);
  const p = profile.preferences;

  const prefsAlert = h('div', { class: 'alert', role: 'status', hidden: true });
  const prefs = h('form', { class: 'form', novalidate: true },
    field({ label: 'Keep scan history for (days)', name: 'retentionDays', type: 'number', required: true, value: String(p.retentionDays),
      hint: 'Between 1 and 365. Older scans are deleted automatically every night.', attrs: { min: 1, max: 365, step: 1 } }),
    field({ label: 'Default scanner category', name: 'defaultCategory', type: 'select', value: p.defaultCategory, options: Object.entries(CATEGORY_LABELS) }),
    h('div', { class: 'field' },
      h('label', { class: 'row', style: 'font-weight:500' },
        h('input', { type: 'checkbox', name: 'storeInputExcerpts', checked: p.storeInputExcerpts }),
        'Store a sanitised excerpt of what I scan'),
      h('div', { class: 'hint' }, 'When off, only the score, level and indicators are stored, not the text you entered. Card numbers and codes are always masked.')),
    prefsAlert,
    h('div', {}, h('button', { class: 'btn btn-primary', type: 'submit' }, 'Save preferences')));
  prefs.addEventListener('submit', async (e) => {
    e.preventDefault();
    prefsAlert.hidden = true;
    const days = Number(prefs.retentionDays.value);
    if (!Number.isInteger(days) || days < 1 || days > 365) {
      showFieldErrors(prefs, [{ field: 'retentionDays', message: 'Enter a whole number from 1 to 365' }]);
      return;
    }
    showFieldErrors(prefs, []);
    try {
      const saved = await api.savePreferences({ retentionDays: days, storeInputExcerpts: prefs.storeInputExcerpts.checked, defaultCategory: prefs.defaultCategory.value });
      sessionStorage.setItem('us-default-category', saved.defaultCategory);
      prefsAlert.className = 'alert success';
      prefsAlert.textContent = 'Preferences saved.';
    } catch (err) {
      if (err.fields.length) showFieldErrors(prefs, err.fields);
      prefsAlert.className = 'alert error';
      prefsAlert.textContent = err.message;
    }
    prefsAlert.hidden = false;
  });

  const delHistory = async () => {
    if (!(await confirmDialog('Delete all scan history?', 'All of your scans will be permanently deleted. Your account and preferences stay.', 'Delete history'))) return;
    const r = await api.deleteHistory();
    toast(`Deleted ${r.deleted} scan(s).`, 'success');
    render(container, [], { navigate });
  };

  const accountForm = h('form', { class: 'form', novalidate: true },
    field({ label: 'Confirm with your password', name: 'password', type: 'password', required: true, attrs: { autocomplete: 'current-password', maxlength: 128 } }),
    h('div', {}, h('button', { class: 'btn btn-danger', type: 'submit' }, 'Delete my account and all data')));
  accountForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const pw = accountForm.password.value;
    if (!pw) { showFieldErrors(accountForm, [{ field: 'password', message: 'Password is required' }]); return; }
    if (!(await confirmDialog('Delete your account?', 'Your account, preferences and every scan will be permanently deleted. This cannot be undone.', 'Delete account'))) return;
    try {
      await api.deleteAccount(pw);
      session.user = null;
      await api.csrf();
      toast('Your account and data were deleted.', 'success');
      navigate('/');
    } catch (err) {
      showFieldErrors(accountForm, [{ field: 'password', message: err.message }]);
    }
  });

  mount(container,
    h('div', { class: 'page-head' }, h('div', {}, h('h1', {}, 'Settings & privacy'), h('p', {}, `Signed in as ${profile.email} · member since ${formatDateTime(profile.memberSince)}`))),
    h('div', { class: 'grid grid-2' },
      h('section', { class: 'card' }, h('h2', {}, 'Preferences'), prefs),
      h('section', { class: 'card stack' },
        h('h2', {}, 'Data retention'),
        h('p', { class: 'small' }, `You have ${dash.totalScans} stored scan(s). `, dash.oldestScanAt ? `The oldest is from ${formatDateTime(dash.oldestScanAt)}.` : ''),
        h('p', { class: 'small muted' }, `Scans older than ${p.retentionDays} days are removed by a nightly job. Deleting a scan removes it and its indicators immediately. Security audit entries (sign-ins, deletions) contain no scan content, store only a hashed IP address and are removed after one year.`),
        h('button', { class: 'btn btn-danger', type: 'button', onclick: delHistory, disabled: dash.totalScans === 0 }, 'Delete all scan history'))),
    h('div', { class: 'grid grid-2', style: 'margin-top:1rem' },
      h('section', { class: 'card' }, h('h2', {}, 'Consent & privacy'),
        h('ul', { class: 'small' },
          h('li', {}, 'You decide what to scan. UniversalShield never reads your messages, calls, contacts or device activity on its own.'),
          h('li', {}, 'Links are never opened by the server, and calls are never recorded.'),
          h('li', {}, 'Recipients, domains and phone numbers are stored only as keyed hashes so we can tell you "you scanned this before" without keeping the value.'),
          h('li', {}, 'No data is shared with third parties. No external reputation provider is configured by default.'),
          h('li', {}, 'Your password is stored only as a BCrypt hash.'))),
      h('section', { class: 'card' }, h('h2', {}, 'Delete my data'),
        h('p', { class: 'small muted' }, 'Deletes your account, preferences and all scans. Audit entries are kept without any link to you.'),
        accountForm)));
}
