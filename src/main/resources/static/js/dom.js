// Tiny DOM helpers. All text goes through textContent, never innerHTML, so user input and
// scanned content can never be interpreted as HTML (XSS-safe by construction).

/**
 * h('div', {class: 'card', onclick: fn}, 'text', childNode, [more children])
 * Attributes starting with "on" become event listeners; others are set as attributes.
 */
export function h(tag, attrs = {}, ...children) {
  const el = document.createElement(tag);
  for (const [key, value] of Object.entries(attrs || {})) {
    if (value === undefined || value === null || value === false) continue;
    if (key.startsWith('on') && typeof value === 'function') {
      el.addEventListener(key.slice(2).toLowerCase(), value);
    } else if (key === 'class') {
      el.className = value;
    } else if (value === true) {
      el.setAttribute(key, '');
    } else {
      el.setAttribute(key, String(value));
    }
  }
  append(el, children);
  return el;
}

function append(el, children) {
  for (const child of children.flat(Infinity)) {
    if (child === null || child === undefined || child === false) continue;
    el.append(child instanceof Node ? child : document.createTextNode(String(child)));
  }
}

const SVG_NS = 'http://www.w3.org/2000/svg';
export function svg(tag, attrs = {}, ...children) {
  const el = document.createElementNS(SVG_NS, tag);
  for (const [k, v] of Object.entries(attrs)) {
    if (v !== undefined && v !== null) el.setAttribute(k, String(v));
  }
  append(el, children);
  return el;
}

export function mount(container, ...nodes) {
  container.replaceChildren(...nodes.flat());
}

export function loading(label = 'Loading…') {
  return h('div', { class: 'state', role: 'status' }, h('div', { class: 'spinner', 'aria-hidden': 'true' }), label);
}

export function emptyState(title, text, action) {
  return h('div', { class: 'state' }, h('div', { class: 'big', 'aria-hidden': 'true' }, '🛡️'),
    h('h3', {}, title), h('p', {}, text), action || null);
}

export function errorState(error, retry) {
  return h('div', { class: 'state' },
    h('div', { class: 'alert error', role: 'alert' }, error.message || 'Something went wrong.'),
    retry ? h('p', {}, h('button', { class: 'btn btn-secondary', onclick: retry, style: 'margin-top:1rem' }, 'Try again')) : null);
}

export function toast(message, type = 'info', ms = 4500) {
  const box = document.getElementById('toasts');
  const t = h('div', { class: 'toast ' + type }, message);
  box.append(t);
  setTimeout(() => t.remove(), ms);
}

/** Accessible confirmation dialog using the native <dialog> element. Resolves true/false. */
export function confirmDialog(title, message, confirmLabel = 'Confirm', danger = true) {
  return new Promise((resolve) => {
    const dlg = h('dialog', { 'aria-labelledby': 'dlg-title' });
    const close = (result) => { dlg.close(); dlg.remove(); resolve(result); };
    dlg.append(
      h('h2', { id: 'dlg-title' }, title),
      h('p', { class: 'muted' }, message),
      h('div', { class: 'row', style: 'justify-content:flex-end' },
        h('button', { class: 'btn btn-ghost', onclick: () => close(false) }, 'Cancel'),
        h('button', { class: danger ? 'btn btn-danger' : 'btn btn-primary', onclick: () => close(true) }, confirmLabel)));
    dlg.addEventListener('cancel', (e) => { e.preventDefault(); close(false); });
    document.body.append(dlg);
    dlg.showModal();
  });
}

/** Shows server-side validation errors next to the matching fields. */
export function showFieldErrors(form, fields) {
  form.querySelectorAll('.error').forEach((e) => { e.textContent = ''; });
  form.querySelectorAll('[aria-invalid]').forEach((e) => e.removeAttribute('aria-invalid'));
  let first = null;
  for (const { field, message } of fields) {
    const key = field.split('.').pop().replace(/\[\d+\]$/, '');
    const input = form.querySelector(`[name="${CSS.escape(key)}"]`);
    const slot = form.querySelector(`[data-error-for="${CSS.escape(key)}"]`);
    if (input) { input.setAttribute('aria-invalid', 'true'); first = first || input; }
    if (slot) slot.textContent = message;
  }
  if (first) first.focus();
  return first !== null;
}

/** A labeled form field with hint and error slot wired up for screen readers. */
export function field({ label, name, type = 'text', hint, required, attrs = {}, options, value }) {
  const id = 'f-' + name;
  const hintId = hint ? id + '-hint' : null;
  const errId = id + '-err';
  const describedBy = [hintId, errId].filter(Boolean).join(' ');
  let control;
  if (type === 'textarea') {
    control = h('textarea', { id, name, required, 'aria-describedby': describedBy, ...attrs });
    if (value) control.value = value;
  } else if (type === 'select') {
    control = h('select', { id, name, required, 'aria-describedby': describedBy, ...attrs },
      options.map(([v, l]) => h('option', { value: v, selected: v === value }, l)));
  } else {
    control = h('input', { id, name, type, required, 'aria-describedby': describedBy, value, ...attrs });
  }
  return h('div', { class: 'field' },
    h('label', { for: id }, label, required ? h('span', { 'aria-hidden': 'true', class: 'muted' }, ' *') : null),
    control,
    hint ? h('div', { class: 'hint', id: hintId }, hint) : null,
    h('div', { class: 'error', id: errId, 'data-error-for': name, 'aria-live': 'polite' }));
}
