-- db/init/03_sessions.sql

CREATE TABLE IF NOT EXISTS sessions
(
    family_id    CHAR(36)    NOT NULL,
    user_id      CHAR(36)    NOT NULL,
    device_id    VARCHAR(255),
    device_name  VARCHAR(255),
    created_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_used_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (family_id),
    CONSTRAINT fk_sessions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_sessions_user_device ON sessions (user_id, device_id);