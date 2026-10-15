// Sign-in and registration pages.
import { api, ApiError } from '../api.js';
import { h, mount, field, showFieldErrors, toast } from '../dom.js';
import { session } from '../state.js';
import { validateEmail, validateRegistration } from '../validation.js';

function submitButton(label) {
  return h('button', { class: 'btn btn-primary', type: 'submit' }, label);
}

async function afterAuth(user, navigate) {
  session.user = user;
  const next = sessionStorage.getItem('us-after-login');
  sessionStorage.removeItem('us-after-login');
  navigate(next && next !== '/login' && next !== '/register' ? next : '/dashboard');
}

function handleError(form, alert, e) {
  if (e instanceof ApiError && e.fields.length) {
    showFieldErrors(form, e.fields);
    alert.textContent = 'Please correct the highlighted fields.';
  } else {
    alert.textContent = e.message;
  }
  alert.hidden = false;
}

export const login = {
  render(container, _params, { navigate }) {
    const alert = h('div', { class: 'alert error', role: 'alert', hidden: true });
    const btn = submitButton('Sign in');
    const form = h('form', { class: 'form', novalidate: true },
      field({ label: 'Email', name: 'email', type: 'email', required: true, attrs: { autocomplete: 'email', maxlength: 254 } }),
      field({ label: 'Password', name: 'password', type: 'password', required: true, attrs: { autocomplete: 'current-password', maxlength: 128 } }),
      alert, btn);
    form.addEventListener('submit', async (ev) => {
      ev.preventDefault();
      alert.hidden = true;
      const email = form.email.value.trim();
      const password = form.password.value;
      const errs = [];
      const e1 = validateEmail(email); if (e1) errs.push({ field: 'email', message: e1 });
      if (!password) errs.push({ field: 'password', message: 'Password is required' });
      if (showFieldErrors(form, errs)) return;
      btn.disabled = true; btn.textContent = 'Signing in…';
      try {
        const user = await api.login(email, password);
        toast(`Welcome back, ${user.displayName}.`, 'success');
        await afterAuth(user, navigate);
      } catch (e) {
        handleError(form, alert, e);
        form.password.value = '';
      } finally {
        btn.disabled = false; btn.textContent = 'Sign in';
      }
    });
    mount(container, h('div', { class: 'auth-wrap card' },
      h('h1', {}, 'Sign in'),
      h('p', { class: 'muted' }, 'Access your scanner, dashboard and private history.'),
      form,
      h('p', { class: 'small muted', style: 'margin-top:1rem' }, 'No account yet? ', h('a', { href: '#/register' }, 'Create one')),
      h('p', { class: 'small muted' }, 'Running locally in the dev profile? A demonstration account is described in docs/DEMO_GUIDE.md.')));
  },
};

export const register = {
  render(container, _params, { navigate }) {
    const alert = h('div', { class: 'alert error', role: 'alert', hidden: true });
    const btn = submitButton('Create account');
    const form = h('form', { class: 'form', novalidate: true },
      field({ label: 'Name', name: 'displayName', required: true, attrs: { autocomplete: 'name', maxlength: 80 } }),
      field({ label: 'Email', name: 'email', type: 'email', required: true, attrs: { autocomplete: 'email', maxlength: 254 } }),
      field({ label: 'Password', name: 'password', type: 'password', required: true, hint: 'At least 10 characters, including a letter and a digit.', attrs: { autocomplete: 'new-password', maxlength: 128 } }),
      field({ label: 'Confirm password', name: 'confirm', type: 'password', required: true, attrs: { autocomplete: 'new-password', maxlength: 128 } }),
      h('div', { class: 'note small' }, 'By creating an account you agree that the scans you submit are stored in your private history (sanitised, and only for the retention period you choose). You can delete them at any time in Settings.'),
      alert, btn);
    form.addEventListener('submit', async (ev) => {
      ev.preventDefault();
      alert.hidden = true;
      const values = { displayName: form.displayName.value, email: form.email.value.trim(), password: form.password.value, confirm: form.confirm.value };
      const errs = validateRegistration(values);
      if (showFieldErrors(form, Object.entries(errs).map(([f, message]) => ({ field: f, message })))) return;
      btn.disabled = true; btn.textContent = 'Creating account…';
      try {
        const user = await api.register(values.displayName.trim(), values.email, values.password);
        toast('Account created. Welcome to UniversalShield!', 'success');
        await afterAuth(user, navigate);
      } catch (e) {
        handleError(form, alert, e);
      } finally {
        btn.disabled = false; btn.textContent = 'Create account';
      }
    });
    mount(container, h('div', { class: 'auth-wrap card' },
      h('h1', {}, 'Create your account'),
      h('p', { class: 'muted' }, 'Free. No payment details, no phone number, no access to your device.'),
      form,
      h('p', { class: 'small muted', style: 'margin-top:1rem' }, 'Already registered? ', h('a', { href: '#/login' }, 'Sign in'))));
  },
};
