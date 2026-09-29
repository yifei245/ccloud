# 会员运营功能设计（P2 第 1 项）

> 状态：已确认（2026-09-29）
> 上游：[总体设计](2026-09-07-ccloud-design.md) 的 P2「module-member 运营功能」；P1 已交付会员用户与认证、全部会员表结构、菜单与按钮权限、字典与演示数据。

## 1. 目标与范围

在 `ccloud-module-member` 中补齐会员运营功能：管理端的会员与运营数据管理、用户端的地址、积分、等级、签到，以及 `member-api` 已定义的 4 个内部接口。

### 1.1 管理端（`/admin-api/member/**`，沿用 P1 已登记的菜单与按钮权限）

| 功能 | 接口 | 权限标识 |
|---|---|---|
| 会员用户 | 分页查询（手机号、昵称、注册时间、最后登录时间、等级、分组、标签）；详情；修改（昵称、头像、状态、性别、生日、备注、标签、分组）；修改积分；修改等级；删除 | `member:user:query / update / delete / update-point / update-level` |
| 标签 | 增删改查、全部列表 | `member:tag:*` |
| 分组 | 增删改查（含启用/停用）、启用中的全部列表 | `member:group:*` |
| 等级 | 增删改查（含启用/停用）、启用中的全部列表 | `member:level:*` |
| 等级变更记录、经验记录 | 分页查询 | `member:level-record:query`、`member:experience-record:query` |
| 积分记录 | 分页查询 | `member:point-record:query` |
| 签到规则 | 增删改查 | `member:sign-in-config:*` |
| 签到记录 | 分页查询 | `member:sign-in-record:query` |
| 会员配置 | 查询、保存（每个租户一条） | `member:config:query / update` |

### 1.2 用户端（`/app-api/member/**`，均需会员登录）

| 功能 | 接口 |
|---|---|
| 收件地址 | 新增、修改、删除、详情、列表、获得默认地址 |
| 积分 | 我的积分记录分页（可按收入 / 支出筛选） |
| 等级与经验 | 启用中的等级列表、我的等级与经验、我的经验记录分页 |
| 签到 | 启用中的签到规则列表、我的签到概况、签到、我的签到记录分页 |

### 1.3 内部接口（`/rpc-api/member/**`）

实现 `member-api` 已定义的 `MemberAddressApi`、`MemberLevelApi`、`MemberExperienceApi`、`MemberPointApi`，接口签名不变。

### 1.4 顺带处理

- 管理员禁用或删除会员时，经 `OAuth2TokenApi.removeAccessTokensByUser` 作废该会员的全部令牌，与 P2 第 0 项对管理员的处理一致。
- 新注册会员按 0 经验确定初始等级；演示数据中的演示会员补上等级（青铜会员）。

### 1.5 不在范围

- 邀请注册奖励、下单送积分、积分抵扣等调用方（P4 商城负责，届时经内部接口调用）。
- 地区数据：收件地址与会员资料只保存前端传入的地区编号，不校验、不转名称，由前端维护区划数据。
- 补签。

## 2. 关键决策

| 决策 | 结论 | 理由 |
|---|---|---|
| 地区数据 | 不引入，只存编号 | 用户选择；P4 商城再评估 |
| 签到超过最大配置天数 | 一直按最大天数的规则发奖励，连续天数照常累加 | 用户选择；鼓励长期连续签到 |
| 断签 | 从第 1 天重新开始，不支持补签 | 常规做法 |
| 经验减少时等级 | 双向变化：按当前经验重算，可降级 | 用户选择；等级与经验始终一致 |
| 并发与防重复 | 数据库行锁：事务内 `SELECT ... FOR UPDATE` 锁定会员行，再查重、改值、写记录、重算等级 | 锁与事务同生命周期，不依赖 Redis，不改表结构；会员维度变更频率低，开销可忽略 |

未采用的方案：Redis 分布式锁（锁在事务提交前释放会读到旧值）；记录表加唯一索引（需改 P1 表结构，且仍需行锁防止扣成负数）。

## 3. 业务规则

### 3.1 会员行锁

`MemberUserMapper` 增加 `selectByIdForUpdate(id)`。积分、经验、等级、签到的所有写操作都在同一事务内先调用它，再做后续步骤。会员不存在时报 `USER_NOT_EXISTS`。

### 3.2 积分（`MemberPointService`）

- 统一入口：`addPoint(userId, point, bizType, bizId)`，`point` 为有符号变动量。内部接口的 `addPoint` / `reducePoint` 与签到、管理员修改都经过它。
- 内部接口的校验：
  - 数量必须大于 0；
  - 业务类型必须存在，方向与 `MemberPointBizTypeEnum.add` 一致：`addPoint` 只接受 `add = true` 的类型，`reducePoint` 只接受 `add = false` 的类型；
  - 违反时报 `POINT_BIZ_TYPE_INVALID`。
- 防重复：同一「会员 + 业务类型 + 业务编号」已有记录时直接返回成功，余额不变。
- 扣减后余额小于 0 时报 `USER_POINT_NOT_ENOUGH`，不写记录。
- 每次变动写一条 `member_point_record`：
  - 标题取业务类型名称；
  - 描述按业务类型的模板生成（`{}` 替换为变动量的绝对值）；
  - 记录变动量（有符号）与变动后的余额。
- 管理员修改积分：输入非 0 的调整量，正加负减，调整后不能小于 0（否则报 `USER_POINT_NOT_ENOUGH`）。业务类型为 `ADMIN`，业务编号自动生成（UUID），保证每次操作都生效。描述按正负号生成：「管理员增加 N 积分」或「管理员扣减 N 积分」（`ADMIN` 的模板只适合增加）。

### 3.3 经验与等级（`MemberLevelService`）

- 统一入口：`addExperience(userId, experience, bizType, bizId)`，规则同积分，差别如下：
  - 方向与 `MemberExperienceBizTypeEnum.add` 一致；违反时报 `EXPERIENCE_BIZ_TYPE_INVALID`；
  - 扣减时经验不足则扣到 0，不报错。记录中的变动量为实际扣减量。
- 重算等级：经验变动后，在已启用的等级中取门槛经验不超过当前经验、门槛最高的一个；没有满足的等级时为空。
- 等级变化时写一条 `member_level_record`，同时记录：
  - 新等级的快照：序号、折扣、门槛经验；
  - 变更时会员的经验；
  - 描述：「升级为 X」或「降级为 X」。
- 管理员修改等级：
  - 必须选择已启用的等级（否则报 `LEVEL_NOT_EXISTS` / `LEVEL_DISABLED`），并填写原因。选择的就是当前等级时不做任何变更。
  - 会员经验调到该等级的门槛。差值非 0 时写一条 `ADMIN` 类型的经验记录，业务编号自动生成，描述为「管理员调整增加 N 经验」或「管理员调整扣减 N 经验」。
  - 写一条等级变更记录，备注为管理员填写的原因。
- 修改等级门槛、停用等级时，不批量重算已有会员。停用等级上的会员保留原等级，下次经验变动时重算。
- 等级配置校验：
  - 等级序号不能重复（`LEVEL_VALUE_EXISTS`）；
  - 序号越大，门槛经验必须越高（`LEVEL_EXPERIENCE_ORDER_INVALID`），与其他所有等级比较；
  - 折扣在 0 到 100 之间（参数校验）。
- 删除等级时，仍有会员处于该等级则报 `LEVEL_HAS_USER`。
- 新注册会员：`MemberUserServiceImpl` 创建会员时按 0 经验计算初始等级。

### 3.4 签到（`MemberSignInRecordService`）

- 「今天」「昨天」按应用默认时区的自然日计算，Docker 镜像设为 `Asia/Shanghai`（实施时核对）。
- 签到步骤（同一事务）：
  1. 锁会员行；
  2. 今天已有签到记录，则报 `SIGN_IN_RECORD_TODAY_EXISTS`；
  3. 最后一条签到记录是昨天的，则连续天数为它的天数加 1，否则为 1；
  4. 奖励规则：在已启用的签到规则中，取「天数不超过连续天数」里天数最大的一条。
     - 规则不连续时（只配了第 1、3、7 天），第 2 天按第 1 天发放。
     - 超过最大天数后，一直按最大天数发放。
     - 没有可用规则时，照常签到，奖励为 0。
  5. 写签到记录（连续天数、获得积分、获得经验）；
  6. 以签到记录编号作为业务编号：积分非 0 时按 `SIGN` 加积分，经验非 0 时按 `SIGN_IN` 加经验。
- 签到概况：
  - 今天是否已签到；
  - 连续天数：最后一次签到在今天或昨天时取其天数，否则为 0；
  - 累计签到天数：签到记录条数。
- 签到规则：天数大于等于 1，同一租户内不能重复（`SIGN_IN_CONFIG_DAY_EXISTS`）；积分与经验大于等于 0。

### 3.5 收件地址（`MemberAddressService`）

- 每人最多 20 个，超出报 `ADDRESS_COUNT_EXCEEDED`。
- 新增时，用户还没有地址则自动设为默认。设为默认时，同一事务内取消该用户其他地址的默认状态。
- 删除默认地址后，不自动另选。
- 只能访问自己的地址。访问他人地址按不存在处理（`ADDRESS_NOT_EXISTS`）；内部接口 `getAddress(id, userId)` 返回 null。

### 3.6 标签、分组、会员配置、会员用户

- 标签名称、分组名称在同一租户内不能重复（`TAG_NAME_EXISTS` / `GROUP_NAME_EXISTS`）。
- 仍有会员使用的标签、分组不能删除（`TAG_HAS_USER` / `GROUP_HAS_USER`）。标签按 `tag_ids` 逗号列表匹配。
- 修改会员时：
  - 标签必须全部存在（`TAG_NOT_EXISTS`）；
  - 分组必须存在且启用（`GROUP_NOT_EXISTS` / `GROUP_DISABLED`）。
- 会员配置每个租户一条：没有时保存即新增，查询返回 null 时由前端按默认值展示。
- 禁用会员（状态改为关闭）、删除会员后，调用 `OAuth2TokenApi.removeAccessTokensByUser(userId, MEMBER)`。

## 4. 错误码

在 `ErrorCodeConstants` 补充以下错误码，并在 `sql/mysql/dml/06-i18n.sql` 登记中英文文案（由现有一致性测试校验）：

| 段 | 错误码 | 编号 | 中文 |
|---|---|---|---|
| 地址 | `ADDRESS_COUNT_EXCEEDED` | 1_004_004_001 | 收件地址最多 {} 个 |
| 标签 | `TAG_NOT_EXISTS` | 1_004_006_000 | 用户标签不存在 |
| | `TAG_NAME_EXISTS` | 1_004_006_001 | 用户标签已经存在 |
| | `TAG_HAS_USER` | 1_004_006_002 | 用户标签下存在用户，无法删除 |
| 积分 | `POINT_BIZ_TYPE_INVALID` | 1_004_008_000 | 积分业务类型与操作不符 |
| 签到 | `SIGN_IN_CONFIG_NOT_EXISTS` | 1_004_009_000 | 签到规则不存在 |
| | `SIGN_IN_CONFIG_DAY_EXISTS` | 1_004_009_001 | 签到天数规则已存在 |
| | `SIGN_IN_RECORD_TODAY_EXISTS` | 1_004_010_000 | 今日已签到，请勿重复签到 |
| 等级 | `LEVEL_VALUE_EXISTS` | 1_004_011_002 | 用户等级值已被使用 |
| | `LEVEL_EXPERIENCE_ORDER_INVALID` | 1_004_011_003 | 等级越高，升级所需经验必须越多 |
| | `LEVEL_HAS_USER` | 1_004_011_004 | 用户等级下存在用户，无法删除 |
| | `LEVEL_DISABLED` | 1_004_011_005 | 用户等级已停用 |
| 经验 | `EXPERIENCE_BIZ_TYPE_INVALID` | 1_004_013_000 | 经验业务类型与操作不符 |
| 分组 | `GROUP_NOT_EXISTS` | 1_004_012_000 | 用户分组不存在 |
| | `GROUP_NAME_EXISTS` | 1_004_012_001 | 用户分组已经存在 |
| | `GROUP_HAS_USER` | 1_004_012_002 | 用户分组下存在用户，无法删除 |
| | `GROUP_DISABLED` | 1_004_012_003 | 用户分组已停用 |

编号如与现有测试约定冲突，实施时按测试要求调整，并同步本表。

## 5. 代码结构

按功能分包，沿用会员模块与 system 模块的分层（`controller/admin`、`controller/app`、`service`、`dal/dataobject`、`dal/mysql`）：

| 包 | 内容 |
|---|---|
| `address` | 收件地址；`MemberAddressApiImpl` |
| `tag` | 标签 |
| `group` | 分组 |
| `level` | 等级、等级变更记录、经验记录；`MemberLevelService` 负责经验与等级；`MemberLevelApiImpl`、`MemberExperienceApiImpl` |
| `point` | 积分记录；`MemberPointService` 负责积分；`MemberPointApiImpl` |
| `signin` | 签到规则、签到记录 |
| `config` | 会员配置 |
| `user` | 在已有基础上补管理端接口 |

内部接口实现放在 `api/{address,level,point}`，与现有的 `MemberUserApiImpl` 并列。积分、经验、等级的变更只能经 `MemberPointService`、`MemberLevelService`，其他代码不直接改 `member_user` 的这三个字段。

## 6. 测试与验收

先写测试再实现，关键逻辑做变异检查（改坏代码确认测试能发现）。

- **业务逻辑测试**（真实 MySQL 与 Redis）：覆盖第 3 节每条规则，重点是：
  - 余额不足；
  - 重复业务编号；
  - 方向不符；
  - 经验扣到 0；
  - 升级、降级、停用等级；
  - 管理员改等级同步经验；
  - 签到跨天、断签、超过最大天数、规则不连续、无规则；
  - 默认地址切换、地址数量上限、访问他人地址；
  - 被使用的标签、分组、等级不能删除。
- **并发测试**：多线程同时扣同一会员的积分、同时签到。验证余额不会扣成负数、每天只有一条签到记录、记录与余额一致。
- **接口测试**：
  - 管理端按钮权限生效，没有权限返回 403；
  - 用户端只能访问自己的数据；
  - 管理员令牌访问用户端接口按未登录处理。
- **内部接口测试**：4 个接口的路径与参数与 `member-api` 定义一致，沿用 `FeignClientContractTest`。
- **错误码测试**：新增错误码的中英文文案，由现有一致性测试校验。
- **验收**：
  - `mvn test` 全部通过；
  - `scripts/verify-e2e.sh` 增加一组经网关的会员运营场景：
    1. 管理员给演示会员加积分、改等级；
    2. 会员签到；
    3. 查询积分记录与等级变更记录；
    4. 新增并查询收件地址；
    5. 经内部接口重复加积分只生效一次（直接请求 member 服务端口，内部接口不经网关）；
  - `scripts/verify-all.sh` 全量通过。
- **文档**：
  - [模块说明](../../模块说明.md) 更新 member 职责，移除后续阶段表中的该项；
  - [接口鉴权说明](../../接口鉴权说明.md) 用户端一节补充新接口。
