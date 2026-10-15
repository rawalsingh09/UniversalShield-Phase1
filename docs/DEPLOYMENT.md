# Deployment and setup guide

This guide starts from a fresh Windows 10/11 laptop. macOS and Linux users can use the `.sh`
scripts and `./mvnw` instead of the `.cmd` files.

You need only a **JDK 17 or newer**. Maven does not need to be installed, because the project
includes the Maven Wrapper (`mvnw.cmd`), which downloads the right Maven version on first use.
PostgreSQL and Docker are optional: the default `dev` profile uses an embedded H2 database file.

## 1. Install Java on Windows

1. Download **Eclipse Temurin JDK 17** (or 21) as an `.msi` from https://adoptium.net/.
2. Run the installer. On the "Custom Setup" page enable **Set JAVA_HOME variable** and
   **Add to PATH**.
3. Open a **new** Command Prompt and check:

   ```bat
   java -version
   echo %JAVA_HOME%
   ```

   You should see version 17 or newer and a path such as `C:\Program Files\Eclipse Adoptium\jdk-17...`.

If you skipped the option, set it manually: Start → "Edit the system environment variables" →
Environment Variables → New system variable `JAVA_HOME` = the JDK folder, then edit `Path` and
add `%JAVA_HOME%\bin`. Open a new terminal afterwards.

Optional: **Node.js 18+** from https://nodejs.org/ to run the frontend unit tests, and **Git**
from https://git-scm.com/ to upload to GitHub.

## 2. Get the project

Unzip `UniversalShield.zip` to a path **without spaces or non-English characters**, for
example `C:\Projects\UniversalShield`. Then:

```bat
cd C:\Projects\UniversalShield
```

## 3. Run locally (dev profile, no database install)

```bat
scripts\run-dev.cmd
```

The first run downloads Maven and the dependencies (needs internet, a few hundred MB). Wait for
`Started UniversalShieldApplication`, then open **http://localhost:8080**.

Sign in with the demo account `demo@universalshield.local` / `ShieldDemo2026`, or register your
own. The H2 database file is created in `data\devdb.mv.db`. Stop the app with `Ctrl+C`.

Other useful commands:

| Task | Command |
|---|---|
| Run all tests | `scripts\run-tests.cmd` |
| Build the jar (runs tests) | `scripts\build.cmd` → `target\universalshield.jar` |
| Run the built jar | `java -jar target\universalshield.jar` |
| Train the optional ML models | `scripts\train-models.cmd` |
| Different port | `set PORT=9090` then `java -jar target\universalshield.jar` |

## 4. Configuration (environment variables)

All settings have safe development defaults. The full list is in [`.env.example`](../.env.example).

| Variable | Default | Notes |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | `dev`, `prod` (or `test` for tests) |
| `PORT` | 8080 | HTTP port |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | local PostgreSQL | Used by the prod profile |
| `APP_HASH_SECRET` | dev-only value | **Required in prod**: at least 32 random characters, must not contain "change-me" |
| `COOKIE_SECURE` | `true` in prod | Set `false` only for plain `http://localhost` |
| `CORS_ALLOWED_ORIGINS` | empty | Extra origins for a separately hosted frontend; empty = same origin only |
| `API_DOCS_PUBLIC` | `true` dev / `false` prod | Expose Swagger UI and OpenAPI without login |
| `RISK_THRESHOLD_MEDIUM/HIGH/CRITICAL` | 30 / 60 / 85 | Risk level boundaries |
| `TRANSACTION_LARGE_AMOUNT` | 10000 | "Large amount" when there is little history |
| `RETENTION_DEFAULT_DAYS` | 90 | Default history retention for new users |
| `RATE_LIMIT_ENABLED`, `RATE_LIMIT_AUTH_PER_MINUTE`, `RATE_LIMIT_SCANS_PER_MINUTE` | true, 10, 30 | Per-client limits |
| `ML_ENABLED`, `ML_MODEL_DIR` | true, `models` | Optional ML models |
| `DEMO_PASSWORD` | `ShieldDemo2026` | Dev profile demo account only |

Generate a random secret in PowerShell:

```powershell
[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }))
```

## 5. Run against a local PostgreSQL (prod profile)

1. Install PostgreSQL 16 from https://www.postgresql.org/download/windows/ and remember the
   password you set for the `postgres` user.
2. Create a database and user (SQL Shell "psql", logged in as `postgres`):

   ```sql
   CREATE USER universalshield WITH PASSWORD 'choose-a-long-password';
   CREATE DATABASE universalshield OWNER universalshield;
   ```

3. Run the jar with the prod profile (Command Prompt):

   ```bat
   set SPRING_PROFILES_ACTIVE=prod
   set DB_URL=jdbc:postgresql://localhost:5432/universalshield
   set DB_USERNAME=universalshield
   set DB_PASSWORD=choose-a-long-password
   set APP_HASH_SECRET=paste-at-least-32-random-characters-here
   set COOKIE_SECURE=false
   java -jar target\universalshield.jar
   ```

Flyway creates the tables on first start. The prod profile creates **no demo account**, so
register a user in the web app.

## 6. Docker and Docker Compose

Requires Docker Desktop (with WSL 2 on Windows).

```bat
copy .env.example .env
notepad .env          REM set DB_PASSWORD and APP_HASH_SECRET
docker compose up --build
```

Open http://localhost:8080. Compose starts `postgres:16-alpine` with a named volume `pgdata`
and the app with the prod profile. Stop with `Ctrl+C`; `docker compose down` removes the
containers, and `docker compose down -v` also deletes the database volume.

The `Dockerfile` is multi-stage: it builds with Maven in `maven:3.9-eclipse-temurin-17`, then
copies only the jar into `eclipse-temurin:17-jre`, runs as a non-root user, and has a health
check on `/api/health`.

## 7. Database migrations

Migrations live in `src/main/resources/db/migration` and run automatically at startup. To
change the schema, add a new file `V2__describe_change.sql` and never edit an applied one. If
startup fails with `Validate failed: Migrations have failed validation`, someone edited an
applied migration. Restore it and add a new version instead.

## 8. Upload to GitHub

```bat
cd C:\Projects\UniversalShield
git init
git add .
git commit -m "UniversalShield initial version"
git branch -M main
git remote add origin https://github.com/<your-user>/UniversalShield.git
git push -u origin main
```

`.gitignore` already excludes `target/`, the H2 database files, `.env`, trained model files and
logs. Before pushing, run `git status` and make sure no `.env` file or database file is listed.

## 9. Deploy to a cloud host (example: Render or Railway)

Both offer a managed PostgreSQL and can build the `Dockerfile` directly from a GitHub repo.
Free tiers and their limits change often, so check the provider's current pricing page.

1. Push the project to GitHub (section 8).
2. Create a **PostgreSQL** instance on the platform. Note its host, port, database, user and password.
3. Create a **Web Service** from the repository, choose **Docker** as the runtime (it finds the
   `Dockerfile`).
4. Set environment variables on the service:
   - `SPRING_PROFILES_ACTIVE=prod`
   - `DB_URL=jdbc:postgresql://<host>:<port>/<database>` (JDBC form, not the `postgres://` URL)
   - `DB_USERNAME`, `DB_PASSWORD`
   - `APP_HASH_SECRET` = a new random value of at least 32 characters
   - leave `COOKIE_SECURE` unset (true), because the platform serves HTTPS
5. The platform sets `PORT`, and the app reads it. Use `/api/health` as the health-check path.
6. Deploy and open the HTTPS URL. Register an account.

**HTTPS:** both platforms terminate TLS at their proxy and forward `X-Forwarded-*` headers,
which the prod profile trusts. Do not expose the prod profile directly to the internet without
such a proxy (see SECURITY.md).

**Secrets:** keep them only in the platform's environment settings. Never commit them. If one
leaks, rotate it. Changing `APP_HASH_SECRET` only breaks the "seen before" link to old scans.

## 10. Backups

```bash
# Backup (PostgreSQL client tools, or inside the db container)
pg_dump -h localhost -U universalshield -d universalshield -F c -f universalshield-2026-10-09.dump
# Restore into an empty database
pg_restore -h localhost -U universalshield -d universalshield --clean universalshield-2026-10-09.dump
```

With Compose: `docker compose exec db pg_dump -U universalshield -F c universalshield > backup.dump`.
Managed databases on cloud platforms usually include automatic daily backups. Check that they are on.

## 11. Windows troubleshooting

| Symptom | Cause and fix |
|---|---|
| `'java' is not recognized` or `JAVA_HOME is not set` | JDK not installed or not on PATH. See section 1, then open a **new** terminal |
| `error: release version 17 not supported` | `JAVA_HOME` points to an old JDK 8/11. Point it to JDK 17+ |
| `Port 8080 was already in use` | Another program uses the port. Find it with `netstat -ano \| findstr :8080`, stop it with `taskkill /PID <pid> /F`, or run with `set PORT=9090` |
| `Database may be already in use` (H2) | Another copy of the app is still running, or a DB tool has the file open. Close it, or stop all `java.exe` in Task Manager |
| Want a clean dev database | Stop the app and delete `data\devdb.mv.db` (and `data\devdb.trace.db` if present) |
| `mvnw.cmd` fails downloading | Needs internet the first time. Behind a proxy, set `MAVEN_OPTS=-Dhttps.proxyHost=... -Dhttps.proxyPort=...` |
| `'mvnw' is not recognized` in PowerShell | Use `.\mvnw.cmd` (PowerShell needs `.\`) |
| Scripts fail with odd characters / `'\r' command not found` in Git Bash | Line endings were changed. The `.cmd` files must be CRLF and `mvnw`/`.sh` files LF. Re-extract the ZIP, or set `git config core.autocrlf false` before cloning |
| Path with spaces breaks a script | Move the project to e.g. `C:\Projects\UniversalShield` |
| Prod profile exits: `APP_HASH_SECRET must be set to a random value of at least 32 characters in production.` | Intentional safety check. Set a long random secret |
| Login works but POST requests fail with `403` | Stale CSRF token or blocked cookies. Reload the page and allow cookies for localhost |
| Browser shows a blank page | Hard-refresh (`Ctrl+F5`). Check the browser console. The app needs a modern browser (Chrome, Edge, Firefox) |
| Docker: `DB_PASSWORD` required error | Create `.env` from `.env.example` and fill it in |
| Antivirus blocks `mvnw.cmd` downloads | Allow the `%USERPROFILE%\.m2` folder, or install Maven 3.9+ and run `mvn` instead of `mvnw.cmd` |
