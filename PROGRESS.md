# UniversalShield build progress

Working copy: /home/claude/UniversalShield (cloud workspace). Deliverables: /mnt/project-files/universalshield/

## Completed (2026-10-09)
- Stage 1-2: empty workspace; architecture, schema (Flyway V1), API designed.
- Stage 3: RiskEngine (noisy-OR over correlation groups, 95 cap without verified external data), 5 heuristic detectors, optional Naive Bayes ML module + TrainingCli.
- Stage 4: vanilla JS SPA (landing, auth, dashboard, scanner, result, history, settings, about).
- Stage 5: session auth + CSRF cookie, BCrypt, ownership checks, rate limiting, retention job, audit events.
- Stage 6: 161 Java tests + 10 Node frontend tests + Playwright UI smoke script - all passing on the final build.
- Stage 7: Dockerfile, docker-compose.yml, .env.example; runtime image + compose verified with PostgreSQL 16 (Maven stage of docker build blocked by sandbox TLS proxy).
- Stage 8: README, SECURITY, LICENSE, docs/ (ARCHITECTURE, REQUEST_FLOW, SCORING, ML, API, DATABASE, DEPLOYMENT, TESTING, DEMO_GUIDE, FILE_GUIDE, diagrams), data/README.
- Stage 9: final `mvnw -B clean package` = 161 tests, 0 failures; UI smoke passed on dev/H2 and prod/PostgreSQL 16.15.
- Stage 10: UniversalShield.zip written to /mnt/project-files/universalshield/.
- Late fixes found while documenting: look-alike tokens in hyphenated domains (paypa1-secure-login.top), "digital arrest" call phrases (secrecy, money transfer), "enter my UPI PIN to receive".

## Not done / possible next steps
- Calibrate rule weights on a real labelled dataset (docs/SCORING.md section 8).
- Email verification, password reset, MFA, account lockout.
- A verified reputation provider (would lift the 95 cap).
- Push to GitHub (waiting for the owner's go-ahead).
