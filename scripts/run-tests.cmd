@echo off
REM Runs all Java tests (unit + integration). Frontend tests run too if Node.js is installed.
cd /d "%~dp0.."
call mvnw.cmd -B test
if errorlevel 1 exit /b 1
where node >nul 2>nul
if %errorlevel%==0 (
  node --test "src/test/frontend/*.test.mjs"
) else (
  echo Node.js not found - skipping frontend unit tests.
)
