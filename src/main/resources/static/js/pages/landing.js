// Landing page: product introduction, features, how it works, privacy principles.
import { h, mount } from '../dom.js';
import { session } from '../state.js';

const FEATURES = [
  ['✉', 'Message Fraud Detector', 'Paste an SMS, email, chat or social message. Detects phishing language, impersonation, OTP requests, urgency, fake prizes and payment demands, and shows exactly which signs matched.'],
  ['🔗', 'URL & Website Risk', 'Checks link structure: HTTPS, look-alike domains, brand misuse, suspicious subdomains, shorteners and obfuscation. The link is never opened by our server.'],
  ['₹', 'Transaction Screening', 'Describe a payment before you send it. Flags new recipients, unusual amounts compared with your own history, repeated attempts and "enter PIN to receive money" tricks.'],
  ['☎', 'Call Fraud Screening', 'Describe a call you received. Detects fake police or bank callers, threats, remote-access requests and urgent payment demands. Calls are never recorded.'],
  ['⚙', 'App Permission Analyzer', 'Enter or upload an app\'s permissions. Flags SMS, accessibility, overlay, notification and device-admin access, and whether it fits what the app claims to do.'],
  ['Σ', 'One explainable Risk Engine', 'All detectors feed a single engine that groups correlated signs, scores 0-100, reports evidence strength separately and lists concrete next steps.'],
];

const PRIVACY = [
  ['No silent collection', 'UniversalShield only sees what you type or upload. It never reads your messages, contacts, calls or device activity.'],
  ['Sensitive data masked', 'Card numbers, OTPs and codes are removed before anything is stored. You can turn off storing excerpts entirely.'],
  ['You control retention', 'Choose how long history is kept, delete single scans, all history or your whole account at any time.'],
  ['Honest results', 'Every result says whether it came from local heuristics, a trained model or verified external data. No fake accuracy numbers.'],
];

export function render(container) {
  const start = session.user ? '#/scan' : '#/register';
  mount(container,
    h('section', { class: 'hero' },
      h('div', {},
        h('div', { class: 'tagline' }, 'One shield for your digital life'),
        h('h1', {}, 'Check it before you click, share, install or pay.'),
        h('p', { class: 'lead' }, 'UniversalShield combines five fraud detectors into one explainable risk engine. Paste a suspicious message, link, payment request, call description or app permission list and see why it looks risky and what to do next.'),
        h('div', { class: 'row' },
          h('a', { class: 'btn btn-primary', href: start }, 'Start scanning'),
          session.user ? h('a', { class: 'btn btn-secondary', href: '#/dashboard' }, 'Open dashboard')
            : h('a', { class: 'btn btn-secondary', href: '#/login' }, 'Sign in'))),
      h('div', { class: 'hero-visual', 'aria-hidden': 'true' }, h('img', { src: '/assets/logo.svg', alt: '' }))),
    h('section', { class: 'block', 'aria-labelledby': 'features-h' },
      h('h2', { id: 'features-h' }, 'Main features'),
      h('div', { class: 'grid grid-3' }, FEATURES.map(([icon, title, text]) =>
        h('article', { class: 'card feature' }, h('div', { class: 'icon', 'aria-hidden': 'true' }, icon), h('h3', {}, title), h('p', { class: 'muted' }, text))))),
    h('section', { class: 'block grid grid-2', 'aria-labelledby': 'how-h' },
      h('div', {},
        h('h2', { id: 'how-h' }, 'How it works'),
        h('ol', { class: 'steps' },
          h('li', {}, h('strong', {}, 'Choose a category'), h('br'), 'Message, URL, transaction, call or app permissions.'),
          h('li', {}, h('strong', {}, 'Enter what you received'), h('br'), 'Only what you choose to share. Never enter passwords, PINs, CVVs or OTPs.'),
          h('li', {}, h('strong', {}, 'Detectors analyse it'), h('br'), 'Each detector matches documented warning signs; links inside text are checked too.'),
          h('li', {}, h('strong', {}, 'The Risk Engine explains'), h('br'), 'A 0-100 score, a level, evidence strength, matched indicators and recommended actions.'))),
      h('div', { class: 'card accent' },
        h('h3', {}, 'What the score means'),
        h('p', { class: 'muted' }, 'A HIGH or CRITICAL score means strong warning signs, not proof of fraud. A LOW score means few known signs were found, never a guarantee of safety.'),
        h('a', { href: '#/about' }, 'Read the scoring methodology'))),
    h('section', { class: 'block', 'aria-labelledby': 'privacy-h' },
      h('h2', { id: 'privacy-h' }, 'Privacy principles'),
      h('div', { class: 'grid grid-4' }, PRIVACY.map(([title, text]) =>
        h('div', { class: 'card' }, h('h3', {}, title), h('p', { class: 'muted small' }, text))))));
}
