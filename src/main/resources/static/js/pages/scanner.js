// Universal scanner: category tabs, one form per category, client validation, submit to API.
import { api, ApiError } from '../api.js';
import { h, mount, field, showFieldErrors, toast } from '../dom.js';
import { CATEGORY_LABELS } from '../format.js';
import { LIMITS, looksLikeCardNumber, parsePermissionList, validateScan } from '../validation.js';

const COMMON_PERMISSIONS = [
  ['READ_SMS', 'Read SMS'], ['RECEIVE_SMS', 'Receive SMS'], ['SEND_SMS', 'Send SMS'],
  ['BIND_ACCESSIBILITY_SERVICE', 'Accessibility service'], ['SYSTEM_ALERT_WINDOW', 'Display over other apps'],
  ['BIND_NOTIFICATION_LISTENER_SERVICE', 'Read notifications'], ['BIND_DEVICE_ADMIN', 'Device administrator'],
  ['REQUEST_INSTALL_PACKAGES', 'Install other apps'], ['READ_CONTACTS', 'Read contacts'], ['READ_CALL_LOG', 'Read call log'],
  ['ACCESS_FINE_LOCATION', 'Precise location'], ['ACCESS_BACKGROUND_LOCATION', 'Background location'],
  ['RECORD_AUDIO', 'Microphone'], ['CAMERA', 'Camera'], ['READ_PHONE_STATE', 'Phone state'], ['INTERNET', 'Internet'],
];

// Synthetic examples for demonstrations. Invented text; no real people, numbers or accounts.
const SAMPLES = {
  MESSAGE: { channel: 'SMS', sender: 'AX-HDFCBK', text: 'Dear customer, your account will be suspended within 24 hours. Click here http://hdfc-secure-login.top/verify to update KYC and share the OTP you receive to confirm.' },
  URL: { url: 'http://paypal.com.account-verify.top/signin?session=1234' },
  TRANSACTION: { amount: '25000', currency: 'INR', recipient: 'quick-refund-desk@upi', recipientIsNew: 'true', paymentMethod: 'UPI', attemptsLastHour: '3', requestedByOtherParty: 'true', description: 'Customer care said I must enter my UPI PIN to receive a refund for a failed order. They said it is urgent and not to tell anyone.' },
  CALL: { callerClaimedIdentity: 'Bank fraud department', callerNumber: '+1 555 0100', durationMinutes: '15', description: 'Caller said there is suspicious activity on your account and asked me to install AnyDesk so they can secure it, then read out the OTP I receive. Told me not to hang up.' },
  APP_PERMISSION: { appName: 'Mega Flashlight Pro', appCategory: 'FLASHLIGHT', installSource: 'UNKNOWN_SOURCE', permissions: ['CAMERA', 'READ_SMS', 'RECEIVE_SMS', 'BIND_ACCESSIBILITY_SERVICE', 'SYSTEM_ALERT_WINDOW', 'READ_CONTACTS'] },
};

const yesNo = [['', 'Not sure'], ['true', 'Yes'], ['false', 'No']];

function messageForm() {
  return [
    h('div', { class: 'grid grid-2' },
      field({ label: 'Channel', name: 'channel', type: 'select', options: [['SMS', 'SMS'], ['EMAIL', 'Email'], ['CHAT', 'Chat (WhatsApp, Telegram…)'], ['SOCIAL', 'Social media'], ['OTHER', 'Other']] }),
      field({ label: 'Sender (optional)', name: 'sender', hint: 'Name, short code or address shown as the sender.', attrs: { maxlength: LIMITS.shortText } })),
    field({ label: 'Message text', name: 'text', type: 'textarea', required: true, hint: 'Paste the full message. Remove anything personal you do not want stored.', attrs: { maxlength: LIMITS.messageText } }),
  ];
}

function urlForm() {
  return [
    field({ label: 'Link (URL)', name: 'url', required: true, hint: 'UniversalShield analyses the text of the address only. It never opens, downloads or runs the link.', attrs: { maxlength: LIMITS.url, autocomplete: 'off', spellcheck: 'false', inputmode: 'url' } }),
  ];
}

function transactionForm() {
  return [
    h('div', { class: 'note warn small' }, 'Never enter card numbers, CVV, PIN, OTP or online-banking passwords here. UniversalShield never needs them, and masks card-like numbers if pasted.'),
    h('div', { class: 'grid grid-3' },
      field({ label: 'Amount', name: 'amount', type: 'number', required: true, attrs: { min: '0.01', step: '0.01', inputmode: 'decimal' } }),
      field({ label: 'Currency (optional)', name: 'currency', hint: '3-letter code, e.g. INR', attrs: { maxlength: 3, autocomplete: 'off' } }),
      field({ label: 'Payment method', name: 'paymentMethod', type: 'select', options: [['', 'Not sure'], ['UPI', 'UPI'], ['BANK_TRANSFER', 'Bank transfer'], ['CARD', 'Card'], ['WALLET', 'Wallet'], ['GIFT_CARD', 'Gift card'], ['CRYPTO', 'Cryptocurrency'], ['CASH', 'Cash'], ['OTHER', 'Other']] })),
    h('div', { class: 'grid grid-2' },
      field({ label: 'Recipient (optional)', name: 'recipient', hint: 'Name, UPI handle or nickname. Stored only as a keyed hash to recognise repeats.', attrs: { maxlength: LIMITS.shortText } }),
      field({ label: 'Payment attempts in the last hour', name: 'attemptsLastHour', type: 'number', attrs: { min: 0, max: 100, step: 1 } })),
    h('div', { class: 'grid grid-2' },
      field({ label: 'Is this a new recipient?', name: 'recipientIsNew', type: 'select', options: yesNo }),
      field({ label: 'Did the other person ask you to pay?', name: 'requestedByOtherParty', type: 'select', options: yesNo })),
    field({ label: 'What is the payment for? What were you told?', name: 'description', type: 'textarea', attrs: { maxlength: LIMITS.transactionDescription } }),
  ];
}

function callForm() {
  return [
    h('div', { class: 'note small' }, 'UniversalShield never records calls and has no access to caller ID. Describe the call in your own words.'),
    h('div', { class: 'grid grid-3' },
      field({ label: 'Caller claimed to be', name: 'callerClaimedIdentity', hint: 'e.g. "Bank fraud team", "Police"', attrs: { maxlength: LIMITS.shortText } }),
      field({ label: 'Number shown (optional)', name: 'callerNumber', attrs: { maxlength: 25, inputmode: 'tel', autocomplete: 'off' } }),
      field({ label: 'Duration in minutes (optional)', name: 'durationMinutes', type: 'number', attrs: { min: 0, max: 600, step: 1 } })),
    field({ label: 'What did the caller say or ask?', name: 'description', type: 'textarea', required: true, attrs: { maxlength: LIMITS.callDescription } }),
  ];
}

function appForm(onUpload) {
  const fileInput = h('input', { type: 'file', id: 'perm-file', accept: '.txt,.json,.csv', class: 'visually-hidden', onchange: onUpload });
  return [
    h('div', { class: 'grid grid-3' },
      field({ label: 'App name', name: 'appName', required: true, attrs: { maxlength: LIMITS.shortText } }),
      field({ label: 'What does the app say it does?', name: 'appCategory', type: 'select', options: [['OTHER', 'Other'], ['MESSAGING', 'Messaging / SMS'], ['SOCIAL', 'Social'], ['BANKING_FINANCE', 'Banking / finance'], ['LOAN', 'Loan / credit'], ['GAME', 'Game'], ['UTILITY', 'Utility / tools'], ['FLASHLIGHT', 'Flashlight'], ['NAVIGATION', 'Navigation / maps'], ['CAMERA_PHOTO', 'Camera / photo'], ['SHOPPING', 'Shopping'], ['HEALTH', 'Health / fitness'], ['ACCESSIBILITY', 'Accessibility tool'], ['SECURITY', 'Security / antivirus']] }),
      field({ label: 'Installed from', name: 'installSource', type: 'select', options: [['PLAY_STORE', 'Google Play'], ['APP_STORE', 'Apple App Store'], ['OFFICIAL_WEBSITE', "Developer's official website"], ['UNKNOWN_SOURCE', 'APK / link / unknown source']] })),
    h('fieldset', { class: 'field' },
      h('legend', {}, 'Permissions the app requests'),
      h('div', { class: 'check-grid' }, COMMON_PERMISSIONS.map(([code, label]) =>
        h('label', {}, h('input', { type: 'checkbox', name: 'perm', value: code }), h('span', {}, label, h('br'), h('code', { class: 'small' }, code))))),
      h('div', { class: 'error', 'data-error-for': 'permissions', 'aria-live': 'polite' })),
    field({ label: 'Other permissions (optional)', name: 'extraPermissions', type: 'textarea', hint: 'One per line or comma-separated, e.g. android.permission.READ_SMS', attrs: { rows: 3, style: 'min-height:80px' } }),
    h('div', { class: 'row' },
      h('label', { for: 'perm-file', class: 'btn btn-secondary btn-sm', tabindex: 0, role: 'button',
        onkeydown: (e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); fileInput.click(); } } }, 'Upload permission report'),
      fileInput,
      h('span', { class: 'small muted' }, '.txt, .json or .csv, max 64 KB. Parsed on the server and never stored.')),
  ];
}

function readValues(form, category) {
  const v = Object.fromEntries(new FormData(form).entries());
  if (typeof v.currency === 'string') v.currency = v.currency.trim().toUpperCase();
  if (category === 'APP_PERMISSION') {
    const checked = [...form.querySelectorAll('input[name="perm"]:checked')].map((c) => c.value);
    v.permissions = [...new Set([...checked, ...parsePermissionList(v.extraPermissions)])];
  }
  return v;
}

const opt = (s) => (s === undefined || s === null || String(s).trim() === '' ? null : String(s).trim());
const optBool = (s) => (s === 'true' ? true : s === 'false' ? false : null);
const optInt = (s) => (opt(s) === null ? null : Number(s));

export function buildPayload(category, v) {
  switch (category) {
    case 'MESSAGE': return { category, message: { text: v.text, channel: v.channel || 'OTHER', sender: opt(v.sender) } };
    case 'URL': return { category, url: { url: v.url.trim() } };
    case 'TRANSACTION': return { category, transaction: { amount: Number(v.amount), currency: opt(v.currency)?.toUpperCase() ?? null, recipient: opt(v.recipient), recipientIsNew: optBool(v.recipientIsNew), paymentMethod: opt(v.paymentMethod), attemptsLastHour: optInt(v.attemptsLastHour), requestedByOtherParty: optBool(v.requestedByOtherParty), description: opt(v.description) } };
    case 'CALL': return { category, call: { callerClaimedIdentity: opt(v.callerClaimedIdentity), callerNumber: opt(v.callerNumber), description: v.description, durationMinutes: optInt(v.durationMinutes) } };
    case 'APP_PERMISSION': return { category, app: { appName: v.appName.trim(), appCategory: v.appCategory, installSource: v.installSource, permissions: v.permissions } };
    default: throw new Error('Unknown category');
  }
}

function fillSample(form, category) {
  const s = SAMPLES[category];
  form.reset();
  for (const [k, val] of Object.entries(s)) {
    if (k === 'permissions') {
      form.querySelectorAll('input[name="perm"]').forEach((c) => { c.checked = val.includes(c.value); });
    } else if (form.elements[k]) {
      form.elements[k].value = val;
    }
  }
}

export async function render(container, [categoryParam], { navigate }) {
  let category = categoryParam || sessionStorage.getItem('us-default-category');
  if (!category) {
    try {
      category = (await api.profile()).preferences.defaultCategory;
      sessionStorage.setItem('us-default-category', category);
    } catch {
      category = 'MESSAGE';
    }
  }
  const tabs = h('div', { class: 'tabs', role: 'tablist', 'aria-label': 'Scan category' },
    Object.entries(CATEGORY_LABELS).map(([key, label]) => h('button', {
      class: 'tab', role: 'tab', type: 'button', 'aria-selected': String(key === category), 'aria-controls': 'scan-panel',
      onclick: () => navigate('/scan/' + key),
    }, label)));

  const alert = h('div', { class: 'alert error', role: 'alert', hidden: true });
  const submit = h('button', { class: 'btn btn-primary', type: 'submit' }, 'Analyze');
  const form = h('form', { class: 'form', novalidate: true, id: 'scan-panel', role: 'tabpanel', 'aria-label': CATEGORY_LABELS[category] });

  const onUpload = async (ev) => {
    const file = ev.target.files[0];
    ev.target.value = '';
    if (!file) return;
    if (file.size > 64 * 1024) { toast('The file is larger than 64 KB.', 'error'); return; }
    try {
      const parsed = await api.parseAppReport(file);
      if (parsed.appName && !form.appName.value) form.appName.value = parsed.appName;
      const known = new Set(COMMON_PERMISSIONS.map(([c]) => c));
      form.querySelectorAll('input[name="perm"]').forEach((c) => { c.checked = parsed.permissions.includes(c.value) || c.checked; });
      const extra = parsed.permissions.filter((p) => !known.has(p));
      form.extraPermissions.value = [...new Set([...parsePermissionList(form.extraPermissions.value), ...extra])].join('\n');
      toast(`Imported ${parsed.permissions.length} permission(s) from ${file.name}. Review them, then click Analyze.`, 'success');
    } catch (e) {
      toast(e.message, 'error');
    }
  };

  const fields = { MESSAGE: messageForm, URL: urlForm, TRANSACTION: transactionForm, CALL: callForm, APP_PERMISSION: () => appForm(onUpload) }[category]();
  form.append(...fields, alert, h('div', { class: 'row spread' },
    h('button', { class: 'btn btn-ghost btn-sm', type: 'button', onclick: () => { fillSample(form, category); toast('Loaded a synthetic example.'); } }, 'Load synthetic example'),
    h('div', { class: 'row' }, h('button', { class: 'btn btn-ghost', type: 'reset' }, 'Clear'), submit)));

  form.addEventListener('submit', async (ev) => {
    ev.preventDefault();
    alert.hidden = true;
    const values = readValues(form, category);
    const errors = validateScan(category, values);
    if (showFieldErrors(form, Object.entries(errors).map(([f, message]) => ({ field: f, message })))) return;
    if (category === 'TRANSACTION' && looksLikeCardNumber(values.description)) {
      toast('Your description seems to contain a card number. It will be masked before storage. Please avoid entering it.', 'error', 7000);
    }
    submit.disabled = true;
    submit.textContent = 'Analyzing…';
    try {
      const result = await api.createScan(buildPayload(category, values));
      navigate('/result/' + result.id);
    } catch (e) {
      if (e instanceof ApiError && e.fields.length) {
        showFieldErrors(form, e.fields);
        alert.textContent = 'Please correct the highlighted fields.';
      } else {
        alert.textContent = e.message;
      }
      alert.hidden = false;
    } finally {
      submit.disabled = false;
      submit.textContent = 'Analyze';
    }
  });

  mount(container,
    h('div', { class: 'page-head' }, h('div', {}, h('h1', {}, 'Universal scanner'),
      h('p', {}, 'Choose what you want to check. Results are risk assessments, not proof of fraud or safety.'))),
    tabs,
    h('div', { class: 'card' }, form));
}
