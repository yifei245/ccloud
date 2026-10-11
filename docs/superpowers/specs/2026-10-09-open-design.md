# 开放平台模块设计（P2 第 3 项）

> 状态：已确认（2026-10-09），已实现并通过端到端验收（实现与设计的差异见第 10 节）
> 上游：[总体设计](2026-09-07-ccloud-design.md) 的 P2「module-open 全量（含签名鉴权与限流）」。P1 已交付 open 的表结构、菜单、字典，网关的 `/open-api/**` 路由、签名校验（应用登记在 Nacos）与按应用限流。

## 1. 目标与范围

新建 `ccloud-module-open`（`open-api` + `open-biz`），服务名 `ccloud-open-server`，端口 28084，提供开放应用、接口目录、应用接口授权、调用日志、回调通知；网关改为经 RPC 从 open 读取应用；member 提供第一个真实的开放业务接口。

### 1.1 已确认的取舍

| 问题 | 结论 |
|---|---|
| 认证方式 | 只做签名模式，沿用 P1 协议。不做 client_credentials 令牌模式：删除 `open_app_scope` 表与 system 中的 OAuth2 演示客户端 `ccloud-open-demo` |
| 配额 | 只做每秒限流（`open_app.rate_limit`），网关按应用令牌桶；不做每日总量 |
| 开放业务接口 | member 的「按手机号查询会员基本信息」；删除演示数据中两条交易接口 |
| 回调通知 | 通用机制 + 测试回调（`open.test` 事件），本期不接业务事件；交易、支付模块以后经内部接口发送 |
| 调用日志 | 由提供开放接口的业务服务记录（各服务的 `ApiLogFrameworkServiceImpl`），经 Redis Stream 发给 open 写库；被网关拒绝的调用不入表，网关打 warn 日志 |
| 应用密钥 | 加密存储（`CCLOUD_ENCRYPT_KEY`），只在新建与重置时返回一次完整密钥，之后一律掩码 |
| 租户划分 | 应用、应用接口授权、调用日志、回调日志按租户隔离，租户管理员自管；接口目录全局一份，增删改仅限系统租户 |
| 网关取应用 | 按需调用 open 内部接口 + 本地 Caffeine 缓存 + Redis 发布订阅失效 |
| 接口授权位置 | 网关按「方法 + 原始路径」精确匹配应用可调接口，不匹配返回 403 |

### 1.2 非目标

- 前端工程（沿用总体设计）。
- 令牌模式、授权范围（scope）。
- 每日 / 每接口调用总量配额、计费。
- 业务事件订阅配置（应用订阅哪些事件）；本期回调只有测试事件。
- 接口路径通配匹配。
- 记录开放接口的响应内容（属于第三方业务数据，不留存）。

## 2. 服务与请求链路

### 2.1 服务

`ccloud-open-server` 结构照搬 infra / member：五个框架契约（权限、租户、字典、国际化、操作日志）经 RPC 由 system 实现；注册为 XXL-Job 执行器（`ccloud-open-server`），运行回调重试与日志清理任务。依赖 Redis（缓存失效发布、Redis Stream 消费、令牌桶在网关侧）。

### 2.2 一次开放接口调用

1. 第三方带 `X-Open-App-Id`（即 appKey）、`X-Open-Timestamp`、`X-Open-Nonce`、`X-Open-Sign` 调用 `/open-api/member/user/get?mobile=...`。签名协议与 P1 完全一致（见 `docs/接口鉴权说明.md`）。
2. 网关 `OpenApiSignatureFilter` 取应用快照（本地缓存，未命中调 open RPC），依次校验：
   1. 参数齐全、nonce 不超过 64 字符；
   2. 应用存在、开启、未过期 —— 否则 401；
   3. 客户端 IP（连接对端地址，不信任 `X-Forwarded-For`）在白名单内（白名单为空不限）—— 否则 403；
   4. 时间戳在容差内、签名正确 —— 否则 401；
   5. nonce 首次使用（Redis `SETNX`，验签通过后才占用）—— 重复 401，Redis 不可用 503；
   6. 「方法 + 原始路径」在应用可调接口中 —— 否则 403。

   open 不可用且缓存未命中时返回 503（不当作 401）。
3. `RateLimitFilter`：按应用令牌桶，阈值取快照中的 `rateLimit`（容量同值）。
4. 转发：网关用 `InternalHeaderWriter` 写入带签名的内部身份头，`LoginUser = {id: 应用编号, userType: OPEN_APP, tenantId: 应用租户, info: {appKey}}`，并设置 `X-Tenant-Id`；移除 `X-Open-Sign`。外部无法伪造该身份。
5. member 的开放接口按应用租户查询并返回脱敏数据。
6. member 的 `ApiLogFrameworkServiceImpl` 识别出开放应用调用，把框架的 `ApiAccessLog`（`userId` 即应用编号）另发一份到 Redis Stream 主题 `open.api-log`；open 消费后按消息中的租户写 `open_api_log`，appKey 与接口编码由 open 反查。

### 2.3 网关缓存与失效

- 快照缓存：命中 60 秒、未找到 10 秒、最多 10000 条（配置项 `ccloud.open.app-cache-ttl`、`app-cache-not-found-ttl`、`app-cache-max-size`，实际名称见第 10 节）。
- open 在应用新增、修改、删除、重置密钥、接口授权变更后，向 Redis 频道 `ccloud:open:app:invalidate` 发布该应用的 appKey；接口目录新增、修改、删除后发布空消息，表示整体清空。网关各实例订阅并清缓存。
- 失效消息丢失时最多延迟一个缓存周期（60 秒）。

### 2.4 P1 遗留清理

- 网关删除 `ccloud.open.apps` 配置（`application.yaml`、Nacos `ccloud-gateway.yaml`）与 `OpenApiProperties.App`；`OpenApiProperties` 只保留 `timestampTolerance`、`maxBodySize`。
- `docs/接口鉴权说明.md` 删除"P1 阶段的限制"与 Nacos 登记示例。

## 3. 数据表、初始数据与菜单

### 3.1 表结构（修改 `sql/mysql/ddl/04-open.sql`）

| 表 | 调整 |
|---|---|
| `open_app_scope` | 删除 |
| `open_app` | `app_secret` 改 `varchar(255)`，存密文；`app_key` 由系统生成（16 位字母数字），不可修改；其余字段不变 |
| `open_api` | 删除 `module` 列；新增唯一键 `uk_method_path (method, path, deleted)` |
| `open_app_api` | 不变 |
| `open_api_log` | 删除 `response_body`；`result_msg` 记业务失败提示 |
| `open_notify_log` | 新增 `event_id varchar(64) NOT NULL`（唯一键 `uk_event_id (event_id, deleted)`）、`last_notify_time datetime`、`http_status int` |

### 3.2 初始数据

- 演示应用 `ccloud-open-demo`（租户 1）保留，`app_secret` 存为用开发环境默认加密密钥计算的密文，原文 `ccloud-open-demo-secret`；备注注明生产环境须删除（换了加密密钥后无法解密，文档给出删除语句）。
- 接口目录只保留「查询会员信息」`GET /open-api/member/user/get`（编码 `member.user.get`）；删除两条交易接口及其授权行；演示应用授权该接口。
- 删除 `open_app_scope` 演示数据、`system_oauth2_client` 中的 `ccloud-open-demo`。
- 新增字典 `open_notify_status`：0 待通知、10 成功、20 失败待重试、30 放弃。
- 新增 i18n 文案（见第 6 节）。

### 3.3 菜单与租户

- 普通套餐去掉接口目录的新增、修改、删除（215–217），保留开放应用、接口目录查询、调用日志、回调日志。
- 新增按钮：`open:app:assign-api`（可调接口授权）、`open:app:test-notify`（发送测试回调），挂在开放应用菜单下，并加入普通套餐与系统管理员角色。
- 服务端兜底：接口目录增删改要求系统租户。`SystemTenantValidator` 从 infra 移到 `ccloud-spring-boot-starter-biz-tenant`，infra 与 open 共用：`validate(ErrorCode)` 由调用方传入各自的错误码（infra 1_002_008_000、open 1_005_000_000），是否系统租户经新的租户契约方法 `TenantFrameworkService.isSystemTenant` 判断（system 本地查，其他服务经 `TenantApi.isSystemTenant`）。
- 种子一致性测试同步更新（套餐不含 215–217）。

## 4. open 服务功能

### 4.1 应用管理 `/admin-api/open/app/*`

| 接口 | 说明 |
|---|---|
| `create` | 生成 appKey 与 32 位随机密钥（`SecureRandom`，字母数字），返回 `{id, appKey, appSecret}`，密钥仅此一次 |
| `update` | 可改名称、状态、备注、IP 白名单、每秒上限（1–10000）、过期时间、回调地址、联系人；不可改 appKey |
| `delete` | 软删除，同时删除其接口授权 |
| `get` / `page` | 密钥显示掩码（前 4 位 + `****` + 后 4 位） |
| `reset-secret` | 生成新密钥，返回一次；旧密钥随缓存失效立即作废 |
| `assign-api` / `get-api-ids` | 整体替换可调接口，只能选已开启的接口 |
| `test-notify` | 发送 `open.test` 事件，data 为 `{"message":"ccloud test"}`；应用无回调地址时报错 |

- IP 白名单：逗号分隔的 IPv4 / IPv6 地址或 CIDR，保存时校验格式。
- 回调地址：保存时校验 http / https 与格式（内网地址在发送时校验，见 4.5）。
- 变更后发布缓存失效消息（事务提交后）。

### 4.2 接口目录 `/admin-api/open/api/*`

`create / update / delete / get / page / list-all-simple`（勾选授权用，只含开启的接口）。增删改仅限系统租户。路径须以 `/open-api/` 开头、不含查询串与 `..`；方法只能是 GET / POST / PUT / DELETE；`(method, path)` 唯一、`code` 唯一。关闭或删除后，网关快照不再包含它，所有应用对它的调用返回 403。

### 4.3 内部接口（open-api 模块，Feign + 网关 WebClient）

- `GET /rpc-api/open/app/get-by-key?appKey=` → `OpenAppSnapshotDTO {id, appKey, appSecret(明文), tenantId, status, expireTime, ipWhitelist, rateLimit, apis: [{method, path}]}`；不存在返回 data 为 null。忽略租户查询（网关此时不知道租户）。
- `POST /rpc-api/open/notify/send {appId, event, data}` → 返回回调日志编号。

### 4.4 调用日志

- 消费 `open.api-log`：按消息中的 tenantId 显式写库（与 infra API 日志一致的做法）；`api_code` 由 open 按「方法 + 路径」从接口目录反查，查不到留空。
- `/admin-api/open/api-log/page`、`export-excel`。
- XXL-Job `openApiLogCleanJob`：删除 N 天前的调用日志（默认 30，任务参数可改），同时删除 N 天前状态为成功或放弃的回调日志。

### 4.5 回调通知

- **发送**：`notify/send` 写一条回调日志（状态 0、生成 `event_id`、`next_retry_time = now`），事务提交后提交到有界线程池立即发送一次。应用无回调地址时直接记为 30 放弃并写明原因。
- **请求**：`POST notify_url`，`Content-Type: application/json`，请求体 `{"eventId","event","appKey","timestamp","data"}`；请求头 `X-Open-App-Id`、`X-Open-Timestamp`、`X-Open-Event-Id`、`X-Open-Sign = Base64(HmacSHA256(appSecret, timestamp + "\n" + eventId + "\n" + 十六进制小写 SHA-256(请求体)))`。
- **结果**：2xx 为成功（10）；其他状态码、超时、连接失败、地址不合法均为失败，重试次数加一，按退避设置 `next_retry_time`：15 秒、1 分钟、5 分钟、30 分钟、2 小时、6 小时；第 6 次重试仍失败记为放弃（30）。记录 `http_status`、`response_body`（截断到 2048 字符）、`last_notify_time`。
- **重试任务**：XXL-Job `openNotifyRetryJob` 每 15 秒扫描 `status IN (0, 20) AND next_retry_time <= now`（每批上限 100，跨租户），逐条发送；单条失败不影响其他。发送前以条件更新抢占（乐观锁 / 状态 + 时间条件），避免多实例重复发送。
- **防 SSRF**：只允许 http / https；发送时解析域名，任一解析结果为回环、私有、链路本地、组播、未指定地址或云元数据地址（169.254.169.254 等）即拒绝；连接按已校验的地址进行，避免 DNS 重绑定；不跟随重定向；连接、读取超时各 5 秒。`ccloud.open.notify.allow-private-address` 默认 false，仅 local / dev profile 打开。
- **后台**：`/admin-api/open/notify-log/page`、`get`；`retry`：仅失败待重试或放弃的记录，重试次数清零、状态置 0 后立即发送。

## 5. 网关、框架与 member

### 5.1 网关

- `OpenAppService`：WebClient（负载均衡）调用 `get-by-key`，带内部签名（与令牌校验一致）；Caffeine 缓存与 Redis 订阅失效（2.3）。
- `OpenApiSignatureFilter`：按 2.2 的顺序校验；快照与 `LoginUser` 放入 exchange 属性供限流使用。
- `RateLimitFilter`：开放接口阈值取快照 `rateLimit`。
- `InternalHeaderWriter` 复用；appKey 放在 `LoginUser` 现有的 `info` 字段（`info.appKey`），不新增字段。

### 5.2 框架

- `UserTypeEnum` 新增 `OPEN_APP(3, "开放应用")`，`user_type` 字典同步。检查所有按用户类型分支处（令牌、权限校验 `@PreAuthorize`、操作日志、数据权限、租户安全过滤），开放应用身份不得被当作管理员或会员，不得通过管理端 / 用户端接口的权限校验。
- 开放接口身份约束：沿用框架已有的"按路径校验用户类型"机制（`WebFrameworkUtils.getUserTypeByUrl` + `LoginUserFilter`），不用注解，避免漏标——`/open-api/**` 只接受 OPEN_APP 身份，OPEN_APP 身份在其他路径一律不被接受；不被接受的身份视为未登录，由 Spring Security 返回 401。
- 调用日志发送：框架访问日志过滤器不改；提供开放接口的服务在自己的 `ApiLogFrameworkServiceImpl` 中，对 OPEN_APP 且路径以 `/open-api/` 开头的访问日志另发一份到 `open.api-log`，消息体沿用框架的 `ApiAccessLog`（参数已脱敏）。主题常量 `OpenApiLogTopics` 放在 open-api（与 infra 的 `ApiLogTopics` 同一做法）。
- `SystemTenantValidator` 移到 biz-tenant starter，租户契约新增 `isSystemTenant`（3.3）。

### 5.3 member

- `OpenMemberUserController`：`GET /open-api/member/user/get?mobile=`，返回 `{id, nickname, avatar, levelName, point, status, mobile(脱敏 138****8000)}`；手机号未注册报 member 现有错误码 `USER_MOBILE_NOT_EXISTS`。按 LoginUser 的租户查询。
- 不放行匿名访问：`/open-api/**` 仍要求已认证，而 5.2 的约束保证这里只能是开放应用身份；接口上不加 `@PreAuthorize`。

## 6. 错误码与国际化

- open：`1_005_xxx_xxx`，按业务分段（通用 000、应用 001、接口目录 002、回调 003），通用段为「仅限系统租户」；名称用 `_NOT_EXISTS / _EXISTS / _INVALID` 规范；包括：应用不存在、接口不存在、接口编码已存在、方法与路径已存在、接口路径不合法、接口已关闭不可授权、回调地址不合法、应用未配置回调地址、回调日志状态不允许重新通知、IP 白名单格式不合法。
- member：手机号未注册复用 member 现有错误码，不新增。开放应用身份约束返回 401（5.2），不新增错误码。
- i18n id 从 377 起，中英文各一条，`ErrorCodeConstantsTest` 锁定编号与文案。

## 7. 部署

- docker-compose 增加 `ccloud-open`（端口 28084）、Dockerfile、`.env` `OPEN_PORT=28084`。
- Nacos：新增 `ccloud-open-server.yaml`（含 `ccloud.open.notify.*`）；`ccloud-gateway.yaml` 删除 `apps`，新增 `app-cache` 配置说明；open 也导入公共的数据源配置。
- XXL-Job `04-ccloud-init.sh` 增加执行器 `ccloud-open-server` 与两个任务，可重复执行。
- `verify-deps`、`verify-docker`、`verify-nacos`、`verify-mq-schema`、`verify-all` 纳入 open；监控面板显示 open 为 UP。

## 8. 测试与验收

### 8.1 单元 / 集成测试（TDD，本地 MySQL / Redis 测试库）

- open：密钥加密入库、仅返回一次、掩码、重置后旧密钥不可用；授权替换与只能选开启接口；接口目录仅限系统租户与路径校验；快照内容（含忽略租户）；失效消息在提交后发布；日志消费按租户写入与 api_code 反查；回调签名格式、2xx 成功、退避时间表、第 6 次后放弃、手工重新通知、地址校验（含 DNS 解析到内网、重定向不跟随）、抢占防重复。回调对端用进程内 HTTP 服务模拟。
- 网关：MockWebServer 模拟 open；验签各失败分支、过期、IP 白名单（含 CIDR）、未授权 403、open 不可用时缓存命中放行 / 未命中 503、失效消息清缓存（单条与整体）、限流取快照值、内部身份头内容。
- 框架：OPEN_APP 身份不能通过管理端 / 用户端权限校验、非开放接口拒绝 OPEN_APP、开放接口拒绝非 OPEN_APP；`open.api-log` 消息体脱敏。
- member：开放接口只接受开放应用、按租户查询、手机号脱敏。
- 契约：`RpcApiCoverageTest`、Feign 合约测试、`ErrorCodeConstantsTest`、种子一致性测试覆盖新增内容。

### 8.2 端到端（`verify-e2e.sh` 新增开放平台一段，结束时清理）

1. 管理员新建应用，取得一次性密钥；授权「查询会员信息」；回调地址设为脚本在宿主机启动的临时接收服务（docker 内经 `host.docker.internal` 访问）。
2. 签名调用 `/open-api/member/user/get`：成功，手机号已脱敏。
3. 网关拒绝：错误签名 401；nonce 重放 401；未授权路径 403；停用应用后立即 401（验证缓存失效）；重置密钥后旧密钥 401、新密钥成功。
4. 调用日志中查到本次调用（轮询，按应用与时间定位）。
5. 测试回调：接收服务收到请求且验签通过，回调日志为成功；停掉接收服务后再发，记录为失败待重试且 `next_retry_time` 在约 15 秒后。
6. 清理新建的应用、日志、回调记录与临时进程。

## 9. 文档

- `docs/接口鉴权说明.md`：开放接口一节改为从 open 管理应用；补充接口授权、IP 白名单、403 场景、回调验签说明与 Java 示例；删除 P1 限制与 Nacos 登记。
- `README`、`docs/快速开始.md`、`docs/模块说明.md`：加入 open 服务、端口、依赖关系（open 依赖 system 的框架契约；member 等业务服务经 Redis Stream 向 open 发送调用日志；网关经 RPC 读取 open）。

## 10. 实现与设计的差异（2026-10-11 同步）

实现过程中对设计做了以下补充与调整，以实现为准：

### 10.1 网关

- 配置项名：应用缓存的配置是 `ccloud.open.app-cache-ttl`（60 秒）、`app-cache-not-found-ttl`（10 秒）、`app-cache-max-size`（10000）、`app-lookup-timeout`（调用 open 的超时，3 秒），不是 `ccloud.open.app-cache.*`。Nacos 配置包只放出 `app-cache-ttl`，其余用默认值。
- 请求体上限：参与签名的请求体不超过 `ccloud.open.max-body-size`（默认 1MB），超出返回 413。
- 路径过滤：`IllegalPathWebFilter` 对 unreserved 字符（字母、数字、`- . _ ~`）的百分号编码一律返回 400。路由按解码后的路径匹配，而网关的前缀判断（`/open-api/` 验签）按原样字符串进行，`/%6Fpen-api/...` 否则会被路由到开放接口却不经验签。
- 失效订阅：Redis 订阅出错后自动退避重连，重连成功后做一次全量失效，避免断线期间丢失的广播留下旧缓存。open 返回空响应体按不可用处理（503）。
- 校验顺序与 2.2 一致：授权（第 6 步）放在 nonce 之后，未授权的调用也消耗 nonce。

### 10.2 open 服务

- 内部接口的 DTO 名为 `OpenAppSnapshotRespDTO`（应用快照，`toString` 排除密钥以免日志泄露）与 `OpenNotifySendReqDTO`；路径 `GET /rpc-api/open/app/get-by-key`、`POST /rpc-api/open/notify/send`。
- 回调抢占：设计中的「条件更新抢占」实现为带 60 秒租约的抢占（`claim` 写入租约到期时间），回写结果时以仍持有租约为条件（`updateIfLeaseHeld`）；租约内其他执行方不会重复发送，进程在发送中崩溃时租约到期后由重试任务接手。手工重新通知同样是条件更新（只有失败待重试或放弃的记录可以），避免与定时任务并发时把刚成功的记录重置并重发。错误码 `OPEN_NOTIFY_LOG_CANNOT_RETRY`（1_005_003_001）。
- 回调发送的整体期限：除连接、读取超时各 5 秒外，另有总期限（两者之和），到期取消请求，防止对方逐字节缓慢输出而超过租约后被重复发送。
- 防 SSRF 的范围比设计更严：校验识别 IPv4 映射等内嵌写法，并拒绝整个 `169.254.0.0/16` 与 `fd00:ec2::254` 等全部元数据地址，且不受 `allow-private-address` 影响。
- 重试时使用应用当前的回调地址与密钥（管理员修正地址或重置密钥后，后续重试随之生效）。
- 第 6 次重试仍失败记为放弃，因此一个事件最多发送 7 次。
- 调用日志的发送在各服务的 `ApiLogFrameworkServiceImpl` 中（member），消息体为框架的 `ApiAccessLog`；开放应用身份由框架 `LoginUserFilter` 按路径约束（`/open-api/**` 只接受 OPEN_APP，OPEN_APP 在其他路径按未登录）；是否系统租户经 `TenantFrameworkService.isSystemTenant`；open 的 `ErrorCodeConstants.ONLY_SYSTEM_TENANT`（1_005_000_000）供 `SystemTenantValidator` 使用。
- 管理端另有 `GET /admin-api/open/app/get-api-ids`（应用已授权的接口编号）与 `PUT /admin-api/open/notify-log/retry?id=`（手工重新通知）。

### 10.3 部署

- `allow-private-address` 只在 profile 中设置（local / dev 为 `true`，prod 与默认为 `false`），Nacos 配置包 `ccloud-open-server.yaml` 不包含该项：配置包会导入每个环境且优先级高于 profile，写入会让生产默认关闭 SSRF 防护。docker 联调使用 dev profile，回调到宿主机仍可用。`scripts/verify-nacos.sh` 检查配置包中不得出现 `allow-private-address: true`。
- XXL-Job 任务：`openNotifyRetryJob`（`0/15 * * * * ?`）、`openApiLogCleanJob`（每天 03:20）、`redisStreamPendingMessageResendJob`（每分钟）。

### 10.4 信任边界

- 网关 WebClient 调用 `get-by-key` 时**不带内部签名**（设计 5.1 曾写「带内部签名」），与令牌校验的现状一致；业务服务的 `/rpc-api/**` 一律放行，依赖网关阻断与网络隔离。`get-by-key` 返回应用密钥原文，因此业务服务端口（28081–28084 等）只允许网关与服务之间访问，已写入 `docs/接口鉴权说明.md` 与 `docs/快速开始.md`。若要从代码上加固，可另立任务让 `/rpc-api` 校验内部签名头。

### 10.5 验收脚本的覆盖范围

- 第 7 节要求纳入 open 的脚本中，`verify-deps` 与 `verify-mq-schema` 未改动，原因：`verify-deps` 的 BouncyCastle 同版本检查只需一个代表模块（版本由 BOM 与 starter 统一管理，对 open-biz 单独执行依赖树检查只得到 `bcprov-jdk18on` 一个版本）；`verify-mq-schema` 比对的是 infra 的本地消息表 DDL 与 starter-mq 单测脚本，open 不建自己的消息表（共用同一份表结构）。`verify-docker`、`verify-nacos` 已覆盖 open（`verify-all` 只是依次调用各脚本）。

### 10.6 端到端验收

- 8.2 第 5 项中「停掉接收服务后再发」，脚本实际让接收方返回 HTTP 500 来模拟对方失败，效果相同且不需要重启接收进程；验证状态为 20（失败待重试），首次重试间隔为 15 秒。
- 脚本中的请求体先放入变量再传给 `curl`：macOS 自带的 bash 3.2 会把 `"$(...)"` 里 JSON 的 `{a,b}` 当作花括号展开而拆开参数。
