-- db/init/03_sessions.sql

CREATE TABLE IF NOT EXISTS sessions (
    family_id   CHAR(36)     NOT NULL,
    user_id     CHAR(36)     NOT NULL,
    device_name VARCHAR(255),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (family_id),
    CONSTRAINT fk_sessions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);