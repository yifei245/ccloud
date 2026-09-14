-- ----------------------------------------------------------------------------
-- bpm 模块：工作流：表单、分类、流程定义扩展、抄送、监听器、表达式、用户组、OA 请假示例
-- 本文件由 DDL 生成器产出，公共约定与 01-system.sql 一致
-- ----------------------------------------------------------------------------
USE `ccloud`;
-- Flowable 引擎自身的 ACT_* 表由 flowable.database-schema-update=true 在启动时自动创建，
-- 不在此脚本中维护：手写会与引擎版本升级后的表结构冲突。

-- ========================= 表单与分类 =========================

DROP TABLE IF EXISTS `bpm_form`;
CREATE TABLE `bpm_form` (
  `id`            bigint        NOT NULL AUTO_INCREMENT COMMENT '编号',
  `name`          varchar(64)   NOT NULL COMMENT '表单名',
  `status`        tinyint       NOT NULL DEFAULT 0 COMMENT '状态：0 开启 1 关闭',
  `conf`          varchar(1000) NOT NULL COMMENT '表单配置，JSON',
  `fields`        longtext      NOT NULL COMMENT '表单项数组，JSON',
  `remark`        varchar(255)  DEFAULT NULL COMMENT '备注',
  `tenant_id`     bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`       varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`       varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       bit(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='动态表单定义表';
DROP TABLE IF EXISTS `bpm_category`;
CREATE TABLE `bpm_category` (
  `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '分类编号',
  `name`          varchar(30)  NOT NULL DEFAULT '' COMMENT '分类名',
  `code`          varchar(30)  NOT NULL DEFAULT '' COMMENT '分类标志',
  `description`   varchar(255) DEFAULT '' COMMENT '分类描述',
  `status`        tinyint      NOT NULL DEFAULT 0 COMMENT '状态：0 开启 1 关闭',
  `sort`          int          NOT NULL DEFAULT 0 COMMENT '排序',
  `tenant_id`     bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`       varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`       varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  UNIQUE KEY `uk_code` (`code`, `tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程分类表';

-- ========================= 流程定义扩展 =========================

DROP TABLE IF EXISTS `bpm_process_definition_info`;
CREATE TABLE `bpm_process_definition_info` (
  `id`                             bigint        NOT NULL AUTO_INCREMENT COMMENT '编号',
  `process_definition_id`          varchar(64)   NOT NULL COMMENT 'Flowable 流程定义编号',
  `model_id`                       varchar(64)   NOT NULL COMMENT 'Flowable 流程模型编号',
  `model_type`                     tinyint       NOT NULL DEFAULT 10 COMMENT '流程模型类型：10 BPMN 20 仿钉钉',
  `category`                       varchar(64)   DEFAULT NULL COMMENT '流程分类',
  `icon`                           varchar(512)  DEFAULT NULL COMMENT '图标',
  `description`                    varchar(255)  DEFAULT NULL COMMENT '描述',
  `form_type`                      tinyint       NOT NULL COMMENT '表单类型：10 流程表单 20 业务表单',
  `form_id`                        bigint        DEFAULT NULL COMMENT '流程表单编号',
  `form_conf`                      varchar(1000) DEFAULT NULL COMMENT '表单配置快照',
  `form_fields`                    longtext      DEFAULT NULL COMMENT '表单项快照',
  `form_custom_create_path`        varchar(255)  DEFAULT NULL COMMENT '业务表单的提交路由',
  `form_custom_view_path`          varchar(255)  DEFAULT NULL COMMENT '业务表单的查看路由',
  `simple_model`                   longtext      DEFAULT NULL COMMENT '仿钉钉设计器的模型数据，JSON',
  `visible`                        bit(1)        NOT NULL DEFAULT b'1' COMMENT '是否在发起列表中可见',
  `sort`                           int           NOT NULL DEFAULT 0 COMMENT '排序',
  `start_user_ids`                 varchar(256)  DEFAULT NULL COMMENT '可发起用户编号数组，为空表示所有人',
  `start_dept_ids`                 varchar(256)  DEFAULT NULL COMMENT '可发起部门编号数组',
  `manager_user_ids`               varchar(256)  DEFAULT NULL COMMENT '可管理用户编号数组',
  `allow_cancel_running_process`   bit(1)        NOT NULL DEFAULT b'1' COMMENT '是否允许撤销审批中的申请',
  `process_id_rule`                varchar(255)  DEFAULT NULL COMMENT '流程编号生成规则，JSON',
  `auto_approval_type`             tinyint       NOT NULL DEFAULT 0 COMMENT '自动去重类型',
  `title_setting`                  varchar(512)  DEFAULT NULL COMMENT '标题设置，JSON',
  `summary_setting`                varchar(512)  DEFAULT NULL COMMENT '摘要设置，JSON',
  `tenant_id`                      bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`                        varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`                    datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`                        varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`                    datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`                        bit(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  UNIQUE KEY `uk_process_definition_id` (`process_definition_id`),
  KEY `idx_model_id` (`model_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程定义扩展信息表，补充 Flowable 流程定义缺失的业务属性';
DROP TABLE IF EXISTS `bpm_process_instance_copy`;
CREATE TABLE `bpm_process_instance_copy` (
  `id`                      bigint        NOT NULL AUTO_INCREMENT COMMENT '编号',
  `start_user_id`           bigint        NOT NULL COMMENT '发起人编号',
  `process_instance_id`     varchar(64)   NOT NULL COMMENT '流程实例编号',
  `process_instance_name`   varchar(64)   NOT NULL COMMENT '流程实例名',
  `category`                varchar(64)   NOT NULL COMMENT '流程分类',
  `process_definition_id`   varchar(64)   NOT NULL COMMENT '流程定义编号',
  `task_id`                 varchar(64)   DEFAULT NULL COMMENT '发起抄送的任务编号',
  `task_name`               varchar(64)   DEFAULT NULL COMMENT '发起抄送的任务名',
  `activity_id`             varchar(64)   DEFAULT NULL COMMENT '抄送节点编号',
  `user_id`                 bigint        NOT NULL COMMENT '被抄送人编号',
  `reason`                  varchar(1024) DEFAULT NULL COMMENT '抄送意见',
  `tenant_id`               bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`                 varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`             datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`                 varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`             datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`                 bit(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_process_instance_id` (`process_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程抄送表';
DROP TABLE IF EXISTS `bpm_process_listener`;
CREATE TABLE `bpm_process_listener` (
  `id`            bigint        NOT NULL AUTO_INCREMENT COMMENT '编号',
  `name`          varchar(30)   NOT NULL COMMENT '监听器名',
  `type`          varchar(255)  NOT NULL COMMENT '监听类型：execution 执行 task 任务',
  `status`        tinyint       NOT NULL DEFAULT 0 COMMENT '状态：0 开启 1 关闭',
  `event`         varchar(255)  NOT NULL COMMENT '监听事件',
  `value_type`    varchar(255)  NOT NULL COMMENT '值类型：class 类 delegateExpression 委托表达式 expression 表达式',
  `value`         varchar(1024) NOT NULL COMMENT '值',
  `tenant_id`     bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`       varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`       varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       bit(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程监听器表';
DROP TABLE IF EXISTS `bpm_process_expression`;
CREATE TABLE `bpm_process_expression` (
  `id`            bigint        NOT NULL AUTO_INCREMENT COMMENT '编号',
  `name`          varchar(64)   NOT NULL COMMENT '表达式名',
  `status`        tinyint       NOT NULL DEFAULT 0 COMMENT '状态：0 开启 1 关闭',
  `expression`    varchar(1024) NOT NULL COMMENT '表达式',
  `tenant_id`     bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`       varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`       varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       bit(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程表达式表';
DROP TABLE IF EXISTS `bpm_user_group`;
CREATE TABLE `bpm_user_group` (
  `id`            bigint        NOT NULL AUTO_INCREMENT COMMENT '编号',
  `name`          varchar(30)   NOT NULL COMMENT '组名',
  `description`   varchar(255)  DEFAULT '' COMMENT '描述',
  `user_ids`      varchar(1024) NOT NULL COMMENT '成员用户编号数组',
  `status`        tinyint       NOT NULL DEFAULT 0 COMMENT '状态：0 开启 1 关闭',
  `tenant_id`     bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`       varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`       varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       bit(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审批用户组表';

-- ========================= OA 示例 =========================

DROP TABLE IF EXISTS `bpm_oa_leave`;
CREATE TABLE `bpm_oa_leave` (
  `id`                    bigint       NOT NULL AUTO_INCREMENT COMMENT '请假表单主键',
  `user_id`               bigint       NOT NULL COMMENT '申请人编号',
  `type`                  varchar(20)  NOT NULL COMMENT '请假类型',
  `reason`                varchar(200) NOT NULL COMMENT '请假原因',
  `start_time`            datetime     NOT NULL COMMENT '开始时间',
  `end_time`              datetime     NOT NULL COMMENT '结束时间',
  `day`                   tinyint      NOT NULL COMMENT '请假天数',
  `status`                tinyint      NOT NULL COMMENT '审批结果：1 审批中 2 通过 3 不通过 4 已取消',
  `process_instance_id`   varchar(64)  DEFAULT NULL COMMENT '流程实例编号',
  `tenant_id`             bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`               varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`           datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`               varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`           datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`               bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='OA 请假申请表，演示业务表单如何接入流程';
