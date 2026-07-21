-- db/init/01_schema.sql

CREATE TABLE IF NOT EXISTS users
(
    id         CHAR(36)     NOT NULL,
    email      VARCHAR(255) NOT NULL,
    password   VARCHAR(255),
    enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email)
);

CREATE TABLE IF NOT EXISTS roles
(
    id   CHAR(36)    NOT NULL,
    name VARCHAR(50) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_roles_name (name)
);

CREATE TABLE IF NOT EXISTS user_roles
(
    user_id CHAR(36) NOT NULL,
    role_id CHAR(36) NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
);

-- db/init/02_tokens.sql

CREATE TABLE IF NOT EXISTS refresh_tokens
(
    id         CHAR(36)    NOT NULL,
    user_id    CHAR(36)    NOT NULL,
    family_id  CHAR(36)    NOT NULL,
    consumed   BOOLEAN     NOT NULL DEFAULT FALSE,
    revoked    BOOLEAN     NOT NULL DEFAULT FALSE,
    issued_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    expires_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_family ON refresh_tokens (family_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);

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

-- db/init/04_schema.sql

CREATE TABLE IF NOT EXISTS accounts
(
    id      CHAR(36)     NOT NULL,
    user_id CHAR(36)     NOT NULL,
    name    VARCHAR(150) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_account_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS channels
(
    id          CHAR(36)    NOT NULL,
    name        VARCHAR(50) NOT NULL,
    description VARCHAR(2048) DEFAULT '',
    account_id  CHAR(36)    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_channel_account FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS channel_subscriptions
(
    id         CHAR(36) NOT NULL,
    channel_id CHAR(36) NOT NULL,
    account_id CHAR(36) NOT NULL,
    banned     BOOLEAN      DEFAULT FALSE,
    ban_reason VARCHAR(512) DEFAULT NULL,
    banned_at  TIMESTAMP    DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_channel_subscription_channel FOREIGN KEY (channel_id) REFERENCES channels (id) ON DELETE CASCADE,
    CONSTRAINT fk_channel_subscriptions_account FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE CASCADE,
    CONSTRAINT uq_channel_subscription_channel_account UNIQUE (channel_id, account_id)
);

CREATE TABLE IF NOT EXISTS live_streams
(
    id                     CHAR(36)    NOT NULL,
    name                   VARCHAR(50) NOT NULL,
    description            VARCHAR(2048)        DEFAULT '',
    thumbnail              MEDIUMBLOB           DEFAULT NULL,
    thumbnail_content_type VARCHAR(24)          DEFAULT NULL,
    channel_id             CHAR(36)    NOT NULL,
    created_at             TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at             TIMESTAMP            DEFAULT NULL,
    stopped_at             TIMESTAMP            DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_live_stream_channel FOREIGN KEY (channel_id) REFERENCES channels (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS live_stream_chats
(
    id                      CHAR(36)     NOT NULL,
    stream_id               CHAR(36)     NOT NULL,
    channel_subscription_id CHAR(36)     NOT NULL,
    message                 VARCHAR(256) NOT NULL DEFAULT '',
    PRIMARY KEY (id),
    CONSTRAINT fk_live_stream_chat_stream FOREIGN KEY (stream_id) REFERENCES live_streams (id) ON DELETE CASCADE,
    CONSTRAINT fk_live_stream_chat_channel_subscription FOREIGN KEY (channel_subscription_id) REFERENCES channel_subscriptions (id) ON DELETE CASCADE
)
