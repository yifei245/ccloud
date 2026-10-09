-- ----------------------------------------------------------------------------
-- infra 模块：配置、文件、API 日志、代码生成、数据源、代码生成演示、本地消息表
-- 本文件由 DDL 生成器产出，公共约定与 01-system.sql 一致
-- ----------------------------------------------------------------------------
USE `ccloud`;

-- ========================= 参数配置 =========================

DROP TABLE IF EXISTS `infra_config`;
CREATE TABLE `infra_config` (
  `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '参数主键',
  `category`      varchar(50)  NOT NULL COMMENT '参数分组',
  `type`          tinyint      NOT NULL COMMENT '参数类型：1 系统内置 2 自定义',
  `name`          varchar(100) NOT NULL DEFAULT '' COMMENT '参数名称',
  `config_key`    varchar(100) NOT NULL DEFAULT '' COMMENT '参数键名',
  `value`         varchar(500) NOT NULL DEFAULT '' COMMENT '参数键值',
  `visible`       bit(1)       NOT NULL COMMENT '是否可见：不可见的参数不返回给前端，用于存放密钥类配置',
  `remark`        varchar(500) DEFAULT NULL COMMENT '备注',
  `creator`       varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`       varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       bigint       NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='参数配置表，全局共享';

-- ========================= 文件 =========================

DROP TABLE IF EXISTS `infra_file_config`;
CREATE TABLE `infra_file_config` (
  `id`            bigint        NOT NULL AUTO_INCREMENT COMMENT '编号',
  `name`          varchar(63)   NOT NULL COMMENT '配置名',
  `storage`       tinyint       NOT NULL COMMENT '存储器：1 数据库 10 本地磁盘 11 FTP 12 SFTP 20 S3 兼容（含 MinIO / OSS / COS）',
  `remark`        varchar(500)  DEFAULT NULL COMMENT '备注',
  `master`        bit(1)        NOT NULL COMMENT '是否为主配置。上传时默认使用主配置，全局只能有一个',
  `config`        varchar(4096) NOT NULL COMMENT '存储配置，JSON。含访问密钥，读取时需脱敏',
  `creator`       varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`       varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       bigint        NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件存储配置表，全局共享';
DROP TABLE IF EXISTS `infra_file`;
CREATE TABLE `infra_file` (
  `id`            bigint        NOT NULL AUTO_INCREMENT COMMENT '文件编号',
  `config_id`     bigint        DEFAULT NULL COMMENT '配置编号',
  `name`          varchar(256)  DEFAULT NULL COMMENT '原始文件名',
  `path`          varchar(512)  NOT NULL COMMENT '存储路径',
  `url`           varchar(1024) NOT NULL COMMENT '访问地址',
  `type`          varchar(128)  DEFAULT NULL COMMENT '文件 MIME 类型，依据内容识别而非扩展名',
  `size`          int           NOT NULL COMMENT '文件大小，单位字节',
  `tenant_id`     bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`       varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`       varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       bigint        NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_config_path` (`config_id`, `path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件表';
DROP TABLE IF EXISTS `infra_file_content`;
CREATE TABLE `infra_file_content` (
  `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '编号',
  `config_id`     bigint       NOT NULL COMMENT '配置编号',
  `path`          varchar(512) NOT NULL COMMENT '存储路径',
  `content`       mediumblob   NOT NULL COMMENT '文件内容',
  `creator`       varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`       varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       bigint       NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  KEY `idx_config_path` (`config_id`, `path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件内容表，仅数据库存储器使用';

-- ========================= API 日志 =========================

DROP TABLE IF EXISTS `infra_api_access_log`;
CREATE TABLE `infra_api_access_log` (
  `id`                 bigint       NOT NULL AUTO_INCREMENT COMMENT '日志主键',
  `trace_id`           varchar(64)  NOT NULL DEFAULT '' COMMENT '链路追踪编号',
  `user_id`            bigint       NOT NULL DEFAULT 0 COMMENT '用户编号',
  `user_type`          tinyint      NOT NULL DEFAULT 0 COMMENT '用户类型',
  `application_name`   varchar(50)  NOT NULL COMMENT '应用名',
  `request_method`     varchar(16)  NOT NULL DEFAULT '' COMMENT '请求方法',
  `request_url`        varchar(255) NOT NULL DEFAULT '' COMMENT '请求地址',
  `request_params`     text         DEFAULT NULL COMMENT '请求参数',
  `response_body`      text         DEFAULT NULL COMMENT '响应结果',
  `user_ip`            varchar(50)  NOT NULL COMMENT '用户 IP',
  `user_agent`         varchar(512) NOT NULL COMMENT '浏览器 UA',
  `operate_module`     varchar(50)  DEFAULT NULL COMMENT '操作模块',
  `operate_name`       varchar(50)  DEFAULT NULL COMMENT '操作名',
  `operate_type`       tinyint      DEFAULT 0 COMMENT '操作分类',
  `begin_time`         datetime     NOT NULL COMMENT '开始请求时间',
  `end_time`           datetime     NOT NULL COMMENT '结束请求时间',
  `duration`           int          NOT NULL COMMENT '执行时长，单位毫秒',
  `result_code`        int          NOT NULL DEFAULT 0 COMMENT '业务结果码，HTTP 恒为 200 时靠它区分成败',
  `result_msg`         varchar(512) DEFAULT '' COMMENT '结果提示',
  `tenant_id`          bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`            varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`            varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`            bigint       NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_trace_id` (`trace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='API 访问日志表';
DROP TABLE IF EXISTS `infra_api_error_log`;
CREATE TABLE `infra_api_error_log` (
  `id`                             bigint        NOT NULL AUTO_INCREMENT COMMENT '编号',
  `trace_id`                       varchar(64)   NOT NULL COMMENT '链路追踪编号',
  `user_id`                        bigint        NOT NULL DEFAULT 0 COMMENT '用户编号',
  `user_type`                      tinyint       NOT NULL DEFAULT 0 COMMENT '用户类型',
  `application_name`               varchar(50)   NOT NULL COMMENT '应用名',
  `request_method`                 varchar(16)   NOT NULL COMMENT '请求方法',
  `request_url`                    varchar(255)  NOT NULL COMMENT '请求地址',
  `request_params`                 varchar(8000) NOT NULL COMMENT '请求参数',
  `user_ip`                        varchar(50)   NOT NULL COMMENT '用户 IP',
  `user_agent`                     varchar(512)  NOT NULL COMMENT '浏览器 UA',
  `exception_time`                 datetime      NOT NULL COMMENT '异常发生时间',
  `exception_name`                 varchar(128)  NOT NULL DEFAULT '' COMMENT '异常名',
  `exception_message`              text          NOT NULL COMMENT '异常信息',
  `exception_root_cause_message`   text          NOT NULL COMMENT '根因异常信息',
  `exception_stack_trace`          text          NOT NULL COMMENT '异常堆栈',
  `exception_class_name`           varchar(512)  NOT NULL COMMENT '异常发生的类',
  `exception_file_name`            varchar(512)  NOT NULL COMMENT '异常发生的文件',
  `exception_method_name`          varchar(512)  NOT NULL COMMENT '异常发生的方法',
  `exception_line_number`          int           NOT NULL COMMENT '异常发生的行号',
  `process_status`                 tinyint       NOT NULL COMMENT '处理状态：0 未处理 1 已处理 2 已忽略',
  `process_time`                   datetime      DEFAULT NULL COMMENT '处理时间',
  `process_user_id`                int           DEFAULT 0 COMMENT '处理人编号',
  `tenant_id`                      bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`                        varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`                    datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`                        varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`                    datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`                        bigint        NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_process_status` (`process_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='API 异常日志表';

-- ========================= 代码生成 =========================

DROP TABLE IF EXISTS `infra_codegen_table`;
CREATE TABLE `infra_codegen_table` (
  `id`                      bigint       NOT NULL AUTO_INCREMENT COMMENT '编号',
  `data_source_config_id`   bigint       NOT NULL COMMENT '数据源配置编号',
  `scene`                   tinyint      NOT NULL DEFAULT 1 COMMENT '生成场景：1 管理后台 2 用户 App',
  `table_name`              varchar(200) NOT NULL DEFAULT '' COMMENT '表名',
  `table_comment`           varchar(500) NOT NULL DEFAULT '' COMMENT '表描述',
  `remark`                  varchar(500) DEFAULT NULL COMMENT '备注',
  `module_name`             varchar(30)  NOT NULL COMMENT '模块名',
  `business_name`           varchar(30)  NOT NULL COMMENT '业务名',
  `class_name`              varchar(100) NOT NULL DEFAULT '' COMMENT '类名',
  `class_comment`           varchar(50)  NOT NULL COMMENT '类描述',
  `author`                  varchar(50)  NOT NULL COMMENT '作者',
  `template_type`           tinyint      NOT NULL DEFAULT 1 COMMENT '模板类型：1 单表 2 树表 10 主表 11 子表',
  `front_type`              tinyint      NOT NULL COMMENT '前端类型',
  `parent_menu_id`          bigint       DEFAULT NULL COMMENT '父菜单编号',
  `master_table_id`         bigint       DEFAULT NULL COMMENT '主表编号，子表生成时使用',
  `sub_join_column_id`      bigint       DEFAULT NULL COMMENT '子表关联主表的字段编号',
  `sub_join_many`           bit(1)       DEFAULT NULL COMMENT '主表与子表是否一对多',
  `tree_parent_column_id`   bigint       DEFAULT NULL COMMENT '树表的父字段编号',
  `tree_name_column_id`     bigint       DEFAULT NULL COMMENT '树表的名字字段编号',
  `creator`                 varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`             datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`                 varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`             datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`                 bigint       NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='代码生成表定义';
DROP TABLE IF EXISTS `infra_codegen_column`;
CREATE TABLE `infra_codegen_column` (
  `id`                         bigint       NOT NULL AUTO_INCREMENT COMMENT '编号',
  `table_id`                   bigint       NOT NULL COMMENT '表编号',
  `column_name`                varchar(200) NOT NULL COMMENT '字段名',
  `data_type`                  varchar(100) NOT NULL COMMENT '字段类型',
  `column_comment`             varchar(500) NOT NULL COMMENT '字段描述',
  `nullable`                   bit(1)       NOT NULL COMMENT '是否允许为空',
  `primary_key`                bit(1)       NOT NULL COMMENT '是否主键',
  `ordinal_position`           int          NOT NULL COMMENT '排序',
  `java_type`                  varchar(32)  NOT NULL COMMENT 'Java 属性类型',
  `java_field`                 varchar(64)  NOT NULL COMMENT 'Java 属性名',
  `dict_type`                  varchar(200) DEFAULT '' COMMENT '字典类型',
  `example`                    varchar(64)  DEFAULT NULL COMMENT '数据示例',
  `create_operation`           bit(1)       NOT NULL COMMENT '是否为新增操作的字段',
  `update_operation`           bit(1)       NOT NULL COMMENT '是否为更新操作的字段',
  `list_operation`             bit(1)       NOT NULL COMMENT '是否为查询条件字段',
  `list_operation_condition`   varchar(32)  NOT NULL DEFAULT '=' COMMENT '查询条件类型',
  `list_operation_result`      bit(1)       NOT NULL COMMENT '是否为列表结果字段',
  `html_type`                  varchar(32)  NOT NULL COMMENT '显示类型',
  `creator`                    varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`                datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`                    varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`                datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`                    bigint       NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  KEY `idx_table_id` (`table_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='代码生成字段定义';

-- ========================= 数据源 =========================

DROP TABLE IF EXISTS `infra_data_source_config`;
CREATE TABLE `infra_data_source_config` (
  `id`            bigint        NOT NULL AUTO_INCREMENT COMMENT '主键编号',
  `name`          varchar(100)  NOT NULL DEFAULT '' COMMENT '数据源名称',
  `url`           varchar(1024) NOT NULL COMMENT '数据源连接',
  `username`      varchar(255)  NOT NULL COMMENT '用户名',
  `password`      varchar(255)  NOT NULL DEFAULT '' COMMENT '密码，加密存储',
  `creator`       varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`       varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       bigint        NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据源配置表';

-- ========================= 代码生成演示 =========================

DROP TABLE IF EXISTS `infra_demo_student`;
CREATE TABLE `infra_demo_student` (
  `id`            bigint        NOT NULL AUTO_INCREMENT COMMENT '编号',
  `name`          varchar(100)  NOT NULL COMMENT '姓名',
  `sex`           tinyint       NOT NULL COMMENT '性别：1 男 2 女',
  `birthday`      datetime      DEFAULT NULL COMMENT '出生日期',
  `description`   varchar(1000) DEFAULT NULL COMMENT '简介',
  `status`        tinyint       NOT NULL DEFAULT 0 COMMENT '状态：0 开启 1 关闭',
  `creator`       varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`       varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       bigint        NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='演示学生表，代码生成单表模板的演示与验收用';
DROP TABLE IF EXISTS `infra_demo_category`;
CREATE TABLE `infra_demo_category` (
  `id`            bigint        NOT NULL AUTO_INCREMENT COMMENT '编号',
  `name`          varchar(100)  NOT NULL COMMENT '名称',
  `parent_id`     bigint        NOT NULL DEFAULT 0 COMMENT '父编号，0 表示根节点',
  `sort`          int           NOT NULL DEFAULT 0 COMMENT '显示顺序',
  `creator`       varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`       varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       bigint        NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='演示分类表，代码生成树表模板的演示与验收用';

-- ========================= 本地消息表（最终一致性） =========================

-- 以下三张表的列必须与 ccloud-spring-boot-starter-mq 中的 DO 及测试建表脚本严格一致，
-- 由 scripts/verify-mq-schema.sh 在真实 MySQL 中逐列比对。

DROP TABLE IF EXISTS `infra_message_outbox`;
CREATE TABLE `infra_message_outbox` (
  `id`                bigint        NOT NULL AUTO_INCREMENT COMMENT '编号',
  `message_id`        varchar(64)   NOT NULL COMMENT '全局唯一的消息编号，消费侧据此去重',
  `topic`             varchar(128)  NOT NULL COMMENT '消息主题',
  `payload`           text          DEFAULT NULL COMMENT '消息体，JSON',
  `status`            tinyint       NOT NULL DEFAULT 0 COMMENT '状态：0 待投递 1 投递成功 2 投递失败待重试 3 超过重试上限',
  `retry_count`       int           NOT NULL DEFAULT 0 COMMENT '已重试次数',
  `max_retry`         int           NOT NULL DEFAULT 5 COMMENT '最大重试次数',
  `next_retry_time`   datetime      DEFAULT NULL COMMENT '下次重试时间，按指数退避计算',
  `error_msg`         varchar(1000) DEFAULT NULL COMMENT '最近一次失败原因',
  `tenant_id`         bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`           varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`       datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`           varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`       datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`           bigint        NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  UNIQUE KEY `uk_message_id` (`message_id`),
  KEY `idx_status_retry` (`status`, `next_retry_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='本地消息表，与业务数据同库同事务写入';
DROP TABLE IF EXISTS `infra_message_consume_log`;
CREATE TABLE `infra_message_consume_log` (
  `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '编号',
  `message_id`     varchar(64)  NOT NULL COMMENT '消息编号',
  `consumer`       varchar(128) NOT NULL COMMENT '消费者标识，同一消息的多个下游各计一次',
  `consume_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '消费时间',
  `creator`        varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`        varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`        bigint       NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_message_consumer` (`message_id`, `consumer`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息消费记录表。不带 tenant_id，必须列入 ccloud.tenant.ignore-tables';
DROP TABLE IF EXISTS `infra_message_dead_letter`;
CREATE TABLE `infra_message_dead_letter` (
  `id`             bigint        NOT NULL AUTO_INCREMENT COMMENT '编号',
  `message_id`     varchar(64)   NOT NULL COMMENT '消息编号',
  `topic`          varchar(128)  NOT NULL COMMENT '消息主题',
  `payload`        text          DEFAULT NULL COMMENT '消息体，JSON',
  `retry_count`    int           NOT NULL DEFAULT 0 COMMENT '已重试次数',
  `error_msg`      varchar(1000) DEFAULT NULL COMMENT '最终失败原因',
  `handled`        bit(1)        NOT NULL DEFAULT b'0' COMMENT '是否已人工处理',
  `handled_time`   datetime      DEFAULT NULL COMMENT '处理时间',
  `tenant_id`      bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`        varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`    datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`        varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`    datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`        bigint        NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_message_id` (`message_id`),
  KEY `idx_handled` (`handled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息死信表，超过重试上限的消息落于此待人工处理';
