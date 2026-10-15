@echo off
REM Starts UniversalShield locally with the dev profile (embedded H2 database, demo account).
REM Open http://localhost:8080 when you see "Started UniversalShieldApplication".
cd /d "%~dp0.."
call mvnw.cmd -q spring-boot:run
