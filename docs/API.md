# REST API

Base URL in development: `http://localhost:8080`. All endpoints are under `/api`. The live,
machine-readable contract is the OpenAPI document at **`/api/openapi`**, with Swagger UI at
**`/swagger-ui.html`**. Both are public in the dev profile. In the prod profile they need a login
unless `API_DOCS_PUBLIC=true`.

## 1. Conventions

**Envelope.** Every JSON response has the same shape:

```json
{ "success": true, "data": { ... }, "error": null, "timestamp": "2026-10-09T17:18:41.529Z" }
{ "success": false, "data": null, "error": { "code": "VALIDATION_FAILED", "message": "Some fields are invalid.",
  "fields": [ { "field": "message.text", "message": "Message text is required" } ] }, "timestamp": "..." }
```

Rejected values are never echoed in errors, because they may be sensitive.

**Authentication.** Session cookie (`JSESSIONID`, `HttpOnly`, `SameSite=Lax`, `Secure` in
production). Log in once and the browser sends it automatically. Unauthenticated calls to
protected endpoints get `401` JSON, never a redirect to a login page.

**CSRF.** Every `POST`, `PUT` and `DELETE` (including login and register) must send header
`X-XSRF-TOKEN` equal to the value of the `XSRF-TOKEN` cookie. Get the cookie with
`GET /api/auth/csrf`. The token changes after login, so read the cookie again before the next
request. A missing or wrong token gives `403 CSRF_INVALID`.

**IDs.** Scans are identified by a random UUID. Asking for another user's scan returns `404`,
exactly like a scan that does not exist.

**Rate limits** (per client IP, per minute, configurable): 10 auth requests and 30 scan
creations. Over the limit returns `429 RATE_LIMITED`.

## 2. Endpoints

| Method | Path | Auth | Purpose | Success |
|---|---|---|---|---|
| GET | `/api/health` | public | Application and database status | 200 (503 if DB is down) |
| GET | `/api/auth/csrf` | public | Issue the `XSRF-TOKEN` cookie | 200 |
| POST | `/api/auth/register` | public | Create an account and sign in | 201 |
| POST | `/api/auth/login` | public | Sign in | 200 |
| POST | `/api/auth/logout` | session | Sign out, invalidate session | 200 |
| GET | `/api/auth/me` | session | Current user | 200 |
| POST | `/api/scans` | session | Run a scan and store it | 201 |
| GET | `/api/scans` | session | My scans: filters and pagination | 200 |
| GET | `/api/scans/{id}` | session | One scan, full result | 200 |
| DELETE | `/api/scans/{id}` | session | Delete one scan | 200 |
| DELETE | `/api/scans` | session | Delete all my scans | 200 |
| POST | `/api/scans/app-report` | session | Parse an uploaded permission report (does not score it) | 200 |
| GET | `/api/reports/{id}?format=html\|json` | session | Download a report | 200 (file) |
| GET | `/api/dashboard/summary` | session | Counts and recent scans for my dashboard | 200 |
| GET | `/api/detectors` | public | Detector list with versions and availability | 200 |
| GET | `/api/methodology` | public | Ruleset version, thresholds, formula, ML status | 200 |
| GET | `/api/users/me` | session | Profile and privacy preferences | 200 |
| PUT | `/api/users/me/preferences` | session | Update retention, excerpt storage, default category | 200 |
| DELETE | `/api/users/me/history` | session | Delete all my scans (privacy page) | 200 |
| POST | `/api/users/me/delete-account` | session | Delete account and all data (needs password) | 200 |
| GET | `/api/openapi` | public in dev | OpenAPI 3 JSON | 200 |

## 3. Auth

`POST /api/auth/register`

```json
{ "email": "student@example.com", "displayName": "Asha", "password": "CorrectHorse42" }
```

Rules: valid e-mail (max 254), name 2-80 characters, password 10-128 characters with at least
one letter and one digit. Errors: `400 VALIDATION_FAILED`, `409 EMAIL_TAKEN`.

`POST /api/auth/login` with `{ "email": "...", "password": "..." }` returns
`{ "email": "...", "displayName": "..." }`. A wrong e-mail or password both return
`401 UNAUTHORIZED` with the same message, so the API does not reveal which e-mails exist.

## 4. Scans

### Request body of `POST /api/scans`

Set `category` and the **one** matching object:

| category | object | fields (required in bold) |
|---|---|---|
| `MESSAGE` | `message` | **text** (≤ 5000), channel (`SMS`, `EMAIL`, `CHAT`, `SOCIAL`, `OTHER`), sender (≤ 120) |
| `URL` | `url` | **url** (≤ 2048). The URL is analysed as text and never opened |
| `TRANSACTION` | `transaction` | **amount** (0.01 to 1e9), currency (3 letters), recipient, recipientIsNew, paymentMethod (`BANK_TRANSFER`, `UPI`, `CARD`, `WALLET`, `GIFT_CARD`, `CRYPTO`, `CASH`, `OTHER`), attemptsLastHour (0-100), requestedByOtherParty, description (≤ 2000) |
| `CALL` | `call` | **description** (≤ 4000), callerClaimedIdentity, callerNumber (digits, spaces, `+ - ( )`), durationMinutes (0-600) |
| `APP_PERMISSION` | `app` | **appName**, **appCategory** (`MESSAGING`, `SOCIAL`, `BANKING_FINANCE`, `LOAN`, `GAME`, `UTILITY`, `FLASHLIGHT`, `NAVIGATION`, `CAMERA_PHOTO`, `SHOPPING`, `HEALTH`, `ACCESSIBILITY`, `SECURITY`, `OTHER`), **permissions** (1-200 names such as `android.permission.READ_SMS` or `READ_SMS`), installSource (`PLAY_STORE`, `APP_STORE`, `OFFICIAL_WEBSITE`, `UNKNOWN_SOURCE`) |

Missing the matching object gives `400 MISSING_INPUT`.

### Example (real request and response, abridged)

Request:

```http
POST /api/scans
Content-Type: application/json
X-XSRF-TOKEN: <value of the XSRF-TOKEN cookie>

{ "category": "URL", "url": { "url": "http://paypa1-secure-login.top/verify" } }
```

Response `201 Created` (lists shortened with "…"; the full response has 5 indicators):

```json
{
  "success": true,
  "data": {
    "id": "a00ccc63-c24c-4417-98e9-28a31be48809",
    "category": "URL",
    "riskScore": 87,
    "riskLevel": "CRITICAL",
    "evidenceStrength": "STRONG",
    "reasons": [
      "5 warning sign(s) matched across 5 independent group(s). Signs in the same group are counted once.",
      "Look-alike of a well-known domain: 'paypa1-secure-login.top' closely resembles 'paypal'. Typosquatted domains imitate trusted sites.",
      "…"
    ],
    "indicators": [
      {
        "code": "URL_TYPOSQUAT",
        "group": "DECEPTIVE_DOMAIN",
        "title": "Look-alike of a well-known domain",
        "explanation": "'paypa1-secure-login.top' closely resembles 'paypal'. Typosquatted domains imitate trusted sites.",
        "weight": 0.6,
        "detectorId": "url-heuristic",
        "source": "LOCAL_HEURISTIC",
        "evidence": "paypa1-secure-login.top"
      }
    ],
    "recommendations": [
      {
        "action": "Stop. Do not click, pay, install or share anything related to this.",
        "reason": "Several strong warning signs were found together.",
        "priority": 0
      },
      {
        "action": "Do not open this link. Type the official website address yourself.",
        "reason": "The domain appears designed to look like a different, trusted site.",
        "priority": 1
      }
    ],
    "createdAt": "2026-10-09T17:32:20.039307481Z",
    "rulesetVersion": "2026.10-1",
    "detectorVersions": {
      "url-heuristic": "1.0.0"
    },
    "sources": [
      "LOCAL_HEURISTIC"
    ],
    "notes": [
      "HTTPS only means the connection is encrypted. It does not show who runs a site or that it is safe.",
      "Verified domain reputation: not used. These are heuristic findings only."
    ],
    "disclaimers": [
      "This is an automated risk assessment of the information you provided. It is not proof of fraud or of safety.",
      "…"
    ],
    "inputExcerpt": "http://paypa1-secure-login.top/verify",
    "demo": false,
    "scoringMethod": "Indicators are grouped by correlation; only the strongest weight per group counts. Groups are combined with a noisy-OR: score = 100 x (1 - product(1 - weight)). See About > Methodology."
  },
  "error": null,
  "timestamp": "2026-10-09T17:32:20.050334225Z"
}
```

Field meanings:

| Field | Meaning |
|---|---|
| `riskScore` | 0-100, see [SCORING.md](SCORING.md) |
| `riskLevel` | LOW, MEDIUM, HIGH or CRITICAL from the configured thresholds |
| `evidenceStrength` | LIMITED, MODERATE or STRONG. This is separate from the score and says how much independent support the score has |
| `reasons` | human-readable summary, one line per indicator group |
| `indicators` | every matched sign with code, group, weight, detector, source and masked evidence |
| `recommendations` | up to 6 `{action, reason, priority}` items |
| `rulesetVersion`, `detectorVersions` | exact versions that produced the result |
| `sources` | `LOCAL_HEURISTIC`, `TRAINED_MODEL`, `VERIFIED_EXTERNAL` (the last never appears in this version) |
| `notes` | what was and was not checked (e.g. "Verified domain reputation: not used") |
| `disclaimers` | always present, adapted to level and evidence |
| `inputExcerpt` | masked excerpt, or `null` if the user turned excerpt storage off |
| `demo` | `true` only for seeded demo data |

### History: `GET /api/scans`

Query parameters, all optional: `category`, `riskLevel`, `from` and `to` (ISO dates,
`2026-10-01`), `q` (searches the excerpt and indicator titles, ≤ 100 characters), `page` (from 0)
and `size` (1-50, default 10). Results are newest first.

```json
{ "items": [ { "id": "…", "category": "URL", "riskScore": 66, "riskLevel": "HIGH", "evidenceStrength": "STRONG",
    "createdAt": "…", "excerpt": "…", "indicatorCount": 4, "topIndicator": "…", "demo": false } ],
  "page": 0, "size": 10, "totalItems": 1, "totalPages": 1 }
```

Errors: `INVALID_PAGE`, `INVALID_PAGE_SIZE`, `INVALID_DATE_RANGE`, `SEARCH_TOO_LONG` (all 400).

### Permission report upload: `POST /api/scans/app-report`

`multipart/form-data` with a `file` part: `.txt` (one permission per line), `.csv` or `.json`
(`{"appName": "...", "permissions": [...]}`), UTF-8, at most 64 KB. It returns
`{ "appName": "...", "permissions": [...] }` for the user to review in the form. Errors:
`EMPTY_FILE`, `FILE_TOO_LARGE` (413 or 400), `UNSUPPORTED_FILE_TYPE`, `NOT_TEXT`,
`UNPARSEABLE_REPORT`, `NO_PERMISSIONS_FOUND`. Samples are in `data/samples/`.

## 5. Users and privacy

- `PUT /api/users/me/preferences` with
  `{ "retentionDays": 90, "storeInputExcerpts": true, "defaultCategory": "MESSAGE" }`.
  Retention must be 1-365 days.
- `POST /api/users/me/delete-account` with `{ "password": "..." }` deletes the user, their
  preferences and all scans (database cascade), then ends the session.

## 6. Error codes

| HTTP | code | When |
|---|---|---|
| 400 | `VALIDATION_FAILED` | Bean validation failed; see `fields` |
| 400 | `MALFORMED_REQUEST` | Body missing or not valid JSON of the expected shape |
| 400 | `INVALID_PARAMETER` | Query parameter missing or of the wrong type |
| 400 | `MISSING_INPUT`, `INVALID_PAGE`, `INVALID_PAGE_SIZE`, `INVALID_DATE_RANGE`, `SEARCH_TOO_LONG`, `RETENTION_TOO_LONG`, `UNSUPPORTED_FORMAT`, upload codes | See sections above |
| 401 | `UNAUTHORIZED` | Not signed in, or wrong credentials |
| 403 | `CSRF_INVALID` | Missing or stale CSRF token |
| 403 | `FORBIDDEN` | Not allowed |
| 404 | `NOT_FOUND` | No such resource, or it belongs to someone else |
| 405 | `METHOD_NOT_ALLOWED` | Wrong HTTP method |
| 409 | `EMAIL_TAKEN` | Registration with an existing e-mail |
| 413 | `FILE_TOO_LARGE` | Upload over the multipart limit |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | Wrong `Content-Type` |
| 429 | `RATE_LIMITED` | Too many requests |
| 500 | `INTERNAL_ERROR` | Unexpected error; details are logged server-side only |

## 7. Trying it with curl (Git Bash or WSL)

```bash
B=http://localhost:8080; J=cookies.txt
curl -s -c $J -b $J $B/api/auth/csrf > /dev/null
T=$(awk '/XSRF-TOKEN/{print $7}' $J)
curl -s -c $J -b $J -H "X-XSRF-TOKEN: $T" -H "Content-Type: application/json" \
     -d '{"email":"demo@universalshield.local","password":"ShieldDemo2026"}' $B/api/auth/login
T=$(awk '/XSRF-TOKEN/{print $7}' $J)   # token rotates after login
curl -s -c $J -b $J -H "X-XSRF-TOKEN: $T" -H "Content-Type: application/json" \
     -d '{"category":"MESSAGE","message":{"text":"Share the OTP sent to your phone to stop the blocked account.","channel":"SMS"}}' \
     $B/api/scans
```

Windows PowerShell users can do the same with `Invoke-WebRequest -WebSession`, or simply use
Swagger UI: open `/swagger-ui.html`, sign in through the web app in the same browser first, and
Swagger requests will carry the session cookie (Swagger UI also needs the `X-XSRF-TOKEN` header
for POST/PUT/DELETE; copy the cookie value from the browser's developer tools).
