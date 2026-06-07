-- db/init/02_tokens.sql

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          CHAR(36)    NOT NULL,
    user_id     CHAR(36)    NOT NULL,
    family_id   CHAR(36)    NOT NULL,
    consumed    BOOLEAN     NOT NULL DEFAULT FALSE,
    revoked     BOOLEAN     NOT NULL DEFAULT FALSE,
    issued_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at  TIMESTAMP   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_family ON refresh_tokens (family_id);
