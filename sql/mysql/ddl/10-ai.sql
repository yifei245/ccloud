-- ----------------------------------------------------------------------------
-- ai 模块：API 密钥、模型、角色、对话、绘图、音乐、写作、思维导图、知识库
-- 本文件由 DDL 生成器产出，公共约定与 01-system.sql 一致
-- ----------------------------------------------------------------------------
USE `ccloud`;

-- ========================= 模型与密钥 =========================

DROP TABLE IF EXISTS `ai_api_key`;
CREATE TABLE `ai_api_key` (
  `id`            bigint        NOT NULL AUTO_INCREMENT COMMENT '编号',
  `name`          varchar(255)  NOT NULL COMMENT '名称',
  `api_key`       varchar(1024) NOT NULL COMMENT '密钥，加密存储',
  `platform`      varchar(255)  NOT NULL COMMENT '平台，如 OpenAI / Anthropic / DeepSeek / 通义千问',
  `url`           varchar(255)  DEFAULT NULL COMMENT '自定义 API 地址',
  `status`        tinyint       NOT NULL DEFAULT 0 COMMENT '状态：0 开启 1 关闭',
  `tenant_id`     bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`       varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`       varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       bit(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI API 密钥表';
DROP TABLE IF EXISTS `ai_model`;
CREATE TABLE `ai_model` (
  `id`             bigint      NOT NULL AUTO_INCREMENT COMMENT '编号',
  `key_id`         bigint      NOT NULL COMMENT 'API 密钥编号',
  `name`           varchar(64) NOT NULL COMMENT '模型名字',
  `model`          varchar(64) NOT NULL COMMENT '模型标识',
  `platform`       varchar(32) NOT NULL COMMENT '模型平台',
  `type`           tinyint     NOT NULL COMMENT '模型类型：1 对话 2 图片 3 语音 4 视频 5 向量 6 重排序',
  `sort`           int         NOT NULL DEFAULT 0 COMMENT '排序',
  `status`         tinyint     NOT NULL DEFAULT 0 COMMENT '状态：0 开启 1 关闭',
  `temperature`    double      DEFAULT NULL COMMENT '温度参数',
  `max_tokens`     int         DEFAULT NULL COMMENT '单条回复的最大 Token 数量',
  `max_contexts`   int         DEFAULT NULL COMMENT '上下文的最大消息数量',
  `tenant_id`      bigint      NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`        varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time`    datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`        varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time`    datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`        bit(1)      NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_key_id` (`key_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 模型表';
DROP TABLE IF EXISTS `ai_chat_role`;
CREATE TABLE `ai_chat_role` (
  `id`               bigint        NOT NULL AUTO_INCREMENT COMMENT '角色编号',
  `model_id`         bigint        DEFAULT NULL COMMENT '模型编号',
  `name`             varchar(128)  NOT NULL COMMENT '角色名称',
  `avatar`           varchar(256)  NOT NULL COMMENT '头像',
  `category`         varchar(32)   DEFAULT NULL COMMENT '角色类别',
  `description`      varchar(256)  NOT NULL COMMENT '角色描述',
  `system_message`   varchar(1024) DEFAULT NULL COMMENT '角色设定',
  `user_id`          bigint        DEFAULT NULL COMMENT '用户编号，为空表示公共角色',
  `knowledge_ids`    varchar(255)  DEFAULT NULL COMMENT '引用的知识库编号数组',
  `public_status`    bit(1)        NOT NULL COMMENT '是否公开',
  `sort`             int           NOT NULL DEFAULT 0 COMMENT '排序',
  `status`           tinyint       NOT NULL DEFAULT 0 COMMENT '状态：0 开启 1 关闭',
  `tenant_id`        bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`          varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`      datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`          varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`      datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`          bit(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 聊天角色表';

-- ========================= 对话 =========================

DROP TABLE IF EXISTS `ai_chat_conversation`;
CREATE TABLE `ai_chat_conversation` (
  `id`               bigint        NOT NULL AUTO_INCREMENT COMMENT '对话编号',
  `user_id`          bigint        NOT NULL COMMENT '用户编号',
  `role_id`          bigint        DEFAULT NULL COMMENT '聊天角色',
  `title`            varchar(256)  NOT NULL COMMENT '对话标题',
  `model_id`         bigint        NOT NULL COMMENT '模型编号',
  `model`            varchar(32)   NOT NULL COMMENT '模型标识',
  `pinned`           bit(1)        NOT NULL COMMENT '是否置顶',
  `pinned_time`      datetime      DEFAULT NULL COMMENT '置顶时间',
  `system_message`   varchar(1024) DEFAULT NULL COMMENT '角色设定',
  `temperature`      double        NOT NULL COMMENT '温度参数',
  `max_tokens`       int           NOT NULL COMMENT '单条回复的最大 Token 数量',
  `max_contexts`     int           NOT NULL COMMENT '上下文的最大消息数量',
  `tenant_id`        bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`          varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`      datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`          varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`      datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`          bit(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 聊天对话表';
DROP TABLE IF EXISTS `ai_chat_message`;
CREATE TABLE `ai_chat_message` (
  `id`                bigint        NOT NULL AUTO_INCREMENT COMMENT '消息编号',
  `conversation_id`   bigint        NOT NULL COMMENT '对话编号',
  `reply_id`          bigint        DEFAULT NULL COMMENT '回复编号',
  `type`              varchar(16)   NOT NULL COMMENT '消息类型：user / assistant / system',
  `user_id`           bigint        NOT NULL COMMENT '用户编号',
  `role_id`           bigint        DEFAULT NULL COMMENT '角色编号',
  `model`             varchar(32)   NOT NULL COMMENT '模型标识',
  `model_id`          bigint        NOT NULL COMMENT '模型编号',
  `content`           longtext      NOT NULL COMMENT '消息内容',
  `use_context`       bit(1)        DEFAULT NULL COMMENT '是否携带上下文',
  `segment_ids`       varchar(2048) DEFAULT NULL COMMENT '引用的知识库段落编号数组',
  `tenant_id`         bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`           varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`       datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`           varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`       datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`           bit(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_conversation_id` (`conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 聊天消息表';

-- ========================= 创作 =========================

DROP TABLE IF EXISTS `ai_image`;
CREATE TABLE `ai_image` (
  `id`              bigint        NOT NULL AUTO_INCREMENT COMMENT '编号',
  `user_id`         bigint        NOT NULL COMMENT '用户编号',
  `prompt`          varchar(2000) NOT NULL COMMENT '提示词',
  `platform`        varchar(64)   NOT NULL COMMENT '平台',
  `model_id`        bigint        NOT NULL COMMENT '模型编号',
  `model`           varchar(64)   NOT NULL COMMENT '模型标识',
  `width`           int           NOT NULL COMMENT '图片宽度',
  `height`          int           NOT NULL COMMENT '图片高度',
  `status`          tinyint       NOT NULL COMMENT '绘画状态：10 进行中 20 成功 30 失败',
  `finish_time`     datetime      DEFAULT NULL COMMENT '完成时间',
  `error_message`   varchar(1024) DEFAULT NULL COMMENT '错误信息',
  `pic_url`         varchar(2048) DEFAULT NULL COMMENT '图片地址',
  `public_status`   bit(1)        NOT NULL DEFAULT b'0' COMMENT '是否公开',
  `options`         varchar(1024) DEFAULT NULL COMMENT '绘制参数，JSON',
  `buttons`         varchar(2048) DEFAULT NULL COMMENT 'Midjourney 按钮，JSON',
  `task_id`         varchar(1024) DEFAULT NULL COMMENT '任务编号',
  `tenant_id`       bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`         varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`     datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`         varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`     datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`         bit(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 绘画表';
DROP TABLE IF EXISTS `ai_music`;
CREATE TABLE `ai_music` (
  `id`              bigint        NOT NULL AUTO_INCREMENT COMMENT '编号',
  `user_id`         bigint        NOT NULL COMMENT '用户编号',
  `title`           varchar(200)  NOT NULL COMMENT '音乐名称',
  `lyric`           text          DEFAULT NULL COMMENT '歌词',
  `image_url`       varchar(600)  DEFAULT NULL COMMENT '图片地址',
  `audio_url`       varchar(600)  DEFAULT NULL COMMENT '音频地址',
  `video_url`       varchar(600)  DEFAULT NULL COMMENT '视频地址',
  `status`          tinyint       DEFAULT NULL COMMENT '音乐状态',
  `description`     varchar(1000) DEFAULT NULL COMMENT '描述词',
  `prompt`          varchar(1000) DEFAULT NULL COMMENT '提示词',
  `platform`        varchar(64)   NOT NULL COMMENT '模型平台',
  `model`           varchar(64)   NOT NULL COMMENT '模型',
  `generate_mode`   tinyint       DEFAULT NULL COMMENT '生成模式',
  `tags`            varchar(600)  DEFAULT NULL COMMENT '音乐风格标签',
  `duration`        double        DEFAULT NULL COMMENT '音乐时长',
  `public_status`   bit(1)        NOT NULL DEFAULT b'0' COMMENT '是否发布',
  `task_id`         varchar(255)  DEFAULT NULL COMMENT '任务编号',
  `error_message`   varchar(1024) DEFAULT NULL COMMENT '错误信息',
  `tenant_id`       bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`         varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`     datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`         varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`     datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`         bit(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 音乐表';
DROP TABLE IF EXISTS `ai_write`;
CREATE TABLE `ai_write` (
  `id`                  bigint        NOT NULL AUTO_INCREMENT COMMENT '编号',
  `user_id`             bigint        NOT NULL COMMENT '用户编号',
  `type`                tinyint       DEFAULT NULL COMMENT '写作类型：1 撰写 2 回复',
  `platform`            varchar(255)  NOT NULL COMMENT '平台',
  `model_id`            bigint        NOT NULL COMMENT '模型编号',
  `model`               varchar(255)  NOT NULL COMMENT '模型',
  `prompt`              text          DEFAULT NULL COMMENT '生成内容提示',
  `generated_content`   text          DEFAULT NULL COMMENT '生成的内容',
  `original_content`    text          DEFAULT NULL COMMENT '原文',
  `length`              tinyint       DEFAULT NULL COMMENT '长度提示词',
  `format`              tinyint       DEFAULT NULL COMMENT '格式提示词',
  `tone`                tinyint       DEFAULT NULL COMMENT '语气提示词',
  `language`            tinyint       DEFAULT NULL COMMENT '语言提示词',
  `error_message`       varchar(1024) DEFAULT NULL COMMENT '错误信息',
  `tenant_id`           bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`             varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`         datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`             varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`         datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`             bit(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 写作表';
DROP TABLE IF EXISTS `ai_mind_map`;
CREATE TABLE `ai_mind_map` (
  `id`                  bigint        NOT NULL AUTO_INCREMENT COMMENT '编号',
  `user_id`             bigint        NOT NULL COMMENT '用户编号',
  `platform`            varchar(255)  NOT NULL COMMENT '平台',
  `model_id`            bigint        NOT NULL COMMENT '模型编号',
  `model`               varchar(255)  NOT NULL COMMENT '模型',
  `prompt`              varchar(2000) NOT NULL COMMENT '生成内容提示',
  `generated_content`   varchar(3072) DEFAULT NULL COMMENT '生成的内容，Markdown',
  `error_message`       varchar(1024) DEFAULT NULL COMMENT '错误信息',
  `tenant_id`           bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`             varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`         datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`             varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`         datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`             bit(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 思维导图表';

-- ========================= 知识库 =========================

DROP TABLE IF EXISTS `ai_knowledge`;
CREATE TABLE `ai_knowledge` (
  `id`                     bigint        NOT NULL AUTO_INCREMENT COMMENT '编号',
  `name`                   varchar(255)  NOT NULL COMMENT '知识库名称',
  `description`            varchar(1024) DEFAULT NULL COMMENT '知识库描述',
  `embedding_model_id`     bigint        NOT NULL COMMENT '向量模型编号',
  `embedding_model`        varchar(32)   NOT NULL COMMENT '向量模型标识',
  `top_k`                  int           NOT NULL COMMENT '召回 topK',
  `similarity_threshold`   double        NOT NULL COMMENT '相似度阈值',
  `status`                 tinyint       NOT NULL DEFAULT 0 COMMENT '状态：0 开启 1 关闭',
  `tenant_id`              bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`                varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`            datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`                varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`            datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`                bit(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 知识库表';
DROP TABLE IF EXISTS `ai_knowledge_document`;
CREATE TABLE `ai_knowledge_document` (
  `id`                   bigint        NOT NULL AUTO_INCREMENT COMMENT '编号',
  `knowledge_id`         bigint        NOT NULL COMMENT '知识库编号',
  `name`                 varchar(255)  NOT NULL COMMENT '文档名称',
  `url`                  varchar(1024) NOT NULL COMMENT '文件地址',
  `content`              longtext      NOT NULL COMMENT '内容',
  `content_length`       int           NOT NULL COMMENT '字符数',
  `tokens`               int           NOT NULL COMMENT 'Token 数量',
  `segment_max_tokens`   int           NOT NULL COMMENT '分片最大 Token 数',
  `retrieval_count`      int           NOT NULL DEFAULT 0 COMMENT '召回次数',
  `status`               tinyint       NOT NULL DEFAULT 0 COMMENT '状态：0 开启 1 关闭',
  `tenant_id`            bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`              varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`          datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`              varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`          datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`              bit(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_knowledge_id` (`knowledge_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 知识库文档表';
DROP TABLE IF EXISTS `ai_knowledge_segment`;
CREATE TABLE `ai_knowledge_segment` (
  `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '编号',
  `knowledge_id`      bigint       NOT NULL COMMENT '知识库编号',
  `document_id`       bigint       NOT NULL COMMENT '文档编号',
  `content`           longtext     NOT NULL COMMENT '分段内容',
  `content_length`    int          NOT NULL COMMENT '字符数',
  `vector_id`         varchar(100) DEFAULT NULL COMMENT '向量库中的编号',
  `tokens`            int          NOT NULL COMMENT 'Token 数量',
  `retrieval_count`   int          NOT NULL DEFAULT 0 COMMENT '召回次数',
  `status`            tinyint      NOT NULL DEFAULT 0 COMMENT '状态：0 开启 1 关闭',
  `tenant_id`         bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`           varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`           varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`           bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_document_id` (`document_id`),
  KEY `idx_knowledge_id` (`knowledge_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 知识库分段表。向量本身存于向量库，此处保存原文与向量编号';
