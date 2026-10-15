# UniversalShield

**One shield for your digital life.**

UniversalShield is a unified digital-safety platform that helps people evaluate suspicious activity
*before* they click a link, share information, install an app or send money. Five fraud detectors
feed one explainable **Risk Engine** that returns a 0-100 risk score, a risk level, a separate
evidence-strength rating, the exact warning signs that matched and concrete next steps.

| Module | What you enter | What it looks for |
|---|---|---|
| Message Fraud Detector | SMS, email, chat or social text | phishing language, impersonation, OTP/PIN requests, urgency, threats, fake prizes, payment demands |
| URL & Website Risk | a link | no HTTPS, look-alike/typosquat domains, brand misuse, deep subdomains, `@` tricks, punycode, shorteners, executable downloads |
| Transaction Screening | payment metadata | new recipient, amount vs. your own history, repeated attempts, gift card/crypto, "enter PIN to receive money" |
| Call Fraud Screening | your description of a call | fake police/bank callers, "digital arrest", remote-access requests, secrecy, urgent payment |
| App Permission Analyzer | permissions (typed or uploaded) | SMS, accessibility, overlay, notification, device-admin access, banking-trojan and predatory-loan patterns |

An optional, locally trained **Naive Bayes** model can add a capped supporting signal. Without a
trained model it is clearly shown as *Unavailable* and everything else works.

> **Honesty rules built into the product:** a HIGH score is never "fraud proven", a LOW score is never
> "safe", HTTPS is never treated as proof of safety, no accuracy figure is displayed that was not
> computed by the training pipeline, and every result states whether it came from local heuristics,
> a trained model or verified external data (none is configured by default).

## Technology

Java 17+ · Spring Boot 3.5 (Web, Validation, Security, Data JPA) · Hibernate · Flyway ·
PostgreSQL (production) / H2 (dev & tests) · springdoc OpenAPI · HTML5, CSS3, vanilla JavaScript ·
JUnit 5, Mockito, Spring Boot integration tests · Maven (wrapper included) · Docker & Docker Compose.

## Quick start on Windows (no database install needed)

Requirements: **JDK 17 or newer** (see [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md#1-install-java-on-windows)).
Maven is *not* required; the included wrapper downloads it.

```bat
cd UniversalShield
mvnw.cmd spring-boot:run
```

Open <http://localhost:8080>. The `dev` profile uses an embedded H2 database stored in
`data\devdb.mv.db` and creates a **demonstration account** with synthetic scans:

- Email: `demo@universalshield.local`
- Password: `ShieldDemo2026` (dev profile only; override with the `DEMO_PASSWORD` environment variable)

Or register your own account from the landing page.

## Common commands

| Task | Windows | macOS/Linux |
|---|---|---|
| Run (dev, H2) | `mvnw.cmd spring-boot:run` or `scripts\run-dev.cmd` | `./mvnw spring-boot:run` |
| Run all tests | `mvnw.cmd test` or `scripts\run-tests.cmd` | `./mvnw test` |
| Frontend unit tests (Node 18+) | `node --test "src/test/frontend/*.test.mjs"` | same |
| Build jar | `mvnw.cmd clean package` | `./mvnw clean package` |
| Run jar | `java -jar target\universalshield.jar` | `java -jar target/universalshield.jar` |
| Train optional ML models | `scripts\train-models.cmd` | see [docs/ML.md](docs/ML.md) |
| Production-like stack | `copy .env.example .env` then `docker compose up --build` | `cp .env.example .env && docker compose up --build` |

API documentation (dev): <http://localhost:8080/swagger-ui.html> · OpenAPI JSON: `/api/openapi`.

## Project layout

```
UniversalShield/
├── pom.xml, mvnw, mvnw.cmd, .mvn/      Maven build + wrapper
├── Dockerfile, docker-compose.yml       container build and PostgreSQL stack
├── .env.example                         every environment variable, documented
├── src/main/java/com/universalshield/
│   ├── engine/        RiskEngine, scoring, shared models (FraudEvent, DetectorResult, FraudResult…)
│   ├── detectors/     message, url, transaction, call, app (+ shared text signals)
│   ├── ml/            optional Naive Bayes model, loader, detector; ml/training = offline pipeline
│   ├── api/ auth/ user/   REST controllers
│   ├── service/ reporting/ repository/ domain/ dto/   business logic and persistence
│   ├── security/ config/ exception/   Spring Security, configuration, error handling
├── src/main/resources/
│   ├── application*.yml                 dev / test / prod profiles
│   ├── db/migration/                    Flyway migrations
│   └── static/                          the frontend (index.html, css/, js/, assets/)
├── src/test/                            JUnit tests, frontend tests, Playwright smoke script
├── data/samples/                        synthetic datasets and sample permission reports
├── models/                              where trained models are written (empty by default)
├── scripts/                             Windows .cmd and shell helper scripts
└── docs/                                architecture, API, database, deployment, testing, demo, file guide
```

## Documentation

| Document | Contents |
|---|---|
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | layers, components, extension points, design decisions |
| [docs/REQUEST_FLOW.md](docs/REQUEST_FLOW.md) | end-to-end flow of a scan, with Mermaid diagrams |
| [docs/SCORING.md](docs/SCORING.md) | the risk formula, weights, thresholds and how to calibrate them |
| [docs/ML.md](docs/ML.md) | the optional ML module: data schema, training, evaluation, limits |
| [docs/API.md](docs/API.md) | every endpoint with examples and error codes |
| [docs/DATABASE.md](docs/DATABASE.md) | schema, ER diagram, constraints, retention |
| [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) | Windows setup, configuration, Docker, cloud deployment, troubleshooting |
| [docs/TESTING.md](docs/TESTING.md) | test strategy, commands, latest results |
| [docs/DEMO_GUIDE.md](docs/DEMO_GUIDE.md) | a step-by-step script for presenting the project |
| [docs/FILE_GUIDE.md](docs/FILE_GUIDE.md) | an explanation of every file in the project |
| [SECURITY.md](SECURITY.md) | security controls, limitations and responsible testing |

## Limitations (read before presenting)

- Detection is rule-based (plus an optional small model). It recognises known tactics and
  structures; new scripts, other languages and image-only messages can be missed.
- Rule weights are expert-assigned starting values, not statistically calibrated. See docs/SCORING.md.
- The bundled datasets are small and synthetic, so any model metrics computed on them are optimistic
  and do not describe real-world accuracy.
- No external reputation service is integrated by default; extension interfaces are provided.

## License

MIT, see [LICENSE](LICENSE).
