// HTTP client for the UniversalShield REST API.
// Sends the session cookie automatically (same origin) and echoes the XSRF-TOKEN cookie
// in the X-XSRF-TOKEN header on state-changing requests (Spring Security CSRF protection).

export class ApiError extends Error {
  constructor(status, code, message, fields = []) {
    super(message);
    this.status = status;
    this.code = code;
    this.fields = fields;
  }
}

function readCookie(name) {
  const match = document.cookie.split('; ').find((c) => c.startsWith(name + '='));
  return match ? decodeURIComponent(match.split('=')[1]) : null;
}

let csrfReady = null;

/** Makes sure the CSRF cookie exists before the first POST/PUT/DELETE. */
export function ensureCsrf(force = false) {
  if (force || !csrfReady || !readCookie('XSRF-TOKEN')) {
    csrfReady = fetch('/api/auth/csrf', { credentials: 'same-origin' }).catch(() => null);
  }
  return csrfReady;
}

export async function request(method, path, body, { isForm = false } = {}) {
  const headers = { Accept: 'application/json' };
  if (method !== 'GET') {
    await ensureCsrf();
    const token = readCookie('XSRF-TOKEN');
    if (token) headers['X-XSRF-TOKEN'] = token;
  }
  let payload;
  if (body !== undefined && body !== null) {
    if (isForm) {
      payload = body;
    } else {
      headers['Content-Type'] = 'application/json';
      payload = JSON.stringify(body);
    }
  }
  let res;
  try {
    res = await fetch(path, { method, headers, body: payload, credentials: 'same-origin' });
  } catch {
    throw new ApiError(0, 'NETWORK', 'Cannot reach the server. Check that UniversalShield is running.');
  }
  let json = null;
  try {
    json = await res.json();
  } catch {
    // non-JSON response
  }
  if (!res.ok || !json || json.success === false) {
    const err = json && json.error ? json.error : {};
    if (err.code === 'CSRF_INVALID') ensureCsrf(true);
    throw new ApiError(res.status, err.code || 'HTTP_' + res.status,
      err.message || `Request failed (${res.status}).`, err.fields || []);
  }
  return json.data;
}

export const api = {
  csrf: () => ensureCsrf(true),
  me: () => request('GET', '/api/auth/me'),
  login: (email, password) => request('POST', '/api/auth/login', { email, password }),
  register: (displayName, email, password) => request('POST', '/api/auth/register', { displayName, email, password }),
  logout: () => request('POST', '/api/auth/logout'),
  createScan: (payload) => request('POST', '/api/scans', payload),
  listScans: (params) => request('GET', '/api/scans?' + new URLSearchParams(params).toString()),
  getScan: (id) => request('GET', '/api/scans/' + encodeURIComponent(id)),
  deleteScan: (id) => request('DELETE', '/api/scans/' + encodeURIComponent(id)),
  deleteAllScans: () => request('DELETE', '/api/scans'),
  parseAppReport: (file) => {
    const fd = new FormData();
    fd.append('file', file);
    return request('POST', '/api/scans/app-report', fd, { isForm: true });
  },
  dashboard: () => request('GET', '/api/dashboard/summary'),
  detectors: () => request('GET', '/api/detectors'),
  methodology: () => request('GET', '/api/methodology'),
  health: () => request('GET', '/api/health'),
  profile: () => request('GET', '/api/users/me'),
  savePreferences: (prefs) => request('PUT', '/api/users/me/preferences', prefs),
  deleteHistory: () => request('DELETE', '/api/users/me/history'),
  deleteAccount: (password) => request('POST', '/api/users/me/delete-account', { password }),
};
