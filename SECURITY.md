# Security policy

UniversalShield is an academic project. It handles user-supplied text that may itself be
malicious, so it is built defensively. This document describes the controls, their limits and how
to test responsibly.

## Controls implemented

| Area | Control | Where |
|---|---|---|
| Passwords | BCrypt via Spring's `DelegatingPasswordEncoder`; never returned or logged (`toString()` masks request DTOs) | `SecurityConfig`, `AuthService`, `dto/*Request` |
| Authentication | Server-side session (cookie `JSESSIONID`, HttpOnly, SameSite=Lax, Secure in prod); session id rotated on login; 30 min timeout | `application*.yml`, `AuthService` |
| CSRF | Double-submit cookie: `XSRF-TOKEN` cookie + `X-XSRF-TOKEN` header required on every POST/PUT/DELETE, including login and register | `SecurityConfig`, `static/js/api.js` |
| Authorization | All `/api/**` except health, detectors, methodology, CSRF, login and register require a session; unknown non-API paths are denied | `SecurityConfig` |
| Ownership | Every scan query includes the user id; another user's scan returns 404 (not 403) so ids cannot be probed | `ScanRepository`, `ScanService` |
| Input validation | Bean Validation on every DTO (length limits, patterns, ranges); category/payload shape check; request-param validation | `engine/input/*`, `dto/*`, `ScanService` |
| Error handling | Uniform JSON errors; rejected values are never echoed; no stack traces in responses | `GlobalExceptionHandler`, `server.error.*` |
| SQL injection | Spring Data JPA / JPQL / Criteria with bound parameters only; LIKE wildcards escaped | `repository/*`, `ScanService.filter` |
| XSS | Frontend builds DOM with `textContent` only (no `innerHTML` with data); HTML reports escape every value; strict Content-Security-Policy (`script-src 'self'`) | `static/js/dom.js`, `ReportService`, `SecurityConfig` |
| Security headers | CSP, `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: no-referrer`, `Permissions-Policy` | `SecurityConfig` |
| Rate limiting | 10 login/register attempts per IP per minute, 30 scans per user per minute (configurable); 429 + `Retry-After` | `RateLimiter`, `RateLimitFilter` |
| Sensitive data | Card numbers (Luhn), codes after OTP/PIN/CVV/password keywords and long digit runs are masked before storage; excerpt storage can be turned off; recipients/domains/numbers stored only as keyed HMAC hashes | `InputSanitizer`, `SubjectHasher` |
| Logging | No request bodies, passwords, OTPs or scan content are logged | whole codebase |
| Uploads | Permission reports only: `.txt/.json/.csv`, max 64 KB, strict UTF-8, no NUL bytes, parsed in memory and never stored | `ScanController.parseReport`, `PermissionReportParser` |
| SSRF | The URL detector never fetches URLs, resolves DNS or follows redirects. Reputation interfaces look up domains by name only | `UrlRiskDetector`, `DomainReputationProvider` |
| CORS | Off by default (same origin). Explicit allow-list via `CORS_ALLOWED_ORIGINS` | `SecurityConfig` |
| Secrets | All secrets come from environment variables; prod refuses to start with a missing/weak `APP_HASH_SECRET` | `.env.example`, `ProductionSafetyCheck` |
| API docs | Swagger UI/OpenAPI public in dev, login-protected in prod by default | `universalshield.api-docs.public` |
| Data retention | Per-user retention (1-365 days) enforced nightly; delete one scan, all history, or the whole account (password re-confirmation) | `RetentionService`, `UserService` |
| Model loading | Only JSON word counts are loaded (no serialized Java objects, no executable code), size-limited and validated | `MlModelService` |
| Container | Non-root user, JRE-only runtime image, health check | `Dockerfile` |

## Known limitations

- The rate limiter is in memory and per instance. With several instances use a shared store
  (Redis + Bucket4j) behind the same `RateLimiter` contract.
- There is no account lockout, email verification, password reset or MFA. These would be needed
  before real-world use.
- Sessions are in memory; a restart signs everyone out. Use Spring Session (JDBC/Redis) for
  multiple instances.
- `X-Forwarded-*` headers are trusted in the prod profile (`server.forward-headers-strategy=framework`)
  because hosting platforms terminate TLS at a proxy. Do not expose the app directly to the internet
  with that setting unless the proxy strips client-supplied forwarding headers.
- Audit events store a hashed IP. With a weak `APP_HASH_SECRET` IPv4 hashes could be brute-forced;
  use a long random secret.
- Detection quality is limited (see README and docs/SCORING.md). UniversalShield gives advice; it
  must never be used to make automated decisions about people.

## Future live URL inspection (not implemented)

If live fetching is ever added it must: resolve the host once and reject private, loopback,
link-local, multicast and metadata ranges (including IPv6 and DNS-rebinding); allow only
http/https on ports 80/443; disable redirects or re-validate every hop; set short timeouts and size
limits; never execute scripts; and run in an isolated egress-restricted worker.

## Responsible testing

- Test only your own local or deployed instance. Do not scan, attack or load-test systems you do not own.
- Use synthetic data. Never paste real OTPs, passwords, card numbers or other people's messages.
- Do not use real phishing kits or live malicious URLs in demonstrations; the samples in
  `data/samples` use invented domains (do not visit them).
- Report suspected vulnerabilities privately to the project maintainer rather than opening a public issue.
