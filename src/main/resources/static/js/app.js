// Application bootstrap and hash-based router. Each route maps to a page module exposing
// render(container, params). Protected routes redirect to #/login when signed out.
import { api, ensureCsrf } from './api.js';
import { h, mount, errorState, toast } from './dom.js';
import { session } from './state.js';
import * as landing from './pages/landing.js';
import * as auth from './pages/auth.js';
import * as dashboard from './pages/dashboard.js';
import * as scanner from './pages/scanner.js';
import * as result from './pages/result.js';
import * as history from './pages/history.js';
import * as settings from './pages/settings.js';
import * as about from './pages/about.js';

const routes = [
  { pattern: /^\/?$/, page: landing, title: 'Home' },
  { pattern: /^\/login$/, page: auth.login, title: 'Sign in', guestOnly: true },
  { pattern: /^\/register$/, page: auth.register, title: 'Create account', guestOnly: true },
  { pattern: /^\/dashboard$/, page: dashboard, title: 'Dashboard', protected: true },
  { pattern: /^\/scan(?:\/(MESSAGE|URL|TRANSACTION|CALL|APP_PERMISSION))?$/, page: scanner, title: 'Scanner', protected: true },
  { pattern: /^\/result\/([0-9a-f-]{36})$/, page: result, title: 'Scan result', protected: true },
  { pattern: /^\/history$/, page: history, title: 'History', protected: true },
  { pattern: /^\/settings$/, page: settings, title: 'Settings & privacy', protected: true },
  { pattern: /^\/about$/, page: about, title: 'About & methodology' },
];

const app = document.getElementById('app');

export function navigate(path) {
  if (location.hash === '#' + path) {
    route();
  } else {
    location.hash = path;
  }
}

function renderNav(currentPath) {
  const nav = document.getElementById('nav');
  const link = (href, label) => h('a', { href: '#' + href, 'aria-current': currentPath === href ? 'page' : null }, label);
  const items = session.user
    ? [link('/dashboard', 'Dashboard'), link('/scan', 'Scan'), link('/history', 'History'), link('/settings', 'Settings'),
      link('/about', 'About'),
      h('button', { class: 'linklike', type: 'button', onclick: logout }, `Sign out (${session.user.displayName})`)]
    : [link('/', 'Home'), link('/about', 'About'), link('/login', 'Sign in'),
      h('a', { href: '#/register', class: 'btn btn-primary btn-sm' }, 'Get started')];
  mount(nav, items);
}

async function logout() {
  try {
    await api.logout();
  } catch {
    // session may already be gone; continue
  }
  session.user = null;
  await ensureCsrf(true);
  toast('You have been signed out.', 'success');
  navigate('/');
}

async function route() {
  const path = decodeURIComponent(location.hash.replace(/^#/, '')) || '/';
  const match = routes.map((r) => ({ r, m: path.match(r.pattern) })).find((x) => x.m);
  document.getElementById('nav').classList.remove('open');
  document.getElementById('nav-toggle').setAttribute('aria-expanded', 'false');
  if (!match) {
    renderNav(path);
    mount(app, h('div', { class: 'state' }, h('h1', {}, 'Page not found'), h('p', {}, h('a', { href: '#/' }, 'Go to the home page'))));
    return;
  }
  const { r, m } = match;
  if (r.protected && !session.user) {
    sessionStorage.setItem('us-after-login', path);
    navigate('/login');
    return;
  }
  if (r.guestOnly && session.user) {
    navigate('/dashboard');
    return;
  }
  renderNav(path);
  document.title = `${r.title} · UniversalShield`;
  try {
    await r.page.render(app, m.slice(1), { navigate });
  } catch (e) {
    if (e.status === 401) {
      session.user = null;
      navigate('/login');
      return;
    }
    mount(app, errorState(e, route));
  }
  app.focus({ preventScroll: true });
  window.scrollTo(0, 0);
}

async function boot() {
  document.getElementById('nav-toggle').addEventListener('click', (e) => {
    const nav = document.getElementById('nav');
    const open = nav.classList.toggle('open');
    e.currentTarget.setAttribute('aria-expanded', String(open));
  });
  await ensureCsrf();
  try {
    session.user = await api.me();
  } catch {
    session.user = null;
  }
  window.addEventListener('hashchange', route);
  route();
}

boot();
