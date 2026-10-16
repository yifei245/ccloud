# Nacos 配置包

`config/` 下每个文件对应 Nacos 中的一个配置，dataId 即文件名，group 为 `DEFAULT_GROUP`，命名空间为 `public`。

| dataId | 导入的服务 | 内容 |
|---|---|---|
| `ccloud-common.yaml` | 全部 | 内部签名时效、租户校验缓存、文案刷新、消息重投 |
| `ccloud-datasource.yaml` | system、infra、member、open、pay | 连接池的通用行为 |
| `ccloud-gateway.yaml` | gateway | 限流阈值、令牌缓存、开放接口时间戳偏差与应用缓存 |
| `ccloud-system-server.yaml` | system | 登录失败锁定、短信验证码 |
| `ccloud-infra-server.yaml` | infra | 文件大小上限、访问日志与异常日志保留天数 |
| `ccloud-member-server.yaml` | member | 会员登录失败锁定、C 端功能开关 |
| `ccloud-open-server.yaml` | open | 回调的连接与读取超时、调用日志保留天数（回调内网开关 `allow-private-address` 不在配置包中，见约定 5） |
| `ccloud-pay-server.yaml` | pay | 钱包充值应用标识、充值单有效期、定时任务批量与补查时间（模拟支付开关与回调地址不在配置包中，见约定 6） |

## 导入

- docker compose 启动时由 `ccloud-nacos-init` 自动导入，已存在的 dataId 跳过，不覆盖控制台改过的值。
- 手动导入：`scripts/nacos-import.sh`（跳过已存在的），`scripts/nacos-import.sh --overwrite`（以仓库文件为准全部覆盖）。
  Nacos 地址默认取 `docker/.env` 的 `NACOS_PORT`，其他环境用 `NACOS_SERVER=http://host:8848/nacos` 指定。
- 校验：`scripts/verify-nacos.sh`；全部服务运行时加 `--full`，另外校验优先级与即时生效。

Nacos 3 已移除 v1、v2 的配置发布接口，脚本使用 `/nacos/v3/admin/cs/config`。

## 约定

1. **优先级**：各服务在 `application-{profile}.yaml` 中导入这些配置，因此 Nacos 高于 profile 文件，profile 文件高于 `application.yaml`。
   若写在 `application.yaml` 中导入，profile 文件会反过来覆盖 Nacos。
2. **不放连接地址与密钥**：同一份配置包会导入每个环境的 Nacos，而 Nacos 又高于 profile 文件；
   写入数据库地址会覆盖本地开发的配置，写入密钥则会进入仓库。`verify-nacos.sh` 会拒绝此类键。
   开放平台应用的密钥只在各环境的 Nacos 控制台中填写。
3. **生效方式**：文件中标注"即时生效"的项绑定在 `@ConfigurationProperties` 上，控制台发布后即生效；其余需重启服务。
4. 网关匿名白名单 `ccloud.security.ignore-urls` 不在此配置，须留在网关包内的 `application.yaml`，由 system、member 的一致性测试校验。
5. **回调内网开关只在 profile 中设置**：open 的 `ccloud.open.notify.allow-private-address` 由 profile 决定（local / dev 为 `true`，prod 与默认为 `false`），
   配置包不得包含该项，`verify-nacos.sh` 发现 `allow-private-address: true` 即失败。
   **已导入过旧配置包的环境**（2026-10 之前的 `ccloud-open-server.yaml` 含 `allow-private-address: true`）：导入脚本对已存在的 dataId 跳过，
   不会自动更新，须在该环境 Nacos 控制台的 `ccloud-open-server.yaml` 中手工删除这一行并发布，否则生产会关闭回调的 SSRF 防护；
   也可用 `scripts/nacos-import.sh --overwrite` 以仓库文件为准覆盖（会同时覆盖控制台改过的其他值）。
6. **模拟支付开关只在 profile 中设置**：pay 的 `ccloud.pay.mock-enabled` 只在 local、dev 的 profile 中为 `true`，配置包不得包含该项，
   `verify-nacos.sh` 发现 `mock-enabled: true` 即失败。生产必须为 `false`，否则任何人都能经免登录的确认页让 Mock 订单"支付成功"。
   渠道回调地址前缀 `ccloud.pay.notify-base-url` 属于各环境的部署地址，同样不放进配置包。
