# File guide

Every file in the project, with seven points each:

1. **Type and location**, 2. **Purpose**, 3. **Key contents**, 4. **Depends on** (other project files it uses),
5. **Used by**, 6. **Tests**, 7. **Change notes** (what to watch when editing).

Points 3-6 for Java and JavaScript files are derived from the source code itself, so they stay accurate.
Generated files (`target/`), local databases (`data/devdb.*`), trained model files and `node_modules/` are not part of the project and are not listed.

## Contents

- [Root and build files](#root-and-build-files) (13 files)
- [Documentation](#documentation) (13 files)
- [Data and models](#data-and-models) (6 files)
- [Scripts](#scripts) (6 files)
- [Java: application, configuration and engine](#java-application-configuration-and-engine) (26 files)
- [Java: detectors and ML](#java-detectors-and-ml) (27 files)
- [Java: web, security, services and persistence](#java-web-security-services-and-persistence) (59 files)
- [Resources: configuration and database](#resources-configuration-and-database) (5 files)
- [Frontend (static)](#frontend-static) (19 files)
- [Tests](#tests) (27 files)

## Root and build files

### `.dockerignore`

1. **Type and location:** Configuration file at `.dockerignore`
2. **Purpose:** Keeps the Docker build context small and secret-free.
3. **Key contents:** Excludes `target/`, `.git`, `data/`, `.env`, `node_modules`, models.
4. **Depends on:** No other project files.
5. **Used by:** People (read or run directly).
6. **Tests:** Not code; no automated tests.
7. **Change notes:** If the image build suddenly needs a file, check it is not excluded here.

### `.env.example`

1. **Type and location:** Configuration file at `.env.example`
2. **Purpose:** Documented template of every environment variable.
3. **Key contents:** Database, security (`APP_HASH_SECRET`, `COOKIE_SECURE`, CORS, API docs), tuning (thresholds, retention, rate limits, ML), dev demo password. Values are placeholders.
4. **Depends on:** No other project files.
5. **Used by:** People (read or run directly).
6. **Tests:** Not code; no automated tests.
7. **Change notes:** Copy to `.env` and fill in; never put real secrets in this file. Keep it in sync with `application*.yml`.

### `.gitignore`

1. **Type and location:** Configuration file at `.gitignore`
2. **Purpose:** Keeps build output, local databases, secrets and trained models out of Git.
3. **Key contents:** `target/`, `data/devdb*`, `.env`, `*.log`, `models/*` except its README, `node_modules/`, `*.zip`, IDE folders.
4. **Depends on:** No other project files.
5. **Used by:** People (read or run directly).
6. **Tests:** Not code; no automated tests.
7. **Change notes:** Add patterns before committing new generated files. Never remove `.env`.

### `Dockerfile`

1. **Type and location:** Configuration file at `Dockerfile`
2. **Purpose:** Builds the production container image.
3. **Key contents:** Stage 1 `maven:3.9-eclipse-temurin-17` builds the jar; stage 2 `eclipse-temurin:17-jre` copies it, creates a non-root user, sets `SPRING_PROFILES_ACTIVE=prod`, exposes 8080, adds a `HEALTHCHECK` on `/api/health`.
4. **Depends on:** No other project files.
5. **Used by:** People (read or run directly).
6. **Tests:** Runtime stage and Compose stack verified (docs/TESTING.md section 5 explains the build-stage caveat).
7. **Change notes:** Keep the runtime stage JRE-only and non-root. Secrets are passed at runtime, never baked in.

### `LICENSE`

1. **Type and location:** Configuration file at `LICENSE`
2. **Purpose:** MIT licence for the project.
3. **Key contents:** Standard MIT text.
4. **Depends on:** No other project files.
5. **Used by:** People (read or run directly).
6. **Tests:** Not code; no automated tests.
7. **Change notes:** Replace the copyright holder line with the authors' names if required by the institution.

### `PROGRESS.md`

1. **Type and location:** Markdown document at `PROGRESS.md`
2. **Purpose:** Build log of the project stages, kept so work can be resumed after an interruption.
3. **Key contents:** Completed stages, remaining work, next exact action.
4. **Depends on:** No other project files.
5. **Used by:** People (read or run directly).
6. **Tests:** Not code; no automated tests.
7. **Change notes:** Informational only; safe to delete for submission if not wanted.

### `README.md`

1. **Type and location:** Markdown document at `README.md`
2. **Purpose:** Project front page for GitHub and the first thing a reader opens.
3. **Key contents:** What it does, features, honest limitations, quick start for Windows, commands, project layout, links to every document.
4. **Depends on:** No other project files.
5. **Used by:** People (read or run directly).
6. **Tests:** Not code; no automated tests.
7. **Change notes:** Keep claims in sync with TESTING.md (test counts) and SCORING.md (method).

### `SECURITY.md`

1. **Type and location:** Markdown document at `SECURITY.md`
2. **Purpose:** Security policy and the list of implemented controls.
3. **Key contents:** Controls table (auth, CSRF, headers/CSP, XSS, ownership, validation, rate limiting, hashing, masking, uploads, model loading, container), known limitations, rules for any future live URL fetching (SSRF), responsible testing.
4. **Depends on:** No other project files.
5. **Used by:** People (read or run directly).
6. **Tests:** Not code; no automated tests.
7. **Change notes:** Update when a control is added or removed. Do not claim protections that are not implemented.

### `docker-compose.yml`

1. **Type and location:** YAML configuration at `docker-compose.yml`
2. **Purpose:** Runs a production-like stack locally: PostgreSQL 16 plus the app.
3. **Key contents:** `db` (postgres:16-alpine, named volume, health check) and `app` (built from the Dockerfile, waits for a healthy db, prod profile). `DB_PASSWORD` and `APP_HASH_SECRET` are required via `${VAR:?}`.
4. **Depends on:** No other project files.
5. **Used by:** People (read or run directly).
6. **Tests:** Verified: stack healthy and UI smoke test passed.
7. **Change notes:** `docker compose down -v` deletes the database volume. `COOKIE_SECURE` defaults to false only because localhost has no HTTPS.

### `mvnw`

1. **Type and location:** Configuration file at `mvnw`
2. **Purpose:** Maven Wrapper launcher for macOS/Linux/Git Bash.
3. **Key contents:** Downloads the Maven version from `.mvn/wrapper/maven-wrapper.properties` into `~/.m2/wrapper` and runs it.
4. **Depends on:** No other project files.
5. **Used by:** People (read or run directly).
6. **Tests:** Verified with `mvnw -v` and the final build.
7. **Change notes:** Generated file; do not edit. Must keep LF line endings and the executable bit.

### `mvnw.cmd`

1. **Type and location:** Windows batch script at `mvnw.cmd`
2. **Purpose:** Maven Wrapper launcher for Windows.
3. **Key contents:** Same as `mvnw` but a batch/PowerShell script, so Windows users need no Maven install.
4. **Depends on:** No other project files.
5. **Used by:** People (read or run directly).
6. **Tests:** Verified with `mvnw -v` and the final build.
7. **Change notes:** Generated file; do not edit. Must keep CRLF line endings.

### `pom.xml`

1. **Type and location:** Maven POM (XML) at `pom.xml`
2. **Purpose:** Maven build definition: Java 17, Spring Boot 3.5 parent, all dependencies and plugins.
3. **Key contents:** Dependencies: web, validation, security, data-jpa, flyway (+ PostgreSQL module), postgresql and h2 drivers, springdoc-openapi, spring-boot-starter-test, spring-security-test. `finalName` universalshield so the jar is always `target/universalshield.jar`; exec-maven-plugin points at `TrainingCli`.
4. **Depends on:** No other project files.
5. **Used by:** People (read or run directly).
6. **Tests:** Every build runs it.
7. **Change notes:** Change versions one at a time and run `mvnw.cmd -B clean package`. Keep the Boot parent and springdoc versions compatible (see springdoc's compatibility table).

### `.mvn/wrapper/maven-wrapper.properties`

1. **Type and location:** Properties file at `.mvn/wrapper/maven-wrapper.properties`
2. **Purpose:** Tells the wrapper which Maven version to download.
3. **Key contents:** `distributionUrl` for Maven 3.9.11, `distributionType=only-script` (no wrapper jar in the repo).
4. **Depends on:** No other project files.
5. **Used by:** People (read or run directly).
6. **Tests:** Not code; no automated tests.
7. **Change notes:** Change the version here to upgrade Maven for everyone.

## Documentation

### `docs/API.md`

1. **Type and location:** Markdown document at `docs/API.md`
2. **Purpose:** REST API reference.
3. **Key contents:** Conventions (envelope, session auth, CSRF, ids, rate limits), endpoint table, request/response examples, error codes, curl walkthrough.
4. **Depends on:** No other project files.
5. **Used by:** People (read or run directly).
6. **Tests:** Not code; no automated tests.
7. **Change notes:** Add every new endpoint here and check `/api/openapi` matches.

### `docs/ARCHITECTURE.md`

1. **Type and location:** Markdown document at `docs/ARCHITECTURE.md`
2. **Purpose:** Explains the system's structure.
3. **Key contents:** Architecture diagram, layer table, components, profiles, extension points, design decisions with trade-offs.
4. **Depends on:** No other project files.
5. **Used by:** People (read or run directly).
6. **Tests:** Not code; no automated tests.
7. **Change notes:** Update the diagram (`docs/diagrams/architecture.mmd`) when packages or major components change.

### `docs/DATABASE.md`

1. **Type and location:** Markdown document at `docs/DATABASE.md`
2. **Purpose:** Database reference.
3. **Key contents:** ER diagram, table descriptions, constraints and indexes, what is and is not stored, retention, demo seed, migration rules.
4. **Depends on:** No other project files.
5. **Used by:** People (read or run directly).
6. **Tests:** Not code; no automated tests.
7. **Change notes:** Update together with each new Flyway migration.

### `docs/DEMO_GUIDE.md`

1. **Type and location:** Markdown document at `docs/DEMO_GUIDE.md`
2. **Purpose:** Script for presenting the project.
3. **Key contents:** Preparation, an 11-step demo with ready-to-paste synthetic inputs, likely questions with honest answers, recovery tips.
4. **Depends on:** No other project files.
5. **Used by:** People (read or run directly).
6. **Tests:** Not code; no automated tests.
7. **Change notes:** Re-run the demo inputs after changing detector rules; their scores may change.

### `docs/DEPLOYMENT.md`

1. **Type and location:** Markdown document at `docs/DEPLOYMENT.md`
2. **Purpose:** Setup and deployment guide.
3. **Key contents:** Windows Java install, local run, configuration table, local PostgreSQL, Docker, migrations, GitHub upload, cloud deployment, backups, Windows troubleshooting.
4. **Depends on:** No other project files.
5. **Used by:** People (read or run directly).
6. **Tests:** Not code; no automated tests.
7. **Change notes:** Re-test the steps after changing scripts, ports or environment variables.

### `docs/FILE_GUIDE.md`

1. **Type and location:** Markdown document at `docs/FILE_GUIDE.md`
2. **Purpose:** This file: seven points about every file in the project.
3. **Key contents:** One section per file: location/type, purpose, key contents, depends on, used by, tests, change notes.
4. **Depends on:** No other project files.
5. **Used by:** People (read or run directly).
6. **Tests:** Not code; no automated tests.
7. **Change notes:** Regenerate after adding or renaming files.

### `docs/ML.md`

1. **Type and location:** Markdown document at `docs/ML.md`
2. **Purpose:** Documents the optional ML module.
3. **Key contents:** Algorithm, features, dataset format, commands, leakage prevention, real metrics with baselines, how the model affects the score, limitations.
4. **Depends on:** No other project files.
5. **Used by:** People (read or run directly).
6. **Tests:** Not code; no automated tests.
7. **Change notes:** Re-run training and paste the new evaluation tables when data or code changes.

### `docs/REQUEST_FLOW.md`

1. **Type and location:** Markdown document at `docs/REQUEST_FLOW.md`
2. **Purpose:** Walks one scan request end to end.
3. **Key contents:** Flowchart, sequence diagram, a 20-step table naming each file and method, and summaries of other flows.
4. **Depends on:** No other project files.
5. **Used by:** People (read or run directly).
6. **Tests:** Not code; no automated tests.
7. **Change notes:** Keep method names in sync with the code when refactoring.

### `docs/SCORING.md`

1. **Type and location:** Markdown document at `docs/SCORING.md`
2. **Purpose:** The exact risk scoring method.
3. **Key contents:** Vocabulary, noisy-OR formula, 95 cap, thresholds, evidence strength, weight tables per detector, a real worked example, recommendations, calibration plan.
4. **Depends on:** No other project files.
5. **Used by:** People (read or run directly).
6. **Tests:** Not code; no automated tests.
7. **Change notes:** Any change to weights or the formula must bump `RiskEngine.RULESET_VERSION` and update this file.

### `docs/TESTING.md`

1. **Type and location:** Markdown document at `docs/TESTING.md`
2. **Purpose:** Test strategy and the latest real results.
3. **Key contents:** Test pyramid, what each suite covers, exact commands, last observed output, what was verified manually, gaps.
4. **Depends on:** No other project files.
5. **Used by:** People (read or run directly).
6. **Tests:** Not code; no automated tests.
7. **Change notes:** Only paste results that were actually observed.

### `docs/diagrams/architecture.mmd`

1. **Type and location:** Mermaid diagram source at `docs/diagrams/architecture.mmd`
2. **Purpose:** Mermaid source of the architecture diagram.
3. **Key contents:** Browser SPA, security filters, controllers, services, engine, detectors, repositories, database, optional model files.
4. **Depends on:** No other project files.
5. **Used by:** People (read or run directly).
6. **Tests:** Not code; no automated tests.
7. **Change notes:** Embedded in ARCHITECTURE.md and REQUEST_FLOW.md; update those copies too.

### `docs/diagrams/er-diagram.mmd`

1. **Type and location:** Mermaid diagram source at `docs/diagrams/er-diagram.mmd`
2. **Purpose:** Mermaid source of the ER diagram.
3. **Key contents:** Six tables with keys and the most important columns.
4. **Depends on:** No other project files.
5. **Used by:** People (read or run directly).
6. **Tests:** Not code; no automated tests.
7. **Change notes:** Embedded in DATABASE.md; update after schema migrations.

### `docs/diagrams/scan-sequence.mmd`

1. **Type and location:** Mermaid diagram source at `docs/diagrams/scan-sequence.mmd`
2. **Purpose:** Mermaid source of the scan sequence diagram.
3. **Key contents:** User → SPA → filters → ScanController → ScanService → ScanHistoryService/RiskEngine/detectors → repository → DB → response.
4. **Depends on:** No other project files.
5. **Used by:** People (read or run directly).
6. **Tests:** Not code; no automated tests.
7. **Change notes:** Embedded in REQUEST_FLOW.md; update both.

## Data and models

### `data/README.md`

1. **Type and location:** Markdown document at `data/README.md`
2. **Purpose:** Describes the bundled datasets and their schemas.
3. **Key contents:** File list, CSV schema `id,label,text`, permission report formats, how the synthetic data was made, licence, safety note about fictional domains.
4. **Depends on:** No other project files.
5. **Used by:** People (read or run directly).
6. **Tests:** Not code; no automated tests.
7. **Change notes:** Document any new dataset here, including its source and licence.

### `data/samples/messages_synthetic.csv`

1. **Type and location:** CSV dataset at `data/samples/messages_synthetic.csv`
2. **Purpose:** Synthetic labelled messages for training/evaluating the optional message model.
3. **Key contents:** 100 rows (`id,label,text`), 50 fraud (label 1) and 50 legitimate (label 0), written for this project.
4. **Depends on:** No other project files.
5. **Used by:** People (read or run directly).
6. **Tests:** `MlPipelineTest` trains on this file through `TrainingCli`; also used by `scripts/train-models.cmd`.
7. **Change notes:** Keep ids unique and texts distinct; duplicates are removed by the splitter anyway. Never add real people's messages.

### `data/samples/sample_permission_report.json`

1. **Type and location:** JSON data at `data/samples/sample_permission_report.json`
2. **Purpose:** Same example in JSON form.
3. **Key contents:** `{"appName": ..., "permissions": [...]}`.
4. **Depends on:** No other project files.
5. **Used by:** People (read or run directly).
6. **Tests:** Used by `scripts/train-models.cmd` or the demo; not read by the automated tests.
7. **Change notes:** Safe to edit; must stay valid JSON.

### `data/samples/sample_permission_report.txt`

1. **Type and location:** Plain text data at `data/samples/sample_permission_report.txt`
2. **Purpose:** Example permission report to demo the upload feature.
3. **Key contents:** One Android permission per line for a fictional flashlight app.
4. **Depends on:** No other project files.
5. **Used by:** People (read or run directly).
6. **Tests:** Used by `scripts/train-models.cmd` or the demo; not read by the automated tests.
7. **Change notes:** Safe to edit; keep it under 64 KB.

### `data/samples/urls_synthetic.csv`

1. **Type and location:** CSV dataset at `data/samples/urls_synthetic.csv`
2. **Purpose:** Synthetic labelled URLs for the optional URL model.
3. **Key contents:** 80 rows, 40 fraud and 40 legitimate. Fraud examples use fictional or reserved domains.
4. **Depends on:** No other project files.
5. **Used by:** People (read or run directly).
6. **Tests:** Used by `scripts/train-models.cmd` or the demo; not read by the automated tests.
7. **Change notes:** Do not open these URLs. Do not add live malicious links.

### `models/README.md`

1. **Type and location:** Markdown document at `models/README.md`
2. **Purpose:** Explains the (initially empty) models folder.
3. **Key contents:** How to create models with `TrainingCli`, what files are written, that the app works without them.
4. **Depends on:** No other project files.
5. **Used by:** People (read or run directly).
6. **Tests:** Not code; no automated tests.
7. **Change notes:** Generated `model.json`/`metadata.json`/`evaluation.md` files are git-ignored.

## Scripts

### `scripts/build.cmd`

1. **Type and location:** Windows batch script at `scripts/build.cmd`
2. **Purpose:** Windows: build the runnable jar (runs tests first).
3. **Key contents:** `mvnw.cmd -B clean package`, prints how to run `target\universalshield.jar`.
4. **Depends on:** No other project files.
5. **Used by:** People (read or run directly).
6. **Tests:** Not automated; the equivalent commands were run during the final build (docs/TESTING.md).
7. **Change notes:** Keep CRLF line endings.

### `scripts/run-dev.cmd`

1. **Type and location:** Windows batch script at `scripts/run-dev.cmd`
2. **Purpose:** Windows: start the app in dev mode.
3. **Key contents:** `mvnw.cmd -q spring-boot:run` from the project root.
4. **Depends on:** No other project files.
5. **Used by:** People (read or run directly).
6. **Tests:** Not automated; the equivalent commands were run during the final build (docs/TESTING.md).
7. **Change notes:** Keep CRLF line endings.

### `scripts/run-dev.sh`

1. **Type and location:** Shell script at `scripts/run-dev.sh`
2. **Purpose:** macOS/Linux: start the app in dev mode.
3. **Key contents:** `./mvnw -q spring-boot:run`.
4. **Depends on:** No other project files.
5. **Used by:** People (read or run directly).
6. **Tests:** Not automated; the equivalent commands were run during the final build (docs/TESTING.md).
7. **Change notes:** Keep LF line endings and the executable bit.

### `scripts/run-tests.cmd`

1. **Type and location:** Windows batch script at `scripts/run-tests.cmd`
2. **Purpose:** Windows: run all tests.
3. **Key contents:** `mvnw.cmd -B test`, then the Node frontend tests if `node` is on PATH.
4. **Depends on:** No other project files.
5. **Used by:** People (read or run directly).
6. **Tests:** Not automated; the equivalent commands were run during the final build (docs/TESTING.md).
7. **Change notes:** Keep CRLF line endings.

### `scripts/run-tests.sh`

1. **Type and location:** Shell script at `scripts/run-tests.sh`
2. **Purpose:** macOS/Linux: run Java and frontend tests.
3. **Key contents:** `./mvnw -B test` then `node --test` on the frontend tests if Node exists.
4. **Depends on:** No other project files.
5. **Used by:** People (read or run directly).
6. **Tests:** Not automated; the equivalent commands were run during the final build (docs/TESTING.md).
7. **Change notes:** Keep LF line endings.

### `scripts/train-models.cmd`

1. **Type and location:** Windows batch script at `scripts/train-models.cmd`
2. **Purpose:** Windows: train both optional ML models from the synthetic data.
3. **Key contents:** Two `TrainingCli` runs via `exec:java`, writing to `models/message` and `models/url`.
4. **Depends on:** No other project files.
5. **Used by:** People (read or run directly).
6. **Tests:** Not automated; the equivalent commands were run during the final build (docs/TESTING.md).
7. **Change notes:** Restart the app afterwards to load the models.

## Java: application, configuration and engine

### `src/main/java/com/universalshield/UniversalShieldApplication.java`

1. **Type and location:** Java class (@SpringBootApplication) in package `com.universalshield`
2. **Purpose:** Entry point of the UniversalShield application. Starts the embedded web server, the REST API and the static dashboard.
3. **Key contents:** Public methods: `main()`
4. **Depends on:** No other project files.
5. **Used by:** Spring (picked up automatically by component scanning or as a configuration file).
6. **Tests:** Covered indirectly by the Spring integration tests (`*IntegrationTest`), which run the whole application.
7. **Change notes:** Application entry point.

### `src/main/java/com/universalshield/config/AppProperties.java`

1. **Type and location:** Java record (@ConfigurationProperties) in package `com.universalshield.config`
2. **Purpose:** Type-safe view of the `universalshield.*` settings in application.yml. Secrets (hash secret, demo password) come from environment variables.
3. **Key contents:** Fields: `risk`, `security`, `retention`, `demo`; Public methods: `Risk()`, `Thresholds()`, `Security()`, `RateLimit()`, `Retention()`, `Demo()`
4. **Depends on:** No other project files.
5. **Used by:** `AuthService`, `DemoDataSeeder`, `EngineConfig`, `ProductionSafetyCheck`, `RateLimitFilter`, `SecurityConfig`, `SubjectHasher`, `UserService`
6. **Tests:** `ProductionSafetyCheckTest`
7. **Change notes:** Configuration is read at startup; restart after changing properties.

### `src/main/java/com/universalshield/config/DemoDataSeeder.java`

1. **Type and location:** Java class (@Component) in package `com.universalshield.config`
2. **Purpose:** Creates a clearly labeled demonstration account with scans produced by running the real engine on SYNTHETIC examples. Enabled only when universalshield.demo.seed=true and a demo password is supplied (the dev profile does this).
3. **Key contents:** Public methods: `seed()`
4. **Depends on:** `AppPermissionInput`, `AppProperties`, `AuthService`, `CallInput`, `MessageInput`, `RegisterRequest`, `ScanCategory`, `ScanRequest`, `ScanService`, `TransactionInput`, `UrlInput`, `User`, `UserRepository`
5. **Used by:** Spring (picked up automatically by component scanning or as a configuration file).
6. **Tests:** Not run by tests (dev profile only); exercised every time the dev profile starts and by the UI smoke test.
7. **Change notes:** Configuration is read at startup; restart after changing properties.

### `src/main/java/com/universalshield/config/EngineConfig.java`

1. **Type and location:** Java class (@Configuration) in package `com.universalshield.config`
2. **Purpose:** Wires the framework-free `RiskEngine` with every detector bean and configured thresholds.
3. **Key contents:** Public methods: `clock()`, `recommendationCatalog()`, `riskThresholds()`, `riskEngine()`
4. **Depends on:** `AppProperties`, `DetectionModule`, `RecommendationCatalog`, `RiskEngine`, `RiskThresholds`
5. **Used by:** Spring (picked up automatically by component scanning or as a configuration file).
6. **Tests:** Covered indirectly by the Spring integration tests (`*IntegrationTest`), which run the whole application.
7. **Change notes:** Configuration is read at startup; restart after changing properties.

### `src/main/java/com/universalshield/config/OpenApiConfig.java`

1. **Type and location:** Java class (@Configuration) in package `com.universalshield.config`
2. **Purpose:** OpenAPI metadata. The spec is served at /api/openapi and the UI at /swagger-ui.html.
3. **Key contents:** Public methods: `universalShieldOpenApi()`
4. **Depends on:** No other project files.
5. **Used by:** Spring (picked up automatically by component scanning or as a configuration file).
6. **Tests:** Covered indirectly by the Spring integration tests (`*IntegrationTest`), which run the whole application.
7. **Change notes:** Configuration is read at startup; restart after changing properties.

### `src/main/java/com/universalshield/config/ProductionSafetyCheck.java`

1. **Type and location:** Java class (@Component, @Profile) in package `com.universalshield.config`
2. **Purpose:** Refuses to start in production with the development hash secret or a weak one.
3. **Key contents:** Constants: `MIN_SECRET_LENGTH`
4. **Depends on:** `AppProperties`
5. **Used by:** Spring (picked up automatically by component scanning or as a configuration file).
6. **Tests:** `ProductionSafetyCheckTest`
7. **Change notes:** Configuration is read at startup; restart after changing properties.

### `src/main/java/com/universalshield/engine/DetectionModule.java`

1. **Type and location:** Java interface in package `com.universalshield.engine`
2. **Purpose:** Contract every detector implements. To add a detector, implement this interface and register it as a Spring bean; the `RiskEngine` discovers it automatically.
3. **Key contents:** See source.
4. **Depends on:** `DetectorResult`, `FraudEvent`, `ResultSource`, `ScanCategory`
5. **Used by:** `AppPermissionDetector`, `CallFraudDetector`, `DetectorCatalogService`, `EngineConfig`, `MessageFraudDetector`, `MlTextDetector`, `RiskEngine`, `TransactionFraudDetector`, `UrlRiskDetector`
6. **Tests:** `RiskEngineTest`
7. **Change notes:** Plain Java with no Spring dependency, so it can be unit-tested directly. Changing scoring behaviour requires bumping `RiskEngine.RULESET_VERSION` and updating docs/SCORING.md.

### `src/main/java/com/universalshield/engine/DetectorResult.java`

1. **Type and location:** Java record in package `com.universalshield.engine`
2. **Purpose:** Output of a single `DetectionModule` for one event.
3. **Key contents:** Fields: `detectorId`, `detectorVersion`, `source`, `indicators`, `notes`, `inputSufficient`
4. **Depends on:** `Indicator`, `ResultSource`
5. **Used by:** `AppPermissionDetector`, `CallFraudDetector`, `DetectionModule`, `MessageFraudDetector`, `MlTextDetector`, `RiskEngine`, `RiskScorer`, `TransactionFraudDetector`, `UrlRiskDetector`
6. **Tests:** `AppPermissionDetectorTest`, `CallFraudDetectorTest`, `DetectorTestSupport`, `RiskEngineTest`, `RiskScorerTest`, `UrlRiskDetectorTest`
7. **Change notes:** Plain Java with no Spring dependency, so it can be unit-tested directly. Changing scoring behaviour requires bumping `RiskEngine.RULESET_VERSION` and updating docs/SCORING.md.

### `src/main/java/com/universalshield/engine/EvidenceStrength.java`

1. **Type and location:** Java enum in package `com.universalshield.engine`
2. **Purpose:** How much independent evidence supports the assessment. Deliberately separate from the risk score: a score of 40 backed by three independent signal groups is better supported than a score of 40 from one keyword in a ten-character message.
3. **Key contents:** See source.
4. **Depends on:** No other project files.
5. **Used by:** `FraudResult`, `RiskEngine`, `RiskScorer`, `Scan`
6. **Tests:** `PersistenceTest`, `RiskEngineTest`, `RiskScorerTest`
7. **Change notes:** Plain Java with no Spring dependency, so it can be unit-tested directly. Changing scoring behaviour requires bumping `RiskEngine.RULESET_VERSION` and updating docs/SCORING.md.

### `src/main/java/com/universalshield/engine/FraudEvent.java`

1. **Type and location:** Java record in package `com.universalshield.engine`
2. **Purpose:** Something the user wants screened. Exactly one input matching `#category()` is set.
3. **Key contents:** Fields: `category`, `message`, `url`, `transaction`, `call`, `app`, `history`; Public methods: `ofMessage()`, `ofUrl()`, `ofTransaction()`, `ofCall()`, `ofApp()`, `freeText()`
4. **Depends on:** `AppPermissionInput`, `CallInput`, `MessageInput`, `ScanCategory`, `ScanHistory`, `TransactionInput`, `UrlInput`
5. **Used by:** `AppPermissionDetector`, `CallFraudDetector`, `DetectionModule`, `MessageFraudDetector`, `MlTextDetector`, `RiskEngine`, `ScanService`, `TrainingCli`, `TransactionFraudDetector`, `UrlRiskDetector`
6. **Tests:** `AppPermissionDetectorTest`, `CallFraudDetectorTest`, `MessageFraudDetectorTest`, `MlPipelineTest`, `RiskEngineTest`, `TransactionFraudDetectorTest`, `UrlRiskDetectorTest`
7. **Change notes:** Plain Java with no Spring dependency, so it can be unit-tested directly. Changing scoring behaviour requires bumping `RiskEngine.RULESET_VERSION` and updating docs/SCORING.md.

### `src/main/java/com/universalshield/engine/FraudResult.java`

1. **Type and location:** Java record in package `com.universalshield.engine`
2. **Purpose:** The unified, explainable output of the `RiskEngine`.
3. **Key contents:** Fields: `resultId`, `category`, `riskScore`, `riskLevel`, `evidenceStrength`, `reasons`, `indicators`, `recommendations`, `timestamp`, `rulesetVersion`, `detectorVersions`, `sources`, `notes`, `disclaimers`
4. **Depends on:** `EvidenceStrength`, `Indicator`, `Recommendation`, `ResultSource`, `RiskLevel`, `ScanCategory`
5. **Used by:** `RiskEngine`, `ScanService`
6. **Tests:** `RiskEngineTest`
7. **Change notes:** Plain Java with no Spring dependency, so it can be unit-tested directly. Changing scoring behaviour requires bumping `RiskEngine.RULESET_VERSION` and updating docs/SCORING.md.

### `src/main/java/com/universalshield/engine/Indicator.java`

1. **Type and location:** Java record in package `com.universalshield.engine`
2. **Purpose:** One matched warning sign.
3. **Key contents:** Fields: `code`, `group`, `title`, `explanation`, `weight`, `detectorId`, `source`, `evidence`
4. **Depends on:** `ResultSource`
5. **Used by:** `AppPermissionDetector`, `CallFraudDetector`, `DetectorResult`, `FraudResult`, `MessageFraudDetector`, `MlTextDetector`, `RecommendationCatalog`, `ReportService`, `RiskEngine`, `RiskScorer`, `ScanService`, `TextSignals`, `TransactionFraudDetector`, `UrlRiskDetector`
6. **Tests:** `DetectorTestSupport`, `RiskEngineTest`, `RiskLevelTest`, `RiskScorerTest`
7. **Change notes:** Plain Java with no Spring dependency, so it can be unit-tested directly. Changing scoring behaviour requires bumping `RiskEngine.RULESET_VERSION` and updating docs/SCORING.md.

### `src/main/java/com/universalshield/engine/Recommendation.java`

1. **Type and location:** Java record in package `com.universalshield.engine`
2. **Purpose:** A concrete next step for the user.
3. **Key contents:** Fields: `action`, `reason`, `priority`
4. **Depends on:** No other project files.
5. **Used by:** `FraudResult`, `RecommendationCatalog`, `RecommendationListConverter`, `ReportService`, `Scan`, `ScanResponse`
6. **Tests:** `PersistenceTest`
7. **Change notes:** Plain Java with no Spring dependency, so it can be unit-tested directly. Changing scoring behaviour requires bumping `RiskEngine.RULESET_VERSION` and updating docs/SCORING.md.

### `src/main/java/com/universalshield/engine/RecommendationCatalog.java`

1. **Type and location:** Java class in package `com.universalshield.engine`
2. **Purpose:** Maps indicator groups and risk levels to concrete next steps. Group keys may be prefixed by a namespace ("url:", "ml:"); the suffix is used for lookup.
3. **Key contents:** Public methods: `recommend()`; Constants: `MAX_RECOMMENDATIONS`
4. **Depends on:** `Indicator`, `Recommendation`, `RiskLevel`
5. **Used by:** `EngineConfig`, `RiskEngine`, `TrainingCli`
6. **Tests:** `RiskEngineTest`
7. **Change notes:** Plain Java with no Spring dependency, so it can be unit-tested directly. Changing scoring behaviour requires bumping `RiskEngine.RULESET_VERSION` and updating docs/SCORING.md.

### `src/main/java/com/universalshield/engine/ResultSource.java`

1. **Type and location:** Java enum in package `com.universalshield.engine`
2. **Purpose:** Where a piece of evidence came from. Shown to the user on every result.
3. **Key contents:** See source.
4. **Depends on:** No other project files.
5. **Used by:** `AppPermissionDetector`, `CallFraudDetector`, `DetectionModule`, `DetectorResult`, `FraudResult`, `Indicator`, `MessageFraudDetector`, `MlTextDetector`, `RiskEngine`, `ScanIndicator`, `ScanService`, `TextSignals`, `TransactionFraudDetector`, `UrlRiskDetector`
6. **Tests:** `CallFraudDetectorTest`, `MlPipelineTest`, `PersistenceTest`, `RiskEngineTest`, `RiskLevelTest`, `RiskScorerTest`, `UrlRiskDetectorTest`
7. **Change notes:** Plain Java with no Spring dependency, so it can be unit-tested directly. Changing scoring behaviour requires bumping `RiskEngine.RULESET_VERSION` and updating docs/SCORING.md.

### `src/main/java/com/universalshield/engine/RiskEngine.java`

1. **Type and location:** Java class in package `com.universalshield.engine`
2. **Purpose:** Runs every applicable `DetectionModule` on a `FraudEvent` and combines their evidence into one explainable `FraudResult`. Pure Java; no Spring or database access.
3. **Key contents:** Public methods: `modules()`, `thresholds()`, `evaluate()`; Constants: `RULESET_VERSION`, `DISCLAIMER_GENERAL`, `DISCLAIMER_LOW`, `DISCLAIMER_HIGH`, `DISCLAIMER_LIMITED`, `DISCLAIMER_NO_EXTERNAL`, `MAX_SCORE_WITHOUT_EXTERNAL`
4. **Depends on:** `DetectionModule`, `DetectorResult`, `EvidenceStrength`, `FraudEvent`, `FraudResult`, `Indicator`, `RecommendationCatalog`, `ResultSource`, `RiskLevel`, `RiskScorer`, `RiskThresholds`
5. **Used by:** `DetectorCatalogService`, `DetectorController`, `EngineConfig`, `ScanService`, `TrainingCli`
6. **Tests:** `RiskEngineTest`, `ScanServiceTest`
7. **Change notes:** Plain Java with no Spring dependency, so it can be unit-tested directly. Changing scoring behaviour requires bumping `RiskEngine.RULESET_VERSION` and updating docs/SCORING.md.

### `src/main/java/com/universalshield/engine/RiskLevel.java`

1. **Type and location:** Java enum in package `com.universalshield.engine`
2. **Purpose:** Coarse risk band derived from the 0-100 risk score using configurable thresholds. A HIGH or CRITICAL level means "strong warning signs", never "fraud proven".
3. **Key contents:** Values: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`; Public methods: `fromScore()`
4. **Depends on:** `RiskThresholds`
5. **Used by:** `DashboardService`, `FraudResult`, `RecommendationCatalog`, `RiskEngine`, `Scan`, `ScanController`, `ScanHistoryService`, `ScanRepository`, `ScanService`
6. **Tests:** `PersistenceTest`, `RiskEngineTest`, `RiskLevelTest`
7. **Change notes:** Plain Java with no Spring dependency, so it can be unit-tested directly. Changing scoring behaviour requires bumping `RiskEngine.RULESET_VERSION` and updating docs/SCORING.md.

### `src/main/java/com/universalshield/engine/RiskScorer.java`

1. **Type and location:** Java class in package `com.universalshield.engine`
2. **Purpose:** Turns indicators into a 0-100 score and an evidence-strength rating. Formula (documented in docs/SCORING.md): Indicators are grouped by `Indicator#group()`.
3. **Key contents:** Public methods: `Breakdown()`, `score()`, `evidence()`; Constants: `EVIDENCE_MIN_WEIGHT`
4. **Depends on:** `DetectorResult`, `EvidenceStrength`, `Indicator`
5. **Used by:** `RiskEngine`
6. **Tests:** `RiskScorerTest`
7. **Change notes:** Plain Java with no Spring dependency, so it can be unit-tested directly. Changing scoring behaviour requires bumping `RiskEngine.RULESET_VERSION` and updating docs/SCORING.md.

### `src/main/java/com/universalshield/engine/RiskThresholds.java`

1. **Type and location:** Java record in package `com.universalshield.engine`
2. **Purpose:** Inclusive lower bounds of the MEDIUM, HIGH and CRITICAL bands. Configured via universalshield.risk.thresholds.* in application.yml.
3. **Key contents:** Fields: `medium`, `high`, `critical`; Constants: `DEFAULT`
4. **Depends on:** No other project files.
5. **Used by:** `EngineConfig`, `RiskEngine`, `RiskLevel`, `TrainingCli`
6. **Tests:** `RiskEngineTest`, `RiskLevelTest`
7. **Change notes:** Plain Java with no Spring dependency, so it can be unit-tested directly. Changing scoring behaviour requires bumping `RiskEngine.RULESET_VERSION` and updating docs/SCORING.md.

### `src/main/java/com/universalshield/engine/ScanCategory.java`

1. **Type and location:** Java enum in package `com.universalshield.engine`
2. **Purpose:** The five kinds of input UniversalShield can screen.
3. **Key contents:** Values: `MESSAGE`, `URL`, `TRANSACTION`, `CALL`, `APP_PERMISSION`; Public methods: `label()`
4. **Depends on:** No other project files.
5. **Used by:** `AppPermissionDetector`, `CallFraudDetector`, `DashboardService`, `DemoDataSeeder`, `DetectionModule`, `FraudEvent`, `FraudResult`, `MessageFraudDetector`, `MlTextDetector`, `Scan`, `ScanController`, `ScanHistoryService`, `ScanRepository`, `ScanRequest`, `ScanService`, `TransactionFraudDetector`, `UrlRiskDetector`
6. **Tests:** `InputSanitizerTest`, `MessageFraudDetectorTest`, `PersistenceTest`, `RetentionServiceIntegrationTest`, `RiskEngineTest`, `ScanServiceTest`
7. **Change notes:** Plain Java with no Spring dependency, so it can be unit-tested directly. Changing scoring behaviour requires bumping `RiskEngine.RULESET_VERSION` and updating docs/SCORING.md.

### `src/main/java/com/universalshield/engine/ScanHistory.java`

1. **Type and location:** Java record in package `com.universalshield.engine`
2. **Purpose:** The user's own relevant history, supplied to detectors so they can spot unusual behaviour. Only the user's own prior scans are used.
3. **Key contents:** Fields: `previousTransactionAmounts`, `priorScansOfSubject`, `priorHighRiskOfSubject`; Constants: `EMPTY`
4. **Depends on:** No other project files.
5. **Used by:** `FraudEvent`, `ScanHistoryService`, `ScanService`
6. **Tests:** `CallFraudDetectorTest`, `MessageFraudDetectorTest`, `TransactionFraudDetectorTest`, `UrlRiskDetectorTest`
7. **Change notes:** Plain Java with no Spring dependency, so it can be unit-tested directly. Changing scoring behaviour requires bumping `RiskEngine.RULESET_VERSION` and updating docs/SCORING.md.

### `src/main/java/com/universalshield/engine/input/AppPermissionInput.java`

1. **Type and location:** Java record in package `com.universalshield.engine.input`
2. **Purpose:** App permission details entered by the user or parsed from an uploaded permission report.
3. **Key contents:** Fields: `appName`, `appCategory`, `installSource`, `permissions`
4. **Depends on:** No other project files.
5. **Used by:** `AppPermissionDetector`, `DemoDataSeeder`, `FraudEvent`, `ScanRequest`
6. **Tests:** `AppPermissionDetectorTest`
7. **Change notes:** Validation annotations here are the server-side contract; keep `static/js/validation.js` limits identical.

### `src/main/java/com/universalshield/engine/input/CallInput.java`

1. **Type and location:** Java record in package `com.universalshield.engine.input`
2. **Purpose:** The user's own description of a phone call. UniversalShield never records calls and has no access to live caller-ID data.
3. **Key contents:** Fields: `callerClaimedIdentity`, `callerNumber`, `description`, `durationMinutes`
4. **Depends on:** No other project files.
5. **Used by:** `DemoDataSeeder`, `FraudEvent`, `ScanRequest`
6. **Tests:** `CallFraudDetectorTest`, `ScanServiceTest`
7. **Change notes:** Validation annotations here are the server-side contract; keep `static/js/validation.js` limits identical.

### `src/main/java/com/universalshield/engine/input/MessageInput.java`

1. **Type and location:** Java record in package `com.universalshield.engine.input`
2. **Purpose:** A message the user pasted for screening.
3. **Key contents:** Fields: `text`, `channel`, `sender`
4. **Depends on:** No other project files.
5. **Used by:** `DemoDataSeeder`, `FraudEvent`, `ScanRequest`, `TrainingCli`
6. **Tests:** `InputSanitizerTest`, `MessageFraudDetectorTest`, `MlPipelineTest`, `RetentionServiceIntegrationTest`, `RiskEngineTest`, `ScanServiceTest`, `UrlRiskDetectorTest`
7. **Change notes:** Validation annotations here are the server-side contract; keep `static/js/validation.js` limits identical.

### `src/main/java/com/universalshield/engine/input/TransactionInput.java`

1. **Type and location:** Java record in package `com.universalshield.engine.input`
2. **Purpose:** Metadata about a payment the user is about to make or was asked to make. Never includes card numbers, CVV, PIN or OTP; the service masks them if pasted.
3. **Key contents:** Fields: `amount`, `currency`, `recipient`, `recipientIsNew`, `paymentMethod`, `attemptsLastHour`, `requestedByOtherParty`, `description`
4. **Depends on:** No other project files.
5. **Used by:** `DemoDataSeeder`, `FraudEvent`, `ScanRequest`
6. **Tests:** `InputSanitizerTest`, `TransactionFraudDetectorTest`
7. **Change notes:** Validation annotations here are the server-side contract; keep `static/js/validation.js` limits identical.

### `src/main/java/com/universalshield/engine/input/UrlInput.java`

1. **Type and location:** Java record in package `com.universalshield.engine.input`
2. **Purpose:** A URL to analyze structurally. The server never fetches it.
3. **Key contents:** Fields: `url`
4. **Depends on:** No other project files.
5. **Used by:** `DemoDataSeeder`, `FraudEvent`, `ScanRequest`, `TrainingCli`
6. **Tests:** `MessageFraudDetectorTest`, `ScanServiceTest`, `UrlRiskDetectorTest`
7. **Change notes:** Validation annotations here are the server-side contract; keep `static/js/validation.js` limits identical.

## Java: detectors and ML

### `src/main/java/com/universalshield/detectors/app/AppPermissionDetector.java`

1. **Type and location:** Java class (@Component) in package `com.universalshield.detectors.app`
2. **Purpose:** Flags potentially excessive app permissions and explains whether each appears justified by the app's stated purpose. Permission analysis can never prove that an app is malware.
3. **Key contents:** Public methods: `appliesTo()`, `analyze()`; Constants: `ID`, `VERSION`, `JUSTIFIED_FACTOR`, `MIN_WEIGHT`, `MINIMAL_PURPOSE`
4. **Depends on:** `AppPermissionInput`, `DetectionModule`, `DetectorResult`, `FraudEvent`, `Indicator`, `PermissionCatalog`, `PermissionRule`, `ResultSource`, `ScanCategory`
5. **Used by:** Spring (picked up automatically by component scanning or as a configuration file).
6. **Tests:** `AppPermissionDetectorTest`
7. **Change notes:** Changing a weight or rule changes scores: bump the detector `VERSION`, update docs/SCORING.md, and run the detector tests.

### `src/main/java/com/universalshield/detectors/app/PermissionCatalog.java`

1. **Type and location:** Java class in package `com.universalshield.detectors.app`
2. **Purpose:** Built-in knowledge base of sensitive and normal Android permissions.
3. **Key contents:** Public methods: `normalize()`, `rule()`, `isNormal()`; Constants: `NORMAL`, `RULES`
4. **Depends on:** `PermissionRule`
5. **Used by:** `AppPermissionDetector`, `PermissionReportParser`
6. **Tests:** `AppPermissionDetectorTest`
7. **Change notes:** Changing a weight or rule changes scores: bump the detector `VERSION`, update docs/SCORING.md, and run the detector tests.

### `src/main/java/com/universalshield/detectors/app/PermissionReportParser.java`

1. **Type and location:** Java class (@Component) in package `com.universalshield.detectors.app`
2. **Purpose:** Extracts permission names from an uploaded permission report. Supported formats: JSON: {"appName": "...", "permissions": ["android.permission.READ_SMS", ...]} Text output of `aapt dump permissions` / `adb shell dumpsys package`: any `android.permission.X` token Plain list: one permission per line or comma-separated (READ_SMS, CAMERA) The file content is parsed in memory and never stored.
3. **Key contents:** Public methods: `ParsedReport()`, `parse()`; Constants: `MAX_PERMISSIONS`, `ANDROID_PERMISSION`, `BARE_NAME`
4. **Depends on:** `PermissionCatalog`
5. **Used by:** `ScanController`
6. **Tests:** `AppPermissionDetectorTest`
7. **Change notes:** Changing a weight or rule changes scores: bump the detector `VERSION`, update docs/SCORING.md, and run the detector tests.

### `src/main/java/com/universalshield/detectors/app/PermissionRule.java`

1. **Type and location:** Java record in package `com.universalshield.detectors.app`
2. **Purpose:** Risk knowledge about one Android permission.
3. **Key contents:** Fields: `group`, `weight`, `title`, `explanation`, `justifiedFor`
4. **Depends on:** No other project files.
5. **Used by:** `AppPermissionDetector`, `PermissionCatalog`
6. **Tests:** Covered indirectly by the Spring integration tests (`*IntegrationTest`), which run the whole application.
7. **Change notes:** Changing a weight or rule changes scores: bump the detector `VERSION`, update docs/SCORING.md, and run the detector tests.

### `src/main/java/com/universalshield/detectors/call/CallFraudDetector.java`

1. **Type and location:** Java class (@Component) in package `com.universalshield.detectors.call`
2. **Purpose:** Analyses user-entered call descriptions for impersonation, threats, remote-access requests, and fake support patterns. Never records calls.
3. **Key contents:** Public methods: `appliesTo()`, `analyze()`; Constants: `ID`, `VERSION`, `MIN_WORDS`
4. **Depends on:** `DetectionModule`, `DetectorResult`, `FraudEvent`, `Indicator`, `PhoneReputationProvider`, `ReputationVerdict`, `ResultSource`, `ScanCategory`, `TextSignals`
5. **Used by:** Spring (picked up automatically by component scanning or as a configuration file).
6. **Tests:** `CallFraudDetectorTest`
7. **Change notes:** Changing a weight or rule changes scores: bump the detector `VERSION`, update docs/SCORING.md, and run the detector tests.

### `src/main/java/com/universalshield/detectors/call/NotConfiguredPhoneReputationProvider.java`

1. **Type and location:** Java class (@Component) in package `com.universalshield.detectors.call`
2. **Purpose:** Default provider: reports honestly that no phone-number reputation data is available.
3. **Key contents:** Public methods: `lookup()`
4. **Depends on:** `PhoneReputationProvider`, `ReputationVerdict`
5. **Used by:** Spring (picked up automatically by component scanning or as a configuration file).
6. **Tests:** `CallFraudDetectorTest`
7. **Change notes:** Changing a weight or rule changes scores: bump the detector `VERSION`, update docs/SCORING.md, and run the detector tests.

### `src/main/java/com/universalshield/detectors/call/PhoneReputationProvider.java`

1. **Type and location:** Java interface in package `com.universalshield.detectors.call`
2. **Purpose:** Extension point for a verified phone-number reputation service. UniversalShield ships without one and never claims live caller-ID knowledge.
3. **Key contents:** See source.
4. **Depends on:** `ReputationVerdict`
5. **Used by:** `CallFraudDetector`, `NotConfiguredPhoneReputationProvider`
6. **Tests:** Covered indirectly by the Spring integration tests (`*IntegrationTest`), which run the whole application.
7. **Change notes:** Changing a weight or rule changes scores: bump the detector `VERSION`, update docs/SCORING.md, and run the detector tests.

### `src/main/java/com/universalshield/detectors/common/TextSignal.java`

1. **Type and location:** Java record in package `com.universalshield.detectors.common`
2. **Purpose:** A reusable text pattern describing one kind of manipulation tactic.
3. **Key contents:** Fields: `code`, `group`, `title`, `explanation`, `defaultWeight`, `pattern`, `negatable`
4. **Depends on:** No other project files.
5. **Used by:** `TextSignals`
6. **Tests:** Covered indirectly by the Spring integration tests (`*IntegrationTest`), which run the whole application.
7. **Change notes:** Changing a weight or rule changes scores: bump the detector `VERSION`, update docs/SCORING.md, and run the detector tests.

### `src/main/java/com/universalshield/detectors/common/TextSignals.java`

1. **Type and location:** Java class in package `com.universalshield.detectors.common`
2. **Purpose:** Shared library of social-engineering text signals used by the message, call and transaction detectors. Keeping them in one place means a tactic is described and weighted consistently everywhere.
3. **Key contents:** Public methods: `match()`, `evidence()`, `wordCount()`; Constants: `FLAGS`, `NEGATION`, `MAX_EVIDENCE`, `URGENCY`, `OTP_REQUEST`, `CREDENTIAL_REQUEST`, `SECRET_REQUEST`, `KYC` ...
4. **Depends on:** `Indicator`, `ResultSource`, `TextSignal`
5. **Used by:** `CallFraudDetector`, `MessageFraudDetector`, `TransactionFraudDetector`, `UrlRiskDetector`
6. **Tests:** `TextSignalsTest`
7. **Change notes:** Changing a weight or rule changes scores: bump the detector `VERSION`, update docs/SCORING.md, and run the detector tests.

### `src/main/java/com/universalshield/detectors/message/MessageFraudDetector.java`

1. **Type and location:** Java class (@Component) in package `com.universalshield.detectors.message`
2. **Purpose:** Analyses user-supplied SMS, email, chat and social-message text for social-engineering tactics. Produces indicators for phishing language, impersonation, OTP requests, urgency, credential harvesting, threats, prize lures and suspicious payment demands.
3. **Key contents:** Public methods: `appliesTo()`, `analyze()`; Constants: `ID`, `VERSION`, `MIN_WORDS`
4. **Depends on:** `DetectionModule`, `DetectorResult`, `FraudEvent`, `Indicator`, `ResultSource`, `ScanCategory`, `TextSignals`
5. **Used by:** `TrainingCli`
6. **Tests:** `MessageFraudDetectorTest`
7. **Change notes:** Changing a weight or rule changes scores: bump the detector `VERSION`, update docs/SCORING.md, and run the detector tests.

### `src/main/java/com/universalshield/detectors/transaction/TransactionFraudDetector.java`

1. **Type and location:** Java class (@Component) in package `com.universalshield.detectors.transaction`
2. **Purpose:** Analyses user-submitted transaction metadata for warning signs of fraud. Never collects card numbers, CVV, PIN or OTP.
3. **Key contents:** Public methods: `appliesTo()`, `analyze()`; Constants: `ID`, `VERSION`, `MIN_HISTORY`, `DEFAULT_LARGE_AMOUNT`
4. **Depends on:** `DetectionModule`, `DetectorResult`, `FraudEvent`, `Indicator`, `ResultSource`, `ScanCategory`, `TextSignals`
5. **Used by:** Spring (picked up automatically by component scanning or as a configuration file).
6. **Tests:** `TransactionFraudDetectorTest`
7. **Change notes:** Changing a weight or rule changes scores: bump the detector `VERSION`, update docs/SCORING.md, and run the detector tests.

### `src/main/java/com/universalshield/detectors/url/DomainReputationProvider.java`

1. **Type and location:** Java interface in package `com.universalshield.detectors.url`
2. **Purpose:** Extension point for a verified domain-reputation service (for example a Safe-Browsing style API or a commercial threat-intelligence feed). Implementations must look up the domain by name only.
3. **Key contents:** See source.
4. **Depends on:** `ReputationVerdict`
5. **Used by:** `NotConfiguredDomainReputationProvider`, `UrlRiskDetector`
6. **Tests:** `UrlRiskDetectorTest`
7. **Change notes:** Changing a weight or rule changes scores: bump the detector `VERSION`, update docs/SCORING.md, and run the detector tests.

### `src/main/java/com/universalshield/detectors/url/NotConfiguredDomainReputationProvider.java`

1. **Type and location:** Java class (@Component) in package `com.universalshield.detectors.url`
2. **Purpose:** Default provider: honestly reports that no external reputation data is available.
3. **Key contents:** Public methods: `lookup()`
4. **Depends on:** `DomainReputationProvider`, `ReputationVerdict`
5. **Used by:** `TrainingCli`
6. **Tests:** `UrlRiskDetectorTest`
7. **Change notes:** Changing a weight or rule changes scores: bump the detector `VERSION`, update docs/SCORING.md, and run the detector tests.

### `src/main/java/com/universalshield/detectors/url/ParsedUrl.java`

1. **Type and location:** Java record in package `com.universalshield.detectors.url`
2. **Purpose:** Minimal, dependency-free URL decomposition used for structural analysis. It never resolves DNS or opens connections.
3. **Key contents:** Fields: `original`, `scheme`, `userInfo`, `host`, `port`, `pathAndQuery`; Public methods: `parse()`, `labels()`, `isIpLiteral()`, `registrableDomain()`, `subdomainLabels()`, `registrableLabel()`, `tld()`; Constants: `WITH_SCHEME`, `SPECIAL_SCHEME`, `SECOND_LEVEL`
4. **Depends on:** No other project files.
5. **Used by:** `ScanService`, `UrlRiskDetector`
6. **Tests:** `UrlRiskDetectorTest`
7. **Change notes:** Changing a weight or rule changes scores: bump the detector `VERSION`, update docs/SCORING.md, and run the detector tests.

### `src/main/java/com/universalshield/detectors/url/ReputationVerdict.java`

1. **Type and location:** Java record in package `com.universalshield.detectors.url`
2. **Purpose:** Answer from a reputation provider.
3. **Key contents:** Fields: `status`, `provider`, `detail`; Public methods: `notConfigured()`
4. **Depends on:** No other project files.
5. **Used by:** `CallFraudDetector`, `DomainReputationProvider`, `NotConfiguredDomainReputationProvider`, `NotConfiguredPhoneReputationProvider`, `PhoneReputationProvider`, `UrlRiskDetector`
6. **Tests:** `CallFraudDetectorTest`, `UrlRiskDetectorTest`
7. **Change notes:** Changing a weight or rule changes scores: bump the detector `VERSION`, update docs/SCORING.md, and run the detector tests.

### `src/main/java/com/universalshield/detectors/url/UrlRiskDetector.java`

1. **Type and location:** Java class (@Component) in package `com.universalshield.detectors.url`
2. **Purpose:** Structural URL analysis. Works on the URL text only: it never resolves, fetches or executes anything, so there is no SSRF exposure.
3. **Key contents:** Public methods: `appliesTo()`, `analyze()`, `extractUrls()`; Constants: `ID`, `VERSION`, `MAX_EMBEDDED_URLS`, `BRANDS`, `SHORTENERS`, `RISKY_TLDS`, `LURE_WORDS`, `DOWNLOAD_EXT` ...
4. **Depends on:** `DetectionModule`, `DetectorResult`, `DomainReputationProvider`, `FraudEvent`, `Indicator`, `ParsedUrl`, `ReputationVerdict`, `ResultSource`, `ScanCategory`, `TextSignals`
5. **Used by:** `TrainingCli`
6. **Tests:** `UrlRiskDetectorTest`
7. **Change notes:** Changing a weight or rule changes scores: bump the detector `VERSION`, update docs/SCORING.md, and run the detector tests.

### `src/main/java/com/universalshield/ml/LabeledText.java`

1. **Type and location:** Java record in package `com.universalshield.ml`
2. **Purpose:** One labeled example. label: 1 = fraud/phishing, 0 = legitimate.
3. **Key contents:** Fields: `id`, `label`, `text`
4. **Depends on:** No other project files.
5. **Used by:** `DatasetLoader`, `DatasetSplitter`, `NaiveBayesModel`, `TrainingCli`
6. **Tests:** `MlPipelineTest`
7. **Change notes:** Model files are data only (JSON). Never load serialized Java objects or executable models.

### `src/main/java/com/universalshield/ml/MlModelService.java`

1. **Type and location:** Java class (@Service) in package `com.universalshield.ml`
2. **Purpose:** Loads trained models produced by the offline training pipeline from ` / /model.json` and `metadata.json`. Only JSON data is loaded, never executable code.
3. **Key contents:** Public methods: `LoadedModel()`, `load()`, `model()`, `anyAvailable()`, `status()`; Constants: `MAX_MODEL_BYTES`
4. **Depends on:** `MlTask`, `ModelMetadata`, `NaiveBayesModel`
5. **Used by:** `DetectorController`, `MlTextDetector`
6. **Tests:** `MlPipelineTest`
7. **Change notes:** Model files are data only (JSON). Never load serialized Java objects or executable models.

### `src/main/java/com/universalshield/ml/MlTask.java`

1. **Type and location:** Java enum in package `com.universalshield.ml`
2. **Purpose:** Text-classification tasks supported by the optional ML module.
3. **Key contents:** Values: `MESSAGE`, `URL`; Public methods: `dirName()`
4. **Depends on:** No other project files.
5. **Used by:** `MlModelService`, `MlTextDetector`, `NaiveBayesModel`, `Tokenizer`, `TrainingCli`
6. **Tests:** `MlPipelineTest`
7. **Change notes:** Model files are data only (JSON). Never load serialized Java objects or executable models.

### `src/main/java/com/universalshield/ml/MlTextDetector.java`

1. **Type and location:** Java class (@Component) in package `com.universalshield.ml`
2. **Purpose:** Optional detector backed by a locally trained Naive Bayes model. Unavailable (and therefore skipped by the engine) until a model has been trained with `TrainingCli`.
3. **Key contents:** Public methods: `appliesTo()`, `analyze()`; Constants: `ID`, `MAX_WEIGHT`, `REPORT_THRESHOLD`
4. **Depends on:** `DetectionModule`, `DetectorResult`, `FraudEvent`, `Indicator`, `MlModelService`, `MlTask`, `ResultSource`, `ScanCategory`
5. **Used by:** Spring (picked up automatically by component scanning or as a configuration file).
6. **Tests:** `MlPipelineTest`
7. **Change notes:** Model files are data only (JSON). Never load serialized Java objects or executable models.

### `src/main/java/com/universalshield/ml/ModelMetadata.java`

1. **Type and location:** Java record in package `com.universalshield.ml`
2. **Purpose:** Contents of metadata.json, written by the training pipeline next to model.json. Every number here was computed by `TrainingCli`; nothing is hand-entered.
3. **Key contents:** Fields: `modelVersion`, `task`, `algorithm`, `createdAt`, `datasetFile`, `datasetSha256`, `datasetDescription`, `rowsLoaded`, `duplicatesRemoved`, `trainSize`, `testSize`, `splitSeed`, `testRatio`, `decisionThreshold`, `vocabularySize`, `modelMetrics`, `baselineMetrics`, `limitations`
4. **Depends on:** No other project files.
5. **Used by:** `MlModelService`, `TrainingCli`
6. **Tests:** Covered indirectly by the Spring integration tests (`*IntegrationTest`), which run the whole application.
7. **Change notes:** Model files are data only (JSON). Never load serialized Java objects or executable models.

### `src/main/java/com/universalshield/ml/NaiveBayesModel.java`

1. **Type and location:** Java class in package `com.universalshield.ml`
2. **Purpose:** Multinomial Naive Bayes binary classifier with Laplace smoothing, written in plain Java so a student can read and explain every line. Label 1 = fraud, 0 = legitimate.
3. **Key contents:** Public methods: `State()`, `fromState()`, `train()`, `state()`, `vocabularySize()`, `probabilityFraud()`, `predict()`
4. **Depends on:** `LabeledText`, `MlTask`, `Tokenizer`
5. **Used by:** `MlModelService`, `TrainingCli`
6. **Tests:** `MlPipelineTest`
7. **Change notes:** Model files are data only (JSON). Never load serialized Java objects or executable models.

### `src/main/java/com/universalshield/ml/Tokenizer.java`

1. **Type and location:** Java class in package `com.universalshield.ml`
2. **Purpose:** Converts text into features for the Naive Bayes model. Shared by training and prediction so both see identical features.
3. **Key contents:** Public methods: `tokenize()`, `normalize()`
4. **Depends on:** `MlTask`
5. **Used by:** `DatasetSplitter`, `NaiveBayesModel`
6. **Tests:** `MlPipelineTest`
7. **Change notes:** Model files are data only (JSON). Never load serialized Java objects or executable models.

### `src/main/java/com/universalshield/ml/training/BinaryMetrics.java`

1. **Type and location:** Java record in package `com.universalshield.ml.training`
2. **Purpose:** Confusion matrix and derived metrics for a binary classifier (positive class = fraud). Undefined ratios (division by zero) are reported as 0.0 and flagged in `#toMap()`.
3. **Key contents:** Fields: `tp`, `fp`, `tn`, `fn`; Public methods: `of()`, `precision()`, `recall()`, `falsePositiveRate()`, `accuracy()`, `f1()`, `toMap()`
4. **Depends on:** No other project files.
5. **Used by:** `TrainingCli`
6. **Tests:** `MlPipelineTest`
7. **Change notes:** Command-line only; never call from a web request. Keep the split seeded and deduplicated to avoid leakage.

### `src/main/java/com/universalshield/ml/training/DatasetLoader.java`

1. **Type and location:** Java class in package `com.universalshield.ml.training`
2. **Purpose:** Reads a labeled CSV dataset with header `id,label,text` (RFC-4180 quoting). See data/README.md for the schema.
3. **Key contents:** Public methods: `load()`
4. **Depends on:** `LabeledText`
5. **Used by:** `TrainingCli`
6. **Tests:** `MlPipelineTest`
7. **Change notes:** Command-line only; never call from a web request. Keep the split seeded and deduplicated to avoid leakage.

### `src/main/java/com/universalshield/ml/training/DatasetSplitter.java`

1. **Type and location:** Java class in package `com.universalshield.ml.training`
2. **Purpose:** Reproducible, leakage-aware train/test split. De-duplicates examples by normalised text (lower-case, digits mapped to 0), so the same message with a different code or amount cannot appear in both train and test.
3. **Key contents:** Public methods: `Split()`, `split()`
4. **Depends on:** `LabeledText`, `Tokenizer`
5. **Used by:** `TrainingCli`
6. **Tests:** `MlPipelineTest`
7. **Change notes:** Command-line only; never call from a web request. Keep the split seeded and deduplicated to avoid leakage.

### `src/main/java/com/universalshield/ml/training/TrainingCli.java`

1. **Type and location:** Java class in package `com.universalshield.ml.training`
2. **Purpose:** Offline training and evaluation pipeline. Separate from the web application: it reads a CSV, splits it reproducibly, trains Naive Bayes, compares against two baselines, and writes model.json, metadata.json and evaluation.md.
3. **Key contents:** Public methods: `main()`, `Result()`, `run()`; Constants: `DECISION_THRESHOLD`, `HEURISTIC_THRESHOLD`
4. **Depends on:** `BinaryMetrics`, `DatasetLoader`, `DatasetSplitter`, `FraudEvent`, `LabeledText`, `MessageFraudDetector`, `MessageInput`, `MlTask`, `ModelMetadata`, `NaiveBayesModel`, `NotConfiguredDomainReputationProvider`, `RecommendationCatalog`, `RiskEngine`, `RiskThresholds`, `UrlInput`, `UrlRiskDetector`
5. **Used by:** Spring (picked up automatically by component scanning or as a configuration file).
6. **Tests:** `MlPipelineTest`
7. **Change notes:** Command-line only; never call from a web request. Keep the split seeded and deduplicated to avoid leakage.

## Java: web, security, services and persistence

### `src/main/java/com/universalshield/api/DashboardController.java`

1. **Type and location:** Java class (@RestController, @Tag) in package `com.universalshield.api`
2. **Purpose:** REST endpoint `GET /api/dashboard/summary` for the signed-in user's dashboard.
3. **Key contents:** Endpoints: `GET /api/dashboard/summary`; Public methods: `summary()`
4. **Depends on:** `ApiResponse`, `AppUserPrincipal`, `DashboardService`, `DashboardSummary`
5. **Used by:** Spring (picked up automatically by component scanning or as a configuration file).
6. **Tests:** `UserPrivacyIntegrationTest (over HTTP)`
7. **Change notes:** Keep endpoints thin: validate, call a service, wrap in `ApiResponse`. Document new endpoints in docs/API.md.

### `src/main/java/com/universalshield/api/DetectorController.java`

1. **Type and location:** Java class (@RestController, @Tag) in package `com.universalshield.api`
2. **Purpose:** Public, read-only information about detectors, thresholds and ML status.
3. **Key contents:** Endpoints: `GET /api/detectors`, `GET /api/methodology`; Public methods: `detectors()`, `methodology()`
4. **Depends on:** `ApiResponse`, `DetectorCatalogService`, `DetectorInfo`, `MlModelService`, `RiskEngine`
5. **Used by:** Spring (picked up automatically by component scanning or as a configuration file).
6. **Tests:** `PublicEndpointsIntegrationTest (over HTTP)`
7. **Change notes:** Keep endpoints thin: validate, call a service, wrap in `ApiResponse`. Document new endpoints in docs/API.md.

### `src/main/java/com/universalshield/api/HealthController.java`

1. **Type and location:** Java class (@RestController, @Tag) in package `com.universalshield.api`
2. **Purpose:** Liveness and database connectivity check, used by Docker and hosting health checks.
3. **Key contents:** Endpoints: `GET /api/health`; Public methods: `health()`
4. **Depends on:** `ApiResponse`
5. **Used by:** Spring (picked up automatically by component scanning or as a configuration file).
6. **Tests:** `PublicEndpointsIntegrationTest (over HTTP)`
7. **Change notes:** Keep endpoints thin: validate, call a service, wrap in `ApiResponse`. Document new endpoints in docs/API.md.

### `src/main/java/com/universalshield/api/ReportController.java`

1. **Type and location:** Java class (@RestController, @Tag) in package `com.universalshield.api`
2. **Purpose:** Downloadable reports for a single scan (owner only).
3. **Key contents:** Endpoints: `GET /api/reports/{id}`; Public methods: `report()`
4. **Depends on:** `AppUserPrincipal`, `BadRequestException`, `ReportService`, `ScanResponse`, `ScanService`
5. **Used by:** Spring (picked up automatically by component scanning or as a configuration file).
6. **Tests:** `ReportIntegrationTest (over HTTP)`, `ScanApiIntegrationTest (over HTTP)`
7. **Change notes:** Keep endpoints thin: validate, call a service, wrap in `ApiResponse`. Document new endpoints in docs/API.md.

### `src/main/java/com/universalshield/api/ScanController.java`

1. **Type and location:** Java class (@RestController, @Tag) in package `com.universalshield.api`
2. **Purpose:** Create, list, view and delete the signed-in user's scans.
3. **Key contents:** Endpoints: `GET /api/scans/{id}`, `DELETE /api/scans/{id}`, `POST /api/scans/app-report`, `POST /api/scans`, `GET /api/scans`, `DELETE /api/scans`; Public methods: `create()`, `list()`, `get()`, `delete()`, `deleteAll()`, `parseReport()`; Constants: `MAX_REPORT_BYTES`, `REPORT_EXTENSIONS`
4. **Depends on:** `ApiResponse`, `AppUserPrincipal`, `BadRequestException`, `PageResponse`, `PermissionReportParser`, `PermissionReportResponse`, `RiskLevel`, `ScanCategory`, `ScanRequest`, `ScanResponse`, `ScanService`, `ScanSummary`
5. **Used by:** Spring (picked up automatically by component scanning or as a configuration file).
6. **Tests:** `IntegrationTestBase (over HTTP)`, `PublicEndpointsIntegrationTest (over HTTP)`, `ScanApiIntegrationTest (over HTTP)`, `UserPrivacyIntegrationTest (over HTTP)`
7. **Change notes:** Keep endpoints thin: validate, call a service, wrap in `ApiResponse`. Document new endpoints in docs/API.md.

### `src/main/java/com/universalshield/auth/AuthController.java`

1. **Type and location:** Java class (@RestController, @Tag) in package `com.universalshield.auth`
2. **Purpose:** Authentication endpoints. Logout (POST /api/auth/logout) is handled by Spring Security's logout filter configured in SecurityConfig.
3. **Key contents:** Endpoints: `GET /api/auth/csrf`, `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me`; Public methods: `SessionUser()`, `csrf()`, `register()`, `login()`, `me()`
4. **Depends on:** `ApiResponse`, `AppUserPrincipal`, `AuthService`, `LoginRequest`, `RegisterRequest`
5. **Used by:** Spring (picked up automatically by component scanning or as a configuration file).
6. **Tests:** `AuthIntegrationTest (over HTTP)`, `CsrfCookieHttpTest (over HTTP)`, `IntegrationTestBase (over HTTP)`, `RateLimitTest (over HTTP)`, `UserPrivacyIntegrationTest (over HTTP)`
7. **Change notes:** Never log passwords; keep error messages identical for unknown e-mail and wrong password.

### `src/main/java/com/universalshield/auth/AuthService.java`

1. **Type and location:** Java class (@Service) in package `com.universalshield.auth`
2. **Purpose:** Registration and session login. Passwords are hashed with BCrypt (via Spring's delegating encoder); the session id is rotated on login to prevent session fixation.
3. **Key contents:** Public methods: `normalizeEmail()`, `register()`, `login()`
4. **Depends on:** `ApiException`, `AppProperties`, `AppUserPrincipal`, `AuditService`, `ConflictException`, `LoginRequest`, `RegisterRequest`, `User`, `UserPreference`, `UserPreferenceRepository`, `UserRepository`
5. **Used by:** `AuthController`, `DemoDataSeeder`
6. **Tests:** Covered indirectly by the Spring integration tests (`*IntegrationTest`), which run the whole application.
7. **Change notes:** Never log passwords; keep error messages identical for unknown e-mail and wrong password.

### `src/main/java/com/universalshield/domain/AuditEvent.java`

1. **Type and location:** Java class (@Entity) in package `com.universalshield.domain`
2. **Purpose:** Security-relevant event (registration, login success/failure, data deletion). Justified for detecting brute-force attempts and proving deletion requests were honoured.
3. **Key contents:** Public methods: `getUserId()`, `getEventType()`, `getDetail()`, `getCreatedAt()`
4. **Depends on:** No other project files.
5. **Used by:** `AuditEventRepository`, `AuditService`
6. **Tests:** Covered indirectly by the Spring integration tests (`*IntegrationTest`), which run the whole application.
7. **Change notes:** Entities must match the Flyway schema exactly (`ddl-auto: validate`). Schema changes need a new migration.

### `src/main/java/com/universalshield/domain/Auditable.java`

1. **Type and location:** Java class (@MappedSuperclass) in package `com.universalshield.domain`
2. **Purpose:** Adds created_at / updated_at columns maintained automatically by JPA callbacks.
3. **Key contents:** Public methods: `getCreatedAt()`, `setCreatedAt()`, `getUpdatedAt()`
4. **Depends on:** No other project files.
5. **Used by:** `Scan`, `User`, `UserPreference`
6. **Tests:** Covered indirectly by the Spring integration tests (`*IntegrationTest`), which run the whole application.
7. **Change notes:** Entities must match the Flyway schema exactly (`ddl-auto: validate`). Schema changes need a new migration.

### `src/main/java/com/universalshield/domain/DetectorMetadata.java`

1. **Type and location:** Java class (@Entity) in package `com.universalshield.domain`
2. **Purpose:** Record of each detector version that has run on this installation, so stored results can always be traced to the detector version that produced them.
3. **Key contents:** Public methods: `update()`, `getDetectorId()`, `getDisplayName()`, `getVersion()`, `getCategories()`, `getSourceType()`, `getDescription()`, `getUpdatedAt()`
4. **Depends on:** No other project files.
5. **Used by:** `DetectorCatalogService`, `DetectorMetadataRepository`
6. **Tests:** Covered indirectly by the Spring integration tests (`*IntegrationTest`), which run the whole application.
7. **Change notes:** Entities must match the Flyway schema exactly (`ddl-auto: validate`). Schema changes need a new migration.

### `src/main/java/com/universalshield/domain/RecommendationListConverter.java`

1. **Type and location:** Java class (@Converter) in package `com.universalshield.domain`
2. **Purpose:** Stores recommendations as a JSON array in a TEXT column.
3. **Key contents:** Public methods: `convertToDatabaseColumn()`, `convertToEntityAttribute()`; Constants: `MAPPER`
4. **Depends on:** `Recommendation`
5. **Used by:** `Scan`
6. **Tests:** Covered indirectly by the Spring integration tests (`*IntegrationTest`), which run the whole application.
7. **Change notes:** Entities must match the Flyway schema exactly (`ddl-auto: validate`). Schema changes need a new migration.

### `src/main/java/com/universalshield/domain/Scan.java`

1. **Type and location:** Java class (@Entity) in package `com.universalshield.domain`
2. **Purpose:** One stored screening result, owned by exactly one user.
3. **Key contents:** Public methods: `addIndicator()`, `getId()`, `getPublicId()`, `getUser()`, `getCategory()`, `getRiskScore()`, `getRiskLevel()`, `getEvidenceStrength()`, `getSources()`, `getRulesetVersion()`, `getDetectorVersions()`, `getInputExcerpt()` ...
4. **Depends on:** `Auditable`, `EvidenceStrength`, `Recommendation`, `RecommendationListConverter`, `RiskLevel`, `ScanCategory`, `ScanIndicator`, `StringListConverter`, `User`
5. **Used by:** `ScanIndicator`, `ScanMapper`, `ScanRepository`, `ScanService`
6. **Tests:** `PersistenceTest`, `ScanApiIntegrationTest`, `ScanServiceTest`
7. **Change notes:** Entities must match the Flyway schema exactly (`ddl-auto: validate`). Schema changes need a new migration.

### `src/main/java/com/universalshield/domain/ScanIndicator.java`

1. **Type and location:** Java class (@Entity) in package `com.universalshield.domain`
2. **Purpose:** One matched indicator belonging to a `Scan`. Deleted together with its scan.
3. **Key contents:** Public methods: `getCode()`, `getGroup()`, `getTitle()`, `getExplanation()`, `getWeight()`, `getDetectorId()`, `getSource()`, `getEvidence()`
4. **Depends on:** `ResultSource`, `Scan`
5. **Used by:** `Scan`, `ScanMapper`, `ScanService`
6. **Tests:** `PersistenceTest`
7. **Change notes:** Entities must match the Flyway schema exactly (`ddl-auto: validate`). Schema changes need a new migration.

### `src/main/java/com/universalshield/domain/StringListConverter.java`

1. **Type and location:** Java class (@Converter) in package `com.universalshield.domain`
2. **Purpose:** Stores a List&lt;String&gt; as a JSON array in a TEXT column (portable across H2 and PostgreSQL).
3. **Key contents:** Public methods: `convertToDatabaseColumn()`, `convertToEntityAttribute()`; Constants: `MAPPER`
4. **Depends on:** No other project files.
5. **Used by:** `Scan`
6. **Tests:** Covered indirectly by the Spring integration tests (`*IntegrationTest`), which run the whole application.
7. **Change notes:** Entities must match the Flyway schema exactly (`ddl-auto: validate`). Schema changes need a new migration.

### `src/main/java/com/universalshield/dto/ApiError.java`

1. **Type and location:** Java record in package `com.universalshield.dto`
2. **Purpose:** Error body. `fields` lists validation problems by field name; rejected values are deliberately never echoed back (they could contain sensitive data).
3. **Key contents:** Fields: `code`, `message`, `fields`; Public methods: `FieldProblem()`, `of()`
4. **Depends on:** No other project files.
5. **Used by:** `ApiResponse`, `GlobalExceptionHandler`, `RateLimitFilter`, `SecurityConfig`
6. **Tests:** Covered indirectly by the Spring integration tests (`*IntegrationTest`), which run the whole application.
7. **Change notes:** DTOs are the public API shape; changing a field is a breaking change for the frontend.

### `src/main/java/com/universalshield/dto/ApiResponse.java`

1. **Type and location:** Java record in package `com.universalshield.dto`
2. **Purpose:** Consistent envelope for every JSON response: {success, data, error, timestamp}.
3. **Key contents:** Fields: `success`, `data`, `error`, `timestamp`; Public methods: `ok()`, `fail()`
4. **Depends on:** `ApiError`
5. **Used by:** `AuthController`, `DashboardController`, `DetectorController`, `GlobalExceptionHandler`, `HealthController`, `RateLimitFilter`, `ScanController`, `SecurityConfig`, `UserController`
6. **Tests:** Covered indirectly by the Spring integration tests (`*IntegrationTest`), which run the whole application.
7. **Change notes:** DTOs are the public API shape; changing a field is a breaking change for the frontend.

### `src/main/java/com/universalshield/dto/DashboardSummary.java`

1. **Type and location:** Java record in package `com.universalshield.dto`
2. **Purpose:** Statistics computed live from the signed-in user's own stored scans.
3. **Key contents:** Fields: `totalScans`, `byCategory`, `byRiskLevel`, `recentScans`, `recentHighRisk`, `demoScans`, `oldestScanAt`, `retentionDays`, `statisticsNote`
4. **Depends on:** `ScanSummary`
5. **Used by:** `DashboardController`, `DashboardService`
6. **Tests:** Covered indirectly by the Spring integration tests (`*IntegrationTest`), which run the whole application.
7. **Change notes:** DTOs are the public API shape; changing a field is a breaking change for the frontend.

### `src/main/java/com/universalshield/dto/DeleteAccountRequest.java`

1. **Type and location:** Java record in package `com.universalshield.dto`
2. **Purpose:** Re-authentication is required before an account and all its data are deleted.
3. **Key contents:** Fields: `password`; Public methods: `toString()`
4. **Depends on:** No other project files.
5. **Used by:** `UserController`
6. **Tests:** Covered indirectly by the Spring integration tests (`*IntegrationTest`), which run the whole application.
7. **Change notes:** DTOs are the public API shape; changing a field is a breaking change for the frontend.

### `src/main/java/com/universalshield/dto/DetectorInfo.java`

1. **Type and location:** Java record in package `com.universalshield.dto`
2. **Purpose:** Public description of a detection module and whether it is currently operational.
3. **Key contents:** Fields: `id`, `displayName`, `version`, `categories`, `sourceType`, `available`, `description`
4. **Depends on:** No other project files.
5. **Used by:** `DetectorCatalogService`, `DetectorController`
6. **Tests:** Covered indirectly by the Spring integration tests (`*IntegrationTest`), which run the whole application.
7. **Change notes:** DTOs are the public API shape; changing a field is a breaking change for the frontend.

### `src/main/java/com/universalshield/dto/IndicatorDto.java`

1. **Type and location:** Java record in package `com.universalshield.dto`
2. **Purpose:** An indicator as returned by the API.
3. **Key contents:** Fields: `code`, `group`, `title`, `explanation`, `weight`, `detectorId`, `source`, `evidence`
4. **Depends on:** No other project files.
5. **Used by:** `ReportService`, `ScanMapper`, `ScanResponse`
6. **Tests:** Covered indirectly by the Spring integration tests (`*IntegrationTest`), which run the whole application.
7. **Change notes:** DTOs are the public API shape; changing a field is a breaking change for the frontend.

### `src/main/java/com/universalshield/dto/LoginRequest.java`

1. **Type and location:** Java record in package `com.universalshield.dto`
2. **Purpose:** Request body for login (e-mail and password).
3. **Key contents:** Fields: `email`, `password`; Public methods: `toString()`
4. **Depends on:** No other project files.
5. **Used by:** `AuthController`, `AuthService`
6. **Tests:** Covered indirectly by the Spring integration tests (`*IntegrationTest`), which run the whole application.
7. **Change notes:** DTOs are the public API shape; changing a field is a breaking change for the frontend.

### `src/main/java/com/universalshield/dto/PageResponse.java`

1. **Type and location:** Java record in package `com.universalshield.dto`
2. **Purpose:** Stable pagination envelope (page numbers start at 0).
3. **Key contents:** Fields: `items`, `page`, `size`, `totalItems`, `totalPages`; Public methods: `of()`
4. **Depends on:** No other project files.
5. **Used by:** `ScanController`, `ScanService`
6. **Tests:** Covered indirectly by the Spring integration tests (`*IntegrationTest`), which run the whole application.
7. **Change notes:** DTOs are the public API shape; changing a field is a breaking change for the frontend.

### `src/main/java/com/universalshield/dto/PermissionReportResponse.java`

1. **Type and location:** Java record in package `com.universalshield.dto`
2. **Purpose:** Permissions extracted from an uploaded report, for the user to review before scanning.
3. **Key contents:** Fields: `appName`, `permissions`
4. **Depends on:** No other project files.
5. **Used by:** `ScanController`
6. **Tests:** Covered indirectly by the Spring integration tests (`*IntegrationTest`), which run the whole application.
7. **Change notes:** DTOs are the public API shape; changing a field is a breaking change for the frontend.

### `src/main/java/com/universalshield/dto/PreferencesDto.java`

1. **Type and location:** Java record in package `com.universalshield.dto`
2. **Purpose:** Request/response body for a user's privacy preferences.
3. **Key contents:** Fields: `retentionDays`, `storeInputExcerpts`, `defaultCategory`
4. **Depends on:** No other project files.
5. **Used by:** `UserController`, `UserProfileResponse`, `UserService`
6. **Tests:** Covered indirectly by the Spring integration tests (`*IntegrationTest`), which run the whole application.
7. **Change notes:** DTOs are the public API shape; changing a field is a breaking change for the frontend.

### `src/main/java/com/universalshield/dto/RegisterRequest.java`

1. **Type and location:** Java record in package `com.universalshield.dto`
2. **Purpose:** Request body for registration with validation rules for e-mail, name and password strength.
3. **Key contents:** Fields: `email`, `displayName`, `password`; Public methods: `toString()`
4. **Depends on:** No other project files.
5. **Used by:** `AuthController`, `AuthService`, `DemoDataSeeder`
6. **Tests:** Covered indirectly by the Spring integration tests (`*IntegrationTest`), which run the whole application.
7. **Change notes:** DTOs are the public API shape; changing a field is a breaking change for the frontend.

### `src/main/java/com/universalshield/dto/ScanRequest.java`

1. **Type and location:** Java record in package `com.universalshield.dto`
2. **Purpose:** Body of POST /api/scans. Set `category` and the one matching input object, e.g.
3. **Key contents:** Fields: `category`, `message`, `url`, `transaction`, `call`, `app`
4. **Depends on:** `AppPermissionInput`, `CallInput`, `MessageInput`, `ScanCategory`, `TransactionInput`, `UrlInput`
5. **Used by:** `DemoDataSeeder`, `InputSanitizer`, `ScanController`, `ScanService`
6. **Tests:** `InputSanitizerTest`, `RetentionServiceIntegrationTest`, `ScanServiceTest`
7. **Change notes:** DTOs are the public API shape; changing a field is a breaking change for the frontend.

### `src/main/java/com/universalshield/dto/ScanResponse.java`

1. **Type and location:** Java record in package `com.universalshield.dto`
2. **Purpose:** Full, explainable scan result. Identical for POST /api/scans and GET /api/scans/{id}.
3. **Key contents:** Fields: `id`, `category`, `riskScore`, `riskLevel`, `evidenceStrength`, `reasons`, `indicators`, `recommendations`, `createdAt`, `rulesetVersion`, `detectorVersions`, `sources`, `notes`, `disclaimers`, `inputExcerpt`, `demo`, `scoringMethod`
4. **Depends on:** `IndicatorDto`, `Recommendation`
5. **Used by:** `ReportController`, `ReportService`, `ScanController`, `ScanMapper`, `ScanService`
6. **Tests:** Covered indirectly by the Spring integration tests (`*IntegrationTest`), which run the whole application.
7. **Change notes:** DTOs are the public API shape; changing a field is a breaking change for the frontend.

### `src/main/java/com/universalshield/dto/ScanSummary.java`

1. **Type and location:** Java record in package `com.universalshield.dto`
2. **Purpose:** Compact row used by the history list and dashboard.
3. **Key contents:** Fields: `id`, `category`, `riskScore`, `riskLevel`, `evidenceStrength`, `createdAt`, `excerpt`, `indicatorCount`, `topIndicator`, `demo`
4. **Depends on:** No other project files.
5. **Used by:** `DashboardSummary`, `ScanController`, `ScanMapper`, `ScanService`
6. **Tests:** Covered indirectly by the Spring integration tests (`*IntegrationTest`), which run the whole application.
7. **Change notes:** DTOs are the public API shape; changing a field is a breaking change for the frontend.

### `src/main/java/com/universalshield/dto/UserProfileResponse.java`

1. **Type and location:** Java record in package `com.universalshield.dto`
2. **Purpose:** What the API reveals about the signed-in user. Never includes the password hash.
3. **Key contents:** Fields: `email`, `displayName`, `memberSince`, `preferences`
4. **Depends on:** `PreferencesDto`
5. **Used by:** `UserController`, `UserService`
6. **Tests:** Covered indirectly by the Spring integration tests (`*IntegrationTest`), which run the whole application.
7. **Change notes:** DTOs are the public API shape; changing a field is a breaking change for the frontend.

### `src/main/java/com/universalshield/exception/ApiException.java`

1. **Type and location:** Java class in package `com.universalshield.exception`
2. **Purpose:** Base class for errors that map to a specific HTTP status and a stable error code.
3. **Key contents:** Public methods: `getStatus()`, `getCode()`
4. **Depends on:** No other project files.
5. **Used by:** `AuthService`, `BadRequestException`, `ConflictException`, `GlobalExceptionHandler`, `NotFoundException`, `UserService`
6. **Tests:** Covered indirectly by the Spring integration tests (`*IntegrationTest`), which run the whole application.
7. **Change notes:** Error codes are part of the API contract (docs/API.md). Never echo rejected values or stack traces.

### `src/main/java/com/universalshield/exception/BadRequestException.java`

1. **Type and location:** Java class in package `com.universalshield.exception`
2. **Purpose:** 400 for semantically invalid input that bean validation cannot express.
3. **Key contents:** See source.
4. **Depends on:** `ApiException`
5. **Used by:** `ReportController`, `ScanController`, `ScanService`, `UserService`
6. **Tests:** `ScanServiceTest`
7. **Change notes:** Error codes are part of the API contract (docs/API.md). Never echo rejected values or stack traces.

### `src/main/java/com/universalshield/exception/ConflictException.java`

1. **Type and location:** Java class in package `com.universalshield.exception`
2. **Purpose:** 409, e.g. registering an email that already exists.
3. **Key contents:** See source.
4. **Depends on:** `ApiException`
5. **Used by:** `AuthService`
6. **Tests:** Covered indirectly by the Spring integration tests (`*IntegrationTest`), which run the whole application.
7. **Change notes:** Error codes are part of the API contract (docs/API.md). Never echo rejected values or stack traces.

### `src/main/java/com/universalshield/exception/GlobalExceptionHandler.java`

1. **Type and location:** Java class (@RestControllerAdvice) in package `com.universalshield.exception`
2. **Purpose:** Converts every exception into the standard `ApiResponse` error envelope with an appropriate status code. Unexpected errors are logged by type only and return a generic message, so stack traces and input data never reach the client.
3. **Key contents:** Public methods: `api()`, `invalid()`, `constraint()`, `unreadable()`, `badParam()`, `tooLarge()`, `method()`, `mediaType()`, `noResource()`, `unauthenticated()`, `denied()`, `unexpected()`
4. **Depends on:** `ApiError`, `ApiException`, `ApiResponse`
5. **Used by:** Spring (picked up automatically by component scanning or as a configuration file).
6. **Tests:** Covered indirectly by the Spring integration tests (`*IntegrationTest`), which run the whole application.
7. **Change notes:** Error codes are part of the API contract (docs/API.md). Never echo rejected values or stack traces.

### `src/main/java/com/universalshield/exception/NotFoundException.java`

1. **Type and location:** Java class in package `com.universalshield.exception`
2. **Purpose:** 404. Also used when a resource exists but belongs to another user, so the API never reveals whether someone else's scan id is valid.
3. **Key contents:** See source.
4. **Depends on:** `ApiException`
5. **Used by:** `ScanService`, `UserService`
6. **Tests:** `ScanServiceTest`
7. **Change notes:** Error codes are part of the API contract (docs/API.md). Never echo rejected values or stack traces.

### `src/main/java/com/universalshield/reporting/ReportService.java`

1. **Type and location:** Java class (@Service) in package `com.universalshield.reporting`
2. **Purpose:** Generates downloadable reports. The HTML report is a self-contained file with every value HTML-escaped, so malicious text from a scanned message cannot execute when the report is opened.
3. **Key contents:** Public methods: `json()`, `html()`, `esc()`; Constants: `TS`
4. **Depends on:** `Indicator`, `IndicatorDto`, `Recommendation`, `ScanResponse`
5. **Used by:** `ReportController`
6. **Tests:** `ReportIntegrationTest`
7. **Change notes:** All values must stay HTML-escaped in the HTML report.

### `src/main/java/com/universalshield/repository/AuditEventRepository.java`

1. **Type and location:** Java interface (Spring Data repository) in package `com.universalshield.repository`
2. **Purpose:** Spring Data repository for `AuditEvent`, including deletion of old events.
3. **Key contents:** See source.
4. **Depends on:** `AuditEvent`
5. **Used by:** `AuditService`, `RetentionService`, `UserService`
6. **Tests:** `AuthIntegrationTest`, `UserPrivacyIntegrationTest`
7. **Change notes:** Every scan query must include the owner's `userId`; that is how ownership is enforced.

### `src/main/java/com/universalshield/repository/DetectorMetadataRepository.java`

1. **Type and location:** Java interface (Spring Data repository) in package `com.universalshield.repository`
2. **Purpose:** Spring Data repository for `DetectorMetadata` rows.
3. **Key contents:** See source.
4. **Depends on:** `DetectorMetadata`
5. **Used by:** `DetectorCatalogService`
6. **Tests:** Covered indirectly by the Spring integration tests (`*IntegrationTest`), which run the whole application.
7. **Change notes:** Every scan query must include the owner's `userId`; that is how ownership is enforced.

### `src/main/java/com/universalshield/repository/ScanRepository.java`

1. **Type and location:** Java interface (Spring Data repository) in package `com.universalshield.repository`
2. **Purpose:** Every query is scoped by user id so one user can never read another user's scans. All queries are parameterised (no string concatenation), preventing SQL injection.
3. **Key contents:** See source.
4. **Depends on:** `RiskLevel`, `Scan`, `ScanCategory`
5. **Used by:** `DashboardService`, `RetentionService`, `ScanHistoryService`, `ScanService`, `UserService`
6. **Tests:** `PersistenceTest`, `RetentionServiceIntegrationTest`, `ScanApiIntegrationTest`, `ScanServiceTest`
7. **Change notes:** Every scan query must include the owner's `userId`; that is how ownership is enforced.

### `src/main/java/com/universalshield/security/AppUserDetailsService.java`

1. **Type and location:** Java class (@Service) in package `com.universalshield.security`
2. **Purpose:** Loads users by email for Spring Security's password authentication.
3. **Key contents:** Public methods: `loadUserByUsername()`
4. **Depends on:** `AppUserPrincipal`, `UserRepository`
5. **Used by:** `SecurityConfig`
6. **Tests:** Covered through `AuthIntegrationTest`, `CsrfCookieHttpTest`, `RateLimitTest`, `PublicEndpointsIntegrationTest` and the ownership tests in `ScanApiIntegrationTest`.
7. **Change notes:** Security-sensitive: run the security and API integration tests after any change, and update SECURITY.md.

### `src/main/java/com/universalshield/security/AppUserPrincipal.java`

1. **Type and location:** Java class in package `com.universalshield.security`
2. **Purpose:** Authenticated user stored in the HTTP session's security context.
3. **Key contents:** Public methods: `getId()`, `getDisplayName()`, `toString()`
4. **Depends on:** No other project files.
5. **Used by:** `AppUserDetailsService`, `AuthController`, `AuthService`, `DashboardController`, `ReportController`, `ScanController`, `UserController`
6. **Tests:** Covered through `AuthIntegrationTest`, `CsrfCookieHttpTest`, `RateLimitTest`, `PublicEndpointsIntegrationTest` and the ownership tests in `ScanApiIntegrationTest`.
7. **Change notes:** Security-sensitive: run the security and API integration tests after any change, and update SECURITY.md.

### `src/main/java/com/universalshield/security/CsrfCookieFilter.java`

1. **Type and location:** Java class in package `com.universalshield.security`
2. **Purpose:** Spring Security loads CSRF tokens lazily. Touching the token on every request makes sure the XSRF-TOKEN cookie is always present for the JavaScript frontend to echo back.
3. **Key contents:** See source.
4. **Depends on:** No other project files.
5. **Used by:** `SecurityConfig`
6. **Tests:** Covered through `AuthIntegrationTest`, `CsrfCookieHttpTest`, `RateLimitTest`, `PublicEndpointsIntegrationTest` and the ownership tests in `ScanApiIntegrationTest`.
7. **Change notes:** Security-sensitive: run the security and API integration tests after any change, and update SECURITY.md.

### `src/main/java/com/universalshield/security/RateLimitFilter.java`

1. **Type and location:** Java class in package `com.universalshield.security`
2. **Purpose:** Limits login/registration attempts per client IP and scan submissions per user. Responds 429 with the standard error envelope when exceeded.
3. **Key contents:** See source.
4. **Depends on:** `ApiError`, `ApiResponse`, `AppProperties`, `RateLimiter`
5. **Used by:** `SecurityConfig`
6. **Tests:** Covered through `AuthIntegrationTest`, `CsrfCookieHttpTest`, `RateLimitTest`, `PublicEndpointsIntegrationTest` and the ownership tests in `ScanApiIntegrationTest`.
7. **Change notes:** Security-sensitive: run the security and API integration tests after any change, and update SECURITY.md.

### `src/main/java/com/universalshield/security/RateLimiter.java`

1. **Type and location:** Java class in package `com.universalshield.security`
2. **Purpose:** In-memory fixed-window rate limiter (one window per key per minute). Good enough for a single instance.
3. **Key contents:** Public methods: `tryAcquire()`, `reset()`; Constants: `WINDOW_MILLIS`, `MAX_KEYS`
4. **Depends on:** No other project files.
5. **Used by:** `RateLimitFilter`, `SecurityConfig`
6. **Tests:** `RateLimitTest`
7. **Change notes:** Security-sensitive: run the security and API integration tests after any change, and update SECURITY.md.

### `src/main/java/com/universalshield/security/SecurityConfig.java`

1. **Type and location:** Java class (@Configuration) in package `com.universalshield.security`
2. **Purpose:** Security configuration: Session-cookie authentication (HttpOnly, SameSite=Lax, Secure in production). CSRF protection with the double-submit cookie pattern for the JavaScript frontend.
3. **Key contents:** Public methods: `passwordEncoder()`, `authenticationManager()`, `securityContextRepository()`, `rateLimiter()`, `filterChain()`, `corsConfigurationSource()`; Constants: `CSP`
4. **Depends on:** `ApiError`, `ApiResponse`, `AppProperties`, `AppUserDetailsService`, `CsrfCookieFilter`, `RateLimitFilter`, `RateLimiter`
5. **Used by:** Spring (picked up automatically by component scanning or as a configuration file).
6. **Tests:** Covered through `AuthIntegrationTest`, `CsrfCookieHttpTest`, `RateLimitTest`, `PublicEndpointsIntegrationTest` and the ownership tests in `ScanApiIntegrationTest`.
7. **Change notes:** Security-sensitive: run the security and API integration tests after any change, and update SECURITY.md.

### `src/main/java/com/universalshield/security/SubjectHasher.java`

1. **Type and location:** Java class (@Component) in package `com.universalshield.security`
2. **Purpose:** Keyed HMAC-SHA256 of identifiers such as a recipient, domain, phone number or IP address. Lets the app recognise "you scanned this before" without storing the identifier itself.
3. **Key contents:** Public methods: `hash()`
4. **Depends on:** `AppProperties`
5. **Used by:** `AuditService`, `ScanService`
6. **Tests:** `ScanServiceTest`
7. **Change notes:** Security-sensitive: run the security and API integration tests after any change, and update SECURITY.md.

### `src/main/java/com/universalshield/service/AuditService.java`

1. **Type and location:** Java class (@Service) in package `com.universalshield.service`
2. **Purpose:** Records security-relevant events. Never stores passwords or scan content; IPs are hashed.
3. **Key contents:** Public methods: `record()`; Constants: `REGISTERED`, `LOGIN_SUCCESS`, `LOGIN_FAILURE`, `HISTORY_DELETED`, `ACCOUNT_DELETED`
4. **Depends on:** `AuditEvent`, `AuditEventRepository`, `SubjectHasher`
5. **Used by:** `AuthService`, `UserService`
6. **Tests:** Covered indirectly by the Spring integration tests (`*IntegrationTest`), which run the whole application.
7. **Change notes:** Transactional business logic. Keep controllers thin and put rules here.

### `src/main/java/com/universalshield/service/DashboardService.java`

1. **Type and location:** Java class (@Service) in package `com.universalshield.service`
2. **Purpose:** Builds dashboard statistics from the user's own stored scans. No hardcoded or global numbers.
3. **Key contents:** Public methods: `summary()`; Constants: `NOTE`
4. **Depends on:** `DashboardSummary`, `RiskLevel`, `ScanCategory`, `ScanMapper`, `ScanRepository`, `UserPreference`, `UserPreferenceRepository`
5. **Used by:** `DashboardController`
6. **Tests:** Covered indirectly by the Spring integration tests (`*IntegrationTest`), which run the whole application.
7. **Change notes:** Transactional business logic. Keep controllers thin and put rules here.

### `src/main/java/com/universalshield/service/DetectorCatalogService.java`

1. **Type and location:** Java class (@Service) in package `com.universalshield.service`
2. **Purpose:** Lists detectors and records their current versions in the detector_metadata table at startup.
3. **Key contents:** Public methods: `syncMetadata()`, `list()`
4. **Depends on:** `DetectionModule`, `DetectorInfo`, `DetectorMetadata`, `DetectorMetadataRepository`, `RiskEngine`
5. **Used by:** `DetectorController`
6. **Tests:** Covered indirectly by the Spring integration tests (`*IntegrationTest`), which run the whole application.
7. **Change notes:** Transactional business logic. Keep controllers thin and put rules here.

### `src/main/java/com/universalshield/service/InputSanitizer.java`

1. **Type and location:** Java class (@Component) in package `com.universalshield.service`
2. **Purpose:** Removes sensitive values before anything is stored. Detectors analyse the original text in memory; only the sanitised excerpt is persisted, and only if the user allows it.
3. **Key contents:** Public methods: `sanitize()`, `excerpt()`; Constants: `EXCERPT_MAX`, `CARD`, `SECRET_AFTER_KEYWORD`, `LONG_DIGITS`
4. **Depends on:** `ScanRequest`
5. **Used by:** `ScanService`
6. **Tests:** `InputSanitizerTest`, `ScanServiceTest`
7. **Change notes:** Transactional business logic. Keep controllers thin and put rules here.

### `src/main/java/com/universalshield/service/RetentionService.java`

1. **Type and location:** Java class (@Service) in package `com.universalshield.service`
2. **Purpose:** Enforces each user's chosen retention period by deleting older scans, and removes audit events older than a year. Runs daily (universalshield.retention.cron).
3. **Key contents:** Public methods: `purgeExpired()`; Constants: `AUDIT_RETENTION`
4. **Depends on:** `AuditEventRepository`, `ScanRepository`, `UserPreference`, `UserPreferenceRepository`
5. **Used by:** Spring (picked up automatically by component scanning or as a configuration file).
6. **Tests:** `RetentionServiceIntegrationTest`
7. **Change notes:** Transactional business logic. Keep controllers thin and put rules here.

### `src/main/java/com/universalshield/service/ScanHistoryService.java`

1. **Type and location:** Java class (@Service) in package `com.universalshield.service`
2. **Purpose:** Builds the `ScanHistory` context from the user's own earlier scans only.
3. **Key contents:** Public methods: `forUser()`; Constants: `MAX_AMOUNTS`, `HIGH`
4. **Depends on:** `RiskLevel`, `ScanCategory`, `ScanHistory`, `ScanRepository`
5. **Used by:** `ScanService`
6. **Tests:** `ScanServiceTest`
7. **Change notes:** Transactional business logic. Keep controllers thin and put rules here.

### `src/main/java/com/universalshield/service/ScanMapper.java`

1. **Type and location:** Java class (@Component) in package `com.universalshield.service`
2. **Purpose:** Converts persisted scans into API DTOs.
3. **Key contents:** Public methods: `toResponse()`, `toSummary()`; Constants: `SCORING_METHOD`
4. **Depends on:** `IndicatorDto`, `Scan`, `ScanIndicator`, `ScanResponse`, `ScanSummary`
5. **Used by:** `DashboardService`, `ScanService`
6. **Tests:** `ScanServiceTest`
7. **Change notes:** Transactional business logic. Keep controllers thin and put rules here.

### `src/main/java/com/universalshield/service/ScanService.java`

1. **Type and location:** Java class (@Service) in package `com.universalshield.service`
2. **Purpose:** Orchestrates a scan: validates the request shape, gathers the user's history, runs the `RiskEngine`, stores a privacy-preserving record and returns the explainable result. Every read and delete is scoped to the requesting user's id.
3. **Key contents:** Public methods: `create()`, `createEntity()`, `get()`, `find()`, `list()`, `delete()`, `deleteAll()`; Constants: `MAX_PAGE_SIZE`
4. **Depends on:** `BadRequestException`, `FraudEvent`, `FraudResult`, `Indicator`, `InputSanitizer`, `NotFoundException`, `PageResponse`, `ParsedUrl`, `ResultSource`, `RiskEngine`, `RiskLevel`, `Scan`, `ScanCategory`, `ScanHistory`, `ScanHistoryService`, `ScanIndicator`, `ScanMapper`, `ScanRepository`, `ScanRequest`, `ScanResponse`, `ScanSummary`, `SubjectHasher`, `User`, `UserPreference`, `UserPreferenceRepository`, `UserRepository`
5. **Used by:** `DemoDataSeeder`, `ReportController`, `ScanController`
6. **Tests:** `RetentionServiceIntegrationTest`, `ScanServiceTest`
7. **Change notes:** Transactional business logic. Keep controllers thin and put rules here.

### `src/main/java/com/universalshield/user/User.java`

1. **Type and location:** Java class (@Entity) in package `com.universalshield.user`
2. **Purpose:** A registered account. The password is only ever stored as a BCrypt hash.
3. **Key contents:** Public methods: `getId()`, `getEmail()`, `getDisplayName()`, `getPasswordHash()`, `getRole()`, `isEnabled()`, `setDisplayName()`, `toString()`
4. **Depends on:** `Auditable`
5. **Used by:** `AuthService`, `DemoDataSeeder`, `Scan`, `ScanService`, `UserRepository`, `UserService`
6. **Tests:** `IntegrationTestBase`, `PersistenceTest`, `RetentionServiceIntegrationTest`
7. **Change notes:** Every query must be scoped to the current user's id.

### `src/main/java/com/universalshield/user/UserController.java`

1. **Type and location:** Java class (@RestController, @Tag) in package `com.universalshield.user`
2. **Purpose:** Signed-in user's profile, preferences and data deletion.
3. **Key contents:** Endpoints: `PUT /api/users/me/preferences`, `POST /api/users/me/delete-account`, `DELETE /api/users/me/history`, `GET /api/users/me`; Public methods: `profile()`, `preferences()`, `deleteAccount()`, `deleteHistory()`
4. **Depends on:** `ApiResponse`, `AppUserPrincipal`, `DeleteAccountRequest`, `PreferencesDto`, `UserProfileResponse`, `UserService`
5. **Used by:** Spring (picked up automatically by component scanning or as a configuration file).
6. **Tests:** `UserPrivacyIntegrationTest (over HTTP)`
7. **Change notes:** Every query must be scoped to the current user's id.

### `src/main/java/com/universalshield/user/UserPreference.java`

1. **Type and location:** Java class (@Entity) in package `com.universalshield.user`
2. **Purpose:** Per-user privacy and convenience settings. One row per user (user_id is the primary key).
3. **Key contents:** Public methods: `getUserId()`, `getRetentionDays()`, `isStoreInputExcerpts()`, `getDefaultCategory()`, `update()`
4. **Depends on:** `Auditable`
5. **Used by:** `AuthService`, `DashboardService`, `RetentionService`, `ScanService`, `UserPreferenceRepository`, `UserService`
6. **Tests:** `RetentionServiceIntegrationTest`
7. **Change notes:** Every query must be scoped to the current user's id.

### `src/main/java/com/universalshield/user/UserPreferenceRepository.java`

1. **Type and location:** Java interface (Spring Data repository) in package `com.universalshield.user`
2. **Purpose:** Spring Data repository for `UserPreference`.
3. **Key contents:** See source.
4. **Depends on:** `UserPreference`
5. **Used by:** `AuthService`, `DashboardService`, `RetentionService`, `ScanService`, `UserService`
6. **Tests:** `RetentionServiceIntegrationTest`, `ScanServiceTest`
7. **Change notes:** Every query must be scoped to the current user's id.

### `src/main/java/com/universalshield/user/UserRepository.java`

1. **Type and location:** Java interface (Spring Data repository) in package `com.universalshield.user`
2. **Purpose:** Spring Data repository for `User` (find by e-mail, case-insensitive checks).
3. **Key contents:** See source.
4. **Depends on:** `User`
5. **Used by:** `AppUserDetailsService`, `AuthService`, `DemoDataSeeder`, `ScanService`, `UserService`
6. **Tests:** `AuthIntegrationTest`, `PersistenceTest`, `RetentionServiceIntegrationTest`, `ScanServiceTest`, `UserPrivacyIntegrationTest`
7. **Change notes:** Every query must be scoped to the current user's id.

### `src/main/java/com/universalshield/user/UserService.java`

1. **Type and location:** Java class (@Service) in package `com.universalshield.user`
2. **Purpose:** Profile, preferences and the "delete my data" flows.
3. **Key contents:** Public methods: `profile()`, `updatePreferences()`, `deleteHistory()`, `deleteAccount()`
4. **Depends on:** `ApiException`, `AppProperties`, `AuditEventRepository`, `AuditService`, `BadRequestException`, `NotFoundException`, `PreferencesDto`, `ScanRepository`, `User`, `UserPreference`, `UserPreferenceRepository`, `UserProfileResponse`, `UserRepository`
5. **Used by:** `UserController`
6. **Tests:** Covered indirectly by the Spring integration tests (`*IntegrationTest`), which run the whole application.
7. **Change notes:** Every query must be scoped to the current user's id.

## Resources: configuration and database

### `src/main/resources/application-dev.yml`

1. **Type and location:** YAML configuration at `src/main/resources/application-dev.yml`
2. **Purpose:** Development profile: no PostgreSQL needed.
3. **Key contents:** H2 file database `./data/devdb` in PostgreSQL mode, demo seed on with `DEMO_PASSWORD`, API docs public, debug-friendly logging.
4. **Depends on:** No other project files.
5. **Used by:** Spring (picked up automatically by component scanning or as a configuration file).
6. **Tests:** Used by the UI smoke test run against the dev profile.
7. **Change notes:** Never use this profile on a public server (known demo password).

### `src/main/resources/application-prod.yml`

1. **Type and location:** YAML configuration at `src/main/resources/application-prod.yml`
2. **Purpose:** Production profile template.
3. **Key contents:** Datasource strictly from env, pool size, forward-headers strategy for TLS-terminating proxies, secure cookie, API docs private, demo seed off.
4. **Depends on:** No other project files.
5. **Used by:** Spring (picked up automatically by component scanning or as a configuration file).
6. **Tests:** Not covered by the automated suite; verified manually by running the prod profile on PostgreSQL 16 (see docs/TESTING.md). `ProductionSafetyCheckTest` covers the secret check.
7. **Change notes:** Secrets only via environment variables. `ProductionSafetyCheck` enforces a strong `APP_HASH_SECRET`.

### `src/main/resources/application-test.yml`

1. **Type and location:** YAML configuration at `src/main/resources/application-test.yml`
2. **Purpose:** Profile used by automated tests.
3. **Key contents:** In-memory H2, ML disabled, rate limiting disabled, demo seed off, fixed hash secret.
4. **Depends on:** No other project files.
5. **Used by:** Spring (picked up automatically by component scanning or as a configuration file).
6. **Tests:** Loaded by every Spring integration test (`*IntegrationTest`, `PersistenceTest`), which fail if configuration or schema is wrong.
7. **Change notes:** Tests that need rate limiting or ML enable it explicitly.

### `src/main/resources/application.yml`

1. **Type and location:** YAML configuration at `src/main/resources/application.yml`
2. **Purpose:** Base configuration shared by all profiles.
3. **Key contents:** Default profile `dev`; datasource from env; JPA `validate`; Flyway; 64 KB upload limit; Jackson ISO dates; server port and error settings (no stack traces); session cookie flags (HttpOnly, SameSite=Lax, 30 min timeout); springdoc paths; all `universalshield.*` settings with env-var overrides.
4. **Depends on:** No other project files.
5. **Used by:** Spring (picked up automatically by component scanning or as a configuration file).
6. **Tests:** Loaded by every Spring integration test (`*IntegrationTest`, `PersistenceTest`), which fail if configuration or schema is wrong.
7. **Change notes:** Every property should have a safe default or an env var. Bound to `AppProperties`; unknown keys are ignored, typos are silent, so double-check names.

### `src/main/resources/db/migration/V1__initial_schema.sql`

1. **Type and location:** SQL migration at `src/main/resources/db/migration/V1__initial_schema.sql`
2. **Purpose:** Flyway migration that creates the whole schema.
3. **Key contents:** Tables users, user_preferences, scans, scan_indicators, detector_metadata, audit_events with FKs, CHECK constraints and indexes. Portable to H2 and PostgreSQL.
4. **Depends on:** No other project files.
5. **Used by:** Spring (picked up automatically by component scanning or as a configuration file).
6. **Tests:** Loaded by every Spring integration test (`*IntegrationTest`, `PersistenceTest`), which fail if configuration or schema is wrong.
7. **Change notes:** Never edit after it has run on a real database; add `V2__...sql`. Entities must match or Hibernate validation fails at startup.

## Frontend (static)

### `src/main/resources/static/favicon.svg`

1. **Type and location:** SVG image at `src/main/resources/static/favicon.svg`
2. **Purpose:** Browser tab icon.
3. **Key contents:** Small shield SVG in the brand colours.
4. **Depends on:** No other project files.
5. **Used by:** `index.html`
6. **Tests:** Exercised in a real browser by `ui-smoke.mjs`; `PublicEndpointsIntegrationTest` checks the frontend is served with security headers.
7. **Change notes:** Static asset.

### `src/main/resources/static/index.html`

1. **Type and location:** HTML page at `src/main/resources/static/index.html`
2. **Purpose:** The single HTML page of the SPA.
3. **Key contents:** Header with navigation, `<main id="app">` mount point, footer, toast region, native `<dialog>` for confirmations; loads `js/app.js` as a module and `css/main.css`. No inline scripts (CSP `script-src 'self'`).
4. **Depends on:** No other project files.
5. **Used by:** Served to the browser.
6. **Tests:** Exercised in a real browser by `ui-smoke.mjs`; `PublicEndpointsIntegrationTest` checks the frontend is served with security headers.
7. **Change notes:** Do not add inline `<script>` or `onclick=` attributes; they would be blocked by the CSP.

### `src/main/resources/static/assets/logo.svg`

1. **Type and location:** SVG image at `src/main/resources/static/assets/logo.svg`
2. **Purpose:** Logo shown in the header and landing page.
3. **Key contents:** Shield mark SVG.
4. **Depends on:** No other project files.
5. **Used by:** `index.html, pages`
6. **Tests:** Exercised in a real browser by `ui-smoke.mjs`; `PublicEndpointsIntegrationTest` checks the frontend is served with security headers.
7. **Change notes:** Static asset.

### `src/main/resources/static/css/main.css`

1. **Type and location:** Stylesheet at `src/main/resources/static/css/main.css`
2. **Purpose:** All styling: dark navy theme with cyan accents, responsive layout.
3. **Key contents:** Design tokens on `:root`, layout, header/nav (mobile toggle), cards, forms, buttons, tabs, risk badges (colour + text), gauge, bar charts, tables, dialog, toasts, print styles, `prefers-reduced-motion`.
4. **Depends on:** No other project files.
5. **Used by:** `index.html`
6. **Tests:** Exercised in a real browser by `ui-smoke.mjs`; `PublicEndpointsIntegrationTest` checks the frontend is served with security headers.
7. **Change notes:** Risk colours are always paired with text labels; keep contrast at WCAG AA when changing tokens.

### `src/main/resources/static/js/api.js`

1. **Type and location:** JavaScript ES module at `src/main/resources/static/js/api.js`
2. **Purpose:** The only module that talks to the backend.
3. **Key contents:** `request()` wrapper around `fetch` (same-origin cookies, JSON, CSRF header, envelope unwrapping, `ApiError`), `ensureCsrf()`, and the `api` object with one method per endpoint.
4. **Depends on:** No other project files.
5. **Used by:** `about.js`, `app.js`, `auth.js`, `dashboard.js`, `history.js`, `result.js`, `scanner.js`, `settings.js`
6. **Tests:** Exercised in a real browser by `ui-smoke.mjs`; `PublicEndpointsIntegrationTest` checks the frontend is served with security headers.
7. **Change notes:** Add new endpoints to the `api` object instead of calling `fetch` elsewhere.

### `src/main/resources/static/js/app.js`

1. **Type and location:** JavaScript ES module at `src/main/resources/static/js/app.js`
2. **Purpose:** SPA entry point and hash router.
3. **Key contents:** Route table (public vs. signed-in pages), `navigate()`, route guard redirecting to `/login`, navigation rendering, logout, global error handling.
4. **Depends on:** `api.js`, `dom.js`, `state.js`, `landing.js`, `auth.js`, `dashboard.js`, `scanner.js`, `result.js`, `history.js`, `settings.js`, `about.js`
5. **Used by:** `index.html`
6. **Tests:** Exercised in a real browser by `ui-smoke.mjs`; `PublicEndpointsIntegrationTest` checks the frontend is served with security headers.
7. **Change notes:** Add a page by importing it and adding a route; mark it `auth: true` if it needs a login.

### `src/main/resources/static/js/charts.js`

1. **Type and location:** JavaScript ES module at `src/main/resources/static/js/charts.js`
2. **Purpose:** Draws the dashboard charts from real API counts.
3. **Key contents:** Accessible horizontal bar charts (SVG-free, CSS bars) with text labels and values, plus a risk gauge; empty state when there is no data.
4. **Depends on:** `dom.js`, `format.js`
5. **Used by:** `dashboard.js`, `result.js`
6. **Tests:** Exercised in a real browser by `ui-smoke.mjs`; `PublicEndpointsIntegrationTest` checks the frontend is served with security headers.
7. **Change notes:** Never draw placeholder data. Colours come from CSS tokens and are always labelled.

### `src/main/resources/static/js/dom.js`

1. **Type and location:** JavaScript ES module at `src/main/resources/static/js/dom.js`
2. **Purpose:** Tiny DOM helpers that make XSS-safe rendering the default.
3. **Key contents:** `h(tag, attrs, ...children)` builds elements using `textContent` for strings, `clear()`, `toast()`, `confirmDialog()` using the native `<dialog>`.
4. **Depends on:** No other project files.
5. **Used by:** `about.js`, `app.js`, `auth.js`, `charts.js`, `dashboard.js`, `history.js`, `landing.js`, `result.js`, `scanner.js`, `settings.js`
6. **Tests:** Exercised in a real browser by `ui-smoke.mjs`; `PublicEndpointsIntegrationTest` checks the frontend is served with security headers.
7. **Change notes:** Never add an innerHTML path here; all pages rely on this being safe.

### `src/main/resources/static/js/format.js`

1. **Type and location:** JavaScript ES module at `src/main/resources/static/js/format.js`
2. **Purpose:** Formatting helpers and display labels.
3. **Key contents:** `CATEGORY_LABELS`, level/evidence labels and descriptions, date formatting, `truncate`, distribution percentages from real counts, history query-string builder.
4. **Depends on:** No other project files.
5. **Used by:** `charts.js`, `dashboard.js`, `history.js`, `result.js`, `scanner.js`, `settings.js`
6. **Tests:** `format.test.mjs`
7. **Change notes:** Pure functions; covered by `format.test.mjs`.

### `src/main/resources/static/js/state.js`

1. **Type and location:** JavaScript ES module at `src/main/resources/static/js/state.js`
2. **Purpose:** Small client-side session state.
3. **Key contents:** Current user cache, `setUser`/`getUser`, listeners to refresh the navigation on login/logout.
4. **Depends on:** No other project files.
5. **Used by:** `app.js`, `auth.js`, `dashboard.js`, `landing.js`, `settings.js`
6. **Tests:** Exercised in a real browser by `ui-smoke.mjs`; `PublicEndpointsIntegrationTest` checks the frontend is served with security headers.
7. **Change notes:** Holds no secrets; the session cookie is HttpOnly and not readable by JS.

### `src/main/resources/static/js/validation.js`

1. **Type and location:** JavaScript ES module at `src/main/resources/static/js/validation.js`
2. **Purpose:** Client-side validation that mirrors the server rules.
3. **Key contents:** `validateEmail`, `validatePassword`, `validateRegistration`, `validateScan` per category, `parsePermissionList`, `looksLikeCardNumber`, `luhn`, shared `LIMITS`.
4. **Depends on:** No other project files.
5. **Used by:** `auth.js`, `scanner.js`
6. **Tests:** `validation.test.mjs`
7. **Change notes:** Convenience only: the server validates again. Keep limits equal to the Jakarta annotations.

### `src/main/resources/static/js/pages/about.js`

1. **Type and location:** JavaScript ES module at `src/main/resources/static/js/pages/about.js`
2. **Purpose:** About and methodology page.
3. **Key contents:** Detector list with versions and availability, formula, thresholds, ML status, limitations, disclaimer.
4. **Depends on:** `api.js`, `dom.js`
5. **Used by:** `app.js`
6. **Tests:** Exercised in a real browser by `ui-smoke.mjs`; `PublicEndpointsIntegrationTest` checks the frontend is served with security headers.
7. **Change notes:** Data comes from `/api/detectors` and `/api/methodology`, so it updates automatically.

### `src/main/resources/static/js/pages/auth.js`

1. **Type and location:** JavaScript ES module at `src/main/resources/static/js/pages/auth.js`
2. **Purpose:** Login and registration pages.
3. **Key contents:** Forms with client validation, server error display per field, redirect to the dashboard after success.
4. **Depends on:** `api.js`, `dom.js`, `state.js`, `validation.js`
5. **Used by:** `app.js`
6. **Tests:** Exercised in a real browser by `ui-smoke.mjs`; `PublicEndpointsIntegrationTest` checks the frontend is served with security headers.
7. **Change notes:** Never log or store the password client-side.

### `src/main/resources/static/js/pages/dashboard.js`

1. **Type and location:** JavaScript ES module at `src/main/resources/static/js/pages/dashboard.js`
2. **Purpose:** Dashboard page.
3. **Key contents:** Totals, risk-level and category charts from `/api/dashboard/summary`, recent and recent high-risk scans, demo-data notice, empty state.
4. **Depends on:** `api.js`, `dom.js`, `charts.js`, `format.js`, `state.js`
5. **Used by:** `app.js`
6. **Tests:** Exercised in a real browser by `ui-smoke.mjs`; `PublicEndpointsIntegrationTest` checks the frontend is served with security headers.
7. **Change notes:** Only real counts are shown.

### `src/main/resources/static/js/pages/history.js`

1. **Type and location:** JavaScript ES module at `src/main/resources/static/js/pages/history.js`
2. **Purpose:** Scan history page.
3. **Key contents:** Filters (category, level, dates, search), pagination, table of summaries, open/delete with confirmation, delete-all.
4. **Depends on:** `api.js`, `dom.js`, `format.js`
5. **Used by:** `app.js`
6. **Tests:** Exercised in a real browser by `ui-smoke.mjs`; `PublicEndpointsIntegrationTest` checks the frontend is served with security headers.
7. **Change notes:** Filter values are passed as query parameters built in `format.js`.

### `src/main/resources/static/js/pages/landing.js`

1. **Type and location:** JavaScript ES module at `src/main/resources/static/js/pages/landing.js`
2. **Purpose:** Landing page.
3. **Key contents:** Hero, the five fraud categories, how it works, what the score means, privacy summary, calls to action.
4. **Depends on:** `dom.js`, `state.js`
5. **Used by:** `app.js`
6. **Tests:** Exercised in a real browser by `ui-smoke.mjs`; `PublicEndpointsIntegrationTest` checks the frontend is served with security headers.
7. **Change notes:** Static content; keep claims consistent with docs.

### `src/main/resources/static/js/pages/result.js`

1. **Type and location:** JavaScript ES module at `src/main/resources/static/js/pages/result.js`
2. **Purpose:** Shows one scan result.
3. **Key contents:** Gauge, level badge, evidence strength, reasons, indicator table, recommendations, notes, disclaimers, versions, export buttons (HTML/JSON), delete.
4. **Depends on:** `api.js`, `dom.js`, `charts.js`, `format.js`
5. **Used by:** `app.js`
6. **Tests:** Exercised in a real browser by `ui-smoke.mjs`; `PublicEndpointsIntegrationTest` checks the frontend is served with security headers.
7. **Change notes:** Render every server value with `h()`/`textContent`, never as HTML.

### `src/main/resources/static/js/pages/scanner.js`

1. **Type and location:** JavaScript ES module at `src/main/resources/static/js/pages/scanner.js`
2. **Purpose:** The scanner page for all five categories.
3. **Key contents:** Category tabs, one form per category, synthetic example loader, permission file upload, `readValues`, `buildPayload`, submit handling with field errors, card-number warning.
4. **Depends on:** `api.js`, `dom.js`, `format.js`, `validation.js`
5. **Used by:** `app.js`
6. **Tests:** Exercised in a real browser by `ui-smoke.mjs`; `PublicEndpointsIntegrationTest` checks the frontend is served with security headers.
7. **Change notes:** New input fields need matching changes in `validation.js`, the input record and `ScanRequest`.

### `src/main/resources/static/js/pages/settings.js`

1. **Type and location:** JavaScript ES module at `src/main/resources/static/js/pages/settings.js`
2. **Purpose:** Settings and privacy page.
3. **Key contents:** Retention days, store-excerpts switch, default category, delete history, delete account with password confirmation.
4. **Depends on:** `api.js`, `dom.js`, `format.js`, `state.js`
5. **Used by:** `app.js`
6. **Tests:** Exercised in a real browser by `ui-smoke.mjs`; `PublicEndpointsIntegrationTest` checks the frontend is served with security headers.
7. **Change notes:** Destructive actions always go through `confirmDialog()`.

## Tests

### `src/test/e2e/ui-smoke.mjs`

1. **Type and location:** JavaScript ES module (Node) at `src/test/e2e/ui-smoke.mjs`
2. **Purpose:** Optional end-to-end UI smoke test in a real browser (Playwright).
3. **Key contents:** Cases: landing page renders; client-side validation shows errors; registration signs in and shows empty dashboard; benign URL → LOW; empty scan blocked with message; dashboard shows charts; history filter by level; history delete with confirmation; preferences saved; about page loads detectors; logout and route guard
4. **Depends on:** No other project files.
5. **Used by:** Run by hand against a running app.
6. **Tests:** This file is itself a test or test helper.
7. **Change notes:** Needs a running app and `npm i -D playwright`. Usage: `node src/test/e2e/ui-smoke.mjs http://localhost:8080`. Set `PW_CHROMIUM` to use an existing Chromium.

### `src/test/frontend/format.test.mjs`

1. **Type and location:** JavaScript ES module (Node) at `src/test/frontend/format.test.mjs`
2. **Purpose:** Unit tests for `format.js`.
3. **Key contents:** Cases: labels and descriptions; distribution computes percentages from real counts only; history params drop empty filters; truncate and dates
4. **Depends on:** `format.js`
5. **Used by:** Node's test runner (`node --test`).
6. **Tests:** This file is itself a test or test helper.
7. **Change notes:** Run with `node --test "src/test/frontend/*.test.mjs"`.

### `src/test/frontend/validation.test.mjs`

1. **Type and location:** JavaScript ES module (Node) at `src/test/frontend/validation.test.mjs`
2. **Purpose:** Unit tests for `validation.js` (Node's built-in test runner).
3. **Key contents:** Cases: email validation; password rules mirror the server; registration reports every invalid field and mismatched confirmation; scan validation per category; permission list parsing de-duplicates and splits on separators; card number detection uses Luhn
4. **Depends on:** `validation.js`
5. **Used by:** Node's test runner (`node --test`).
6. **Tests:** This file is itself a test or test helper.
7. **Change notes:** Run with `node --test "src/test/frontend/*.test.mjs"`.

### `src/test/java/com/universalshield/api/AuthIntegrationTest.java`

1. **Type and location:** Java class in package `com.universalshield.api`
2. **Purpose:** Integration tests (full Spring context, H2, MockMvc) for the auth area.
3. **Key contents:** Test methods: `registerHashesPasswordAndStartsSession`, `duplicateEmailIsConflict`, `weakPasswordAndBadEmailAreRejectedWithFieldErrors`, `loginSucceedsWithCorrectPasswordAndFailsGenerically`, `logoutEndsSession`, `loginWithoutCsrfTokenIsForbidden`, `csrfEndpointIssuesToken`, `malformedJsonIsBadRequest`
4. **Depends on:** `AuditEventRepository`, `UserRepository`
5. **Used by:** Maven Surefire (`mvnw test`) runs it.
6. **Tests:** This file is itself a test or test helper.
7. **Change notes:** Tests use the `test` profile (in-memory H2). Never weaken an assertion just to make a test pass; fix the code or document the behaviour change.

### `src/test/java/com/universalshield/api/IntegrationTestBase.java`

1. **Type and location:** Java class (@SpringBootTest) in package `com.universalshield.api`
2. **Purpose:** Boots the full application on an in-memory H2 database (test profile) with MockMvc.
3. **Key contents:** Test support code.
4. **Depends on:** `User`
5. **Used by:** Maven Surefire (`mvnw test`) runs it.
6. **Tests:** This file is itself a test or test helper.
7. **Change notes:** Tests use the `test` profile (in-memory H2). Never weaken an assertion just to make a test pass; fix the code or document the behaviour change.

### `src/test/java/com/universalshield/api/PublicEndpointsIntegrationTest.java`

1. **Type and location:** Java class in package `com.universalshield.api`
2. **Purpose:** Integration tests (full Spring context, H2, MockMvc) for the public endpoints area.
3. **Key contents:** Test methods: `healthReportsDatabaseUp`, `detectorsListsAllFiveHeuristicsAndUnavailableMl`, `methodologyExposesThresholdsAndMlStatus`, `openApiSpecIsServed`, `frontendIsServedWithSecurityHeaders`, `unknownApiPathRequiresAuthAndThen404s`, `wrongMethodIs405`
4. **Depends on:** No other project files.
5. **Used by:** Maven Surefire (`mvnw test`) runs it.
6. **Tests:** This file is itself a test or test helper.
7. **Change notes:** Tests use the `test` profile (in-memory H2). Never weaken an assertion just to make a test pass; fix the code or document the behaviour change.

### `src/test/java/com/universalshield/api/ReportIntegrationTest.java`

1. **Type and location:** Java class in package `com.universalshield.api`
2. **Purpose:** Integration tests (full Spring context, H2, MockMvc) for the report area.
3. **Key contents:** Test methods: `htmlReportIsDownloadableAndEscapesScannedContent`, `jsonReportContainsFullResult`, `unsupportedFormatAndUnknownIdAreErrors`, `escapeHandlesAllSpecialCharacters`
4. **Depends on:** `ReportService`
5. **Used by:** Maven Surefire (`mvnw test`) runs it.
6. **Tests:** This file is itself a test or test helper.
7. **Change notes:** Tests use the `test` profile (in-memory H2). Never weaken an assertion just to make a test pass; fix the code or document the behaviour change.

### `src/test/java/com/universalshield/api/ScanApiIntegrationTest.java`

1. **Type and location:** Java class in package `com.universalshield.api`
2. **Purpose:** Integration tests (full Spring context, H2, MockMvc) for the scan API area.
3. **Key contents:** Test methods: `everyDetectorFlowReturnsStructuredExplainableResult`, `benignInputsScoreLow`, `validationErrors`, `unauthenticatedRequestsGet401AndMissingCsrfGets403`, `usersCannotSeeOrDeleteEachOthersScans`, `historyDeletionSingleAndAll`, `listSupportsFiltersSearchAndPagination`, `sensitiveValuesAreMaskedBeforeStorage`, `repeatedHighRiskSubjectIsRecognisedFromHistory`, `permissionReportUploadIsParsedAndValidated`
4. **Depends on:** `Scan`, `ScanRepository`
5. **Used by:** Maven Surefire (`mvnw test`) runs it.
6. **Tests:** This file is itself a test or test helper.
7. **Change notes:** Tests use the `test` profile (in-memory H2). Never weaken an assertion just to make a test pass; fix the code or document the behaviour change.

### `src/test/java/com/universalshield/api/UserPrivacyIntegrationTest.java`

1. **Type and location:** Java class in package `com.universalshield.api`
2. **Purpose:** Integration tests (full Spring context, H2, MockMvc) for the user privacy area.
3. **Key contents:** Test methods: `profileNeverExposesPasswordHash`, `preferencesAreValidatedAndApplied`, `deleteHistoryKeepsAccount`, `deleteAccountRequiresPasswordAndRemovesEverything`, `dashboardSummaryReflectsOnlyOwnData`
4. **Depends on:** `AuditEventRepository`, `UserRepository`
5. **Used by:** Maven Surefire (`mvnw test`) runs it.
6. **Tests:** This file is itself a test or test helper.
7. **Change notes:** Tests use the `test` profile (in-memory H2). Never weaken an assertion just to make a test pass; fix the code or document the behaviour change.

### `src/test/java/com/universalshield/detectors/AppPermissionDetectorTest.java`

1. **Type and location:** Java class in package `com.universalshield.detectors`
2. **Purpose:** Automated tests for `AppPermissionDetector`.
3. **Key contents:** Test methods: `flashlightWithSmsAndAccessibilityIsBankingTrojanPattern`, `justifiedPermissionsGetReducedWeight`, `loanAppReadingContactsIsFlagged`, `normalPermissionsProduceNoIndicators`, `unknownPermissionsAreListedNotScored`, `duplicatePermissionsCountOnce`, `normalizeStripsPrefix`, `parserHandlesAaptTextJsonAndPlainLists`
4. **Depends on:** `AppPermissionDetector`, `AppPermissionInput`, `DetectorResult`, `FraudEvent`, `PermissionCatalog`, `PermissionReportParser`
5. **Used by:** Maven Surefire (`mvnw test`) runs it.
6. **Tests:** This file is itself a test or test helper.
7. **Change notes:** Tests use the `test` profile (in-memory H2). Never weaken an assertion just to make a test pass; fix the code or document the behaviour change.

### `src/test/java/com/universalshield/detectors/CallFraudDetectorTest.java`

1. **Type and location:** Java class in package `com.universalshield.detectors`
2. **Purpose:** Automated tests for `CallFraudDetector`.
3. **Key contents:** Test methods: `digitalArrestScriptIsDetected`, `fakeBankSupportAskingForRemoteAccessAndOtp`, `ordinaryCallHasNoIndicatorsAndHonestNotes`, `shortDescriptionIsInsufficient`, `shortNumberFormat`, `verifiedPhoneReputationIsExternal`, `knownRiskyCaller`
4. **Depends on:** `CallFraudDetector`, `CallInput`, `DetectorResult`, `FraudEvent`, `NotConfiguredPhoneReputationProvider`, `ReputationVerdict`, `ResultSource`, `ScanHistory`
5. **Used by:** Maven Surefire (`mvnw test`) runs it.
6. **Tests:** This file is itself a test or test helper.
7. **Change notes:** Tests use the `test` profile (in-memory H2). Never weaken an assertion just to make a test pass; fix the code or document the behaviour change.

### `src/test/java/com/universalshield/detectors/DetectorTestSupport.java`

1. **Type and location:** Java class in package `com.universalshield.detectors`
2. **Purpose:** Small helpers shared by the detector tests.
3. **Key contents:** Test support code.
4. **Depends on:** `DetectorResult`, `Indicator`
5. **Used by:** Maven Surefire (`mvnw test`) runs it.
6. **Tests:** This file is itself a test or test helper.
7. **Change notes:** Tests use the `test` profile (in-memory H2). Never weaken an assertion just to make a test pass; fix the code or document the behaviour change.

### `src/test/java/com/universalshield/detectors/MessageFraudDetectorTest.java`

1. **Type and location:** Java class in package `com.universalshield.detectors`
2. **Purpose:** Automated tests for `MessageFraudDetector`.
3. **Key contents:** Test methods: `appliesOnlyToMessages`, `phishingMessageMatchesMultipleTactics`, `prizeAndFeeLure`, `giftCardSecrecyAndRemoteAccess`, `ordinaryMessageHasNoIndicators`, `bankOtpNotificationIsNotFlaggedAsOtpRequest`, `veryShortMessageIsMarkedInsufficient`, `freeMailSenderClaimingToBeBankIsFlagged`, `senderBrandNotMentionedInBodyIsFlagged`, `priorHighRiskSenderAddsKnownRiskyIndicator`
4. **Depends on:** `FraudEvent`, `MessageFraudDetector`, `MessageInput`, `ScanCategory`, `ScanHistory`, `UrlInput`
5. **Used by:** Maven Surefire (`mvnw test`) runs it.
6. **Tests:** This file is itself a test or test helper.
7. **Change notes:** Tests use the `test` profile (in-memory H2). Never weaken an assertion just to make a test pass; fix the code or document the behaviour change.

### `src/test/java/com/universalshield/detectors/TextSignalsTest.java`

1. **Type and location:** Java class in package `com.universalshield.detectors`
2. **Purpose:** Automated tests for `TextSignals`.
3. **Key contents:** Test methods: `detectsOtpRequest`, `negatedOtpWarningFromBankIsNotARequest`, `overridesChangeWeight`, `evidenceMasksLongDigitRunsAndIsShortened`, `digitalArrestCallSignals`, `bankWarningNotToTellAnyoneTheOtpIsNotSecrecy`, `pinToReceiveMoneyWithPossessive`, `blankTextYieldsNothing`
4. **Depends on:** `TextSignals`
5. **Used by:** Maven Surefire (`mvnw test`) runs it.
6. **Tests:** This file is itself a test or test helper.
7. **Change notes:** Tests use the `test` profile (in-memory H2). Never weaken an assertion just to make a test pass; fix the code or document the behaviour change.

### `src/test/java/com/universalshield/detectors/TransactionFraudDetectorTest.java`

1. **Type and location:** Java class in package `com.universalshield.detectors`
2. **Purpose:** Automated tests for `TransactionFraudDetector`.
3. **Key contents:** Test methods: `receiveMoneyPinTrickIsDetected`, `amountFarAboveMedianHistory`, `amountModeratelyAboveMedianHistory`, `withoutEnoughHistoryUsesConfiguredLargeAmountAndSaysSo`, `repeatedAttemptsScaleWeight`, `giftCardAndCryptoMethods`, `knownRiskyRecipient`, `ordinaryPaymentHasNoIndicators`, `amountOnlyIsInsufficient`, `cardNumberInDescriptionTriggersWarningNote`
4. **Depends on:** `FraudEvent`, `ScanHistory`, `TransactionFraudDetector`, `TransactionInput`
5. **Used by:** Maven Surefire (`mvnw test`) runs it.
6. **Tests:** This file is itself a test or test helper.
7. **Change notes:** Tests use the `test` profile (in-memory H2). Never weaken an assertion just to make a test pass; fix the code or document the behaviour change.

### `src/test/java/com/universalshield/detectors/UrlRiskDetectorTest.java`

1. **Type and location:** Java class in package `com.universalshield.detectors`
2. **Purpose:** Automated tests for `UrlRiskDetector`.
3. **Key contents:** Test methods: `brandOnUnrelatedDomainWithLureWordsAndNoHttps`, `legitimateBrandDomainIsNotFlaggedAsDeceptive`, `httpsAloneNeverMeansSafeAndNoteSaysSo`, `ordinaryHyphenatedDomainsAreNotTyposquats`, `ipHostAndPort`, `userInfoTrickIsObfuscation`, `scriptSchemeIsCritical`, `punycodeShortenerAndDownload`, `deepSubdomainsAndOpenRedirect`, `ordinarySiteHasNoIndicators`, `malformedUrlIsReportedAsInsufficient`, `embeddedLinksInMessagesAreAnalysedWithPrefixedGroups`, `messagesWithoutLinksDoNotTriggerUrlDetector`, `verifiedReputationIsLabeledExternal`, `notConfiguredReputationIsStatedHonestly`, `previouslyHighRiskDomain`, `parsedUrlComputesRegistrableDomain`, `extractsAtMostFiveUrls`
4. **Depends on:** `DetectorResult`, `DomainReputationProvider`, `FraudEvent`, `MessageInput`, `NotConfiguredDomainReputationProvider`, `ParsedUrl`, `ReputationVerdict`, `ResultSource`, `ScanHistory`, `UrlInput`, `UrlRiskDetector`
5. **Used by:** Maven Surefire (`mvnw test`) runs it.
6. **Tests:** This file is itself a test or test helper.
7. **Change notes:** Tests use the `test` profile (in-memory H2). Never weaken an assertion just to make a test pass; fix the code or document the behaviour change.

### `src/test/java/com/universalshield/engine/RiskEngineTest.java`

1. **Type and location:** Java class in package `com.universalshield.engine`
2. **Purpose:** Configurable fake detector.
3. **Key contents:** Test methods: `producesCompleteStructuredResult`, `heuristicOnlyScoresAreCappedBelowCertainty`, `verifiedExternalEvidenceCanReach100AndRemovesNoExternalDisclaimer`, `lowResultCarriesNoGuaranteeDisclaimerAndLimitedEvidence`, `unavailableModulesAreSkipped`, `failsClearlyWhenNoDetectorApplies`, `indicatorsFromSeveralDetectorsAreMergedAndSortedByWeight`, `recommendationsAreDeduplicatedAndLimited`
4. **Depends on:** `DetectionModule`, `DetectorResult`, `EvidenceStrength`, `FraudEvent`, `FraudResult`, `Indicator`, `MessageInput`, `RecommendationCatalog`, `ResultSource`, `RiskEngine`, `RiskLevel`, `RiskThresholds`, `ScanCategory`
5. **Used by:** Maven Surefire (`mvnw test`) runs it.
6. **Tests:** This file is itself a test or test helper.
7. **Change notes:** Tests use the `test` profile (in-memory H2). Never weaken an assertion just to make a test pass; fix the code or document the behaviour change.

### `src/test/java/com/universalshield/engine/RiskLevelTest.java`

1. **Type and location:** Java class in package `com.universalshield.engine`
2. **Purpose:** Automated tests for `RiskLevel`.
3. **Key contents:** Test methods: `defaultThresholdBoundaries`, `customThresholdsAreRespected`, `scoresOutsideRangeAreRejected`, `inconsistentThresholdsAreRejected`, `indicatorWeightMustBeInRange`
4. **Depends on:** `Indicator`, `ResultSource`, `RiskLevel`, `RiskThresholds`
5. **Used by:** Maven Surefire (`mvnw test`) runs it.
6. **Tests:** This file is itself a test or test helper.
7. **Change notes:** Tests use the `test` profile (in-memory H2). Never weaken an assertion just to make a test pass; fix the code or document the behaviour change.

### `src/test/java/com/universalshield/engine/RiskScorerTest.java`

1. **Type and location:** Java class in package `com.universalshield.engine`
2. **Purpose:** Automated tests for `RiskScorer`.
3. **Key contents:** Test methods: `noIndicatorsScoresZero`, `singleIndicatorScoresItsWeight`, `correlatedIndicatorsInSameGroupAreNotDoubleCounted`, `independentGroupsCombineWithNoisyOr`, `scoreStaysWithinBoundsForManyStrongIndicators`, `manyWeakSignsHaveDiminishingReturns`, `evidenceStrengthCountsIndependentGroups`, `weakGroupsBelowMinimumDoNotCountAsEvidence`, `insufficientInputAlwaysGivesLimitedEvidence`
4. **Depends on:** `DetectorResult`, `EvidenceStrength`, `Indicator`, `ResultSource`, `RiskScorer`
5. **Used by:** Maven Surefire (`mvnw test`) runs it.
6. **Tests:** This file is itself a test or test helper.
7. **Change notes:** Tests use the `test` profile (in-memory H2). Never weaken an assertion just to make a test pass; fix the code or document the behaviour change.

### `src/test/java/com/universalshield/ml/MlPipelineTest.java`

1. **Type and location:** Java class in package `com.universalshield.ml`
2. **Purpose:** Automated tests for `MlPipeline`.
3. **Key contents:** Test methods: `loaderReadsQuotedCsvAndValidatesHeader`, `splitIsReproducibleStratifiedAndLeakFree`, `conflictingLabelsAreDropped`, `metricsMatchHandComputedValues`, `naiveBayesLearnsSeparableData`, `trainingCliWritesModelMetadataAndReportThatServiceCanLoad`, `missingOrDisabledModelIsReportedUnavailable`, `corruptModelFileIsRejectedSafely`
4. **Depends on:** `BinaryMetrics`, `DatasetLoader`, `DatasetSplitter`, `FraudEvent`, `LabeledText`, `MessageInput`, `MlModelService`, `MlTask`, `MlTextDetector`, `NaiveBayesModel`, `ResultSource`, `Tokenizer`, `TrainingCli`
5. **Used by:** Maven Surefire (`mvnw test`) runs it.
6. **Tests:** This file is itself a test or test helper.
7. **Change notes:** Tests use the `test` profile (in-memory H2). Never weaken an assertion just to make a test pass; fix the code or document the behaviour change.

### `src/test/java/com/universalshield/repository/PersistenceTest.java`

1. **Type and location:** Java class in package `com.universalshield.repository`
2. **Purpose:** Verifies the Flyway schema, JPA mappings, constraints and cascades on H2.
3. **Key contents:** Test methods: `scanRoundTripsWithJsonColumnsAndIndicators`, `deletingUserCascadesToScansAndIndicators`, `databaseConstraintsRejectInvalidData`, `riskScoreCheckConstraint`
4. **Depends on:** `EvidenceStrength`, `Recommendation`, `ResultSource`, `RiskLevel`, `Scan`, `ScanCategory`, `ScanIndicator`, `ScanRepository`, `User`, `UserRepository`
5. **Used by:** Maven Surefire (`mvnw test`) runs it.
6. **Tests:** This file is itself a test or test helper.
7. **Change notes:** Tests use the `test` profile (in-memory H2). Never weaken an assertion just to make a test pass; fix the code or document the behaviour change.

### `src/test/java/com/universalshield/security/CsrfCookieHttpTest.java`

1. **Type and location:** Java class (@SpringBootTest) in package `com.universalshield.security`
2. **Purpose:** End-to-end check over a real HTTP connection (embedded server on a random port) of the double-submit CSRF cookie and session-cookie flags the JavaScript frontend relies on.
3. **Key contents:** Test methods: `csrfCookieIsIssuedAndRequiredForStateChangingRequests`
4. **Depends on:** No other project files.
5. **Used by:** Maven Surefire (`mvnw test`) runs it.
6. **Tests:** This file is itself a test or test helper.
7. **Change notes:** Tests use the `test` profile (in-memory H2). Never weaken an assertion just to make a test pass; fix the code or document the behaviour change.

### `src/test/java/com/universalshield/security/ProductionSafetyCheckTest.java`

1. **Type and location:** Java class in package `com.universalshield.security`
2. **Purpose:** Automated tests for `ProductionSafetyCheck`.
3. **Key contents:** Test methods: `refusesDefaultOrShortSecretInProduction`
4. **Depends on:** `AppProperties`, `ProductionSafetyCheck`
5. **Used by:** Maven Surefire (`mvnw test`) runs it.
6. **Tests:** This file is itself a test or test helper.
7. **Change notes:** Tests use the `test` profile (in-memory H2). Never weaken an assertion just to make a test pass; fix the code or document the behaviour change.

### `src/test/java/com/universalshield/security/RateLimitTest.java`

1. **Type and location:** Java class in package `com.universalshield.security`
2. **Purpose:** Automated tests for `RateLimit`.
3. **Key contents:** Test methods: `limiterAllowsUpToLimitPerWindowThenResets`, `loginAttemptsAreLimitedWith429`
4. **Depends on:** `RateLimiter`
5. **Used by:** Maven Surefire (`mvnw test`) runs it.
6. **Tests:** This file is itself a test or test helper.
7. **Change notes:** Tests use the `test` profile (in-memory H2). Never weaken an assertion just to make a test pass; fix the code or document the behaviour change.

### `src/test/java/com/universalshield/service/InputSanitizerTest.java`

1. **Type and location:** Java class in package `com.universalshield.service`
2. **Purpose:** Automated tests for `InputSanitizer`.
3. **Key contents:** Test methods: `masksLuhnValidCardNumbers`, `masksCodesAfterKeywords`, `masksRemainingLongDigitRuns`, `excerptIsTruncatedAndSanitised`, `luhn`
4. **Depends on:** `InputSanitizer`, `MessageInput`, `ScanCategory`, `ScanRequest`, `TransactionInput`
5. **Used by:** Maven Surefire (`mvnw test`) runs it.
6. **Tests:** This file is itself a test or test helper.
7. **Change notes:** Tests use the `test` profile (in-memory H2). Never weaken an assertion just to make a test pass; fix the code or document the behaviour change.

### `src/test/java/com/universalshield/service/RetentionServiceIntegrationTest.java`

1. **Type and location:** Java class in package `com.universalshield.service`
2. **Purpose:** Integration tests (full Spring context, H2, MockMvc) for the retention service area.
3. **Key contents:** Test methods: `scansOlderThanUserRetentionArePurged`
4. **Depends on:** `MessageInput`, `RetentionService`, `ScanCategory`, `ScanRepository`, `ScanRequest`, `ScanService`, `User`, `UserPreference`, `UserPreferenceRepository`, `UserRepository`
5. **Used by:** Maven Surefire (`mvnw test`) runs it.
6. **Tests:** This file is itself a test or test helper.
7. **Change notes:** Tests use the `test` profile (in-memory H2). Never weaken an assertion just to make a test pass; fix the code or document the behaviour change.

### `src/test/java/com/universalshield/service/ScanServiceTest.java`

1. **Type and location:** Java class in package `com.universalshield.service`
2. **Purpose:** Automated tests for `ScanService`.
3. **Key contents:** Test methods: `rejectsCategoryWithoutMatchingInput`, `anotherUsersScanIsNotFound`, `listValidatesPagingDatesAndSearch`, `subjectsAreDerivedPerCategory`
4. **Depends on:** `BadRequestException`, `CallInput`, `InputSanitizer`, `MessageInput`, `NotFoundException`, `RiskEngine`, `Scan`, `ScanCategory`, `ScanHistoryService`, `ScanMapper`, `ScanRepository`, `ScanRequest`, `ScanService`, `SubjectHasher`, `UrlInput`, `UserPreferenceRepository`, `UserRepository`
5. **Used by:** Maven Surefire (`mvnw test`) runs it.
6. **Tests:** This file is itself a test or test helper.
7. **Change notes:** Tests use the `test` profile (in-memory H2). Never weaken an assertion just to make a test pass; fix the code or document the behaviour change.
