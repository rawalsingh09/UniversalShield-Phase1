@echo off
REM Runs the tests and builds target\universalshield.jar
cd /d "%~dp0.."
call mvnw.cmd -B clean package
if errorlevel 1 exit /b 1
echo.
echo Built target\universalshield.jar - run it with:  java -jar target\universalshield.jar
