-- ----------------------------------------------------------------------------
-- product 模块：商品分类、品牌、属性、SPU/SKU、评价、收藏、浏览记录
-- 本文件由 DDL 生成器产出，公共约定与 01-system.sql 一致
-- ----------------------------------------------------------------------------
USE `ccloud`;
-- 库存扣减必须用 stock = stock - ? WHERE stock >= ? 的原子更新，禁止先查后改——
-- 秒杀、拼团的并发下，先查后改必然超卖。

-- ========================= 分类与品牌 =========================

DROP TABLE IF EXISTS `product_category`;
CREATE TABLE `product_category` (
  `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '分类编号',
  `parent_id`     bigint       NOT NULL COMMENT '父分类编号，0 表示一级分类',
  `name`          varchar(255) NOT NULL COMMENT '分类名称',
  `pic_url`       varchar(255) NOT NULL COMMENT '分类图',
  `sort`          int          NOT NULL DEFAULT 0 COMMENT '排序',
  `status`        tinyint      NOT NULL DEFAULT 0 COMMENT '状态：0 开启 1 关闭',
  `tenant_id`     bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`       varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`       varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品分类表';
DROP TABLE IF EXISTS `product_brand`;
CREATE TABLE `product_brand` (
  `id`            bigint        NOT NULL AUTO_INCREMENT COMMENT '品牌编号',
  `name`          varchar(255)  NOT NULL COMMENT '品牌名称',
  `pic_url`       varchar(255)  NOT NULL COMMENT '品牌图片',
  `sort`          int           NOT NULL DEFAULT 0 COMMENT '排序',
  `description`   varchar(1024) DEFAULT '' COMMENT '品牌描述',
  `status`        tinyint       NOT NULL DEFAULT 0 COMMENT '状态：0 开启 1 关闭',
  `tenant_id`     bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`       varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`       varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       bit(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品品牌表';

-- ========================= 属性 =========================

DROP TABLE IF EXISTS `product_property`;
CREATE TABLE `product_property` (
  `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '编号',
  `name`          varchar(64)  NOT NULL DEFAULT '' COMMENT '名称',
  `remark`        varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id`     bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`       varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`       varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品属性项表，如颜色、尺码';
DROP TABLE IF EXISTS `product_property_value`;
CREATE TABLE `product_property_value` (
  `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '编号',
  `property_id`   bigint       NOT NULL COMMENT '属性项编号',
  `name`          varchar(128) NOT NULL DEFAULT '' COMMENT '名称',
  `remark`        varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id`     bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`       varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`       varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_property_id` (`property_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品属性值表，如红色、XL';

-- ========================= SPU 与 SKU =========================

DROP TABLE IF EXISTS `product_spu`;
CREATE TABLE `product_spu` (
  `id`                     bigint        NOT NULL AUTO_INCREMENT COMMENT '商品编号',
  `name`                   varchar(128)  NOT NULL COMMENT '商品名称',
  `keyword`                varchar(256)  NOT NULL DEFAULT '' COMMENT '关键字',
  `introduction`           varchar(256)  NOT NULL DEFAULT '' COMMENT '商品简介',
  `description`            text          DEFAULT NULL COMMENT '商品详情，富文本',
  `category_id`            bigint        NOT NULL COMMENT '商品分类编号',
  `brand_id`               bigint        DEFAULT NULL COMMENT '商品品牌编号',
  `pic_url`                varchar(256)  NOT NULL COMMENT '商品封面图',
  `slider_pic_urls`        varchar(2000) DEFAULT '' COMMENT '商品轮播图，JSON 数组',
  `sort`                   int           NOT NULL DEFAULT 0 COMMENT '排序',
  `status`                 tinyint       NOT NULL COMMENT '商品状态：-1 回收站 0 下架 1 上架',
  `spec_type`              bit(1)        NOT NULL DEFAULT b'0' COMMENT '规格类型：0 单规格 1 多规格',
  `price`                  int           NOT NULL DEFAULT 0 COMMENT '商品价格，取 SKU 最低价，单位：分',
  `market_price`           int           NOT NULL DEFAULT 0 COMMENT '市场价，单位：分',
  `cost_price`             int           NOT NULL DEFAULT 0 COMMENT '成本价，单位：分',
  `stock`                  int           NOT NULL DEFAULT 0 COMMENT '库存，SKU 库存之和',
  `delivery_types`         varchar(32)   NOT NULL DEFAULT '' COMMENT '配送方式数组',
  `delivery_template_id`   bigint        DEFAULT NULL COMMENT '物流配置模板编号',
  `give_integral`          int           NOT NULL DEFAULT 0 COMMENT '赠送积分',
  `sub_commission_type`    bit(1)        NOT NULL DEFAULT b'0' COMMENT '分销类型：0 默认 1 自行设置',
  `sales_count`            int           DEFAULT 0 COMMENT '商品销量',
  `virtual_sales_count`    int           DEFAULT 0 COMMENT '虚拟销量',
  `browse_count`           int           DEFAULT 0 COMMENT '浏览量',
  `tenant_id`              bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`                varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`            datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`                varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`            datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`                bit(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品 SPU 表';
DROP TABLE IF EXISTS `product_sku`;
CREATE TABLE `product_sku` (
  `id`                       bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `spu_id`                   bigint       NOT NULL COMMENT 'SPU 编号',
  `properties`               varchar(512) DEFAULT NULL COMMENT '属性数组，JSON',
  `price`                    int          NOT NULL DEFAULT 0 COMMENT '商品价格，单位：分',
  `market_price`             int          NOT NULL DEFAULT 0 COMMENT '市场价，单位：分',
  `cost_price`               int          NOT NULL DEFAULT 0 COMMENT '成本价，单位：分',
  `bar_code`                 varchar(64)  DEFAULT NULL COMMENT '商品条码',
  `pic_url`                  varchar(256) NOT NULL COMMENT '图片地址',
  `stock`                    int          NOT NULL DEFAULT 0 COMMENT '库存，扣减须原子更新',
  `weight`                   double       DEFAULT NULL COMMENT '商品重量，单位 kg',
  `volume`                   double       DEFAULT NULL COMMENT '商品体积，单位 m³',
  `first_brokerage_price`    int          NOT NULL DEFAULT 0 COMMENT '一级分销佣金，单位：分',
  `second_brokerage_price`   int          NOT NULL DEFAULT 0 COMMENT '二级分销佣金，单位：分',
  `sales_count`              int          DEFAULT 0 COMMENT '商品销量',
  `tenant_id`                bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`                  varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`              datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`                  varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`              datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`                  bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_spu_id` (`spu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品 SKU 表';

-- ========================= 评价、收藏、浏览 =========================

DROP TABLE IF EXISTS `product_comment`;
CREATE TABLE `product_comment` (
  `id`                   bigint        NOT NULL AUTO_INCREMENT COMMENT '评论编号',
  `user_id`              bigint        DEFAULT NULL COMMENT '评价人用户编号',
  `user_nickname`        varchar(255)  DEFAULT NULL COMMENT '评价人昵称',
  `user_avatar`          varchar(1024) DEFAULT NULL COMMENT '评价人头像',
  `anonymous`            bit(1)        DEFAULT NULL COMMENT '是否匿名',
  `order_id`             bigint        DEFAULT NULL COMMENT '交易订单编号',
  `order_item_id`        bigint        DEFAULT NULL COMMENT '交易订单项编号',
  `spu_id`               bigint        DEFAULT NULL COMMENT '商品 SPU 编号',
  `spu_name`             varchar(255)  DEFAULT NULL COMMENT '商品 SPU 名称',
  `sku_id`               bigint        DEFAULT NULL COMMENT '商品 SKU 编号',
  `sku_pic_url`          varchar(1024) DEFAULT NULL COMMENT '商品 SKU 图片',
  `sku_properties`       varchar(512)  DEFAULT NULL COMMENT '商品 SKU 属性，JSON',
  `visible`              bit(1)        DEFAULT NULL COMMENT '是否可见',
  `scores`               tinyint       DEFAULT NULL COMMENT '评分星级 1-5',
  `description_scores`   tinyint       DEFAULT NULL COMMENT '描述星级',
  `benefit_scores`       tinyint       DEFAULT NULL COMMENT '服务星级',
  `content`              varchar(1024) DEFAULT NULL COMMENT '评价内容',
  `pic_urls`             varchar(4096) DEFAULT NULL COMMENT '评价图片地址数组',
  `reply_status`         bit(1)        DEFAULT NULL COMMENT '商家是否回复',
  `reply_user_id`        bigint        DEFAULT NULL COMMENT '回复管理员编号',
  `reply_content`        varchar(1024) DEFAULT NULL COMMENT '商家回复内容',
  `reply_time`           datetime      DEFAULT NULL COMMENT '商家回复时间',
  `tenant_id`            bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`              varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`          datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`              varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`          datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`              bit(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_spu_id` (`spu_id`),
  UNIQUE KEY `uk_order_item` (`order_item_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品评价表';
DROP TABLE IF EXISTS `product_favorite`;
CREATE TABLE `product_favorite` (
  `id`            bigint      NOT NULL AUTO_INCREMENT COMMENT '编号',
  `user_id`       bigint      NOT NULL COMMENT '用户编号',
  `spu_id`        bigint      NOT NULL COMMENT '商品 SPU 编号',
  `tenant_id`     bigint      NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`       varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`       varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       bit(1)      NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  UNIQUE KEY `uk_user_spu` (`user_id`, `spu_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品收藏表';
DROP TABLE IF EXISTS `product_browse_history`;
CREATE TABLE `product_browse_history` (
  `id`             bigint      NOT NULL AUTO_INCREMENT COMMENT '记录编号',
  `user_id`        bigint      NOT NULL COMMENT '用户编号',
  `spu_id`         bigint      NOT NULL COMMENT '商品 SPU 编号',
  `user_deleted`   bit(1)      NOT NULL DEFAULT b'0' COMMENT '用户是否删除。与逻辑删除分开，运营统计仍需保留',
  `tenant_id`      bigint      NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`        varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time`    datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`        varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time`    datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`        bit(1)      NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_user_create_time` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品浏览记录表';
