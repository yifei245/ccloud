# ccloud

![JDK](https://img.shields.io/badge/JDK-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.16-brightgreen)
![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2025.0.3-brightgreen)
![License](https://img.shields.io/badge/License-MIT-yellow)

ccloud 是一个基于 Spring Cloud 的微服务中台：多租户、多终端（Web / App / 小程序 / H5）、多用户类型（管理员 / 会员 / 开放平台应用），
内置自研 OAuth2 与 SSO、按钮级权限、部门数据权限、国际化、本地消息表最终一致性。

当前交付的是 **P1 阶段**：框架层 16 个模块、网关、system（系统管理）、member（会员）、monitor（服务监控），以及 docker 编排与 Nacos 配置包。
infra、open、pay、bpm、商城、AI 等模块在后续阶段交付，见[项目结构](#项目结构)。

环境：JDK 21 · Spring Boot 3.5.16 · Spring Cloud 2025.0.3 · Spring Cloud Alibaba 2025.0.0.0 · Nacos 3.2.4 · MySQL 8.4 · Redis 7

## 技术栈

| 分类 | 选型 | 版本 |
|---|---|---|
| 运行时 | JDK | 21 |
| 框架 | Spring Boot | 3.5.16 |
| 微服务 | Spring Cloud | 2025.0.3 |
| 注册 / 配置中心 | Spring Cloud Alibaba + Nacos | 2025.0.0.0 / 3.2.4 |
| 网关 | Spring Cloud Gateway（Server WebFlux） | 4.3.x |
| 限流 | 网关 Redis 令牌桶（多实例共享配额） | 随 Spring Cloud Gateway |
| 安全 | Spring Security | 6.5.x |
| ORM / 多数据源 | MyBatis Plus / dynamic-datasource | 3.5.17 / 4.5.0 |
| 数据库 / 连接池 | MySQL（驱动 mysql-connector-j）/ Druid | 8.4（9.7.0）/ 1.2.28 |
| 缓存 | Redis + Redisson | 7 / 3.52.0 |
| 接口文档 | springdoc-openapi + Knife4j | 2.9.1 / 4.5.0 |
| 定时任务 | XXL-Job | 3.4.2 |
| 实时通信 | Spring WebSocket + Redis Pub/Sub 广播 | 随 Spring Boot |
| 链路追踪 | Micrometer Tracing + Zipkin | 随 Spring Boot / Zipkin 3.6.1 |
| 服务监控 | Spring Boot Admin | 3.5.10 |
| 对象映射 / Excel | MapStruct / FastExcel | 1.6.3 / 1.3.0 |
| 工具 | Hutool / Guava / fastjson2 | 5.8.47 / 33.7.1-jre / 2.0.65 |
| 社交登录 / 微信 | JustAuth / WxJava | 1.16.7 / 4.8.0 |
| 工作流（P3） | Flowable | 7.2.0 |
| 容器 | Docker + docker compose | — |

与设计文档的差异：限流未采用 Sentinel，改为网关 Redis 令牌桶——Sentinel 网关规则按单个实例计数，多实例部署时实际阈值会成倍放大。

## 项目结构

```
ccloud
├── ccloud-dependencies                   # BOM，全部依赖版本在此仲裁
├── ccloud-framework/                     # ★ 框架层，不含业务，详见 docs/模块说明.md
│   ├── ccloud-common                     #   统一响应、异常、枚举、工具
│   └── ccloud-spring-boot-starter-*      #   web / security / mybatis / redis / mq / job / excel / protection /
│                                         #   biz-tenant / biz-data-permission / rpc / monitor / websocket / i18n / test
├── ccloud-gateway                        # ★ 网关：路由、鉴权与内部身份头、限流、开放接口签名、文档聚合
├── ccloud-monitor                        #   服务健康面板（Spring Boot Admin）
├── ccloud-module-system/  (api, biz)     # ★ 认证 / OAuth2 / SSO / 用户 / 角色 / 菜单 / 部门 / 岗位 / 字典 / 租户 /
│                                         #   社交登录 / 短信 / 邮件 / 站内信 / 通知公告 / 国际化 / 操作与登录日志
├── ccloud-module-member/  (api, biz)     # ★ C 端会员与认证（密码、短信、社交、小程序一键登录）
├── docker/                               # docker compose 编排、中间件配置、Nacos 配置包
├── sql/mysql/                            # 建表与初始化数据
├── scripts/                              # 构建、部署与校验脚本
└── docs/                                 # 使用文档与设计文档
```

后续阶段的模块（尚未包含在仓库中）：`ccloud-module-infra`（★ 文件、配置、代码生成、定时任务管理）、
`ccloud-module-open`（★ 开放平台）、`ccloud-module-pay`、`ccloud-module-bpm`、商城（product / trade / promotion）、`ccloud-module-ai`。

`★` 为核心模块，必须保留。**裁剪非核心模块**：删除模块目录，从根 `pom.xml` 的 `<modules>` 中移除，
再删除 `docker/docker-compose.yml` 中对应的服务即可。模块之间只通过 `-api` 子模块（DTO、Feign 接口、错误码）耦合，
`-biz` 不被任何模块依赖，因此删除一个业务模块不会影响其他模块编译。

## 快速开始

需要 JDK 21、Maven 3.9+、Docker（含 compose v2），以及本机已有的 MySQL、Redis、Nacos、Zipkin、XXL-Job 调度中心
（ccloud 直接连接它们，不再自带一套，约定见 [docs/快速开始.md](docs/快速开始.md#本机中间件)）。三种方式：

1. **一键启动全部**（网关 + system + member + monitor，连接本机中间件）：

   ```bash
   scripts/docker-up.sh
   ```

   首次启动会在本机中间件中初始化 `ccloud` 库、XXL-Job 任务与 Nacos 的 `ccloud` 命名空间，再打包、构建镜像并启动，约需 5–10 分钟。
   停止用 `scripts/docker-down.sh`，只停 ccloud 的应用，本机中间件不受影响。

2. **只初始化，在 IDE 中运行服务**：

   ```bash
   scripts/docker-up.sh init
   ```

   然后以 `local` profile 依次运行 `SystemServerApplication`、`MemberServerApplication`、`GatewayServerApplication`。

3. **从源码构建**：

   ```bash
   mvn clean package -DskipTests
   ```

   可执行 jar 位于各应用模块的 `target/` 下；`mvn test` 运行全部测试（连接本机已有的 MySQL 与 Redis，配置见 [快速开始](docs/快速开始.md#方式三从源码构建)）。

端口、库编号与命名空间在 `docker/.env`；MySQL、Redis 密码取自本机容器配置，Nacos 账号写在 `~/.ccloud/middleware.env`。

## 登录账号

以下为初始化数据与 `docker/.env` 中的开发默认值，**任何对外环境都必须修改**。

| 端 | 账号 | 密码 | 说明 |
|---|---|---|---|
| 管理端 | `admin` | `admin123` | 超级管理员，租户「ccloud 总部」（编号 1） |
| 管理端 | `ccloud` | `ccloud123` | 普通管理员，仅查询与导出类权限，演示按钮级权限差异 |
| C 端 | `13800138000` | `member123` | 会员，租户 1 |
| 开放 API | AppKey `ccloud-open-demo` | `ccloud-open-demo-secret` | 数据见 `sql/mysql/dml/07-demo.sql`；P1 阶段须先在 Nacos 登记，见[接口与鉴权](#接口与鉴权) |
| Nacos | 本机 Nacos 的账号 | | 写在 `~/.ccloud/middleware.env`；ccloud 使用 `ccloud` 命名空间 |
| XXL-Job | `admin` | `ccloud123` | `docker/.env` 的 `XXL_JOB_ADMIN_PASSWORD`（首次登记 ccloud 任务时写入） |
| 服务监控 | `admin` | `ccloud123` | `docker/.env` 的 `CCLOUD_MONITOR_PASSWORD` |

## 访问地址

ccloud 应用的端口见 `docker/.env`，除网关外只绑定本机 127.0.0.1；中间件为本机已有的实例。

| 服务 | 地址 | 说明 |
|---|---|---|
| 网关 | http://127.0.0.1:48080 | 唯一对外入口，前端与第三方都经网关访问 |
| 接口文档 | http://127.0.0.1:48080/doc.html | 聚合各服务的管理端、用户端、开放接口三组文档；生产环境关闭 |
| 服务监控 | http://127.0.0.1:48182 | Spring Boot Admin，从 Nacos 发现全部服务 |
| Nacos | http://127.0.0.1:8848/nacos | 本机 Nacos，ccloud 的服务与配置在 `ccloud` 命名空间 |
| XXL-Job 控制台 | http://127.0.0.1:48180/xxl-job-admin | 已登记 system 的执行器与两个消息重投任务 |
| Zipkin | http://127.0.0.1:9411 | 链路追踪 |
| system / member | http://127.0.0.1:48081 、 http://127.0.0.1:48083 | 仅供本机调试，前端不要直连 |

## 接口与鉴权

四类接口按路径前缀区分，网关据此路由与鉴权。完整说明与示例见 [docs/接口鉴权说明.md](docs/接口鉴权说明.md)。

| 前缀 | 调用方 | 鉴权方式 |
|---|---|---|
| `/admin-api/**` | 管理后台 | `Authorization: Bearer {accessToken}`，令牌由 `/admin-api/system/auth/login` 签发；按钮级权限 `@PreAuthorize` |
| `/app-api/**` | App / 小程序 / H5 | 同上，令牌由 `/app-api/member/auth/*` 签发，`X-Client-Id` 区分终端 |
| `/open-api/**` | 第三方系统 | HMAC-SHA256 签名：`X-Open-App-Id`、`X-Open-Timestamp`、`X-Open-Nonce`、`X-Open-Sign` |
| `/rpc-api/**` | 服务之间 | 仅内部调用，**经网关访问一律返回 403** |

登录、发送验证码等匿名接口须携带 `X-Tenant-Id` 指明租户；已登录的请求由网关按令牌中的租户设置该请求头，请求自带的值会被覆盖。管理员令牌不能访问 `/app-api`，会员令牌不能访问 `/admin-api`。

管理端登录示例：

```bash
curl -s -X POST http://127.0.0.1:48080/admin-api/system/auth/login \
  -H 'Content-Type: application/json' -H 'X-Tenant-Id: 1' \
  -d '{"username":"admin","password":"admin123"}'
# 返回 {"code":0,"data":{"accessToken":"...","refreshToken":"...",...}}

curl -s 'http://127.0.0.1:48080/admin-api/system/user/page?pageNo=1&pageSize=10' \
  -H 'Authorization: Bearer {accessToken}'
```

开放接口签名：

```
签名串 = METHOD + "\n" + 原始路径 + "\n" + 原始查询串 + "\n" + X-Open-Timestamp + "\n" + X-Open-Nonce + "\n" + 十六进制 SHA-256(请求体)
X-Open-Sign = Base64(HmacSHA256(appSecret, 签名串))
```

时间戳为毫秒，允许偏差 5 分钟；nonce 在 10 分钟内不可重复。计算示例代码见 [docs/接口鉴权说明.md](docs/接口鉴权说明.md#开放接口-open-api)。
P1 阶段开放平台服务尚未交付：应用在 Nacos 的 `ccloud-gateway.yaml` 中登记（`ccloud.open.apps`），签名链路已可用，但还没有开放的业务接口。

## 数据库

- 库名 `ccloud`，字符集 `utf8mb4`，排序规则 `utf8mb4_0900_ai_ci`；所有模块共用一个库，业务表带 `tenant_id` 行级隔离。
- 逻辑删除：每张表都有 `deleted bigint NOT NULL DEFAULT 0`，0 表示未删除，删除时写入记录自身的编号。唯一索引一律带上 `deleted`
  （如 `(username, tenant_id, deleted)`），未删除的记录仍受唯一约束，同一个值也可以反复删除、重建。新建表须遵守这一约定，`scripts/verify-sql.sh` 会校验。
- 全量脚本 `sql/mysql/ccloud.sql`（建库 + 全部建表 + 初始化数据），由 `sql/mysql/ddl/`（按模块建表）与 `sql/mysql/dml/`（初始化数据）拼装生成，**不要手工编辑**。
  修改 `ddl/`、`dml/` 后执行 `scripts/build-sql.sh` 重新生成，`scripts/verify-sql.sh` 校验。
- `scripts/docker-up.sh` 在本机 MySQL 中没有 `ccloud` 库时自动导入；需要重新初始化时执行 `scripts/docker-up.sh --reset-db`（只影响 `ccloud` 库）。
  XXL-Job 调度中心的库 `xxl_job` 按 `docker/mysql/init/` 的脚本初始化。

## 常见问题

**提示本机中间件容器未运行。** 先 `docker start mysql redis nacos zipkin xxl-job-admin`；容器名不同时用 `CCLOUD_MYSQL_CONTAINER` 等环境变量指定，
端口不同时用 `docker/.env` 中的同名环境变量覆盖。

**Apple 芯片上 XXL-Job 启动很慢。** 官方镜像只提供 amd64，经模拟运行，首次启动约需 1 分钟，属正常现象。

**接口返回"请求未携带租户编号"。** 请求头缺少 `X-Tenant-Id`。登录等匿名接口依靠它确定租户，演示数据的租户编号为 1。

**管理端登录需要验证码吗？** `local` 与 `dev` profile 默认关闭验证码，便于调试；生产默认开启，由环境变量 `CCLOUD_CAPTCHA_ENABLED` 控制。

**用管理员令牌调用 /app-api 返回 401。** 预期行为：管理员与会员是两套用户体系、编号会重复，服务端按路径校验令牌的用户类型。

**修改了代码，镜像里还是旧逻辑。** 打包必须带 `clean`：只有依赖（如 framework）变化时，Maven 会认为应用 jar 未过期而跳过重新打包。`scripts/docker-up.sh` 已经这样做。

**在 Nacos 控制台改了配置不生效。** 各服务在 `application-{profile}.yaml` 中导入 Nacos 配置，Nacos 优先于包内配置；
配置包中标注"需重启"的项要重启服务。配置包的约定见 [docker/nacos/README.md](docker/nacos/README.md)。

**退出登录、修改密码后旧令牌还能用一会儿。** 网关缓存令牌校验结果 60 秒；经本网关实例登出会立即清除，其他实例最多延迟 60 秒。

## License

[MIT License](LICENSE)
