DROP TABLE IF EXISTS infra_message_outbox;
CREATE TABLE infra_message_outbox (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id      VARCHAR(64)  NOT NULL,
    topic           VARCHAR(128) NOT NULL,
    payload         TEXT,
    status          TINYINT      NOT NULL DEFAULT 0,
    retry_count     INT          NOT NULL DEFAULT 0,
    max_retry       INT          NOT NULL DEFAULT 5,
    next_retry_time TIMESTAMP    NULL,
    error_msg       VARCHAR(1000),
    tenant_id       BIGINT       NOT NULL DEFAULT 0,
    creator         VARCHAR(64)  DEFAULT '',
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         VARCHAR(64)  DEFAULT '',
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_outbox_message_id UNIQUE (message_id)
);

DROP TABLE IF EXISTS infra_message_consume_log;
CREATE TABLE infra_message_consume_log (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id   VARCHAR(64)  NOT NULL,
    consumer     VARCHAR(128) NOT NULL,
    consume_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    creator      VARCHAR(64)  DEFAULT '',
    create_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater      VARCHAR(64)  DEFAULT '',
    update_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_consume_log UNIQUE (message_id, consumer)
);

DROP TABLE IF EXISTS infra_message_dead_letter;
CREATE TABLE infra_message_dead_letter (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id   VARCHAR(64)  NOT NULL,
    topic        VARCHAR(128) NOT NULL,
    payload      TEXT,
    retry_count  INT          NOT NULL DEFAULT 0,
    error_msg    VARCHAR(1000),
    handled      BOOLEAN      NOT NULL DEFAULT FALSE,
    handled_time TIMESTAMP    NULL,
    tenant_id    BIGINT       NOT NULL DEFAULT 0,
    creator      VARCHAR(64)  DEFAULT '',
    create_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater      VARCHAR(64)  DEFAULT '',
    update_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted      BOOLEAN      NOT NULL DEFAULT FALSE
);
