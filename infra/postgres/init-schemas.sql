-- Runs once, when the PostgreSQL volume is empty (docker-entrypoint-initdb.d).
-- Creates the application role with least privilege: it can log in and create schemas in
-- database "marketplace", nothing else. Flyway (T09) creates one schema per module.
-- \getenv needs psql 16 or newer; the image has psql 18.

\getenv app_user MARKETPLACE_DB_USER
\getenv app_password MARKETPLACE_DB_PASSWORD

CREATE ROLE :"app_user" LOGIN PASSWORD :'app_password';
GRANT CONNECT, CREATE ON DATABASE marketplace TO :"app_user";
