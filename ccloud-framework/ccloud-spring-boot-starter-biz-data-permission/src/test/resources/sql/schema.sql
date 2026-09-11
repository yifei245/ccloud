DROP TABLE IF EXISTS demo_user;
CREATE TABLE demo_user (
    id          BIGINT PRIMARY KEY,
    name        VARCHAR(64),
    dept_id     BIGINT,
    creator     VARCHAR(64)  DEFAULT '',
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater     VARCHAR(64)  DEFAULT '',
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE
);
