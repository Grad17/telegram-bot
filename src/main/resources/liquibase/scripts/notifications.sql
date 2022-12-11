-- liquibase formatted sql

-- changeset sk:1
CREATE TABLE notification_task
(
    id           SERIAL PRIMARY KEY,
    chat_id      BIGINT,
    notification TEXT,
    date_time    TIMESTAMP
)
