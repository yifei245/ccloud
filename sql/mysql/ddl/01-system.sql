-- ----------------------------------------------------------------------------
-- system 模块：认证、用户、权限、组织、字典、租户、消息、日志
--
-- 公共约定（全库一致）：
--   审计字段 creator / create_time / updater / update_time / deleted
--   需租户隔离的表带 tenant_id 并建索引
--   状态字段统一 status tinyint，0=开启 1=关闭
-- ----------------------------------------------------------------------------
USE `ccloud`;

-- ========================= 组织架构 =========================

DROP TABLE IF EXISTS `system_dept`;
CREATE TABLE `system_dept` (
  `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '部门编号',
  `name`           varchar(30)  NOT NULL DEFAULT '' COMMENT '部门名称',
  `parent_id`      bigint       NOT NULL DEFAULT 0 COMMENT '父部门编号，0 表示顶级',
  `sort`           int          NOT NULL DEFAULT 0 COMMENT '显示顺序',
  `leader_user_id` bigint       DEFAULT NULL COMMENT '负责人用户编号',
  `phone`          varchar(11)  DEFAULT NULL COMMENT '联系电话',
  `email`          varchar(50)  DEFAULT NULL COMMENT '邮箱',
  `status`         tinyint      NOT NULL DEFAULT 0 COMMENT '状态：0 开启 1 关闭',
  `tenant_id`      bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`        varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`        varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`        bigint       NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='部门表';

DROP TABLE IF EXISTS `system_post`;
CREATE TABLE `system_post` (
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '岗位编号',
  `code`        varchar(64)  NOT NULL COMMENT '岗位编码',
  `name`        varchar(50)  NOT NULL COMMENT '岗位名称',
  `sort`        int          NOT NULL DEFAULT 0 COMMENT '显示顺序',
  `status`      tinyint      NOT NULL DEFAULT 0 COMMENT '状态：0 开启 1 关闭',
  `remark`      varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id`   bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`     varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`     varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     bigint       NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='岗位表';

-- ========================= 用户与权限 =========================

DROP TABLE IF EXISTS `system_users`;
CREATE TABLE `system_users` (
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '用户编号',
  `username`    varchar(30)  NOT NULL COMMENT '用户账号',
  `password`    varchar(100) NOT NULL DEFAULT '' COMMENT '密码，BCrypt 单向哈希',
  `nickname`    varchar(30)  NOT NULL COMMENT '用户昵称',
  `remark`      varchar(500) DEFAULT NULL COMMENT '备注',
  `dept_id`     bigint       DEFAULT NULL COMMENT '部门编号',
  `post_ids`    varchar(255) DEFAULT NULL COMMENT '岗位编号数组，逗号分隔',
  `email`       varchar(50)  DEFAULT '' COMMENT '用户邮箱',
  `mobile`      varchar(11)  DEFAULT '' COMMENT '手机号码',
  `sex`         tinyint      DEFAULT 0 COMMENT '用户性别：0 未知 1 男 2 女',
  `avatar`      varchar(512) DEFAULT '' COMMENT '头像地址',
  `status`      tinyint      NOT NULL DEFAULT 0 COMMENT '状态：0 开启 1 关闭',
  `login_ip`    varchar(50)  DEFAULT '' COMMENT '最后登录 IP',
  `login_date`  datetime     DEFAULT NULL COMMENT '最后登录时间',
  `tenant_id`   bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`     varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`     varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     bigint       NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  -- 唯一性必须带上 tenant_id 与 deleted：不同租户可以有同名账号，
  -- 且逻辑删除后该账号应能被重新注册
  UNIQUE KEY `uk_username` (`username`, `tenant_id`, `deleted`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_mobile` (`mobile`),
  KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管理后台用户表';

DROP TABLE IF EXISTS `system_role`;
CREATE TABLE `system_role` (
  `id`                  bigint       NOT NULL AUTO_INCREMENT COMMENT '角色编号',
  `name`                varchar(30)  NOT NULL COMMENT '角色名称',
  `code`                varchar(100) NOT NULL COMMENT '角色标识',
  `sort`                int          NOT NULL DEFAULT 0 COMMENT '显示顺序',
  `data_scope`          tinyint      NOT NULL DEFAULT 1 COMMENT '数据范围：1 全部 2 指定部门 3 本部门 4 本部门及以下 5 仅本人',
  `data_scope_dept_ids` varchar(500) NOT NULL DEFAULT '' COMMENT '数据范围指定的部门编号数组',
  `status`              tinyint      NOT NULL DEFAULT 0 COMMENT '状态：0 开启 1 关闭',
  `type`                tinyint      NOT NULL DEFAULT 2 COMMENT '角色类型：1 内置 2 自定义',
  `remark`              varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id`           bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`             varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`         datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`             varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`         datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`             bigint       NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表';

DROP TABLE IF EXISTS `system_menu`;
CREATE TABLE `system_menu` (
  `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '菜单编号',
  `name`           varchar(50)  NOT NULL COMMENT '菜单名称',
  `permission`     varchar(100) NOT NULL DEFAULT '' COMMENT '权限标识，如 system:user:create',
  `type`           tinyint      NOT NULL COMMENT '类型：1 目录 2 菜单 3 按钮',
  `sort`           int          NOT NULL DEFAULT 0 COMMENT '显示顺序',
  `parent_id`      bigint       NOT NULL DEFAULT 0 COMMENT '父菜单编号',
  `path`           varchar(200) DEFAULT '' COMMENT '路由地址',
  `icon`           varchar(100) DEFAULT '#' COMMENT '菜单图标',
  `component`      varchar(255) DEFAULT NULL COMMENT '组件路径',
  `component_name` varchar(255) DEFAULT NULL COMMENT '组件名',
  `status`         tinyint      NOT NULL DEFAULT 0 COMMENT '状态：0 开启 1 关闭',
  `visible`        bit(1)       NOT NULL DEFAULT b'1' COMMENT '是否可见',
  `keep_alive`     bit(1)       NOT NULL DEFAULT b'1' COMMENT '是否缓存',
  `always_show`    bit(1)       NOT NULL DEFAULT b'1' COMMENT '是否总是显示',
  `creator`        varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`        varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`        bigint       NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜单权限表，全局共享不做租户隔离，租户可见范围由套餐限定';

DROP TABLE IF EXISTS `system_user_role`;
CREATE TABLE `system_user_role` (
  `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '编号',
  `user_id`     bigint      NOT NULL COMMENT '用户编号',
  `role_id`     bigint      NOT NULL COMMENT '角色编号',
  `tenant_id`   bigint      NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`     varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`     varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     bigint      NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户和角色关联表';

DROP TABLE IF EXISTS `system_role_menu`;
CREATE TABLE `system_role_menu` (
  `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '编号',
  `role_id`     bigint      NOT NULL COMMENT '角色编号',
  `menu_id`     bigint      NOT NULL COMMENT '菜单编号',
  `tenant_id`   bigint      NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`     varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`     varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     bigint      NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_menu_id` (`menu_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色和菜单关联表';

DROP TABLE IF EXISTS `system_user_post`;
CREATE TABLE `system_user_post` (
  `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '编号',
  `user_id`     bigint      NOT NULL DEFAULT 0 COMMENT '用户编号',
  `post_id`     bigint      NOT NULL DEFAULT 0 COMMENT '岗位编号',
  `tenant_id`   bigint      NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`     varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`     varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     bigint      NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户岗位关联表';

-- ========================= 租户 =========================

DROP TABLE IF EXISTS `system_tenant`;
CREATE TABLE `system_tenant` (
  `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '租户编号',
  `name`            varchar(30)  NOT NULL COMMENT '租户名',
  `contact_user_id` bigint       DEFAULT NULL COMMENT '联系人的用户编号',
  `contact_name`    varchar(30)  NOT NULL COMMENT '联系人',
  `contact_mobile`  varchar(500) DEFAULT NULL COMMENT '联系手机',
  `status`          tinyint      NOT NULL DEFAULT 0 COMMENT '状态：0 正常 1 停用',
  `website`         varchar(256) DEFAULT '' COMMENT '绑定域名',
  `package_id`      bigint       NOT NULL COMMENT '租户套餐编号，0 表示系统租户不受套餐限制',
  `expire_time`     datetime     NOT NULL COMMENT '过期时间',
  `account_count`   int          NOT NULL COMMENT '账号数量上限',
  `creator`         varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`         varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`         bigint       NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户表，自身不做租户隔离';

DROP TABLE IF EXISTS `system_tenant_package`;
CREATE TABLE `system_tenant_package` (
  `id`          bigint        NOT NULL AUTO_INCREMENT COMMENT '套餐编号',
  `name`        varchar(30)   NOT NULL COMMENT '套餐名',
  `status`      tinyint       NOT NULL DEFAULT 0 COMMENT '状态：0 正常 1 停用',
  `remark`      varchar(256)  DEFAULT '' COMMENT '备注',
  `menu_ids`    varchar(4096) NOT NULL COMMENT '关联的菜单编号',
  `creator`     varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time` datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`     varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time` datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     bigint        NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户套餐表';

-- ========================= 字典 =========================

DROP TABLE IF EXISTS `system_dict_type`;
CREATE TABLE `system_dict_type` (
  `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '字典主键',
  `name`         varchar(100) NOT NULL DEFAULT '' COMMENT '字典名称',
  `type`         varchar(100) NOT NULL DEFAULT '' COMMENT '字典类型',
  `status`       tinyint      NOT NULL DEFAULT 0 COMMENT '状态：0 开启 1 关闭',
  `remark`       varchar(500) DEFAULT NULL COMMENT '备注',
  `creator`      varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`      varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`      bigint       NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type` (`type`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典类型表，全局共享';

DROP TABLE IF EXISTS `system_dict_data`;
CREATE TABLE `system_dict_data` (
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '字典编码',
  `sort`        int          NOT NULL DEFAULT 0 COMMENT '字典排序',
  `label`       varchar(100) NOT NULL DEFAULT '' COMMENT '字典标签',
  `value`       varchar(100) NOT NULL DEFAULT '' COMMENT '字典键值',
  `dict_type`   varchar(100) NOT NULL DEFAULT '' COMMENT '字典类型',
  `status`      tinyint      NOT NULL DEFAULT 0 COMMENT '状态：0 开启 1 关闭',
  `color_type`  varchar(100) DEFAULT '' COMMENT '颜色类型',
  `css_class`   varchar(100) DEFAULT '' COMMENT 'CSS 样式',
  `remark`      varchar(500) DEFAULT NULL COMMENT '备注',
  `creator`     varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`     varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     bigint       NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  KEY `idx_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典数据表，全局共享';

-- ========================= OAuth2 =========================

DROP TABLE IF EXISTS `system_oauth2_client`;
CREATE TABLE `system_oauth2_client` (
  `id`                              bigint        NOT NULL AUTO_INCREMENT COMMENT '编号',
  `client_id`                       varchar(255)  NOT NULL COMMENT '客户端编号',
  `secret`                          varchar(255)  NOT NULL COMMENT '客户端密钥',
  `name`                            varchar(255)  NOT NULL COMMENT '应用名',
  `logo`                            varchar(255)  NOT NULL DEFAULT '' COMMENT '应用图标',
  `description`                     varchar(255)  DEFAULT NULL COMMENT '应用描述',
  `status`                          tinyint       NOT NULL DEFAULT 0 COMMENT '状态：0 开启 1 关闭',
  `access_token_validity_seconds`   int           NOT NULL COMMENT '访问令牌的有效期，单位秒',
  `refresh_token_validity_seconds`  int           NOT NULL COMMENT '刷新令牌的有效期，单位秒',
  `redirect_uris`                   varchar(255)  NOT NULL COMMENT '可重定向的地址，JSON 数组。SSO 授权码模式必须校验，防开放重定向',
  `authorized_grant_types`          varchar(255)  NOT NULL COMMENT '支持的授权类型，JSON 数组',
  `scopes`                          varchar(255)  NOT NULL DEFAULT '' COMMENT '授权范围，JSON 数组',
  `auto_approve_scopes`             varchar(255)  NOT NULL DEFAULT '' COMMENT '自动通过的授权范围，JSON 数组',
  `authorities`                     varchar(255)  NOT NULL DEFAULT '' COMMENT '权限，JSON 数组',
  `resource_ids`                    varchar(255)  NOT NULL DEFAULT '' COMMENT '资源，JSON 数组',
  `additional_information`          varchar(4096) NOT NULL DEFAULT '' COMMENT '附加信息，JSON 对象',
  `creator`                         varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`                     datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`                         varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`                     datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`                         bigint        NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_client_id` (`client_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='OAuth2 客户端表，每个终端一条记录';

DROP TABLE IF EXISTS `system_oauth2_access_token`;
CREATE TABLE `system_oauth2_access_token` (
  `id`            bigint        NOT NULL AUTO_INCREMENT COMMENT '编号',
  `user_id`       bigint        NOT NULL COMMENT '用户编号',
  `user_type`     tinyint       NOT NULL COMMENT '用户类型：1 会员 2 管理员',
  `user_info`     varchar(512)  NOT NULL DEFAULT '' COMMENT '用户信息快照，JSON',
  `access_token`  varchar(255)  NOT NULL COMMENT '访问令牌',
  `refresh_token` varchar(32)   NOT NULL COMMENT '刷新令牌',
  `client_id`     varchar(255)  NOT NULL COMMENT '客户端编号',
  `scopes`        varchar(255)  DEFAULT '' COMMENT '授权范围，JSON 数组',
  `expires_time`  datetime      NOT NULL COMMENT '过期时间',
  `tenant_id`     bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`       varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`       varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       bigint        NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_access_token` (`access_token`),
  KEY `idx_refresh_token` (`refresh_token`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_expires_time` (`expires_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='OAuth2 访问令牌表，Redis 为主此表为回源兜底';

DROP TABLE IF EXISTS `system_oauth2_refresh_token`;
CREATE TABLE `system_oauth2_refresh_token` (
  `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '编号',
  `user_id`       bigint       NOT NULL COMMENT '用户编号',
  `user_type`     tinyint      NOT NULL COMMENT '用户类型：1 会员 2 管理员',
  `refresh_token` varchar(32)  NOT NULL COMMENT '刷新令牌',
  `client_id`     varchar(255) NOT NULL COMMENT '客户端编号',
  `scopes`        varchar(255) DEFAULT '' COMMENT '授权范围，JSON 数组',
  `expires_time`  datetime     NOT NULL COMMENT '过期时间',
  `tenant_id`     bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`       varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`       varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       bigint       NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refresh_token` (`refresh_token`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='OAuth2 刷新令牌表';

DROP TABLE IF EXISTS `system_oauth2_code`;
CREATE TABLE `system_oauth2_code` (
  `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '编号',
  `user_id`      bigint       NOT NULL COMMENT '用户编号',
  `user_type`    tinyint      NOT NULL COMMENT '用户类型：1 会员 2 管理员',
  `code`         varchar(32)  NOT NULL COMMENT '授权码',
  `client_id`    varchar(255) NOT NULL COMMENT '客户端编号',
  `scopes`       varchar(255) DEFAULT '' COMMENT '授权范围，JSON 数组',
  `expires_time` datetime     NOT NULL COMMENT '过期时间，授权码有效期 5 分钟',
  `redirect_uri` varchar(255) DEFAULT NULL COMMENT '重定向地址。换取令牌时必须与签发时一致',
  `state`        varchar(255) DEFAULT '' COMMENT '状态，由调用方回传用于防 CSRF',
  `tenant_id`    bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`      varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`      varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`      bigint       NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='OAuth2 授权码表，SSO 授权码模式的中转凭据';

DROP TABLE IF EXISTS `system_oauth2_approve`;
CREATE TABLE `system_oauth2_approve` (
  `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '编号',
  `user_id`      bigint       NOT NULL COMMENT '用户编号',
  `user_type`    tinyint      NOT NULL COMMENT '用户类型：1 会员 2 管理员',
  `client_id`    varchar(255) NOT NULL COMMENT '客户端编号',
  `scope`        varchar(255) NOT NULL DEFAULT '' COMMENT '授权范围',
  `approved`     bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否同意',
  `expires_time` datetime     NOT NULL COMMENT '过期时间',
  `tenant_id`    bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`      varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`      varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`      bigint       NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`, `user_type`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='OAuth2 用户授权记录表，统一登出时据此反查已接入的应用';

-- ========================= 社交登录 =========================

DROP TABLE IF EXISTS `system_social_client`;
CREATE TABLE `system_social_client` (
  `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '编号',
  `name`          varchar(255) NOT NULL COMMENT '应用名',
  `social_type`   tinyint      NOT NULL COMMENT '社交平台类型',
  `user_type`     tinyint      NOT NULL COMMENT '用户类型：1 会员 2 管理员',
  `client_id`     varchar(255) NOT NULL COMMENT '客户端编号',
  `client_secret` varchar(255) NOT NULL COMMENT '客户端密钥',
  `agent_id`      varchar(255) DEFAULT NULL COMMENT '代理编号，企业微信等平台使用',
  `status`        tinyint      NOT NULL DEFAULT 0 COMMENT '状态：0 开启 1 关闭',
  `tenant_id`     bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`       varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`       varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       bigint       NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='社交客户端表';

DROP TABLE IF EXISTS `system_social_user`;
CREATE TABLE `system_social_user` (
  `id`             bigint        NOT NULL AUTO_INCREMENT COMMENT '编号',
  `type`           tinyint       NOT NULL COMMENT '社交平台类型',
  `openid`         varchar(64)   NOT NULL COMMENT '社交 openid',
  `token`          varchar(256)  DEFAULT NULL COMMENT '社交 token',
  `raw_token_info` varchar(1024) NOT NULL COMMENT '原始 token 数据，JSON',
  `nickname`       varchar(32)   NOT NULL COMMENT '用户昵称',
  `avatar`         varchar(255)  DEFAULT NULL COMMENT '用户头像',
  `raw_user_info`  varchar(1024) NOT NULL COMMENT '原始用户数据，JSON',
  `code`           varchar(256)  NOT NULL COMMENT '最后一次的认证 code',
  `state`          varchar(256)  DEFAULT NULL COMMENT '最后一次的认证 state',
  `tenant_id`      bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`        varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`    datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`        varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`    datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`        bigint        NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  KEY `idx_type_openid` (`type`, `openid`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='社交用户表，保存从社交平台拉取的原始信息';

DROP TABLE IF EXISTS `system_social_user_bind`;
CREATE TABLE `system_social_user_bind` (
  `id`             bigint      NOT NULL AUTO_INCREMENT COMMENT '编号',
  `user_id`        bigint      NOT NULL COMMENT '关联的本系统用户编号',
  `user_type`      tinyint     NOT NULL COMMENT '用户类型：1 会员 2 管理员',
  `social_type`    tinyint     NOT NULL COMMENT '社交平台类型',
  `social_user_id` bigint      NOT NULL COMMENT '社交用户编号',
  `tenant_id`      bigint      NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`        varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time`    datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`        varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time`    datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`        bigint      NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`, `user_type`),
  KEY `idx_social_user_id` (`social_user_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='社交用户绑定表';

-- ========================= 短信 =========================

DROP TABLE IF EXISTS `system_sms_channel`;
CREATE TABLE `system_sms_channel` (
  `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '编号',
  `signature`    varchar(12)  NOT NULL COMMENT '短信签名',
  `code`         varchar(63)  NOT NULL COMMENT '渠道编码',
  `status`       tinyint      NOT NULL COMMENT '状态：0 开启 1 关闭',
  `remark`       varchar(255) DEFAULT NULL COMMENT '备注',
  `api_key`      varchar(128) NOT NULL COMMENT '短信 API 账号',
  `api_secret`   varchar(128) DEFAULT NULL COMMENT '短信 API 密钥',
  `callback_url` varchar(255) DEFAULT NULL COMMENT '短信发送回调地址',
  `creator`      varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`      varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`      bigint       NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='短信渠道表';

DROP TABLE IF EXISTS `system_sms_template`;
CREATE TABLE `system_sms_template` (
  `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '编号',
  `type`            tinyint      NOT NULL COMMENT '模板类型：1 验证码 2 通知 3 营销',
  `status`          tinyint      NOT NULL COMMENT '状态：0 开启 1 关闭',
  `code`            varchar(63)  NOT NULL COMMENT '模板编码',
  `name`            varchar(63)  NOT NULL COMMENT '模板名称',
  `content`         varchar(255) NOT NULL COMMENT '模板内容',
  `params`          varchar(255) NOT NULL COMMENT '参数数组，JSON',
  `remark`          varchar(255) DEFAULT NULL COMMENT '备注',
  `api_template_id` varchar(63)  NOT NULL COMMENT '短信 API 的模板编号',
  `channel_id`      bigint       NOT NULL COMMENT '短信渠道编号',
  `channel_code`    varchar(63)  NOT NULL COMMENT '短信渠道编码',
  `creator`         varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`         varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`         bigint       NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='短信模板表';

DROP TABLE IF EXISTS `system_sms_log`;
CREATE TABLE `system_sms_log` (
  `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '编号',
  `channel_id`        bigint       NOT NULL COMMENT '短信渠道编号',
  `channel_code`      varchar(63)  NOT NULL COMMENT '短信渠道编码',
  `template_id`       bigint       NOT NULL COMMENT '模板编号',
  `template_code`     varchar(63)  NOT NULL COMMENT '模板编码',
  `template_type`     tinyint      NOT NULL COMMENT '短信类型',
  `template_content`  varchar(255) NOT NULL COMMENT '渲染后的短信内容',
  `template_params`   varchar(255) NOT NULL COMMENT '参数，JSON',
  `api_template_id`   varchar(63)  NOT NULL COMMENT '短信 API 的模板编号',
  `mobile`            varchar(11)  NOT NULL COMMENT '手机号',
  `user_id`           bigint       DEFAULT NULL COMMENT '用户编号',
  `user_type`         tinyint      DEFAULT NULL COMMENT '用户类型',
  `send_status`       tinyint      NOT NULL DEFAULT 0 COMMENT '发送状态：0 初始化 10 发送成功 20 发送失败 30 不发送',
  `send_time`         datetime     DEFAULT NULL COMMENT '发送时间',
  `api_send_code`     varchar(63)  DEFAULT NULL COMMENT 'API 发送结果码',
  `api_send_msg`      varchar(255) DEFAULT NULL COMMENT 'API 发送失败原因',
  `api_request_id`    varchar(255) DEFAULT NULL COMMENT 'API 请求编号',
  `api_serial_no`     varchar(255) DEFAULT NULL COMMENT 'API 序列号',
  `receive_status`    tinyint      NOT NULL DEFAULT 0 COMMENT '接收状态：0 初始化 10 接收成功 20 接收失败',
  `receive_time`      datetime     DEFAULT NULL COMMENT '接收时间',
  `api_receive_code`  varchar(63)  DEFAULT NULL COMMENT 'API 接收结果码',
  `api_receive_msg`   varchar(255) DEFAULT NULL COMMENT 'API 接收结果说明',
  `tenant_id`         bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`           varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`           varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`           bigint       NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  KEY `idx_mobile` (`mobile`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='短信日志表';

DROP TABLE IF EXISTS `system_sms_code`;
CREATE TABLE `system_sms_code` (
  `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '编号',
  `mobile`      varchar(11) NOT NULL COMMENT '手机号',
  `code`        varchar(6)  NOT NULL COMMENT '验证码',
  `create_ip`   varchar(15) NOT NULL COMMENT '创建 IP',
  `scene`       tinyint     NOT NULL COMMENT '发送场景',
  `today_index` tinyint     NOT NULL COMMENT '今日发送的第几条，用于限制每日发送次数',
  `used`        tinyint     NOT NULL COMMENT '是否使用：0 未使用 1 已使用',
  `used_time`   datetime    DEFAULT NULL COMMENT '使用时间',
  `used_ip`     varchar(255) DEFAULT NULL COMMENT '使用 IP',
  `tenant_id`   bigint      NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`     varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`     varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     bigint      NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  KEY `idx_mobile` (`mobile`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='短信验证码表';

-- ========================= 邮件 =========================

DROP TABLE IF EXISTS `system_mail_account`;
CREATE TABLE `system_mail_account` (
  `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '编号',
  `mail`            varchar(255) NOT NULL COMMENT '邮箱',
  `username`        varchar(255) NOT NULL COMMENT '用户名',
  `password`        varchar(255) NOT NULL COMMENT '密码',
  `host`            varchar(255) NOT NULL COMMENT 'SMTP 服务器域名',
  `port`            int          NOT NULL COMMENT 'SMTP 服务器端口',
  `ssl_enable`      bit(1)       NOT NULL COMMENT '是否开启 SSL',
  `starttls_enable` bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否开启 STARTTLS',
  `creator`         varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`         varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`         bigint       NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='邮箱账号表';

DROP TABLE IF EXISTS `system_mail_template`;
CREATE TABLE `system_mail_template` (
  `id`          bigint        NOT NULL AUTO_INCREMENT COMMENT '编号',
  `name`        varchar(63)   NOT NULL COMMENT '模板名称',
  `code`        varchar(63)   NOT NULL COMMENT '模板编码',
  `account_id`  bigint        NOT NULL COMMENT '发送的邮箱账号编号',
  `nickname`    varchar(255)  DEFAULT NULL COMMENT '发送人名称',
  `title`       varchar(255)  NOT NULL COMMENT '模板标题',
  `content`     varchar(10240) NOT NULL COMMENT '模板内容',
  `params`      varchar(255)  NOT NULL COMMENT '参数数组，JSON',
  `status`      tinyint       NOT NULL COMMENT '状态：0 开启 1 关闭',
  `remark`      varchar(255)  DEFAULT NULL COMMENT '备注',
  `creator`     varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time` datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`     varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time` datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     bigint        NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='邮件模板表';

DROP TABLE IF EXISTS `system_mail_log`;
CREATE TABLE `system_mail_log` (
  `id`                bigint        NOT NULL AUTO_INCREMENT COMMENT '编号',
  `user_id`           bigint        DEFAULT NULL COMMENT '用户编号',
  `user_type`         tinyint       DEFAULT NULL COMMENT '用户类型',
  `to_mail`           varchar(255)  NOT NULL COMMENT '接收邮箱地址',
  `account_id`        bigint        NOT NULL COMMENT '邮箱账号编号',
  `from_mail`         varchar(255)  NOT NULL COMMENT '发送邮箱地址',
  `template_id`       bigint        NOT NULL COMMENT '模板编号',
  `template_code`     varchar(63)   NOT NULL COMMENT '模板编码',
  `template_nickname` varchar(255)  DEFAULT NULL COMMENT '模板发送人名称',
  `template_title`    varchar(255)  NOT NULL COMMENT '邮件标题',
  `template_content`  varchar(10240) NOT NULL COMMENT '邮件内容',
  `template_params`   varchar(255)  NOT NULL COMMENT '邮件参数，JSON',
  `send_status`       tinyint       NOT NULL DEFAULT 0 COMMENT '发送状态：0 初始化 10 发送成功 20 发送失败 30 不发送',
  `send_time`         datetime      DEFAULT NULL COMMENT '发送时间',
  `send_message_id`   varchar(255)  DEFAULT NULL COMMENT '发送返回的消息编号',
  `send_exception`    varchar(4096) DEFAULT NULL COMMENT '发送异常',
  `tenant_id`         bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`           varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`       datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`           varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`       datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`           bigint        NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='邮件日志表';

-- ========================= 站内信与公告 =========================

DROP TABLE IF EXISTS `system_notify_template`;
CREATE TABLE `system_notify_template` (
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '编号',
  `name`        varchar(63)  NOT NULL COMMENT '模板名称',
  `code`        varchar(64)  NOT NULL COMMENT '模板编码',
  `nickname`    varchar(255) NOT NULL COMMENT '发送人名称',
  `content`     varchar(1024) NOT NULL COMMENT '模板内容',
  `type`        tinyint      NOT NULL COMMENT '模板类型',
  `params`      varchar(255) NOT NULL COMMENT '参数数组，JSON',
  `status`      tinyint      NOT NULL COMMENT '状态：0 开启 1 关闭',
  `remark`      varchar(255) DEFAULT NULL COMMENT '备注',
  `creator`     varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`     varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     bigint       NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='站内信模板表';

DROP TABLE IF EXISTS `system_notify_message`;
CREATE TABLE `system_notify_message` (
  `id`                bigint        NOT NULL AUTO_INCREMENT COMMENT '编号',
  `user_id`           bigint        NOT NULL COMMENT '用户编号',
  `user_type`         tinyint       NOT NULL COMMENT '用户类型',
  `template_id`       bigint        NOT NULL COMMENT '模板编号',
  `template_code`     varchar(64)   NOT NULL COMMENT '模板编码',
  `template_nickname` varchar(63)   NOT NULL COMMENT '发送人名称',
  `template_content`  varchar(1024) NOT NULL COMMENT '渲染后的内容',
  `template_type`     int           NOT NULL COMMENT '模板类型',
  `template_params`   varchar(255)  NOT NULL COMMENT '参数，JSON',
  `read_status`       bit(1)        NOT NULL COMMENT '是否已读',
  `read_time`         datetime      DEFAULT NULL COMMENT '阅读时间',
  `tenant_id`         bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`           varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`       datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`           varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`       datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`           bigint        NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  KEY `idx_user_read` (`user_id`, `user_type`, `read_status`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='站内信消息表';

DROP TABLE IF EXISTS `system_notice`;
CREATE TABLE `system_notice` (
  `id`          bigint        NOT NULL AUTO_INCREMENT COMMENT '公告编号',
  `title`       varchar(50)   NOT NULL COMMENT '公告标题',
  `content`     text          NOT NULL COMMENT '公告内容',
  `type`        tinyint       NOT NULL COMMENT '公告类型：1 通知 2 公告',
  `status`      tinyint       NOT NULL DEFAULT 0 COMMENT '状态：0 开启 1 关闭',
  `tenant_id`   bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`     varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time` datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`     varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time` datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     bigint        NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知公告表';

-- ========================= 日志 =========================

DROP TABLE IF EXISTS `system_login_log`;
CREATE TABLE `system_login_log` (
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '访问编号',
  `log_type`    bigint       NOT NULL COMMENT '日志类型：100 账号登录 101 社交登录 102 手机登录 200 主动登出 201 强制登出',
  `trace_id`    varchar(64)  NOT NULL DEFAULT '' COMMENT '链路追踪编号',
  `user_id`     bigint       NOT NULL DEFAULT 0 COMMENT '用户编号',
  `user_type`   tinyint      NOT NULL DEFAULT 0 COMMENT '用户类型',
  `username`    varchar(50)  NOT NULL DEFAULT '' COMMENT '用户账号',
  `result`      tinyint      NOT NULL COMMENT '登录结果：0 成功 10 账号或密码不正确 20 用户被禁用 30 验证码不存在 40 验证码不正确',
  `user_ip`     varchar(50)  NOT NULL COMMENT '用户 IP',
  `user_agent`  varchar(512) NOT NULL COMMENT '浏览器 UA',
  `tenant_id`   bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`     varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`     varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     bigint       NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  KEY `idx_username` (`username`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统访问记录表';

DROP TABLE IF EXISTS `system_operate_log`;
CREATE TABLE `system_operate_log` (
  `id`             bigint        NOT NULL AUTO_INCREMENT COMMENT '日志主键',
  `trace_id`       varchar(64)   NOT NULL DEFAULT '' COMMENT '链路追踪编号',
  `user_id`        bigint        NOT NULL COMMENT '用户编号',
  `user_type`      tinyint       NOT NULL DEFAULT 0 COMMENT '用户类型',
  `type`           varchar(50)   NOT NULL DEFAULT '' COMMENT '操作模块类型',
  `sub_type`       varchar(50)   NOT NULL COMMENT '操作名',
  `biz_id`         bigint        NOT NULL COMMENT '操作数据模块编号',
  `action`         varchar(2000) NOT NULL COMMENT '操作内容',
  `success`        bit(1)        NOT NULL DEFAULT b'1' COMMENT '操作结果',
  `extra`          varchar(2000) NOT NULL COMMENT '拓展字段，JSON',
  `request_method` varchar(16)   DEFAULT '' COMMENT '请求方法名',
  `request_url`    varchar(255)  DEFAULT '' COMMENT '请求地址',
  `user_ip`        varchar(50)   DEFAULT NULL COMMENT '用户 IP',
  `user_agent`     varchar(200)  DEFAULT NULL COMMENT '浏览器 UA',
  `tenant_id`      bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`        varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`    datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`        varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`    datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`        bigint        NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_biz` (`type`, `biz_id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志记录表';

-- ========================= 国际化 =========================

DROP TABLE IF EXISTS `system_i18n_message`;
CREATE TABLE `system_i18n_message` (
  `id`          bigint        NOT NULL AUTO_INCREMENT COMMENT '编号',
  `locale`      varchar(16)   NOT NULL COMMENT '语言，如 zh_CN / en_US',
  `code`        varchar(255)  NOT NULL COMMENT '文案编码',
  `content`     varchar(2048) NOT NULL COMMENT '文案内容',
  `source`      tinyint       NOT NULL DEFAULT 1 COMMENT '来源：1 系统文案 2 业务文案',
  `biz_type`    varchar(64)   DEFAULT NULL COMMENT '业务类型，source=2 时使用',
  `biz_id`      bigint        DEFAULT NULL COMMENT '业务编号，source=2 时使用',
  `field`       varchar(64)   DEFAULT NULL COMMENT '业务字段，source=2 时使用',
  `creator`     varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time` datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`     varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time` datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     bigint        NOT NULL DEFAULT 0 COMMENT '是否删除：0 未删除，删除后为记录编号，使唯一索引不与已删除的记录冲突',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_locale_code` (`locale`, `code`, `deleted`),
  KEY `idx_biz` (`biz_type`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='国际化文案表，覆盖 properties 基线';
