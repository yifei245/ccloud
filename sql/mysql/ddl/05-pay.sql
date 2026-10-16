-- ----------------------------------------------------------------------------
-- pay 模块：支付应用、渠道、订单、退款、转账、钱包
-- 本文件由 DDL 生成器产出，公共约定与 01-system.sql 一致
-- ----------------------------------------------------------------------------
USE `ccloud`;
-- 金额一律 int，单位分。浮点数存钱会累积舍入误差，对账时对不平。
-- 所有「商户单号」上都有唯一约束：这是防重复支付、重复退款的最后一道闸，
-- 应用层的幂等判断在并发下挡不住，数据库唯一约束挡得住。

-- ========================= 应用与渠道 =========================

DROP TABLE IF EXISTS `pay_app`;
CREATE TABLE `pay_app` (
  `id`                    bigint        NOT NULL AUTO_INCREMENT COMMENT '应用编号',
  `app_key`               varchar(64)   NOT NULL COMMENT '应用标识',
  `name`                  varchar(64)   NOT NULL COMMENT '应用名',
  `status`                tinyint       NOT NULL DEFAULT 0 COMMENT '状态：0 开启 1 关闭',
  `remark`                varchar(500)  DEFAULT NULL COMMENT '备注',
  `open_app_id`           bigint        DEFAULT NULL COMMENT '绑定的开放应用编号。外部系统经开放平台调用支付开放接口、接收支付回调；为空表示不对外开放',
  `tenant_id`             bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`               varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`           datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`               varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`           datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`               bigint        NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  UNIQUE KEY `uk_app_key` (`app_key`, `tenant_id`, `deleted`),
  UNIQUE KEY `uk_open_app` (`open_app_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付应用表，每个接入支付的业务方一条。内部服务经消息接收结果，外部系统经绑定的开放应用接收回调';
DROP TABLE IF EXISTS `pay_channel`;
CREATE TABLE `pay_channel` (
  `id`            bigint        NOT NULL AUTO_INCREMENT COMMENT '渠道编号',
  `code`          varchar(32)   NOT NULL COMMENT '渠道编码，如 wx_lite / alipay_pc / wallet / mock',
  `status`        tinyint       NOT NULL DEFAULT 0 COMMENT '状态：0 开启 1 关闭',
  `remark`        varchar(500)  DEFAULT NULL COMMENT '备注',
  `fee_rate`      double        NOT NULL DEFAULT 0 COMMENT '渠道费率，单位百分比',
  `app_id`        bigint        NOT NULL COMMENT '应用编号',
  `config`        varchar(4096) NOT NULL COMMENT '渠道配置，JSON。含商户私钥，需加密存储',
  `tenant_id`     bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`       varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`       varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       bigint        NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  UNIQUE KEY `uk_app_code` (`app_id`, `code`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付渠道表';

-- ========================= 支付订单 =========================

DROP TABLE IF EXISTS `pay_order`;
CREATE TABLE `pay_order` (
  `id`                  bigint        NOT NULL AUTO_INCREMENT COMMENT '支付订单编号',
  `app_id`              bigint        NOT NULL COMMENT '应用编号',
  `channel_id`          bigint        DEFAULT NULL COMMENT '渠道编号，用户选择渠道后写入',
  `channel_code`        varchar(32)   DEFAULT NULL COMMENT '渠道编码',
  `pay_method`          varchar(32)   DEFAULT NULL COMMENT '支付方式。聚合渠道下的具体方式，如 wx_lite、alipay_qr；直连渠道为空',
  `merchant_order_id`   varchar(64)   NOT NULL COMMENT '商户订单编号，即业务侧订单号',
  `subject`             varchar(32)   NOT NULL COMMENT '商品标题',
  `body`                varchar(128)  NOT NULL DEFAULT '' COMMENT '商品描述',
  `price`               int           NOT NULL DEFAULT 0 COMMENT '支付金额，单位：分',
  `channel_fee_rate`    double        DEFAULT 0 COMMENT '渠道手续费率，单位百分比',
  `channel_fee_price`   int           NOT NULL DEFAULT 0 COMMENT '渠道手续费，单位：分',
  `status`              tinyint       NOT NULL COMMENT '支付状态：0 待支付 10 支付成功 20 已退款 30 已关闭',
  `user_id`             bigint        DEFAULT NULL COMMENT '付款用户编号。会员端据此校验订单归属，钱包支付据此扣款；外部系统创建的订单为空',
  `user_type`           tinyint       DEFAULT NULL COMMENT '付款用户类型，见 UserTypeEnum',
  `user_ip`             varchar(50)   NOT NULL COMMENT '用户 IP',
  `expire_time`         datetime      NOT NULL COMMENT '订单失效时间，超时未支付由定时任务关闭',
  `success_time`        datetime      DEFAULT NULL COMMENT '支付成功时间',
  `extension_id`        bigint        DEFAULT NULL COMMENT '支付成功的拓展单编号',
  `no`                  varchar(64)   DEFAULT NULL COMMENT '支付成功的外部订单号',
  `refund_price`        int           NOT NULL DEFAULT 0 COMMENT '累计退款金额，单位：分',
  `channel_user_id`     varchar(255)  DEFAULT NULL COMMENT '渠道用户编号，如微信 openid',
  `channel_order_no`    varchar(64)   DEFAULT NULL COMMENT '渠道订单号',
  `tenant_id`           bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`             varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`         datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`             varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`         datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`             bigint        NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  UNIQUE KEY `uk_app_merchant_order` (`app_id`, `merchant_order_id`, `deleted`),
  KEY `idx_status_expire` (`status`, `expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付订单表';
DROP TABLE IF EXISTS `pay_order_extension`;
CREATE TABLE `pay_order_extension` (
  `id`                    bigint        NOT NULL AUTO_INCREMENT COMMENT '支付订单拓展编号',
  `no`                    varchar(64)   NOT NULL COMMENT '外部订单号，传给渠道的商户单号',
  `order_id`              bigint        NOT NULL COMMENT '支付订单编号',
  `channel_id`            bigint        NOT NULL COMMENT '渠道编号',
  `channel_code`          varchar(32)   NOT NULL COMMENT '渠道编码',
  `pay_method`            varchar(32)   DEFAULT NULL COMMENT '支付方式，聚合渠道下的具体方式',
  `user_ip`               varchar(50)   NOT NULL COMMENT '用户 IP',
  `status`                tinyint       NOT NULL COMMENT '支付状态：0 等待 10 成功 30 关闭（渠道明确失败也记为关闭）',
  `channel_extras`        varchar(1024) DEFAULT NULL COMMENT '渠道额外参数，JSON',
  `channel_error_code`    varchar(64)   DEFAULT NULL COMMENT '渠道错误码',
  `channel_error_msg`     varchar(256)  DEFAULT NULL COMMENT '渠道错误信息',
  `channel_notify_data`   varchar(4096) DEFAULT NULL COMMENT '渠道回调原始报文，对账与排查依据',
  `tenant_id`             bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`               varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`           datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`               varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`           datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`               bigint        NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  UNIQUE KEY `uk_no` (`no`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_status_create` (`status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付订单拓展表。每发起一次支付尝试生成一条，一个订单可对应多次尝试';

-- ========================= 退款 =========================

DROP TABLE IF EXISTS `pay_refund`;
CREATE TABLE `pay_refund` (
  `id`                    bigint        NOT NULL AUTO_INCREMENT COMMENT '支付退款编号',
  `no`                    varchar(64)   NOT NULL COMMENT '外部退款号',
  `app_id`                bigint        NOT NULL COMMENT '应用编号',
  `channel_id`            bigint        NOT NULL COMMENT '渠道编号',
  `channel_code`          varchar(32)   NOT NULL COMMENT '渠道编码',
  `order_id`              bigint        NOT NULL COMMENT '支付订单编号',
  `order_no`              varchar(64)   NOT NULL COMMENT '支付订单外部订单号',
  `merchant_order_id`     varchar(64)   NOT NULL COMMENT '商户订单编号',
  `merchant_refund_id`    varchar(64)   NOT NULL COMMENT '商户退款单编号',
  `status`                tinyint       NOT NULL COMMENT '退款状态：0 未退款 10 退款成功 20 退款失败',
  `pay_price`             int           NOT NULL DEFAULT 0 COMMENT '原支付金额，单位：分',
  `refund_price`          int           NOT NULL DEFAULT 0 COMMENT '退款金额，单位：分',
  `reason`                varchar(256)  NOT NULL COMMENT '退款原因',
  `user_ip`               varchar(50)   DEFAULT NULL COMMENT '用户 IP',
  `channel_order_no`      varchar(64)   NOT NULL COMMENT '渠道订单号',
  `channel_refund_no`     varchar(64)   DEFAULT NULL COMMENT '渠道退款单号',
  `success_time`          datetime      DEFAULT NULL COMMENT '退款成功时间',
  `channel_error_code`    varchar(128)  DEFAULT NULL COMMENT '渠道错误码',
  `channel_error_msg`     varchar(256)  DEFAULT NULL COMMENT '渠道错误信息',
  `channel_notify_data`   varchar(4096) DEFAULT NULL COMMENT '渠道回调原始报文',
  `tenant_id`             bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`               varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`           datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`               varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`           datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`               bigint        NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  UNIQUE KEY `uk_app_merchant_refund` (`app_id`, `merchant_refund_id`, `deleted`),
  UNIQUE KEY `uk_no` (`no`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='退款订单表';

-- ========================= 转账 =========================

DROP TABLE IF EXISTS `pay_transfer`;
CREATE TABLE `pay_transfer` (
  `id`                     bigint        NOT NULL AUTO_INCREMENT COMMENT '编号',
  `no`                     varchar(64)   NOT NULL COMMENT '转账单号',
  `app_id`                 bigint        NOT NULL COMMENT '应用编号',
  `channel_id`             bigint        NOT NULL COMMENT '渠道编号',
  `channel_code`           varchar(32)   NOT NULL COMMENT '渠道编码',
  `merchant_transfer_id`   varchar(64)   NOT NULL COMMENT '商户转账单编号',
  `subject`                varchar(512)  NOT NULL COMMENT '转账标题',
  `price`                  int           NOT NULL DEFAULT 0 COMMENT '转账金额，单位：分',
  `user_account`           varchar(64)   NOT NULL COMMENT '收款人账号',
  `user_name`              varchar(64)   DEFAULT NULL COMMENT '收款人姓名',
  `status`                 tinyint       NOT NULL COMMENT '转账状态：0 等待 10 成功 20 失败 5 进行中',
  `success_time`           datetime      DEFAULT NULL COMMENT '转账成功时间',
  `user_ip`                varchar(50)   NOT NULL COMMENT '用户 IP',
  `channel_extras`         varchar(512)  DEFAULT NULL COMMENT '渠道额外参数，JSON',
  `channel_transfer_no`    varchar(64)   DEFAULT NULL COMMENT '渠道转账单号',
  `channel_error_code`     varchar(128)  DEFAULT NULL COMMENT '渠道错误码',
  `channel_error_msg`      varchar(512)  DEFAULT NULL COMMENT '渠道错误信息',
  `channel_notify_data`    varchar(4096) DEFAULT NULL COMMENT '渠道回调原始报文',
  `tenant_id`              bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`                varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`            datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`                varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`            datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`                bigint        NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  UNIQUE KEY `uk_app_merchant_transfer` (`app_id`, `merchant_transfer_id`, `deleted`),
  UNIQUE KEY `uk_no` (`no`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='转账单表，用于佣金提现、钱包提现等出款';

-- ========================= 钱包 =========================

DROP TABLE IF EXISTS `pay_wallet`;
CREATE TABLE `pay_wallet` (
  `id`               bigint      NOT NULL AUTO_INCREMENT COMMENT '编号',
  `user_id`          bigint      NOT NULL COMMENT '用户编号',
  `user_type`        tinyint     NOT NULL COMMENT '用户类型：1 会员 2 管理员',
  `balance`          int         NOT NULL DEFAULT 0 COMMENT '余额，单位：分',
  `total_expense`    int         NOT NULL DEFAULT 0 COMMENT '累计支出，单位：分',
  `total_recharge`   int         NOT NULL DEFAULT 0 COMMENT '累计充值，单位：分',
  `freeze_price`     int         NOT NULL DEFAULT 0 COMMENT '冻结金额，提现审核期间占用，单位：分',
  `tenant_id`        bigint      NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`          varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time`      datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`          varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time`      datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`          bigint      NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  UNIQUE KEY `uk_user` (`user_id`, `user_type`, `tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会员钱包表。余额变更必须用 balance = balance - ? WHERE balance >= ? 的原子更新，禁止先查后改';
DROP TABLE IF EXISTS `pay_wallet_transaction`;
CREATE TABLE `pay_wallet_transaction` (
  `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '编号',
  `no`            varchar(64)  NOT NULL COMMENT '流水号',
  `wallet_id`     bigint       NOT NULL COMMENT '钱包编号',
  `biz_type`      tinyint      NOT NULL COMMENT '业务类型：1 充值 2 充值退款 3 支付 4 支付退款 5 提现',
  `biz_id`        varchar(64)  NOT NULL COMMENT '业务编号',
  `title`         varchar(128) NOT NULL COMMENT '流水说明',
  `price`         int          NOT NULL COMMENT '变动金额，单位分，正数入账负数出账',
  `balance`       int          NOT NULL COMMENT '变动后余额，单位分',
  `tenant_id`     bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`       varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`       varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       bigint       NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  UNIQUE KEY `uk_wallet_biz` (`wallet_id`, `biz_type`, `biz_id`, `deleted`),
  UNIQUE KEY `uk_no` (`no`),
  KEY `idx_biz` (`biz_type`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='钱包流水表。同一业务单据对同一钱包只能入账一次';
DROP TABLE IF EXISTS `pay_wallet_recharge_package`;
CREATE TABLE `pay_wallet_recharge_package` (
  `id`            bigint      NOT NULL AUTO_INCREMENT COMMENT '编号',
  `name`          varchar(32) NOT NULL COMMENT '套餐名',
  `pay_price`     int         NOT NULL DEFAULT 0 COMMENT '支付金额，单位：分',
  `bonus_price`   int         NOT NULL DEFAULT 0 COMMENT '赠送金额，单位：分',
  `status`        tinyint     NOT NULL DEFAULT 0 COMMENT '状态：0 开启 1 关闭',
  `tenant_id`     bigint      NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`       varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`       varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       bigint      NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='钱包充值套餐表';
DROP TABLE IF EXISTS `pay_wallet_recharge`;
CREATE TABLE `pay_wallet_recharge` (
  `id`                   bigint      NOT NULL AUTO_INCREMENT COMMENT '编号',
  `wallet_id`            bigint      NOT NULL COMMENT '钱包编号',
  `total_price`          int         NOT NULL DEFAULT 0 COMMENT '到账总金额，支付金额加赠送金额，单位：分',
  `pay_price`            int         NOT NULL DEFAULT 0 COMMENT '实际支付金额，单位：分',
  `bonus_price`          int         NOT NULL DEFAULT 0 COMMENT '赠送金额，单位：分',
  `package_id`           bigint      DEFAULT NULL COMMENT '充值套餐编号',
  `pay_status`           bit(1)      NOT NULL DEFAULT b'0' COMMENT '是否已支付',
  `pay_order_id`         bigint      DEFAULT NULL COMMENT '支付订单编号',
  `pay_channel_code`     varchar(16) DEFAULT NULL COMMENT '支付渠道',
  `pay_time`             datetime    DEFAULT NULL COMMENT '支付时间',
  `pay_refund_id`        bigint      DEFAULT NULL COMMENT '退款单编号',
  `refund_total_price`   int         NOT NULL DEFAULT 0 COMMENT '退款到账总金额，单位：分',
  `refund_pay_price`     int         NOT NULL DEFAULT 0 COMMENT '退款实际金额，单位：分',
  `refund_bonus_price`   int         NOT NULL DEFAULT 0 COMMENT '退回的赠送金额，单位：分',
  `refund_time`          datetime    DEFAULT NULL COMMENT '退款时间',
  `refund_status`        tinyint     NOT NULL DEFAULT 0 COMMENT '退款状态：0 未退款或退款中（pay_refund_id 非空即退款中） 10 退款成功 20 退款失败',
  `tenant_id`            bigint      NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`              varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time`          datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`              varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time`          datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`              bigint      NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_wallet_id` (`wallet_id`),
  KEY `idx_pay_order_id` (`pay_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='钱包充值表';
