# Testing

## 1. Latest results (observed on 2026-10-09)

Everything below is output that was actually observed on the final code. Nothing is estimated.

| Suite | Command | Result |
|---|---|---|
| Java unit + integration tests | `mvnw.cmd -B clean package` | **161 tests, 0 failures, 0 errors, 0 skipped**, `BUILD SUCCESS` |
| Frontend unit tests (Node 22) | `node --test "src/test/frontend/*.test.mjs"` | **10 tests, 10 pass, 0 fail** |
| UI smoke test (Playwright, Chromium), dev profile + H2 | `node src/test/e2e/ui-smoke.mjs http://localhost:8080` | **passed** (all steps, no browser console errors) |
| UI smoke test, **prod profile on PostgreSQL 16.15** | `node src/test/e2e/ui-smoke.mjs http://localhost:8090` | **passed**. Flyway validated V1, Hibernate validated the schema, `/api/openapi` returned 401 as intended in prod |
| Docker Compose stack (PostgreSQL 16 + app) | `docker compose up` | **healthy, UI smoke passed** on an earlier build of the same code (see caveat below) |
| ML training on synthetic data | `scripts\train-models.cmd` | ran; metrics in [ML.md](ML.md) |

Java tests per class (from the Surefire output of the final build):

| Test class | Tests | Kind |
|---|---|---|
| `engine.RiskScorerTest` | 9 | unit: formula, grouping, rounding, evidence |
| `engine.RiskLevelTest` | 12 | unit: threshold boundaries, custom thresholds, invalid configs, indicator weight range |
| `engine.RiskEngineTest` | 8 | unit: orchestration, 95 cap, disclaimers, versions, recommendations |
| `detectors.TextSignalsTest` | 8 | unit: phrase signals, negation, overrides, masking, digital-arrest phrases, bank warnings not flagged |
| `detectors.MessageFraudDetectorTest` | 10 | unit |
| `detectors.UrlRiskDetectorTest` | 25 | unit: schemes, IP hosts, punycode, brand misuse, typosquats (incl. `paypa1-secure-login`), ordinary hyphenated domains not flagged, embedded links |
| `detectors.TransactionFraudDetectorTest` | 10 | unit: median history, large amount fallback, velocity, methods |
| `detectors.CallFraudDetectorTest` | 7 | unit |
| `detectors.AppPermissionDetectorTest` | 8 | unit: justified permissions, combinations, sideloading |
| `ml.MlPipelineTest` | 8 | unit: CSV parsing, stratified seeded leak-free split, conflicting labels dropped, metrics vs. hand-computed values, CLI writes a model the service loads, corrupt model rejected |
| `service.InputSanitizerTest` | 5 | unit: OTP, card (Luhn) and digit-run masking |
| `service.ScanServiceTest` | 4 | unit with Mockito: shape validation, another user's scan is not found, paging/date/search validation, subject per category |
| `service.RetentionServiceIntegrationTest` | 1 | integration: per-user retention purge |
| `repository.PersistenceTest` | 4 | JPA + Flyway schema on H2: converters, cascades, constraints |
| `api.AuthIntegrationTest` | 8 | MockMvc: register, login, logout, duplicate e-mail, wrong password, CSRF required |
| `api.ScanApiIntegrationTest` | 14 | MockMvc: all categories, validation errors, filters, pagination, **user A cannot read or delete user B's scan** |
| `api.ReportIntegrationTest` | 4 | MockMvc: HTML report (escaped) and JSON report, unsupported format and unknown id |
| `api.PublicEndpointsIntegrationTest` | 7 | MockMvc: health, detectors, methodology, security headers, 401 JSON |
| `api.UserPrivacyIntegrationTest` | 5 | MockMvc: preferences, excerpt switch, delete history, delete account |
| `security.RateLimitTest` | 2 | limiter window logic; login attempts get 429 after the limit |
| `security.CsrfCookieHttpTest` | 1 | **real HTTP** on a random port: `XSRF-TOKEN` cookie and `X-XSRF-TOKEN` header round trip |
| `security.ProductionSafetyCheckTest` | 1 | prod start refused with default or short `APP_HASH_SECRET` |
| **Total** | **161** | |

## 2. How to run (Windows)

```bat
REM all Java tests
mvnw.cmd -B test

REM one class or one method
mvnw.cmd -B test -Dtest=UrlRiskDetectorTest
mvnw.cmd -B test -Dtest=ScanApiIntegrationTest#usersCannotSeeOrDeleteEachOthersScans

REM Java + frontend tests together
scripts\run-tests.cmd

REM frontend unit tests only (needs Node 18+)
node --test "src/test/frontend/*.test.mjs"

REM optional browser smoke test (start the app first, in another window)
npm i -D playwright
npx playwright install chromium
node src\test\e2e\ui-smoke.mjs http://localhost:8080
```

Reports: `target\surefire-reports\` (one `.txt` and `.xml` per test class).

Note for Node 22+: pass the glob in quotes as shown. `node --test src/test/frontend` (a
directory) does not work on Node 22.

## 3. Strategy

- **Unit tests** cover the parts where a mistake would silently give wrong advice: the
  scoring formula, thresholds, each detector's rules, text masking and the ML pipeline. The
  engine has no Spring dependency, so these tests are fast (milliseconds).
- **Integration tests** start the full Spring context with the `test` profile (in-memory H2,
  same Flyway migration as production) and call the real HTTP layer through MockMvc,
  including Spring Security. They verify validation errors, CSRF, sessions and above all
  **ownership**: one user can never see, export or delete another user's data.
- **One real-HTTP test** (`CsrfCookieHttpTest`) checks the CSRF cookie exactly as a browser
  sees it, because MockMvc's CSRF support replaces the token repository.
- **Frontend unit tests** cover the pure modules (`validation.js`, `format.js`).
- **UI smoke test** drives a real Chromium through registration, every scan category, the
  dashboard, history filters and deletion, preferences, the About page and logout, and fails
  on any console error.

## 4. What was verified manually

- Demo inputs in [DEMO_GUIDE.md](DEMO_GUIDE.md) were sent to the running final build. The
  expected scores written there are the observed ones.
- The worked example in [SCORING.md](SCORING.md) was produced by the running app.
- The API example in [API.md](API.md) is a real response.

## 5. Honest caveats

- **Docker image build:** in the build sandbox, the Maven stage inside `docker build` could not
  download dependencies because the sandbox's HTTPS proxy uses its own certificate
  (PKIX error). The runtime stage was verified with the locally built jar, and the Compose stack
  (PostgreSQL + app) came up healthy and passed the UI smoke test. On a normal machine with
  direct internet access the multi-stage build is expected to work, but it was **not observed**
  end to end in this environment.
- **Detection quality is not tested against real fraud.** The tests prove the rules do what
  they are designed to do. They do not prove the rules catch real scams. On the synthetic
  dataset, the message rules miss 2 of 13 fraud messages in the test split (an advance-fee
  "soldier on a UN mission" message and an Instagram copyright-appeal phish). These are left as
  known misses rather than patched, because tuning rules to the test rows would be leakage.
- No load or penetration testing was done. Rate limiting is tested functionally only.
- Browser coverage: Chromium only. Firefox and Safari were not tested.
