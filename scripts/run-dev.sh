#!/usr/bin/env sh
# Starts UniversalShield locally with the dev profile (embedded H2 database, demo account).
cd "$(dirname "$0")/.." && ./mvnw -q spring-boot:run
