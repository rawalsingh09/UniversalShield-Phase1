@echo off
REM Trains the optional Naive Bayes models from the bundled SYNTHETIC datasets.
REM Restart the application afterwards so it loads them.
cd /d "%~dp0.."
call mvnw.cmd -q compile exec:java -Dexec.args="--task message --data data/samples/messages_synthetic.csv --out models/message"
if errorlevel 1 exit /b 1
call mvnw.cmd -q exec:java -Dexec.args="--task url --data data/samples/urls_synthetic.csv --out models/url"
