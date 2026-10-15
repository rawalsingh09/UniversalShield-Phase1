#!/usr/bin/env sh
# Runs all Java tests, then the frontend unit tests if Node.js is available.
set -e
cd "$(dirname "$0")/.."
./mvnw -B test
if command -v node >/dev/null 2>&1; then node --test "src/test/frontend/*.test.mjs"; else echo "Node.js not found - skipping frontend tests"; fi
