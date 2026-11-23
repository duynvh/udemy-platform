-- Runs once, when the PostgreSQL volume is empty (docker-entrypoint-initdb.d, after 10-init-schemas.sql).
-- Keycloak gets its own database and its own role. The application role cannot connect to it.
-- On an existing volume, run this file once by hand (see docs/learn/platform/T07-keycloak-minimal-realm.md).

\getenv kc_user KEYCLOAK_DB_USER
\getenv kc_password KEYCLOAK_DB_PASSWORD

CREATE ROLE :"kc_user" LOGIN PASSWORD :'kc_password';
CREATE DATABASE keycloak OWNER :"kc_user";
-- New databases allow CONNECT for PUBLIC by default; only the owner may connect here.
REVOKE ALL ON DATABASE keycloak FROM PUBLIC;
