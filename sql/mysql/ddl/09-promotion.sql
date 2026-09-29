-- ----------------------------------------------------------------------------
-- promotion 模块：内容、优惠券、满减满折、限时折扣、秒杀、砍价、拼团、积分商城、装修、客服
-- 本文件由 DDL 生成器产出，公共约定与 01-system.sql 一致
-- ----------------------------------------------------------------------------
USE `ccloud`;

-- ========================= 内容 =========================

DROP TABLE IF EXISTS `promotion_banner`;
CREATE TABLE `promotion_banner` (
  `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '编号',
  `title`          varchar(64)  NOT NULL DEFAULT '' COMMENT '标题',
  `url`            varchar(255) NOT NULL DEFAULT '' COMMENT '跳转链接',
  `pic_url`        varchar(255) NOT NULL DEFAULT '' COMMENT '图片链接',
  `sort`           int          NOT NULL DEFAULT 0 COMMENT '排序',
  `status`         tinyint      NOT NULL DEFAULT 0 COMMENT '状态：0 开启 1 关闭',
  `position`       tinyint      NOT NULL DEFAULT 1 COMMENT '位置：1 首页 2 秒杀活动页 3 砍价活动页 4 分销商品页',
  `memo`           varchar(255) DEFAULT NULL COMMENT '描述',
  `browse_count`   int          DEFAULT 0 COMMENT '点击次数',
  `tenant_id`      bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`        varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`        varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`        bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='首页轮播图表';
DROP TABLE IF EXISTS `promotion_article_category`;
CREATE TABLE `promotion_article_category` (
  `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '文章分类编号',
  `name`          varchar(255) NOT NULL COMMENT '分类名称',
  `pic_url`       varchar(255) DEFAULT NULL COMMENT '图标地址',
  `status`        tinyint      NOT NULL DEFAULT 0 COMMENT '状态：0 开启 1 关闭',
  `sort`          int          NOT NULL DEFAULT 0 COMMENT '排序',
  `tenant_id`     bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`       varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`       varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文章分类表';
DROP TABLE IF EXISTS `promotion_article`;
CREATE TABLE `promotion_article` (
  `id`                 bigint       NOT NULL AUTO_INCREMENT COMMENT '文章管理编号',
  `category_id`        bigint       NOT NULL COMMENT '分类编号',
  `spu_id`             bigint       NOT NULL DEFAULT 0 COMMENT '关联商品编号',
  `title`              varchar(255) NOT NULL COMMENT '文章标题',
  `author`             varchar(255) DEFAULT NULL COMMENT '文章作者',
  `pic_url`            varchar(255) NOT NULL COMMENT '文章封面图片',
  `introduction`       varchar(255) DEFAULT NULL COMMENT '文章简介',
  `browse_count`       varchar(255) DEFAULT '0' COMMENT '浏览次数',
  `sort`               int          NOT NULL DEFAULT 0 COMMENT '排序',
  `status`             tinyint      NOT NULL DEFAULT 0 COMMENT '状态：0 开启 1 关闭',
  `recommend_hot`      bit(1)       NOT NULL COMMENT '是否热门',
  `recommend_banner`   bit(1)       NOT NULL COMMENT '是否轮播图',
  `content`            longtext     NOT NULL COMMENT '文章内容',
  `tenant_id`          bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`            varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`            varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`            bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文章表';

-- ========================= 优惠券 =========================

DROP TABLE IF EXISTS `promotion_coupon_template`;
CREATE TABLE `promotion_coupon_template` (
  `id`                     bigint       NOT NULL AUTO_INCREMENT COMMENT '模板编号',
  `name`                   varchar(50)  NOT NULL COMMENT '优惠劵名',
  `description`            varchar(255) DEFAULT NULL COMMENT '优惠券说明',
  `status`                 tinyint      NOT NULL COMMENT '状态：0 开启 1 关闭',
  `total_count`            int          NOT NULL COMMENT '发放数量，-1 表示不限制',
  `take_limit_count`       tinyint      NOT NULL COMMENT '每人限领个数，-1 表示不限制',
  `take_type`              tinyint      NOT NULL COMMENT '领取方式：1 直接领取 2 指定发放 3 新人券',
  `use_price`              int          NOT NULL DEFAULT 0 COMMENT '满多少金额可用，0 表示无门槛，单位：分',
  `product_scope`          tinyint      NOT NULL COMMENT '商品范围：1 全部 2 指定商品 3 指定品类',
  `product_scope_values`   varchar(255) DEFAULT NULL COMMENT '商品范围编号数组',
  `validity_type`          tinyint      NOT NULL COMMENT '生效日期类型：1 固定日期 2 领取之后',
  `valid_start_time`       datetime     DEFAULT NULL COMMENT '固定日期-生效开始时间',
  `valid_end_time`         datetime     DEFAULT NULL COMMENT '固定日期-生效结束时间',
  `fixed_start_term`       int          DEFAULT NULL COMMENT '领取日期-开始天数',
  `fixed_end_term`         int          DEFAULT NULL COMMENT '领取日期-结束天数',
  `discount_type`          tinyint      NOT NULL COMMENT '折扣类型：1 满减 2 折扣',
  `discount_percent`       tinyint      DEFAULT NULL COMMENT '折扣百分比',
  `discount_price`         int          NOT NULL DEFAULT 0 COMMENT '优惠金额，单位：分',
  `discount_limit_price`   int          NOT NULL DEFAULT 0 COMMENT '折扣上限，仅折扣类型使用，单位：分',
  `take_count`             int          NOT NULL DEFAULT 0 COMMENT '已领取数量。领取须用 take_count = take_count + 1 WHERE take_count < total_count 原子更新',
  `use_count`              int          NOT NULL DEFAULT 0 COMMENT '已使用数量',
  `tenant_id`              bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`                varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`            datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`                varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`            datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`                bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='优惠劵模板表';
DROP TABLE IF EXISTS `promotion_coupon`;
CREATE TABLE `promotion_coupon` (
  `id`                     bigint       NOT NULL AUTO_INCREMENT COMMENT '优惠劵编号',
  `template_id`            bigint       NOT NULL COMMENT '优惠劵模板编号',
  `name`                   varchar(50)  NOT NULL COMMENT '优惠劵名',
  `status`                 tinyint      NOT NULL COMMENT '状态：1 未使用 2 已使用 3 已过期',
  `user_id`                bigint       NOT NULL COMMENT '用户编号',
  `take_type`              tinyint      NOT NULL COMMENT '领取类型',
  `use_price`              int          NOT NULL DEFAULT 0 COMMENT '满多少金额可用，单位：分',
  `valid_start_time`       datetime     NOT NULL COMMENT '生效开始时间',
  `valid_end_time`         datetime     NOT NULL COMMENT '生效结束时间',
  `product_scope`          tinyint      NOT NULL COMMENT '商品范围',
  `product_scope_values`   varchar(255) DEFAULT NULL COMMENT '商品范围编号数组',
  `discount_type`          tinyint      NOT NULL COMMENT '折扣类型',
  `discount_percent`       tinyint      DEFAULT NULL COMMENT '折扣百分比',
  `discount_price`         int          NOT NULL DEFAULT 0 COMMENT '优惠金额，单位：分',
  `discount_limit_price`   int          NOT NULL DEFAULT 0 COMMENT '折扣上限，单位：分',
  `use_order_id`           bigint       DEFAULT NULL COMMENT '使用订单编号',
  `use_time`               datetime     DEFAULT NULL COMMENT '使用时间',
  `tenant_id`              bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`                varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`            datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`                varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`            datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`                bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_user_status` (`user_id`, `status`),
  KEY `idx_template_id` (`template_id`),
  KEY `idx_status_valid_end` (`status`, `valid_end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='优惠劵表';

-- ========================= 满减满折与限时折扣 =========================

DROP TABLE IF EXISTS `promotion_reward_activity`;
CREATE TABLE `promotion_reward_activity` (
  `id`                     bigint        NOT NULL AUTO_INCREMENT COMMENT '活动编号',
  `name`                   varchar(255)  NOT NULL COMMENT '活动名称',
  `status`                 tinyint       NOT NULL DEFAULT 0 COMMENT '活动状态：0 开启 1 关闭',
  `start_time`             datetime      NOT NULL COMMENT '活动开始时间',
  `end_time`               datetime      NOT NULL COMMENT '活动结束时间',
  `remark`                 varchar(500)  DEFAULT NULL COMMENT '备注',
  `condition_type`         tinyint       NOT NULL DEFAULT 10 COMMENT '条件类型：10 满 N 元 20 满 N 件',
  `product_scope`          tinyint       NOT NULL COMMENT '商品范围',
  `product_scope_values`   varchar(255)  DEFAULT NULL COMMENT '商品范围编号数组',
  `rules`                  varchar(2000) DEFAULT NULL COMMENT '优惠规则数组，JSON',
  `tenant_id`              bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`                varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`            datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`                varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`            datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`                bit(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='满减送活动表';
DROP TABLE IF EXISTS `promotion_discount_activity`;
CREATE TABLE `promotion_discount_activity` (
  `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '活动编号',
  `name`          varchar(255) NOT NULL COMMENT '活动名称',
  `status`        tinyint      NOT NULL DEFAULT 0 COMMENT '活动状态：0 开启 1 关闭',
  `start_time`    datetime     NOT NULL COMMENT '活动开始时间',
  `end_time`      datetime     NOT NULL COMMENT '活动结束时间',
  `remark`        varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id`     bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`       varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`       varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='限时折扣活动表';
DROP TABLE IF EXISTS `promotion_discount_product`;
CREATE TABLE `promotion_discount_product` (
  `id`                    bigint       NOT NULL AUTO_INCREMENT COMMENT '编号',
  `activity_id`           bigint       NOT NULL COMMENT '活动编号',
  `spu_id`                bigint       NOT NULL COMMENT '商品 SPU 编号',
  `sku_id`                bigint       NOT NULL COMMENT '商品 SKU 编号',
  `discount_type`         tinyint      NOT NULL COMMENT '折扣类型：1 满减 2 折扣',
  `discount_percent`      tinyint      DEFAULT NULL COMMENT '折扣百分比',
  `discount_price`        int          NOT NULL DEFAULT 0 COMMENT '优惠金额，单位：分',
  `activity_name`         varchar(255) NOT NULL COMMENT '活动标题，冗余',
  `activity_status`       tinyint      NOT NULL DEFAULT 0 COMMENT '活动状态，冗余',
  `activity_start_time`   datetime     NOT NULL COMMENT '活动开始时间，冗余',
  `activity_end_time`     datetime     NOT NULL COMMENT '活动结束时间，冗余',
  `tenant_id`             bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`               varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`           datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`               varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`           datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`               bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_activity_id` (`activity_id`),
  KEY `idx_sku_id` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='限时折扣商品表';

-- ========================= 秒杀 =========================

DROP TABLE IF EXISTS `promotion_seckill_config`;
CREATE TABLE `promotion_seckill_config` (
  `id`                bigint        NOT NULL AUTO_INCREMENT COMMENT '编号',
  `name`              varchar(40)   NOT NULL COMMENT '秒杀时段名称',
  `start_time`        varchar(10)   NOT NULL COMMENT '开始时间点，HH:mm:ss',
  `end_time`          varchar(10)   NOT NULL COMMENT '结束时间点，HH:mm:ss',
  `slider_pic_urls`   varchar(2000) DEFAULT '' COMMENT '秒杀轮播图',
  `status`            tinyint       NOT NULL DEFAULT 0 COMMENT '状态：0 开启 1 关闭',
  `tenant_id`         bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`           varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`       datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`           varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`       datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`           bit(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='秒杀时段配置表';
DROP TABLE IF EXISTS `promotion_seckill_activity`;
CREATE TABLE `promotion_seckill_activity` (
  `id`                   bigint       NOT NULL AUTO_INCREMENT COMMENT '秒杀活动编号',
  `name`                 varchar(255) NOT NULL COMMENT '活动名称',
  `status`               tinyint      NOT NULL DEFAULT 0 COMMENT '活动状态：0 开启 1 关闭',
  `start_time`           datetime     NOT NULL COMMENT '活动开始时间',
  `end_time`             datetime     NOT NULL COMMENT '活动结束时间',
  `remark`               varchar(500) DEFAULT NULL COMMENT '备注',
  `spu_id`               bigint       NOT NULL COMMENT '秒杀商品编号',
  `sort`                 int          NOT NULL DEFAULT 0 COMMENT '排序',
  `config_ids`           varchar(255) NOT NULL COMMENT '秒杀时段编号数组',
  `total_limit_count`    int          DEFAULT 0 COMMENT '总限购数量',
  `single_limit_count`   int          DEFAULT 0 COMMENT '单次限够数量',
  `stock`                int          DEFAULT 0 COMMENT '秒杀库存，扣减须原子更新',
  `total_stock`          int          DEFAULT 0 COMMENT '秒杀总库存',
  `tenant_id`            bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`              varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`          datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`              varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`          datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`              bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_spu_id` (`spu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='秒杀活动表';
DROP TABLE IF EXISTS `promotion_seckill_product`;
CREATE TABLE `promotion_seckill_product` (
  `id`                    bigint       NOT NULL AUTO_INCREMENT COMMENT '秒杀参与商品编号',
  `activity_id`           bigint       NOT NULL COMMENT '秒杀活动编号',
  `config_ids`            varchar(255) NOT NULL COMMENT '秒杀时段编号数组',
  `spu_id`                bigint       NOT NULL COMMENT '商品 SPU 编号',
  `sku_id`                bigint       NOT NULL COMMENT '商品 SKU 编号',
  `seckill_price`         int          NOT NULL DEFAULT 0 COMMENT '秒杀金额，单位：分',
  `stock`                 int          NOT NULL COMMENT '秒杀库存，扣减须原子更新',
  `activity_status`       tinyint      NOT NULL DEFAULT 0 COMMENT '活动状态，冗余',
  `activity_start_time`   datetime     NOT NULL COMMENT '活动开始时间，冗余',
  `activity_end_time`     datetime     NOT NULL COMMENT '活动结束时间，冗余',
  `tenant_id`             bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`               varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`           datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`               varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`           datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`               bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_activity_id` (`activity_id`),
  UNIQUE KEY `uk_activity_sku` (`activity_id`, `sku_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='秒杀商品表';

-- ========================= 砍价 =========================

DROP TABLE IF EXISTS `promotion_bargain_activity`;
CREATE TABLE `promotion_bargain_activity` (
  `id`                    bigint       NOT NULL AUTO_INCREMENT COMMENT '砍价活动编号',
  `name`                  varchar(255) NOT NULL COMMENT '活动名称',
  `status`                tinyint      NOT NULL DEFAULT 0 COMMENT '活动状态：0 开启 1 关闭',
  `start_time`            datetime     NOT NULL COMMENT '活动开始时间',
  `end_time`              datetime     NOT NULL COMMENT '活动结束时间',
  `remark`                varchar(500) DEFAULT NULL COMMENT '备注',
  `spu_id`                bigint       NOT NULL COMMENT '商品 SPU 编号',
  `sku_id`                bigint       NOT NULL COMMENT '商品 SKU 编号',
  `bargain_first_price`   int          NOT NULL DEFAULT 0 COMMENT '砍价起始价格，单位：分',
  `bargain_min_price`     int          NOT NULL DEFAULT 0 COMMENT '砍价底价，单位：分',
  `stock`                 int          NOT NULL COMMENT '砍价库存，扣减须原子更新',
  `total_stock`           int          NOT NULL COMMENT '砍价总库存',
  `help_max_count`        int          NOT NULL COMMENT '砍价人数',
  `bargain_count`         int          NOT NULL COMMENT '帮砍次数',
  `total_limit_count`     int          NOT NULL COMMENT '总限购数量',
  `random_min_price`      int          NOT NULL DEFAULT 0 COMMENT '用户每次砍价的最小金额，单位：分',
  `random_max_price`      int          NOT NULL DEFAULT 0 COMMENT '用户每次砍价的最大金额，单位：分',
  `sort`                  int          NOT NULL DEFAULT 0 COMMENT '排序',
  `tenant_id`             bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`               varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`           datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`               varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`           datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`               bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='砍价活动表';
DROP TABLE IF EXISTS `promotion_bargain_record`;
CREATE TABLE `promotion_bargain_record` (
  `id`                    bigint      NOT NULL AUTO_INCREMENT COMMENT '编号',
  `activity_id`           bigint      NOT NULL COMMENT '砍价活动编号',
  `user_id`               bigint      NOT NULL COMMENT '用户编号',
  `spu_id`                bigint      NOT NULL COMMENT '商品 SPU 编号',
  `sku_id`                bigint      NOT NULL COMMENT '商品 SKU 编号',
  `bargain_first_price`   int         NOT NULL DEFAULT 0 COMMENT '砍价起始价格，单位：分',
  `bargain_price`         int         NOT NULL DEFAULT 0 COMMENT '当前砍价，单位：分',
  `status`                tinyint     NOT NULL COMMENT '砍价状态：1 砍价中 2 砍价成功 3 砍价失败',
  `end_time`              datetime    NOT NULL COMMENT '结束时间',
  `order_id`              bigint      DEFAULT NULL COMMENT '订单编号',
  `tenant_id`             bigint      NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`               varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time`           datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`               varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time`           datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`               bit(1)      NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_activity_user` (`activity_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='砍价记录表';
DROP TABLE IF EXISTS `promotion_bargain_help`;
CREATE TABLE `promotion_bargain_help` (
  `id`             bigint      NOT NULL AUTO_INCREMENT COMMENT '编号',
  `activity_id`    bigint      NOT NULL COMMENT '砍价活动编号',
  `record_id`      bigint      NOT NULL COMMENT '砍价记录编号',
  `user_id`        bigint      NOT NULL COMMENT '助力用户编号',
  `reduce_price`   int         NOT NULL DEFAULT 0 COMMENT '减少价格，单位：分',
  `tenant_id`      bigint      NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`        varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time`    datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`        varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time`    datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`        bit(1)      NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  UNIQUE KEY `uk_record_user` (`record_id`, `user_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='砍价助力表。同一用户对同一砍价记录只能助力一次';

-- ========================= 拼团 =========================

DROP TABLE IF EXISTS `promotion_combination_activity`;
CREATE TABLE `promotion_combination_activity` (
  `id`                   bigint       NOT NULL AUTO_INCREMENT COMMENT '活动编号',
  `name`                 varchar(255) NOT NULL COMMENT '活动名称',
  `status`               tinyint      NOT NULL DEFAULT 0 COMMENT '活动状态：0 开启 1 关闭',
  `start_time`           datetime     NOT NULL COMMENT '活动开始时间',
  `end_time`             datetime     NOT NULL COMMENT '活动结束时间',
  `remark`               varchar(500) DEFAULT NULL COMMENT '备注',
  `spu_id`               bigint       NOT NULL COMMENT '商品 SPU 编号',
  `total_limit_count`    int          NOT NULL COMMENT '总限购数量',
  `single_limit_count`   int          NOT NULL COMMENT '单次限购数量',
  `user_size`            int          NOT NULL COMMENT '几人团',
  `virtual_group`        bit(1)       NOT NULL COMMENT '是否虚拟成团',
  `limit_duration`       int          NOT NULL COMMENT '限制时长，单位小时',
  `tenant_id`            bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`              varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`          datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`              varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`          datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`              bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='拼团活动表';
DROP TABLE IF EXISTS `promotion_combination_product`;
CREATE TABLE `promotion_combination_product` (
  `id`                    bigint      NOT NULL AUTO_INCREMENT COMMENT '编号',
  `activity_id`           bigint      NOT NULL COMMENT '拼团活动编号',
  `spu_id`                bigint      NOT NULL COMMENT '商品 SPU 编号',
  `sku_id`                bigint      NOT NULL COMMENT '商品 SKU 编号',
  `combination_price`     int         NOT NULL DEFAULT 0 COMMENT '拼团价格，单位：分',
  `activity_status`       tinyint     NOT NULL DEFAULT 0 COMMENT '活动状态，冗余',
  `activity_start_time`   datetime    NOT NULL COMMENT '活动开始时间，冗余',
  `activity_end_time`     datetime    NOT NULL COMMENT '活动结束时间，冗余',
  `tenant_id`             bigint      NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`               varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time`           datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`               varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time`           datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`               bit(1)      NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  UNIQUE KEY `uk_activity_sku` (`activity_id`, `sku_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='拼团商品表';
DROP TABLE IF EXISTS `promotion_combination_record`;
CREATE TABLE `promotion_combination_record` (
  `id`                  bigint       NOT NULL AUTO_INCREMENT COMMENT '编号',
  `activity_id`         bigint       NOT NULL COMMENT '拼团活动编号',
  `spu_id`              bigint       NOT NULL COMMENT '商品 SPU 编号',
  `pic_url`             varchar(255) NOT NULL COMMENT '商品图片',
  `spu_name`            varchar(255) NOT NULL COMMENT '商品名称',
  `sku_id`              bigint       NOT NULL COMMENT '商品 SKU 编号',
  `count`               int          NOT NULL COMMENT '购买的商品数量',
  `user_id`             bigint       NOT NULL COMMENT '用户编号',
  `nickname`            varchar(255) NOT NULL COMMENT '用户昵称',
  `avatar`              varchar(255) NOT NULL COMMENT '用户头像',
  `head_id`             bigint       NOT NULL DEFAULT 0 COMMENT '团长编号，0 表示本条即团长',
  `status`              tinyint      NOT NULL COMMENT '拼团状态：0 进行中 1 成功 2 失败',
  `order_id`            bigint       NOT NULL COMMENT '订单编号',
  `user_size`           int          NOT NULL COMMENT '开团要求人数',
  `user_count`          int          NOT NULL COMMENT '已加入拼团人数',
  `virtual_group`       bit(1)       NOT NULL COMMENT '是否虚拟拼团',
  `combination_price`   int          NOT NULL DEFAULT 0 COMMENT '拼团商品单价，单位：分',
  `expire_time`         datetime     NOT NULL COMMENT '过期时间',
  `start_time`          datetime     NOT NULL COMMENT '开始时间',
  `end_time`            datetime     DEFAULT NULL COMMENT '结束时间',
  `tenant_id`           bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`             varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`         datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`             varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`         datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`             bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_head_id` (`head_id`),
  KEY `idx_status_expire` (`status`, `expire_time`),
  UNIQUE KEY `uk_order_id` (`order_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='拼团记录表。团长与团员各一条';

-- ========================= 积分商城 =========================

DROP TABLE IF EXISTS `promotion_point_activity`;
CREATE TABLE `promotion_point_activity` (
  `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '积分商城活动编号',
  `spu_id`        bigint       NOT NULL COMMENT '积分商城活动商品',
  `status`        tinyint      NOT NULL DEFAULT 0 COMMENT '状态：0 开启 1 关闭',
  `remark`        varchar(500) DEFAULT NULL COMMENT '备注',
  `sort`          int          NOT NULL DEFAULT 0 COMMENT '排序',
  `stock`         int          NOT NULL COMMENT '积分商城活动库存',
  `total_stock`   int          NOT NULL COMMENT '积分商城活动总库存',
  `tenant_id`     bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`       varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`       varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='积分商城活动表';
DROP TABLE IF EXISTS `promotion_point_product`;
CREATE TABLE `promotion_point_product` (
  `id`                bigint      NOT NULL AUTO_INCREMENT COMMENT '积分商城商品编号',
  `activity_id`       bigint      NOT NULL COMMENT '积分商城活动编号',
  `spu_id`            bigint      NOT NULL COMMENT '商品 SPU 编号',
  `sku_id`            bigint      NOT NULL COMMENT '商品 SKU 编号',
  `count`             int         NOT NULL COMMENT '可兑换次数',
  `point`             int         NOT NULL COMMENT '所需兑换积分',
  `price`             int         NOT NULL DEFAULT 0 COMMENT '所需兑换金额，单位：分',
  `stock`             int         NOT NULL COMMENT '积分商城商品库存',
  `activity_status`   tinyint     NOT NULL COMMENT '积分商城商品状态',
  `tenant_id`         bigint      NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`           varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time`       datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`           varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time`       datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`           bit(1)      NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  UNIQUE KEY `uk_activity_sku` (`activity_id`, `sku_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='积分商城商品表';

-- ========================= 装修与客服 =========================

DROP TABLE IF EXISTS `promotion_diy_template`;
CREATE TABLE `promotion_diy_template` (
  `id`                 bigint        NOT NULL AUTO_INCREMENT COMMENT '编号',
  `name`               varchar(100)  NOT NULL COMMENT '模板名称',
  `used`               bit(1)        NOT NULL DEFAULT b'0' COMMENT '是否使用',
  `used_time`          datetime      DEFAULT NULL COMMENT '使用时间',
  `remark`             varchar(500)  DEFAULT NULL COMMENT '备注',
  `preview_pic_urls`   varchar(2000) DEFAULT NULL COMMENT '预览图，JSON 数组',
  `property`           longtext      DEFAULT NULL COMMENT '模板属性，JSON',
  `tenant_id`          bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`            varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`        datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`            varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`        datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`            bit(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='装修模板表';
DROP TABLE IF EXISTS `promotion_diy_page`;
CREATE TABLE `promotion_diy_page` (
  `id`                 bigint        NOT NULL AUTO_INCREMENT COMMENT '编号',
  `template_id`        bigint        DEFAULT NULL COMMENT '装修模板编号',
  `name`               varchar(100)  NOT NULL COMMENT '页面名称',
  `remark`             varchar(500)  DEFAULT NULL COMMENT '备注',
  `preview_pic_urls`   varchar(2000) DEFAULT NULL COMMENT '预览图，JSON 数组',
  `property`           longtext      DEFAULT NULL COMMENT '页面属性，JSON',
  `tenant_id`          bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`            varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`        datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`            varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`        datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`            bit(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_template_id` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='装修页面表';
DROP TABLE IF EXISTS `promotion_kefu_conversation`;
CREATE TABLE `promotion_kefu_conversation` (
  `id`                           bigint        NOT NULL AUTO_INCREMENT COMMENT '编号',
  `user_id`                      bigint        NOT NULL COMMENT '会话所属用户',
  `last_message_time`            datetime      NOT NULL COMMENT '最后聊天时间',
  `last_message_content`         varchar(2048) NOT NULL COMMENT '最后聊天内容',
  `last_message_content_type`    int           NOT NULL COMMENT '最后发送的消息类型',
  `admin_pinned`                 bit(1)        NOT NULL DEFAULT b'0' COMMENT '管理端是否置顶',
  `user_deleted`                 bit(1)        NOT NULL DEFAULT b'0' COMMENT '用户是否可见',
  `admin_deleted`                bit(1)        NOT NULL DEFAULT b'0' COMMENT '管理员是否可见',
  `admin_unread_message_count`   int           NOT NULL DEFAULT 0 COMMENT '管理员未读消息数',
  `tenant_id`                    bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`                      varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`                  datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`                      varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`                  datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`                      bit(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  UNIQUE KEY `uk_user_id` (`user_id`, `tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客服会话表';
DROP TABLE IF EXISTS `promotion_kefu_message`;
CREATE TABLE `promotion_kefu_message` (
  `id`                bigint        NOT NULL AUTO_INCREMENT COMMENT '编号',
  `conversation_id`   bigint        NOT NULL COMMENT '会话编号',
  `sender_id`         bigint        NOT NULL COMMENT '发送人编号',
  `sender_type`       int           NOT NULL COMMENT '发送人类型',
  `receiver_id`       bigint        DEFAULT NULL COMMENT '接收人编号',
  `receiver_type`     int           DEFAULT NULL COMMENT '接收人类型',
  `content_type`      int           NOT NULL COMMENT '消息类型',
  `content`           varchar(2048) NOT NULL COMMENT '消息',
  `read_status`       bit(1)        NOT NULL DEFAULT b'0' COMMENT '是否已读',
  `tenant_id`         bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`           varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`       datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`           varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`       datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`           bit(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_conversation_id` (`conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客服消息表';
