-- ----------------------------------------------------------------------------
-- open 模块：第三方开放平台：应用、接口目录、应用接口授权、调用日志、回调通知
-- 本文件由 DDL 生成器产出，公共约定与 01-system.sql 一致
-- ----------------------------------------------------------------------------
USE `ccloud`;

-- ========================= 应用与授权 =========================

DROP TABLE IF EXISTS `open_app`;
CREATE TABLE `open_app` (
  `id`               bigint        NOT NULL AUTO_INCREMENT COMMENT '应用编号',
  `name`             varchar(64)   NOT NULL COMMENT '应用名称',
  `app_key`          varchar(64)   NOT NULL COMMENT '应用标识，公开，系统生成，不可修改',
  `app_secret`       varchar(255)  NOT NULL COMMENT '应用密钥，AES 加密存储（ccloud.mybatis.encrypt-key）。签名校验需原文参与 HMAC 计算，不可单向哈希',
  `status`           tinyint       NOT NULL DEFAULT 0 COMMENT '状态：0 开启 1 关闭',
  `remark`           varchar(500)  DEFAULT NULL COMMENT '备注',
  `ip_whitelist`     varchar(1024) DEFAULT NULL COMMENT 'IP 白名单，逗号分隔，为空表示不限制',
  `rate_limit`       int           NOT NULL DEFAULT 100 COMMENT '每秒请求上限，网关按 app_key 维度限流',
  `expire_time`      datetime      DEFAULT NULL COMMENT '过期时间，为空表示长期有效',
  `contact_name`     varchar(64)   DEFAULT NULL COMMENT '联系人',
  `contact_mobile`   varchar(20)   DEFAULT NULL COMMENT '联系电话',
  `notify_url`       varchar(512)  DEFAULT NULL COMMENT '事件回调地址',
  `tenant_id`        bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`          varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`      datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`          varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`      datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`          bigint        NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  UNIQUE KEY `uk_app_key` (`app_key`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='开放应用表。第三方凭 app_key + app_secret 调用 /open-api';
DROP TABLE IF EXISTS `open_api`;
CREATE TABLE `open_api` (
  `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '接口编号',
  `name`          varchar(64)  NOT NULL COMMENT '接口名称',
  `code`          varchar(128) NOT NULL COMMENT '接口编码',
  `path`          varchar(255) NOT NULL COMMENT '接口路径，/open-api/ 开头，网关按 方法 + 路径 精确匹配授权',
  `method`        varchar(16)  NOT NULL COMMENT '请求方法',
  `description`   varchar(512) DEFAULT NULL COMMENT '接口描述',
  `status`        tinyint      NOT NULL DEFAULT 0 COMMENT '状态：0 开启 1 关闭',
  `creator`       varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`       varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       bigint       NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`, `deleted`),
  UNIQUE KEY `uk_method_path` (`method`, `path`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='开放接口目录表，全局共享，只有系统租户可维护';
DROP TABLE IF EXISTS `open_app_api`;
CREATE TABLE `open_app_api` (
  `id`            bigint      NOT NULL AUTO_INCREMENT COMMENT '编号',
  `app_id`        bigint      NOT NULL COMMENT '应用编号',
  `api_id`        bigint      NOT NULL COMMENT '接口编号',
  `tenant_id`     bigint      NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`       varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`       varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       bigint      NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  UNIQUE KEY `uk_app_api` (`app_id`, `api_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='开放应用可调用的接口表';

-- ========================= 日志 =========================

DROP TABLE IF EXISTS `open_api_log`;
CREATE TABLE `open_api_log` (
  `id`               bigint       NOT NULL AUTO_INCREMENT COMMENT '编号',
  `app_id`           bigint       NOT NULL COMMENT '应用编号',
  `app_key`          varchar(64)  NOT NULL COMMENT '应用标识',
  `api_code`         varchar(128) DEFAULT NULL COMMENT '接口编码',
  `request_method`   varchar(16)  NOT NULL COMMENT '请求方法',
  `request_url`      varchar(255) NOT NULL COMMENT '请求地址',
  `request_params`   text         DEFAULT NULL COMMENT '请求参数，敏感字段已脱敏',
  `result_code`      int          NOT NULL DEFAULT 0 COMMENT '业务结果码',
  `result_msg`       varchar(512) DEFAULT '' COMMENT '结果提示，业务失败时为错误提示',
  `user_ip`          varchar(50)  NOT NULL COMMENT '调用方 IP',
  `duration`         int          NOT NULL DEFAULT 0 COMMENT '执行时长，单位毫秒',
  `begin_time`       datetime     NOT NULL COMMENT '开始时间',
  `end_time`         datetime     NOT NULL COMMENT '结束时间',
  `trace_id`         varchar(64)  NOT NULL DEFAULT '' COMMENT '链路追踪编号',
  `tenant_id`        bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`          varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`          varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`          bigint       NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_app_create_time` (`app_id`, `create_time`),
  KEY `idx_trace_id` (`trace_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='开放接口调用日志表';
DROP TABLE IF EXISTS `open_notify_log`;
CREATE TABLE `open_notify_log` (
  `id`                bigint        NOT NULL AUTO_INCREMENT COMMENT '编号',
  `app_id`            bigint        NOT NULL COMMENT '应用编号',
  `event_id`          varchar(64)   NOT NULL COMMENT '事件编号，回调时放入 X-Open-Event-Id，供对方去重',
  `event`             varchar(64)   NOT NULL COMMENT '事件类型，如 order.paid',
  `payload`           text          NOT NULL COMMENT '回调请求体，JSON，签名即对它计算',
  `notify_url`        varchar(512)  NOT NULL COMMENT '回调地址',
  `status`            tinyint       NOT NULL DEFAULT 0 COMMENT '状态：0 待通知 10 成功 20 失败待重试 30 放弃',
  `retry_count`       int           NOT NULL DEFAULT 0 COMMENT '已重试次数',
  `next_retry_time`   datetime      DEFAULT NULL COMMENT '下次重试时间',
  `last_notify_time`  datetime      DEFAULT NULL COMMENT '最近一次通知时间',
  `http_status`       int           DEFAULT NULL COMMENT '最近一次对方返回的 HTTP 状态码',
  `response_body`     varchar(2048) DEFAULT NULL COMMENT '对方最近一次响应内容，截断到 2048 字符；未发出时为失败原因',
  `tenant_id`         bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`           varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`       datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`           varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`       datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`           bigint        NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_status_retry` (`status`, `next_retry_time`),
  KEY `idx_status_create_time` (`status`, `create_time`),
  UNIQUE KEY `uk_event_id` (`event_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='开放应用事件回调日志表，失败按退避策略重试';
