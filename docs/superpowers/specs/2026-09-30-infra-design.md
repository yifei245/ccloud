# 基础设施模块设计（P2 第 2 项）

> 状态：已确认（2026-09-30）
> 上游：[总体设计](2026-09-07-ccloud-design.md) 的 P2「module-infra 全量」；P1 已交付 infra 全部表结构、菜单与按钮权限、字典、网关路由、API 日志的框架契约与本地消息表。

## 1. 目标与范围

新建 `ccloud-module-infra`（`infra-api` + `infra-biz`），服务名 `ccloud-infra-server`，端口 28082，提供参数配置、文件存储、API 日志、消息死信、数据源、数据库文档、代码生成；并把 system / member 中等待 infra 的三处接上（初始密码、注册开关、头像上传）。

### 1.1 已确认的取舍

| 问题 | 结论 |
|---|---|
| 定时任务管理 | 不做。任务统一在 XXL-Job 控制台维护；删除 `infra_job`、`infra_job_log` 两张表及对应菜单、字典、文案 |
| 文件存储器 | 数据库、本地磁盘、S3 兼容三种；不做 FTP / SFTP。S3 用本机长期保留的 MinIO 测试（安装前单独征得同意） |
| API 日志传输 | Redis Stream：各服务发送，infra 消费写库；定时清理 |
| 参数配置 | 全局一份，不分租户；其他服务经 RPC 读取 |
| 代码生成 | 单表 + 树表，场景分管理后台 / 用户 App；只生成后端代码与 SQL；以真实编译并运行生成的测试为验收 |
| 数据库文档 | 自研，读 `information_schema` 输出 HTML 与 Markdown，不引入 screw |
| 文件访问 | 公开读：固定地址，任何人可读 |

### 1.2 非目标

- 前端工程（沿用总体设计）。
- 主子表模板（表中 `master_table_id / sub_join_column_id / sub_join_many` 列保留，日后可加）。
- 私有读、签名地址。
- 参数按租户覆盖。
- MySQL 以外的数据源。

## 2. 服务骨架

- 结构与 member 一致：启动类、安全白名单、`RpcConfiguration` 显式列出调用 system 的客户端、四个 profile 与 logback。
- 权限、租户、字典、国际化、操作日志五个框架契约经 RPC 由 system 实现（与 member 相同，依赖以 `@Lazy` 注入）。
- infra 自身的 API 访问 / 异常日志直接写本地表，不走消息流。
- 注册为 XXL-Job 执行器 `ccloud-infra-server`。
- 网关路由（`/admin-api/infra/**`、`/app-api/infra/**`）与菜单权限 P1 已预置。

### 2.1 租户划分

| 按租户隔离 | 全局共享（列入 `ccloud.tenant.ignore-tables`） |
|---|---|
| `infra_file`、`infra_api_access_log`、`infra_api_error_log` | `infra_config`、`infra_file_config`、`infra_file_content`、`infra_data_source_config`、`infra_codegen_table`、`infra_codegen_column`、`infra_message_consume_log`、`infra_demo_*` |

`infra_message_outbox`、`infra_message_dead_letter` 带租户列，按现状处理；死信管理由平台管理员跨租户查看，查询时忽略租户。

### 2.2 内部接口（`infra-api`，`/rpc-api/infra/**`）

| 接口 | 方法 | 调用方 |
|---|---|---|
| `ConfigApi` | `getConfigValueByKey(String key)`：返回参数值，不存在返回 null | system |
| `FileApi` | `createFile(FileCreateReqDTO{name, content(byte[])})`：按主配置存储，返回访问地址 | system、member（头像） |

API 日志的消息主题常量（`ApiLogTopics`）放在 `infra-api`；消息体直接使用 starter-web 中已有的 `ApiAccessLog` / `ApiErrorLog`（补充 `tenantId` 字段），发送方与 infra 本就共用该 starter，不再另建一套字段相同的消息类。

## 3. 参数配置

- 管理端 `/admin-api/infra/config`：`create / update / delete / get / page / export-excel`，权限 `infra:config:*`。
  - 键名全局唯一。
  - 系统内置参数（`type=1`）不可删除，键名不可修改。
- `GET /admin-api/infra/config/get-value-by-key`：登录即可调用，只返回可见参数，不可见的报"参数不可见"。
- infra 端按键名缓存到 Redis，修改或删除时清除。
- system 的两处读取都直接调 `ConfigApi`，不加本地缓存（低频操作，避免改后要等缓存过期）：
  - 导入用户的初始密码读 `system.user.init-password`；取不到值或 infra 不可用时拒绝导入。
  - 管理员自助注册开关读 `system.auth.register-enabled`；取不到值或 infra 不可用时按关闭处理。
  - 删除 system 配置文件中的 `ccloud.system.auth.register-enabled` 与 `ccloud.system.user.init-password`。
- 种子数据：
  - 新增 `system.auth.register-enabled = false`。
  - 删除 `system.captcha.enable`、`system.login.max-fail-count`、`system.login.lock-minutes` 三条：它们一直由 system 配置文件控制，留在表里会误导。

## 4. 文件

### 4.1 文件配置

- 管理端 `/admin-api/infra/file-config`：`create / update / delete / get / page / update-master / test`，权限 `infra:file-config:*`（`update-master` 归 `update`，`test` 归 `query`）。
- 存储器与配置（`config` 列 JSON，按存储器校验）：

  | 存储器 | 配置项 |
  |---|---|
  | 1 数据库 | `domain` |
  | 10 本地磁盘 | `basePath`、`domain` |
  | 20 S3 兼容 | `endpoint`、`bucket`、`accessKey`、`accessSecret`、`region`、`pathStyle`、`domain` |

- 返回时 `accessSecret` 显示为掩码；修改时提交的仍是掩码则沿用原值。
- 全局只有一个主配置，`update-master` 在同一事务内切换；主配置不可删除。
- `test`：用该配置上传一个小文件并返回地址。
- 各节点按配置编号缓存存储客户端；配置变更后经 Redis 发布订阅通知所有节点重建（与权限、国际化缓存的刷新方式一致）。
- S3 客户端用 AWS SDK v2，HTTP 实现取 `url-connection-client`，排除 Netty 与 Apache 客户端。

### 4.2 上传

- 入口：
  - `POST /admin-api/infra/file/upload`：管理员登录即可；
  - `POST /app-api/infra/file/upload`：会员登录即可；
  - `FileApi.createFile`。
- 类型：用 Tika 按内容识别，只收白名单类型；扩展名按识别结果重新确定，不信任原文件名。
  - 白名单：JPEG、PNG、GIF、WebP、BMP、PDF、Office 文档（doc/docx/xls/xlsx/ppt/pptx）、ZIP、纯文本、CSV、MP3、MP4。
  - HTML、SVG、XML、脚本一律拒绝。
- 大小：默认上限 10MB，配置项 `ccloud.infra.file.max-size`；Spring multipart 与网关的上限同步设置。
- 路径：`yyyyMMdd/{内容 SHA-256}.{扩展名}`。
  - 同一配置下路径已存在就不再写存储；
  - 每次上传都新增一条 `infra_file` 记录（带租户）。
- 删除（管理端 `/admin-api/infra/file`：`page / delete`，权限 `infra:file:*`）：删除记录后，同一 `(config_id, path)` 已无其他记录引用时，才删除存储中的实际内容。
- 本地磁盘：路径规范化后必须仍在 `basePath` 之内，否则拒绝。

### 4.3 读取

- 数据库与本地磁盘存储：`GET /admin-api/infra/file/{configId}/get/**` 免登录，列入安全白名单与网关白名单。
  - 响应头：`Content-Type` 按识别结果，附 `X-Content-Type-Options: nosniff` 与长期缓存。
  - 图片与 PDF 以 `inline` 返回，其余以 `attachment` 返回。
  - 该接口标注 `@ApiAccessLog(enable = false)`。
- S3 存储：地址为 `domain/path`，由存储桶公开读。

### 4.4 头像

- system：`PUT /admin-api/system/user/profile/update-avatar`（multipart）。
- member：`PUT /app-api/member/user/update-avatar`（multipart）。
- 两者都经 `FileApi.createFile` 存储后更新头像地址。
- 原资料修改接口仍可直接填头像地址。

## 5. API 日志

### 5.1 访问日志过滤器修复（`starter-web`）

现状：请求体原样记录，登录密码以明文进入日志；`@ApiAccessLog` 注解未被读取。修复内容：

- 请求处理后从 `HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE` 取控制器方法，按 `@ApiAccessLog` 决定：
  - 是否记录；
  - 是否记请求参数、是否记响应体（取暂存的 `CommonResult` 序列化，截断到 8000 字符）；
  - `operateModule / operateName / operateType`。
- 脱敏：查询参数与 JSON 请求体中的嵌套字段，凡字段名匹配 `DesensitizeUtils` 的敏感规则或列入注解 `sanitizeKeys`，值替换为 `***`。
- multipart 请求不记录请求体。
- `GlobalExceptionHandler` 写入异常日志的请求参数做同样的脱敏。

### 5.2 发送（system、member 等业务服务）

- 各服务在 `framework/contract` 下实现 `ApiAccessLogFrameworkService` 与 `ApiErrorLogFrameworkService`：
  - 提交到有界线程池（队列满则丢弃）；
  - 由 `RedisStreamMessageSender` 发往主题 `infra.api-access-log` / `infra.api-error-log`，消息编号用 UUID。
- 发送失败或线程池已满：本地打警告日志后丢弃，不影响业务。
- 消息流沿用现有的 10 万条近似长度上限。

### 5.3 消费与管理（infra）

- 两个 `AbstractRedisStreamMessageListener` 逐条插入，租户编号按消息写入。
  - 现有消费容器逐条分发，按当前量级无需批量。
  - 投递为至少一次：已写库但未确认时可能重复一条，日志场景接受，不去重。
- 访问日志 `/admin-api/infra/api-access-log`：`page / export-excel`，权限 `infra:api-access-log:*`。
- 异常日志 `/admin-api/infra/api-error-log`：`page / export-excel / update-status`（已处理 / 已忽略，记录处理人与处理时间，只能从未处理状态变更），权限 `infra:api-error-log:*`。
- 清理任务（XXL-Job，每天凌晨执行）：
  - `apiAccessLogCleanJob`：保留天数 `ccloud.infra.api-log.access-retain-days`，默认 30；
  - `apiErrorLogCleanJob`：保留天数 `ccloud.infra.api-log.error-retain-days`，默认 90；
  - 每批物理删除 1 万条，直到删完。

## 6. 消息死信

现状：P1 的本地消息表超过重试上限只把状态置为 3，Redis Stream 消费超过重投上限只打错误日志后丢弃，`infra_message_dead_letter` 从未被写入。本期先在 starter-mq 中补齐写入：

- 本地消息转为死信状态时，同时写一条死信（租户取原消息的租户）；
- Stream 消息重投次数达到上限被丢弃前，写一条死信（租户记 0，失败原因为"消费重试 N 次仍失败"）。

管理端：

- 管理端 `/admin-api/infra/message-dead-letter`：`page`（权限 `infra:message-dead-letter:query`）；`update-handled`、`resend`（权限 `infra:message-dead-letter:update`）。
- `resend`：用原消息编号把死信发回其主题的 Redis Stream，然后标记为已处理。消费侧已按消息编号幂等。

## 7. 数据源与数据库文档

### 7.1 数据源

- 管理端 `/admin-api/infra/data-source-config`：`create / update / delete / get / list`，权限 `infra:data-source-config:*`。
- 主数据源：编号 0，取自应用的 `spring.datasource`，出现在列表首位，不入库，不可修改或删除。
- 其他数据源：
  - 密码用 starter-mybatis 的加密类型处理器存储；
  - 返回时显示为掩码，修改时提交掩码则沿用原值；
  - 保存前做连接测试（5 秒超时），失败不保存。
- 只支持 MySQL。

### 7.2 表结构读取

`DatabaseTableService` 按数据源读取 `information_schema`：表（名称、注释），列（名称、类型、注释、可空、默认值、主键、自增、顺序），索引（名称、唯一性、列）。数据库文档与代码生成共用。

### 7.3 数据库文档

- `GET /admin-api/infra/db-doc/export-html`、`export-markdown`，参数 `dataSourceConfigId`、`download`，权限 `infra:db-doc:query`。
- 内容：目录；每张表的注释、字段表（名称、类型、可空、默认值、注释）、索引表。
- 所有来自数据库的文本都做 HTML / Markdown 转义。
- 预览 HTML 附 `Content-Security-Policy: default-src 'none'; style-src 'unsafe-inline'`。

## 8. 代码生成

### 8.1 接口（`/admin-api/infra/codegen`，权限 `infra:codegen:*`）

| 接口 | 说明 |
|---|---|
| `db/table/list` | 数据源中尚未导入的表，可按表名、注释筛选 |
| `create-list` | 导入选中的表 |
| `table/page`、`detail` | 已导入表的分页；表与字段详情 |
| `update` | 保存表与字段的编辑结果 |
| `sync-from-db` | 按数据库当前结构同步 |
| `delete` | 删除表定义（连同字段） |
| `preview` | 返回 `文件路径 → 内容` |
| `download` | 返回压缩包 |

### 8.2 导入时的推断

- 模块名取表名第一段，类名取其余部分的大驼峰（`member_point_record` → 模块 `member`、类 `PointRecord`），业务名取第二段（`point`）。类描述取表注释，去掉结尾的"表"。
- Java 类型：
  - `bigint` → `Long`；
  - `int / tinyint / smallint` → `Integer`；
  - `bit(1)` → `Boolean`；
  - `decimal` → `BigDecimal`；
  - `datetime / timestamp` → `LocalDateTime`，`date` → `LocalDate`；
  - `char / varchar / text` → `String`；
  - 其余类型报错，提示不支持。
- 公共字段（`creator / create_time / updater / update_time / deleted / tenant_id`）由 `BaseDO` / `TenantBaseDO` 提供，不参与增改查；有 `tenant_id` 列则继承 `TenantBaseDO`。
- 主键不参与新增；其他列默认参与新增、修改与列表结果。
- `name / title` 类字段默认作为查询条件，用模糊匹配；时间类字段默认用区间；`status / type` 类字段默认用等值。
- `html_type` 按类型填默认值（没有前端，仅存档）。

### 8.3 同步

新增的列补上、删除的列移除、类型 / 注释 / 可空变化的列更新；其余列保留用户的编辑。

### 8.4 模板（Velocity）

| 产物 | 位置 |
|---|---|
| DO、Mapper | `{module}-biz/.../dal/dataobject/{business}/`、`dal/mysql/{business}/` |
| Service、ServiceImpl | `service/{business}/` |
| 控制器与三个 VO（分页查询、返回、保存） | 后台：`controller/admin/{business}/`；App：`controller/app/{business}/`，类名加 `App` 前缀 |
| Service 测试（本机 MySQL，写法同现有测试） | `src/test/.../service/{business}/` |
| 错误码片段 | `{module}-api/.../enums/ErrorCodeConstants_{Class}.java.txt`，默认编号取预留区间 `1-099-xxx-xxx`，合并时由使用者重新编号 |
| SQL | 菜单与按钮权限（挂在 `parent_menu_id` 下，编号由数据库自增）、错误码文案 |

- 字典类型用于返回 VO 的 Excel 字典转换注解。
- 树表：
  - 生成 `list` 而不是 `page`；
  - 保存时校验父节点存在、不以自身为父、不形成环；
  - 删除时有子节点则拒绝。
- 压缩包目录与本仓库一致，解压到仓库根目录即可合并。

### 8.5 演示表

新增 `infra_demo_student`（单表：姓名、性别、生日、简介、状态）与 `infra_demo_category`（树表：名称、父编号、排序），全局共享，仅作代码生成演示与验收。

## 9. 对现有内容的调整

- DDL：
  - 删除 `infra_job`、`infra_job_log`；
  - 新增两张演示表。
- 种子数据：
  - 删除定时任务菜单（137–143）及角色、租户套餐中对它们的引用；
  - 删除字典 `infra_job_status`，以及 `infra_file_storage` 中 FTP / SFTP、`infra_codegen_template_type` 中主子表两项；
  - 参数种子按第 3 节调整；
  - 文件配置 `domain` 改为 `http://127.0.0.1:28080`（改端口时遗漏）；
  - 新错误码的国际化文案。
- 校验脚本：`verify-sql`、`verify-docs` 等同步。
- 主设计文档：记录定时任务的调整。

## 10. 部署

- 容器编排：应用由 4 个变为 5 个；infra 的本地磁盘存储挂 ccloud 自己的具名数据卷，`docker-down.sh` 不删除。
- Nacos：新增 `ccloud-infra-server.yaml`，由 `nacos-import.sh` 导入。
- `docker-up.sh init`：登记 infra 执行器与两个清理任务。
- MinIO（做到 S3 阶段时征得同意后安装）：
  - 长期保留的容器 `minio`，只监听 `127.0.0.1:9000`（接口）与 `127.0.0.1:9001`（控制台）；
  - 账号写入 `~/.ccloud/middleware.env`；
  - `scripts/lib/local-middleware.sh` 纳入检查。

## 11. 测试与验收

### 11.1 单元与集成测试

- Service 测试连本机 MySQL / Redis。
- 控制器集成测试基类 `BaseInfraWebIntegrationTest`，覆盖有权限、无权限、会员身份三种情况。
- `RpcApiCoverageTest`。
- 存储：
  - 本地磁盘在临时目录测；
  - S3 连本机 MinIO，连不上即失败，不跳过；
  - 另测越界路径、类型白名单、内容去重、引用计数删除。
- 日志：
  - 过滤器单测脱敏、注解开关、响应体记录；
  - 真实 Redis 测发送 → 消费 → 写库。
- 代码生成：导入推断、同步合并、模板渲染。

### 11.2 `scripts/verify-codegen.sh`

1. 在草稿目录建临时 git 工作区。
2. 生成代码并写入 infra：`infra_demo_student` 用后台场景（单表），`infra_demo_category` 用 App 场景（树表），两种模板、两种场景各覆盖一次。
3. 把错误码片段合并进 infra 的 `ErrorCodeConstants`，然后编译并运行生成的测试。
4. 删除工作区。

`verify-all` 调用它。

### 11.3 端到端验收（`verify-e2e` 新增 infra 部分）

- 参数：
  - 修改初始密码参数后，system 导入用户使用新值；
  - 打开注册开关后才能注册。
- 文件：
  - 上传后可以免登录读取；SVG、HTML 被拒绝；
  - 后台与 App 头像上传；
  - 切换到 MinIO 配置重复一遍上传与读取。
- 日志：
  - 请求后能查到访问日志，登录请求中的密码已脱敏；
  - 触发异常后能查到异常日志。
- 数据源连接测试、数据库文档导出、代码生成预览、死信重新投递。

## 12. 文档

README、`docs/快速开始.md`、`docs/模块说明.md`、`docs/接口鉴权说明.md`（新增免登录文件读取与 App 上传）。

## 13. 实施分阶段

| 阶段 | 内容 |
|---|---|
| 1 | 服务骨架 |
| 2 | 参数配置，接入 system |
| 3 | 文件：数据库与本地磁盘存储，头像上传 |
| 4 | S3 与 MinIO |
| 5 | 日志过滤器修复，日志经消息流写库，定时清理 |
| 6 | 消息死信 |
| 7 | 数据源与数据库文档 |
| 8 | 代码生成 |
| 9 | 代码生成的真实编译验收 |
| 10 | 部署、端到端验收、文档 |
