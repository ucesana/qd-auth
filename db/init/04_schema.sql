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
    id          CHAR(36)    NOT NULL,
    name        VARCHAR(50) NOT NULL,
    description VARCHAR(2048)        DEFAULT '',
    channel_id  CHAR(36)    NOT NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at  TIMESTAMP            DEFAULT NULL,
    stopped_at  TIMESTAMP            DEFAULT NULL,
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
