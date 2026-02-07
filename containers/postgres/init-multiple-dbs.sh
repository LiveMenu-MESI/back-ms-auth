#!/bin/bash
set -e

# Passwords from environment (set in compose from .env). No hardcoded secrets.
KC_PASS_SQL="${KC_DB_PASSWORD//\'/\'\'}"
DB_PASS_SQL="${DB_PASSWORD//\'/\'\'}"

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<EOSQL
    CREATE USER keycloak WITH PASSWORD '${KC_PASS_SQL}';
    CREATE DATABASE keycloak OWNER keycloak;
    GRANT ALL PRIVILEGES ON DATABASE keycloak TO keycloak;

    CREATE USER livemenu WITH PASSWORD '${DB_PASS_SQL}';
    CREATE DATABASE livemenu OWNER livemenu;
    GRANT ALL PRIVILEGES ON DATABASE livemenu TO livemenu;
EOSQL
