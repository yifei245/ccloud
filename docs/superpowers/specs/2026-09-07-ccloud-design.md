# ccloud 微服务平台 — 设计文档

- 日期：2026-09-07
- 状态：待评审
- 参考项目：ruoyi-vue-pro (master-jdk17) 为结构主线，yudao-cloud 为微服务拆分主线，pig / SpringBlade / JeecgBoot 为网关与开放平台参考

## 1. 目标与范围

构建一个**纯 Java 后端**的多租户微服务中台 `ccloud`，对外提供四类接口：

| 接口族 | 路径前缀 | 使用者 | 鉴权方式 |
|---|---|---|---|
| 管理端 | `/admin-api/**` | 运营后台（Web） | OAuth2 Bearer Token，`user_type=ADMIN` |
| 用户端 | `/app-api/**` | App / 小程序 / H5 | OAuth2 Bearer Token，`user_type=MEMBER` |
| 开放 API | `/open-api/**` | 第三方系统 | AppKey + AppSecret 换 token / 请求签名 |
| 内部 RPC | `/rpc-api/**` | 服务间 OpenFeign 调用 | 内部签名头，网关拒绝外部访问 |

非目标：不做前端工程（Vue/UniApp 不在本次范围）。

## 2. 技术栈

| 分类 | 选型 | 版本 |
|---|---|---|
| 运行时 | JDK | 21 |
| 框架 | Spring Boot | 3.5.16 |
| 微服务 | Spring Cloud | 2025.0.3 |
| 注册/配置 | Spring Cloud Alibaba + Nacos | 2025.0.0.0 / Nacos 3.2.4 |
| 网关 | Spring Cloud Gateway (Server WebFlux) | 随 SC |
| 安全 | Spring Security | 6.5.x（随 Boot） |
| ORM | MyBatis Plus | 3.5.17 |
| 多数据源 | dynamic-datasource | 4.5.0 |
| 数据库 | MySQL | 8.4 LTS，驱动 mysql-connector-j 9.7.0 |
| 连接池 | Druid | 1.2.28 |
| 缓存 |  Redis 7 + Redisson | Redisson 3.x LTS 线（4.x 为新主线，P1 构建期实测后锁定） |
| 文档 | springdoc-openapi + Knife4j | 2.9.1 / 4.5.0 |
| 任务 | XXL-Job | 3.4.2 |
| 工作流 | Flowable | 7.x（构建期确认 Boot 3.5 兼容） |
| 对象映射 | MapStruct | 1.6.3 |
| Excel | FastExcel | 1.3.0 |
| 工具 | Hutool / Guava / fastjson2 | 5.8.47 / 33.7.1-jre / 2.0.65 |
| 社交登录 | JustAuth | 1.16.7 |
| 微信 | WxJava | 4.8.x |
| 容器 | Docker + docker compose | — |

## 3. 模块划分

```
ccloud
├── ccloud-dependencies                          # BOM，唯一版本仲裁点
├── ccloud-framework/                            # 框架层（不含业务）
│   ├── ccloud-common                            # Result / 异常 / 枚举 / 分页 / 工具
│   ├── ccloud-spring-boot-starter-web           # 全局异常、统一响应、Swagger、XSS、访问日志
│   ├── ccloud-spring-boot-starter-security      # 认证过滤器、LoginUser、@PreAuthorize 扩展
│   ├── ccloud-spring-boot-starter-mybatis       # MP 配置、分页、逻辑删除、字段填充、加密 TypeHandler
│   ├── ccloud-spring-boot-starter-redis         # Redisson、RedisTemplate、缓存 Key 规范
│   ├── ccloud-spring-boot-starter-mq            # Redis Stream 消息、本地事件总线
│   ├── ccloud-spring-boot-starter-job           # XXL-Job 执行器接入、@TenantJob
│   ├── ccloud-spring-boot-starter-excel         # 导入导出、字典翻译转换器
│   ├── ccloud-spring-boot-starter-protection    # 幂等、限流、分布式锁、签名校验
│   ├── ccloud-spring-boot-starter-biz-tenant    # 多租户拦截（DB 行级 + Redis Key + Job + MQ）
│   ├── ccloud-spring-boot-starter-biz-data-permission  # 数据权限（全部/本部门/本部门及以下/本人/自定义）
│   ├── ccloud-spring-boot-starter-rpc           # OpenFeign 配置、上下文透传、内部签名
│   ├── ccloud-spring-boot-starter-monitor       # TraceId、Actuator、Admin 客户端
│   └── ccloud-spring-boot-starter-test          # 单测基类、H2/Testcontainers 支持
├── ccloud-gateway                               # ★ 网关
├── ccloud-module-system/   (api, biz)           # ★ 系统：认证/用户/角色/菜单/部门/岗位/字典/租户/OAuth2/SSO/社交/短信/邮件/通知/日志
├── ccloud-module-infra/    (api, biz)           # ★ 基础设施：文件/配置/代码生成/API 日志/定时任务/数据源/DB 文档
├── ccloud-module-member/   (api, biz)           # ★ 会员：C 端用户/地址/等级/积分/签到/分组/标签
├── ccloud-module-open/     (api, biz)           # ★ 开放平台：应用/密钥/授权范围/配额/调用日志/回调通知
├── ccloud-module-pay/      (api, biz)           # 支付：应用/渠道/订单/退款/转账/钱包/回调
├── ccloud-module-bpm/      (api, biz)           # 工作流：Flowable 模型/表单/流程/任务/抄送
├── ccloud-module-mall/                          # 商城
│   ├── ccloud-module-product (api, biz)         #   商品/分类/品牌/规格/评论/收藏/浏览
│   ├── ccloud-module-trade   (api, biz)         #   购物车/订单/售后/物流/分销
│   └── ccloud-module-promotion (api, biz)       #   优惠券/秒杀/拼团/砍价/满减/积分商城/装修
└── ccloud-module-ai/       (api, biz)           # AI：模型/对话/绘图/知识库
```

`★` = 核心模块，必须保留。其余模块整目录删除 + 从父 `pom.xml` 移除 `<module>` 即可下线，模块间只通过 `-api` 子模块（DTO + Feign 接口）耦合，无实体/服务直连。

每个业务模块固定两个子模块：
- `xxx-api`：对外暴露的 DTO、枚举、错误码、Feign 客户端接口。被其他模块依赖。
- `xxx-biz`：Controller / Service / Mapper / DO / Convert，Spring Boot 可执行应用。**不被任何模块依赖。**

## 4. 认证与授权设计

### 4.1 数据模型（自研 OAuth2，yudao 同款）

- `system_oauth2_client`：客户端。字段含 `client_id / secret / access_token_validity_seconds / refresh_token_validity_seconds / redirect_uris / authorized_grant_types / scopes / auto_approve_scopes / authorities`。
- `system_oauth2_code`：授权码（SSO 授权码模式中转）。
- `system_oauth2_access_token` / `system_oauth2_refresh_token`：令牌持久化；access token 同时写 Redis（key `ccloud:oauth2_access_token:{token}`，TTL = 有效期），失效时从 DB 回源。
- 令牌载荷：`userId / userType / userInfo / tenantId / clientId / scopes / expiresTime`。

### 4.2 多终端 & 多用户类型

- `user_type`：`1=MEMBER`（C 端）、`2=ADMIN`（管理端）。同一手机号在两套体系里是两条独立记录。
- 终端由 `client_id` 区分：`ccloud-web`（后台）、`ccloud-app`、`ccloud-mp-weixin`、`ccloud-h5`、第三方应用各自一个。每个 client 独立配置 token 时长、可用授权类型、scope。
- 支持的授权类型：`password`（账密/短信）、`authorization_code`（SSO）、`refresh_token`、`client_credentials`（开放 API）、`social`（社交登录，自定义扩展）。

### 4.3 SSO 单点登录流程

```
业务系统A  --302-->  /sso/authorize?client_id&redirect_uri&scope&response_type=code
                     ↓ 未登录 → 引导登录（账密/短信/扫码）
                     ↓ 已登录 → 展示/自动授权(auto_approve)
           <--302--  redirect_uri?code=xxx
业务系统A  --POST--> /oauth2/token  (grant_type=authorization_code, client_id, client_secret, code)
           <-------- access_token / refresh_token
```

- 统一登出：`/oauth2/token` DELETE + 广播 `logout` 事件，各接入系统按 `system_oauth2_approve` 记录反查并清理。
- 单点登录态由 `system` 服务持有，网关与业务服务只做 token 校验。

### 4.4 鉴权链路

1. 网关 `AuthGlobalFilter`：白名单放行；否则取 `Authorization: Bearer xxx` → 调用 `system` 的 `/rpc-api/system/oauth2/check-token`（本地 Caffeine 短缓存 60s）→ 解析出 `userId/userType/tenantId/scopes`。
2. 网关把结果写入内部请求头 `X-Login-User`（Base64+HMAC 签名，密钥来自 Nacos），并**强制剥离**外部传入的同名头，防伪造。
3. 业务服务 `LoginUserFilter` 校验 HMAC → 构建 `LoginUser` → 放入 `SecurityContextHolder` 与 `TenantContextHolder`。
4. 方法级鉴权：`@PreAuthorize("@ss.hasPermission('system:user:create')")` / `hasAnyPermissions` / `hasRole` / `hasScope`（开放 API 用）。

### 4.5 动态菜单与按钮级权限

- `system_menu.type`：1=目录 2=菜单 3=按钮；`permission` 字段是权限标识（如 `system:user:update`），按钮类型的菜单只用于产生权限标识。
- 登录后 `GET /admin-api/system/auth/get-permission-info` 返回：`user` + `roles[]` + `permissions[]` + `menus[]`（树形，含 `component/path/icon/visible/keepAlive/alwaysShow`）。
- 权限缓存：角色→菜单权限集合缓存在 Redis，菜单/角色变更时主动失效并通过 Redis Pub/Sub 通知各节点清本地缓存。
- 超管（`role.code = super_admin`）跳过权限校验，直接拥有全部权限。

### 4.6 开放 API 鉴权

- `open_app`：`app_key / app_secret / status / ip_whitelist / rate_limit / expire_time`。
- 两种模式并存：
  - **Token 模式**：`client_credentials` 换 token，走统一 OAuth2 链路，scope 由 `open_app_scope` 限定。
  - **签名模式**：`X-App-Key / X-Timestamp / X-Nonce / X-Sign`，`sign = HMAC-SHA256(appSecret, 排序后的参数 + body + timestamp + nonce)`；timestamp 5 分钟有效，nonce 用 Redis 去重防重放。
- 网关按 `app_key` 做令牌桶限流；调用明细落 `open_api_log`。

## 5. 多租户

- 模式：**单库行级隔离**（`tenant_id` 字段）。
- DB 层：MP `TenantLineInnerInterceptor` 自动拼 `tenant_id`；`@TenantIgnore` 注解跳过。
- Redis 层：Key 自动加租户前缀。
- 定时任务：`@TenantJob` 遍历所有开启的租户执行。
- 套餐：`system_tenant_package` 限定租户可用菜单集合，租户管理员的角色权限与套餐求交集。

## 6. 数据库

- 库名 `ccloud`，字符集 `utf8mb4 / utf8mb4_0900_ai_ci`。
- 表前缀按模块划分：`system_ / infra_ / member_ / open_ / pay_ / bpm_ / product_ / trade_ / promotion_ / ai_`，另有 Flowable 自建的 `ACT_*`。
- 公共字段：`id, creator, create_time, updater, update_time, deleted, tenant_id`。
- 脚本组织：
  - `sql/mysql/ccloud.sql` — 建库 + 全量建表 + 初始化数据（一键导入）
  - `sql/mysql/ddl/` — 按模块拆分的建表脚本
  - `sql/mysql/dml/` — 初始化数据（菜单、角色、字典、租户、OAuth2 客户端、开放应用示例）
  - `sql/mysql/xxl-job.sql`、`sql/mysql/quartz.sql`（如需）
- 初始账号：
  - 管理端 `admin / admin123`（超管，默认租户「ccloud 总部」，tenant_id=1）
  - 管理端 `ccloud / ccloud123`（普通管理员，演示权限差异）
  - C 端 `13800138000 / member123`
  - 开放应用 `ccloud-open-demo` + secret

## 7. 部署

```
docker/
├── docker-compose.yml            # 全量：mysql/redis/nacos/xxl-job-admin + 全部微服务
├── docker-compose.middleware.yml # 只起中间件，本地 IDE 跑服务时用
├── mysql/init/                   # 首次启动自动导入 ccloud.sql
├── nacos/                        # 配置导入包（各服务 dataId）
└── xxl-job/
每个可执行模块目录下有 Dockerfile（多阶段 + 分层 jar）
```

端口规划：网关 48080 / system 48081 / infra 48082 / member 48083 / open 48084 / pay 48085 / bpm 48086 / product 48087 / trade 48088 / promotion 48089 / ai 48090；Nacos 8848、MySQL 3306、Redis 6379、XXL-Job Admin 48180。

## 8. API 文档

- 每个服务 `springdoc` + `Knife4j`，按 `admin-api / app-api / open-api` 分组。
- 网关聚合：`http://localhost:48080/doc.html`，通过 `knife4j.gateway` 从 Nacos 服务列表动态发现下游文档。
- 生产环境通过配置关闭。

## 9. 交付分期

单轮无法产出全部代码（对标项目量级 ~40 万行）。分 5 期交付，**每期结束时项目可编译、可启动、可用**：

| 期 | 内容 | 完成标志 |
|---|---|---|
| P1 | 根 POM + BOM + 全部 framework starter + gateway + module-system（认证/OAuth2/SSO/用户/角色/菜单/部门/岗位/字典/租户/日志） + SQL + Docker + README | `docker compose up` 后能登录、拿 token、取动态菜单、按钮权限生效、Swagger 可调 |
| P2 | module-infra（文件/配置/代码生成/API 日志/任务/数据源/DB 文档） + module-member + module-open（含签名鉴权与限流） | 三端接口齐备，开放 API 可签名调用 |
| P3 | module-pay（微信/支付宝/钱包/退款/转账/回调重试） + module-bpm（Flowable 模型、表单、审批、抄送） | 可发起支付与审批流 |
| P4 | mall：product + trade + promotion | 可下单、支付、售后、用券 |
| P5 | module-ai + 全局补齐（单测、监控、压测配置、文档完善） | 全量交付 |

## 10. 风险

1. **Spring Cloud Alibaba 2025.0.0.0** 是较新版本，与 SC 2025.0.3 的兼容性在 P1 构建时立即验证；若不通过，回退到 Nacos 官方 `nacos-discovery-spring-boot-starter` + 手写配置。
2. **Flowable 8.0.0** 可能已面向 Spring Boot 4；P3 开始前先验证 7.x 与 Boot 3.5 的组合。
3. **Redisson** 已发布 4.x 主线，P1 构建时实测并锁定与 Boot 3.5 兼容的版本（优先 3.x LTS）。
4. 支付/微信相关模块需要真实商户凭证才能端到端验证；代码按可配置沙箱实现，交付时提供 Mock 渠道以便本地跑通。
