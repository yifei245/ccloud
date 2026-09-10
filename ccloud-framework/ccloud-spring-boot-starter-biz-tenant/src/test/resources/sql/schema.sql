DROP TABLE IF EXISTS demo_tenant;
CREATE TABLE demo_tenant (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(64),
    tenant_id   BIGINT       NOT NULL DEFAULT 0,
    creator     VARCHAR(64)  DEFAULT '',
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater     VARCHAR(64)  DEFAULT '',
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE
);
