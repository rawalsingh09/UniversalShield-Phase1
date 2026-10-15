// Pure client-side validation. Mirrors the server rules for fast feedback; the server
// re-validates everything, so these checks are a convenience, not a security boundary.
// No DOM access, so it is unit-tested with Node (src/test/frontend).

export const LIMITS = Object.freeze({
  messageText: 5000,
  url: 2048,
  callDescription: 4000,
  transactionDescription: 2000,
  shortText: 120,
  passwordMin: 10,
  passwordMax: 128,
  permissions: 200,
});

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PERMISSION = /^[A-Za-z0-9_.]+$/;
const PHONE = /^[+0-9 ()-]{3,25}$/;

export function validateEmail(email) {
  if (!email || !email.trim()) return 'Email is required';
  if (email.length > 254 || !EMAIL.test(email.trim())) return 'Enter a valid email address';
  return null;
}

export function validatePassword(pw) {
  if (!pw) return 'Password is required';
  if (pw.length < LIMITS.passwordMin || pw.length > LIMITS.passwordMax) return 'Password must be 10-128 characters';
  if (!/[A-Za-z]/.test(pw) || !/\d/.test(pw)) return 'Password must contain at least one letter and one digit';
  return null;
}

export function validateRegistration({ displayName, email, password, confirm }) {
  const errors = {};
  if (!displayName || displayName.trim().length < 2 || displayName.trim().length > 80) errors.displayName = 'Name must be 2-80 characters';
  const e = validateEmail(email); if (e) errors.email = e;
  const p = validatePassword(password); if (p) errors.password = p;
  if (password !== confirm) errors.confirm = 'Passwords do not match';
  return errors;
}

/** Splits a free-form permission list (newlines, commas, spaces) into unique names. */
export function parsePermissionList(text) {
  if (!text) return [];
  const seen = new Set();
  for (const raw of text.split(/[\s,;]+/)) {
    const t = raw.trim();
    if (t) seen.add(t);
  }
  return [...seen];
}

/** Returns {field: message} for the scan form of the given category; empty object = valid. */
export function validateScan(category, v) {
  const errors = {};
  const tooLong = (s, max) => s && s.length > max;
  switch (category) {
    case 'MESSAGE':
      if (!v.text || !v.text.trim()) errors.text = 'Paste the message you want to check';
      else if (tooLong(v.text, LIMITS.messageText)) errors.text = `Message must be at most ${LIMITS.messageText} characters`;
      if (tooLong(v.sender, LIMITS.shortText)) errors.sender = 'Sender must be at most 120 characters';
      break;
    case 'URL':
      if (!v.url || !v.url.trim()) errors.url = 'Enter the link you want to check';
      else if (tooLong(v.url, LIMITS.url)) errors.url = 'URL must be at most 2048 characters';
      else if (/\s/.test(v.url.trim())) errors.url = 'A URL cannot contain spaces';
      break;
    case 'TRANSACTION': {
      const amount = Number(v.amount);
      if (v.amount === '' || v.amount === undefined || Number.isNaN(amount)) errors.amount = 'Enter the amount';
      else if (amount <= 0) errors.amount = 'Amount must be greater than zero';
      else if (amount > 1e9) errors.amount = 'Amount is too large';
      if (v.currency && !/^[A-Z]{3}$/.test(v.currency)) errors.currency = 'Use a 3-letter code such as INR or USD';
      if (v.attemptsLastHour !== '' && v.attemptsLastHour !== undefined) {
        const a = Number(v.attemptsLastHour);
        if (!Number.isInteger(a) || a < 0 || a > 100) errors.attemptsLastHour = 'Attempts must be a whole number from 0 to 100';
      }
      if (tooLong(v.recipient, LIMITS.shortText)) errors.recipient = 'Recipient must be at most 120 characters';
      if (tooLong(v.description, LIMITS.transactionDescription)) errors.description = 'Description must be at most 2000 characters';
      break;
    }
    case 'CALL':
      if (!v.description || !v.description.trim()) errors.description = 'Describe what the caller said or asked';
      else if (tooLong(v.description, LIMITS.callDescription)) errors.description = 'Description must be at most 4000 characters';
      if (v.callerNumber && !PHONE.test(v.callerNumber)) errors.callerNumber = 'Use only digits, spaces, +, - and brackets';
      if (v.durationMinutes !== '' && v.durationMinutes !== undefined) {
        const d = Number(v.durationMinutes);
        if (!Number.isInteger(d) || d < 0 || d > 600) errors.durationMinutes = 'Duration must be 0-600 minutes';
      }
      if (tooLong(v.callerClaimedIdentity, LIMITS.shortText)) errors.callerClaimedIdentity = 'Must be at most 120 characters';
      break;
    case 'APP_PERMISSION':
      if (!v.appName || !v.appName.trim()) errors.appName = 'App name is required';
      if (!v.permissions || v.permissions.length === 0) errors.permissions = 'Select or enter at least one permission';
      else if (v.permissions.length > LIMITS.permissions) errors.permissions = 'At most 200 permissions';
      else if (v.permissions.some((p) => !PERMISSION.test(p) || p.length > 120)) errors.permissions = 'Permission names may contain only letters, digits, dots and underscores';
      break;
    default:
      errors.category = 'Choose a category';
  }
  return errors;
}

/** Warns (client-side only) if a transaction description seems to contain a card number. */
export function looksLikeCardNumber(text) {
  if (!text) return false;
  const matches = text.match(/\b(?:\d[ -]?){12,18}\d\b/g) || [];
  return matches.some((m) => luhn(m.replace(/[ -]/g, '')));
}

export function luhn(digits) {
  let sum = 0;
  let dbl = false;
  for (let i = digits.length - 1; i >= 0; i--) {
    let d = digits.charCodeAt(i) - 48;
    if (d < 0 || d > 9) return false;
    if (dbl) { d *= 2; if (d > 9) d -= 9; }
    sum += d;
    dbl = !dbl;
  }
  return digits.length > 0 && sum % 10 === 0;
}
