-- Spring Modulith event publication registry (transactional outbox), schema v2 for PostgreSQL.
-- Source: spring-modulith-events-jdbc 2.0.10, schemas/v2/schema-postgresql.sql. Keep in sync on Modulith upgrades.
-- Modulith finds the table through spring.modulith.events.jdbc.schema=shared.
CREATE TABLE shared.event_publication
(
    id                     UUID                     NOT NULL,
    listener_id            TEXT                     NOT NULL,
    event_type             TEXT                     NOT NULL,
    serialized_event       TEXT                     NOT NULL,
    publication_date       TIMESTAMP WITH TIME ZONE NOT NULL,
    completion_date        TIMESTAMP WITH TIME ZONE,
    status                 TEXT,
    completion_attempts    INT,
    last_resubmission_date TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (id)
);

CREATE INDEX event_publication_serialized_event_hash_idx ON shared.event_publication USING hash (serialized_event);
CREATE INDEX event_publication_by_completion_date_idx ON shared.event_publication (completion_date);
