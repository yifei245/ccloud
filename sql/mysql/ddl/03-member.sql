-- ----------------------------------------------------------------------------
-- member 模块：C 端会员、地址、等级、积分、经验、签到、分组、标签
-- 本文件由 DDL 生成器产出，公共约定与 01-system.sql 一致
-- ----------------------------------------------------------------------------
USE `ccloud`;

-- ========================= 会员 =========================

DROP TABLE IF EXISTS `member_user`;
CREATE TABLE `member_user` (
  `id`                  bigint       NOT NULL AUTO_INCREMENT COMMENT '用户编号',
  `mobile`              varchar(11)  NOT NULL COMMENT '手机号',
  `password`            varchar(100) NOT NULL DEFAULT '' COMMENT '密码，BCrypt 单向哈希',
  `status`              tinyint      NOT NULL DEFAULT 0 COMMENT '状态：0 开启 1 关闭',
  `register_ip`         varchar(32)  NOT NULL COMMENT '注册 IP',
  `register_terminal`   tinyint      DEFAULT NULL COMMENT '注册终端：10 微信小程序 11 公众号 20 H5 31 App',
  `login_ip`            varchar(50)  DEFAULT '' COMMENT '最后登录 IP',
  `login_date`          datetime     DEFAULT NULL COMMENT '最后登录时间',
  `nickname`            varchar(30)  NOT NULL DEFAULT '' COMMENT '用户昵称',
  `avatar`              varchar(512) NOT NULL DEFAULT '' COMMENT '头像',
  `name`                varchar(30)  DEFAULT '' COMMENT '真实姓名',
  `sex`                 tinyint      DEFAULT 0 COMMENT '性别：0 未知 1 男 2 女',
  `birthday`            datetime     DEFAULT NULL COMMENT '出生日期',
  `area_id`             int          DEFAULT NULL COMMENT '所在地区编号',
  `mark`                varchar(255) DEFAULT NULL COMMENT '运营备注',
  `point`               int          DEFAULT 0 COMMENT '当前积分',
  `tag_ids`             varchar(255) DEFAULT NULL COMMENT '标签编号数组，逗号分隔',
  `level_id`            bigint       DEFAULT NULL COMMENT '等级编号',
  `experience`          int          DEFAULT 0 COMMENT '经验值',
  `group_id`            bigint       DEFAULT NULL COMMENT '分组编号',
  `tenant_id`           bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`             varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`         datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`             varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`         datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`             bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  UNIQUE KEY `uk_mobile` (`mobile`, `tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会员用户表。与 system_users 是两套独立体系，同一手机号互不相干';
DROP TABLE IF EXISTS `member_address`;
CREATE TABLE `member_address` (
  `id`               bigint       NOT NULL AUTO_INCREMENT COMMENT '收件地址编号',
  `user_id`          bigint       NOT NULL COMMENT '用户编号',
  `name`             varchar(10)  NOT NULL COMMENT '收件人名称',
  `mobile`           varchar(20)  NOT NULL COMMENT '手机号',
  `area_id`          bigint       NOT NULL COMMENT '地区编号',
  `detail_address`   varchar(250) NOT NULL COMMENT '详细地址',
  `default_status`   bit(1)       NOT NULL COMMENT '是否默认地址',
  `tenant_id`        bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`          varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`          varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`          bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会员收件地址表';

-- ========================= 分组与标签 =========================

DROP TABLE IF EXISTS `member_group`;
CREATE TABLE `member_group` (
  `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '编号',
  `name`          varchar(50)  NOT NULL DEFAULT '' COMMENT '名称',
  `remark`        varchar(500) DEFAULT NULL COMMENT '备注',
  `status`        tinyint      NOT NULL DEFAULT 0 COMMENT '状态：0 开启 1 关闭',
  `tenant_id`     bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`       varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`       varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会员分组表';
DROP TABLE IF EXISTS `member_tag`;
CREATE TABLE `member_tag` (
  `id`            bigint      NOT NULL AUTO_INCREMENT COMMENT '编号',
  `name`          varchar(50) NOT NULL DEFAULT '' COMMENT '标签名称',
  `tenant_id`     bigint      NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`       varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`       varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       bit(1)      NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会员标签表';

-- ========================= 等级与经验 =========================

DROP TABLE IF EXISTS `member_level`;
CREATE TABLE `member_level` (
  `id`                 bigint       NOT NULL AUTO_INCREMENT COMMENT '编号',
  `name`               varchar(30)  NOT NULL DEFAULT '' COMMENT '等级名称',
  `level`              int          NOT NULL DEFAULT 0 COMMENT '等级序号，数值越大等级越高',
  `experience`         int          NOT NULL DEFAULT 0 COMMENT '升级所需经验',
  `discount_percent`   tinyint      NOT NULL DEFAULT 100 COMMENT '享受折扣百分比，100 表示不打折',
  `icon`               varchar(255) DEFAULT '' COMMENT '等级图标',
  `background_url`     varchar(255) DEFAULT '' COMMENT '等级背景图',
  `status`             tinyint      NOT NULL DEFAULT 0 COMMENT '状态：0 开启 1 关闭',
  `tenant_id`          bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`            varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`            varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`            bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会员等级表';
DROP TABLE IF EXISTS `member_level_record`;
CREATE TABLE `member_level_record` (
  `id`                 bigint       NOT NULL AUTO_INCREMENT COMMENT '编号',
  `user_id`            bigint       NOT NULL DEFAULT 0 COMMENT '用户编号',
  `level_id`           bigint       NOT NULL DEFAULT 0 COMMENT '等级编号',
  `level`              int          NOT NULL DEFAULT 0 COMMENT '等级序号',
  `discount_percent`   tinyint      NOT NULL DEFAULT 100 COMMENT '享受折扣百分比',
  `experience`         int          NOT NULL DEFAULT 0 COMMENT '升级所需经验',
  `user_experience`    int          NOT NULL DEFAULT 0 COMMENT '变更时用户的经验',
  `remark`             varchar(255) NOT NULL DEFAULT '' COMMENT '备注',
  `description`        varchar(255) NOT NULL DEFAULT '' COMMENT '描述',
  `tenant_id`          bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`            varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`            varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`            bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会员等级变更记录表';
DROP TABLE IF EXISTS `member_experience_record`;
CREATE TABLE `member_experience_record` (
  `id`                 bigint       NOT NULL AUTO_INCREMENT COMMENT '编号',
  `user_id`            bigint       NOT NULL DEFAULT 0 COMMENT '用户编号',
  `biz_type`           tinyint      NOT NULL DEFAULT 0 COMMENT '业务类型',
  `biz_id`             varchar(255) NOT NULL DEFAULT '' COMMENT '业务编号',
  `title`              varchar(30)  NOT NULL DEFAULT '' COMMENT '标题',
  `description`        varchar(512) NOT NULL DEFAULT '' COMMENT '描述',
  `experience`         int          NOT NULL DEFAULT 0 COMMENT '变动经验，负数表示扣减',
  `total_experience`   int          NOT NULL DEFAULT 0 COMMENT '变动后的总经验',
  `tenant_id`          bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`            varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`            varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`            bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_biz` (`biz_type`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会员经验记录表';

-- ========================= 积分 =========================

DROP TABLE IF EXISTS `member_point_record`;
CREATE TABLE `member_point_record` (
  `id`            bigint        NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `user_id`       bigint        NOT NULL COMMENT '用户编号',
  `biz_id`        varchar(255)  NOT NULL DEFAULT '' COMMENT '业务编号',
  `biz_type`      tinyint       NOT NULL COMMENT '业务类型',
  `title`         varchar(255)  NOT NULL COMMENT '积分标题',
  `description`   varchar(5000) DEFAULT NULL COMMENT '积分描述',
  `point`         int           NOT NULL COMMENT '变动积分，负数表示扣减',
  `total_point`   int           NOT NULL COMMENT '变动后的积分',
  `tenant_id`     bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`       varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`       varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       bit(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_biz` (`biz_type`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会员积分记录表';
DROP TABLE IF EXISTS `member_config`;
CREATE TABLE `member_config` (
  `id`                              bigint      NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `point_trade_deduct_enable`       bit(1)      NOT NULL DEFAULT b'1' COMMENT '是否开启积分抵扣',
  `point_trade_deduct_unit_price`   int         NOT NULL DEFAULT 0 COMMENT '积分抵扣单价，1 积分抵扣多少分，单位：分',
  `point_trade_deduct_max_price`    int         DEFAULT NULL COMMENT '单笔订单最多抵扣积分数',
  `point_trade_give_point`          bigint      DEFAULT NULL COMMENT '每消费 1 元赠送的积分',
  `tenant_id`                       bigint      NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`                         varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time`                     datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`                         varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time`                     datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`                         bit(1)      NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会员配置表，每个租户一条';

-- ========================= 签到 =========================

DROP TABLE IF EXISTS `member_sign_in_config`;
CREATE TABLE `member_sign_in_config` (
  `id`            bigint      NOT NULL AUTO_INCREMENT COMMENT '规则自增主键',
  `day`           int         NOT NULL COMMENT '连续签到第几天',
  `point`         int         NOT NULL DEFAULT 0 COMMENT '奖励积分',
  `experience`    int         NOT NULL DEFAULT 0 COMMENT '奖励经验',
  `status`        tinyint     NOT NULL DEFAULT 0 COMMENT '状态：0 开启 1 关闭',
  `tenant_id`     bigint      NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`       varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`       varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       bit(1)      NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  UNIQUE KEY `uk_day` (`day`, `tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='签到规则表';
DROP TABLE IF EXISTS `member_sign_in_record`;
CREATE TABLE `member_sign_in_record` (
  `id`            bigint      NOT NULL AUTO_INCREMENT COMMENT '签到自增编号',
  `user_id`       bigint      NOT NULL COMMENT '签到用户',
  `day`           int         NOT NULL COMMENT '连续签到的第几天',
  `point`         int         NOT NULL DEFAULT 0 COMMENT '获得的积分',
  `experience`    int         NOT NULL DEFAULT 0 COMMENT '获得的经验',
  `tenant_id`     bigint      NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`       varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`       varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       bit(1)      NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_user_create_time` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='签到记录表';
