# 会员运营功能实施计划（P2 第 1 项）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `ccloud-module-member` 中交付会员运营功能：管理端的会员、标签、分组、等级、积分、签到、会员配置管理，用户端的收件地址、积分、等级、签到，以及 `member-api` 已定义的 4 个内部接口。

**Architecture:** 按功能分包，沿用会员模块与 system 模块已有的分层（Controller → Service → Mapper → DO）。积分、经验、等级的一切变动集中在 `MemberPointService` 与 `MemberLevelService`：事务内先 `SELECT ... FOR UPDATE` 锁定会员行，再查重、改值、写记录、重算等级。表结构 P1 已就位，本期只增加代码与初始化数据。

**Tech Stack:** JDK 21、Spring Boot 3.5.16、MyBatis Plus 3.5.17、MySQL 8.4、Redis 7；测试用 JUnit 5 + AssertJ + MockMvc，连接本机已有的 MySQL（`ccloud_test` 库）与 Redis（15 号库），见 `LocalMySql` / `LocalRedis`。

**Spec:** `docs/superpowers/specs/2026-09-29-member-operation-design.md`

## Global Constraints

每个任务的验收条件都隐含包含本节。

- 分支 `feat/p2-infra-member-open`，只在该分支提交，不推送。
- 构建一律离线：`mvn -o`。单模块测试加 `-am`，例如 `mvn -o -q -pl ccloud-module-member/ccloud-module-member-biz -am test -Dtest=XxxTest -Dsurefire.failIfNoSpecifiedTests=false`。安静模式下测试通过时没有输出，结果看 `target/surefire-reports/TEST-*.xml`。
- 路径前缀写在 Controller 上：管理端 `/admin-api/member/**`，用户端 `/app-api/member/**`，内部 `/rpc-api/member/**`（由 `member-api` 接口定义，不得改动签名）。
- 管理端接口用 `@PreAuthorize("@ss.hasPermission('...')")`，权限标识取自 `sql/mysql/dml/02-menu.sql` 已登记的按钮，不新增菜单。
- 用户端接口的用户编号一律取 `SecurityFrameworkUtils.getLoginUserId()`，不接受请求参数里的用户编号。
- DO 继承 `TenantBaseDO`，注解与 `MemberUserDO` 一致（`@TableName`、`@Data`、`@EqualsAndHashCode(callSuper = true)`、`@ToString(callSuper = true)`、`@Accessors(chain = true)`，主键 `@TableId`），不手工赋值 `tenantId`；时间字段用 `LocalDateTime`。
- Web 集成测试不回滚事务，数据会提交到共享的测试库：插入的等级序号一律 ≥ 90，会员编号用 902xx 段，并在 `@AfterEach` 中删除自己插入的数据（等级按 `level >= 90` 删除）。
- 错误码在 `ErrorCodeConstants` 定义，并在 `sql/mysql/dml/06-i18n.sql` 登记中英文文案；改了 `dml/` 或 `ddl/` 后执行 `scripts/build-sql.sh` 重新生成 `sql/mysql/ccloud.sql`。
- 先写测试再实现；每个任务结束前，对关键逻辑做变异检查：改坏代码，确认有测试失败，再还原。还原后必须 `touch` 被还原的文件，否则 Maven 会沿用改坏时编译出的 class。
- 提交信息用中文 + Conventional Commits 前缀，不加署名行。
- 与用户沟通一律用中文。
- 测试与校验脚本连接本机已有的 MySQL、Redis（见 `LocalMySql` / `LocalRedis`、`scripts/lib/local-mysql.sh`），不临时启动容器；缺少中间件时先问用户，装了不删。

## 共享约定（跨任务，务必逐字一致）

代码根目录简写：
- `{biz}` = `ccloud-module-member/ccloud-module-member-biz/src/main/java/com/ccloud/module/member`
- `{test}` = `ccloud-module-member/ccloud-module-member-biz/src/test/java/com/ccloud/module/member`
- `{api}` = `ccloud-module-member/ccloud-module-member-api/src/main/java/com/ccloud/module/member`

**会员行锁**（Task 1 新增，Task 3、4、5 使用）：

```java
// MemberUserMapper
default MemberUserDO selectByIdForUpdate(Long id) {
    return selectOne(new LambdaQueryWrapperX<MemberUserDO>().eq(MemberUserDO::getId, id).last("FOR UPDATE"));
}
```

**服务签名**（后续任务按此调用）：

```java
// {biz}/service/level/MemberLevelService.java（Task 2 建配置部分，Task 3 补经验与等级）
Long createLevel(MemberLevelSaveReqVO reqVO);
void updateLevel(MemberLevelSaveReqVO reqVO);
void deleteLevel(Long id);
MemberLevelDO getLevel(Long id);
List<MemberLevelDO> getLevelList(Collection<Long> ids);
List<MemberLevelDO> getEnableLevelList();                    // 按 level 升序
PageResult<MemberLevelDO> getLevelPage(MemberLevelPageReqVO reqVO);
MemberLevelDO calculateLevel(int experience);                // 无满足的等级返回 null
void addExperience(Long userId, Integer experience, Integer bizType, String bizId);
void reduceExperience(Long userId, Integer experience, Integer bizType, String bizId);
void updateUserLevel(Long userId, Long levelId, String reason);
PageResult<MemberLevelRecordDO> getLevelRecordPage(MemberLevelRecordPageReqVO reqVO);
PageResult<MemberExperienceRecordDO> getExperienceRecordPage(MemberExperienceRecordPageReqVO reqVO);
PageResult<MemberExperienceRecordDO> getExperienceRecordPage(Long userId, PageParam pageParam);

// {biz}/service/point/MemberPointService.java（Task 4）
void addPoint(Long userId, Integer point, Integer bizType, String bizId);
void reducePoint(Long userId, Integer point, Integer bizType, String bizId);
void updateUserPointByAdmin(Long userId, Integer point);     // point 非 0，正加负减
PageResult<MemberPointRecordDO> getPointRecordPage(MemberPointRecordPageReqVO reqVO);
PageResult<MemberPointRecordDO> getPointRecordPage(Long userId, AppMemberPointRecordPageReqVO reqVO);

// {biz}/service/tag/MemberTagService.java（Task 7）
void validateTagList(Collection<Long> ids);                  // 任一不存在抛 TAG_NOT_EXISTS

// {biz}/service/group/MemberGroupService.java（Task 8）
void validateGroupEnabled(Long id);                          // 不存在 GROUP_NOT_EXISTS，停用 GROUP_DISABLED
```

**Web 集成测试基类**（Task 1 抽取，之后每个带 Controller 的任务都继承它）：`{test}/test/BaseMemberWebIntegrationTest.java`。它提供：
- `asMember(builder, userId)`：以会员身份签名请求；
- `asAdmin(builder, userId)`：以管理员身份签名请求；
- 常量 `ADMIN_ID = 1L`（拥有全部权限）、`NO_PERM_ADMIN_ID = 2L`（没有任何权限）；
- 已有的 `TenantApi`、`OAuth2TokenApi`、`SmsCodeApi`、`LoginLogApi` 桩，以及新增的 `PermissionApi` 桩。

同一基类保证所有 Web 测试共用一个 Spring 上下文。

---

### Task 1: 错误码、演示数据与 Web 测试基类

**Files:**
- Modify: `{api}/enums/ErrorCodeConstants.java`
- Modify: `sql/mysql/dml/06-i18n.sql`（追加 id 267～300，并把文件头注释里的条数改为"member 模块 29 条"）
- Modify: `sql/mysql/dml/01-tenant-and-user.sql:36-37`（演示会员补 `level_id = 1`）
- Regenerate: `sql/mysql/ccloud.sql`
- Modify: `{biz}/dal/mysql/user/MemberUserMapper.java`（新增 `selectByIdForUpdate`）
- Create: `{test}/test/BaseMemberWebIntegrationTest.java`
- Modify: `{test}/MemberSecurityIntegrationTest.java`（改为继承基类，删去重复的容器、桩与签名代码）
- Test: `ccloud-module-member/ccloud-module-member-api/src/test/java/com/ccloud/module/member/enums/ErrorCodeConstantsTest.java`（已有，无需修改）

**Interfaces:**
- Produces: 下表错误码；`MemberUserMapper.selectByIdForUpdate(Long)`；`BaseMemberWebIntegrationTest`。

- [x] **Step 1: 在 `ErrorCodeConstants` 追加错误码**（写在对应段落下）

```java
    // ========== 收件地址 1-004-004-000 ==========
    ErrorCode ADDRESS_COUNT_EXCEEDED = new ErrorCode(1_004_004_001, "收件地址最多 {} 个");

    // ========== 标签 1-004-006-000 ==========
    ErrorCode TAG_NOT_EXISTS = new ErrorCode(1_004_006_000, "用户标签不存在");
    ErrorCode TAG_NAME_EXISTS = new ErrorCode(1_004_006_001, "用户标签已经存在");
    ErrorCode TAG_HAS_USER = new ErrorCode(1_004_006_002, "用户标签下存在用户，无法删除");

    // ========== 积分 1-004-008-000 ==========
    ErrorCode POINT_BIZ_TYPE_INVALID = new ErrorCode(1_004_008_000, "积分业务类型与操作不符");

    // ========== 签到 1-004-009-000 ==========
    ErrorCode SIGN_IN_CONFIG_NOT_EXISTS = new ErrorCode(1_004_009_000, "签到规则不存在");
    ErrorCode SIGN_IN_CONFIG_DAY_EXISTS = new ErrorCode(1_004_009_001, "签到天数规则已存在");
    ErrorCode SIGN_IN_RECORD_TODAY_EXISTS = new ErrorCode(1_004_010_000, "今日已签到，请勿重复签到");

    // ========== 等级 1-004-011-000 ==========
    ErrorCode LEVEL_VALUE_EXISTS = new ErrorCode(1_004_011_002, "用户等级值已被使用");
    ErrorCode LEVEL_EXPERIENCE_ORDER_INVALID = new ErrorCode(1_004_011_003, "等级越高，升级所需经验必须越多");
    ErrorCode LEVEL_HAS_USER = new ErrorCode(1_004_011_004, "用户等级下存在用户，无法删除");
    ErrorCode LEVEL_DISABLED = new ErrorCode(1_004_011_005, "用户等级已停用");

    // ========== 分组 1-004-012-000 ==========
    ErrorCode GROUP_NOT_EXISTS = new ErrorCode(1_004_012_000, "用户分组不存在");
    ErrorCode GROUP_NAME_EXISTS = new ErrorCode(1_004_012_001, "用户分组已经存在");
    ErrorCode GROUP_HAS_USER = new ErrorCode(1_004_012_002, "用户分组下存在用户，无法删除");
    ErrorCode GROUP_DISABLED = new ErrorCode(1_004_012_003, "用户分组已停用");

    // ========== 经验 1-004-013-000 ==========
    ErrorCode EXPERIENCE_BIZ_TYPE_INVALID = new ErrorCode(1_004_013_000, "经验业务类型与操作不符");
```

- [x] **Step 2: 运行确认失败**

Run: `mvn -o -q -pl ccloud-module-member/ccloud-module-member-api -am test -Dtest=ErrorCodeConstantsTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL，`everyCodeShouldHaveBothTranslations` 报 17 个错误码缺少中文与英文文案。

- [x] **Step 3: 在 `06-i18n.sql` 末尾追加文案**。把原最后一行的 `;` 改为 `,`，接着写：

```sql
  (267, 'zh_CN', 'error.code.1004004001', '收件地址最多 {} 个', 1, '1'),
  (268, 'en_US', 'error.code.1004004001', 'At most {} shipping addresses are allowed', 1, '1'),
  (269, 'zh_CN', 'error.code.1004006000', '用户标签不存在', 1, '1'),
  (270, 'en_US', 'error.code.1004006000', 'The member tag does not exist', 1, '1'),
  (271, 'zh_CN', 'error.code.1004006001', '用户标签已经存在', 1, '1'),
  (272, 'en_US', 'error.code.1004006001', 'The member tag already exists', 1, '1'),
  (273, 'zh_CN', 'error.code.1004006002', '用户标签下存在用户，无法删除', 1, '1'),
  (274, 'en_US', 'error.code.1004006002', 'The member tag is in use and cannot be deleted', 1, '1'),
  (275, 'zh_CN', 'error.code.1004008000', '积分业务类型与操作不符', 1, '1'),
  (276, 'en_US', 'error.code.1004008000', 'The point business type does not match the operation', 1, '1'),
  (277, 'zh_CN', 'error.code.1004009000', '签到规则不存在', 1, '1'),
  (278, 'en_US', 'error.code.1004009000', 'The sign-in rule does not exist', 1, '1'),
  (279, 'zh_CN', 'error.code.1004009001', '签到天数规则已存在', 1, '1'),
  (280, 'en_US', 'error.code.1004009001', 'A sign-in rule for this day already exists', 1, '1'),
  (281, 'zh_CN', 'error.code.1004010000', '今日已签到，请勿重复签到', 1, '1'),
  (282, 'en_US', 'error.code.1004010000', 'Already signed in today', 1, '1'),
  (283, 'zh_CN', 'error.code.1004011002', '用户等级值已被使用', 1, '1'),
  (284, 'en_US', 'error.code.1004011002', 'The level value is already in use', 1, '1'),
  (285, 'zh_CN', 'error.code.1004011003', '等级越高，升级所需经验必须越多', 1, '1'),
  (286, 'en_US', 'error.code.1004011003', 'A higher level must require more experience', 1, '1'),
  (287, 'zh_CN', 'error.code.1004011004', '用户等级下存在用户，无法删除', 1, '1'),
  (288, 'en_US', 'error.code.1004011004', 'The member level is in use and cannot be deleted', 1, '1'),
  (289, 'zh_CN', 'error.code.1004011005', '用户等级已停用', 1, '1'),
  (290, 'en_US', 'error.code.1004011005', 'The member level is disabled', 1, '1'),
  (291, 'zh_CN', 'error.code.1004012000', '用户分组不存在', 1, '1'),
  (292, 'en_US', 'error.code.1004012000', 'The member group does not exist', 1, '1'),
  (293, 'zh_CN', 'error.code.1004012001', '用户分组已经存在', 1, '1'),
  (294, 'en_US', 'error.code.1004012001', 'The member group already exists', 1, '1'),
  (295, 'zh_CN', 'error.code.1004012002', '用户分组下存在用户，无法删除', 1, '1'),
  (296, 'en_US', 'error.code.1004012002', 'The member group is in use and cannot be deleted', 1, '1'),
  (297, 'zh_CN', 'error.code.1004012003', '用户分组已停用', 1, '1'),
  (298, 'en_US', 'error.code.1004012003', 'The member group is disabled', 1, '1'),
  (299, 'zh_CN', 'error.code.1004013000', '经验业务类型与操作不符', 1, '1'),
  (300, 'en_US', 'error.code.1004013000', 'The experience business type does not match the operation', 1, '1');
```

- [x] **Step 4: 演示会员补等级**。`01-tenant-and-user.sql` 的 `member_user` 插入语句列表在 `experience` 后加 `level_id`，值加 `1`（青铜会员，门槛 0）：

```sql
INSERT INTO `member_user` (`id`, `mobile`, `password`, `status`, `register_ip`, `register_terminal`, `nickname`, `avatar`, `sex`, `point`, `experience`, `level_id`, `tenant_id`, `creator`) VALUES
  (1, '13800138000', '$2a$04$M0ie1r3Ux77ugXGaO9gfXe4SSHx6RARZ.QhcWXDUb9ErN8o5.sefi', 0, '127.0.0.1', 31, '演示会员', '', 0, 100, 0, 1, 1, '1');
```

然后执行 `scripts/build-sql.sh`。

- [x] **Step 5: 运行确认通过**

Run: 同 Step 2。Expected: PASS。
Run: `scripts/verify-sql.sh`。Expected: 以"校验通过"结束，"错误码中英文文案条数一致"为 OK。

- [x] **Step 6: 新增 `selectByIdForUpdate`**（代码见"共享约定"），并在 `MemberUserMapper` 加以下查询，供 Task 2、7、8 判断"是否仍有会员在用"：

```java
default Long selectCountByLevelId(Long levelId) {
    return selectCount(MemberUserDO::getLevelId, levelId);
}

default Long selectCountByGroupId(Long groupId) {
    return selectCount(MemberUserDO::getGroupId, groupId);
}

default Long selectCountByTagId(Long tagId) {
    return selectCount(new LambdaQueryWrapperX<MemberUserDO>().apply("FIND_IN_SET({0}, tag_ids)", tagId));
}
```

- [x] **Step 7: 抽取 Web 测试基类**。把 `MemberSecurityIntegrationTest` 中的以下内容原样移入 `BaseMemberWebIntegrationTest`（抽象类，保留全部注解）：
  - `@SpringBootTest`、`@AutoConfigureMockMvc`、`@ActiveProfiles`；
  - Redis 容器与 `@DynamicPropertySource`；
  - `MockMvc`、`JdbcTemplate`；
  - 4 个 `@MockitoBean`；
  - 签名逻辑。

  然后新增 `PermissionApi` 桩和两个身份方法：

```java
    protected static final String SECRET = "unit-test-secret";
    protected static final long TENANT = 1L;
    protected static final long ADMIN_ID = 1L;
    protected static final long NO_PERM_ADMIN_ID = 2L;

    @MockitoBean
    protected PermissionApi permissionApi;

    @BeforeEach
    void stubCommonApis() {
        when(tenantApi.validTenant(anyLong())).thenReturn(CommonResult.success(true));
        when(permissionApi.hasAnyPermissions(eq(ADMIN_ID), any())).thenReturn(CommonResult.success(true));
        when(permissionApi.hasAnyPermissions(eq(NO_PERM_ADMIN_ID), any())).thenReturn(CommonResult.success(false));
        when(oauth2TokenApi.removeAccessTokensByUser(anyLong(), anyInt())).thenReturn(CommonResult.success(true));
    }

    protected MockHttpServletRequestBuilder asMember(MockHttpServletRequestBuilder builder, long userId) {
        return asUser(builder, userId, UserTypeEnum.MEMBER);
    }

    protected MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder builder, long userId) {
        return asUser(builder, userId, UserTypeEnum.ADMIN);
    }
```

  `asUser` 改为 `protected`，内容不变。`MemberSecurityIntegrationTest` 改为 `extends BaseMemberWebIntegrationTest`，只保留它自己的 `setUp`、`cleanUp` 与用例；其中对 `tenantApi.validTenant` 的重复打桩删掉。

- [x] **Step 8: 运行会员模块全部测试**

Run: `mvn -o -q -pl ccloud-module-member/ccloud-module-member-biz -am test`
Expected: PASS，测试数与改动前相同（63 个）。

- [x] **Step 9: 提交**

```bash
git add -A ccloud-module-member sql
git commit -m "feat(member): 会员运营错误码与文案、演示会员等级、Web 集成测试基类"
```

---

### Task 2: 等级配置

**Files:**
- Create: `{biz}/dal/dataobject/level/MemberLevelDO.java`、`{biz}/dal/mysql/level/MemberLevelMapper.java`
- Create: `{biz}/service/level/MemberLevelService.java`、`MemberLevelServiceImpl.java`（本任务只含配置部分）
- Create: `{biz}/controller/admin/level/MemberLevelController.java`，`vo/{MemberLevelSaveReqVO, MemberLevelPageReqVO, MemberLevelRespVO, MemberLevelSimpleRespVO}.java`
- Test: `{test}/service/level/MemberLevelServiceImplTest.java`、`{test}/controller/admin/MemberLevelControllerTest.java`

**Interfaces:**
- Consumes: `MemberUserMapper.selectCountByLevelId`（Task 1）。
- Produces: `MemberLevelDO`（字段见下）；"共享约定"中 `MemberLevelService` 的配置部分与 `calculateLevel`；管理端接口：

| 方法 | 路径 | 权限 |
|---|---|---|
| POST | `/admin-api/member/level/create` | `member:level:create` |
| PUT | `/admin-api/member/level/update` | `member:level:update` |
| DELETE | `/admin-api/member/level/delete?id=` | `member:level:delete` |
| GET | `/admin-api/member/level/get?id=` | `member:level:query` |
| GET | `/admin-api/member/level/page` | `member:level:query` |
| GET | `/admin-api/member/level/list-all-simple` | 登录即可，只返回启用的，按 `level` 升序 |

`MemberLevelDO`（`@TableName("member_level")`）：`id, name, level, experience, discountPercent, icon, backgroundUrl, status`。

`MemberLevelSaveReqVO` 的字段与校验：
- `id`：修改时必填；
- `name`：`@NotBlank`，最长 30；
- `level`：`@NotNull @Min(1)`；
- `experience`：`@NotNull @Min(0)`；
- `discountPercent`：`@NotNull @Min(0) @Max(100)`；
- `icon`、`backgroundUrl`：最长 255；
- `status`：`@NotNull @InEnum(CommonStatusEnum.class)`。

`MemberLevelPageReqVO`：`name`（模糊）、`status`。

- [x] **Step 1: 写失败测试** `MemberLevelServiceImplTest extends BaseMemberDbUnitTest`，`@Import(MemberLevelServiceImpl.class)`。本任务先写配置与 `calculateLevel` 的用例：

```java
    @Test
    void createLevel_成功() {
        Long id = levelService.createLevel(level("青铜", 1, 0));
        assertThat(levelMapper.selectById(id).getName()).isEqualTo("青铜");
    }

    @Test
    void createLevel_等级序号重复() {
        levelService.createLevel(level("青铜", 1, 0));
        assertServiceException(() -> levelService.createLevel(level("白银", 1, 100)), LEVEL_VALUE_EXISTS);
    }

    @Test
    void createLevel_序号更高但门槛不高于低等级() {
        levelService.createLevel(level("白银", 2, 1000));
        assertServiceException(() -> levelService.createLevel(level("黄金", 3, 1000)), LEVEL_EXPERIENCE_ORDER_INVALID);
        assertServiceException(() -> levelService.createLevel(level("青铜", 1, 2000)), LEVEL_EXPERIENCE_ORDER_INVALID);
    }

    @Test
    void updateLevel_与自身比较不算冲突() {
        Long id = levelService.createLevel(level("青铜", 1, 0));
        MemberLevelSaveReqVO reqVO = level("青铜会员", 1, 0);
        reqVO.setId(id);
        levelService.updateLevel(reqVO);
        assertThat(levelMapper.selectById(id).getName()).isEqualTo("青铜会员");
    }

    @Test
    void deleteLevel_有会员在用时拒绝() {
        Long id = levelService.createLevel(level("青铜", 1, 0));
        insertUser(id);
        assertServiceException(() -> levelService.deleteLevel(id), LEVEL_HAS_USER);
    }

    @Test
    void calculateLevel_取门槛不超过经验的最高启用等级_停用等级跳过() {
        Long bronze = levelService.createLevel(level("青铜", 1, 0));
        Long silver = levelService.createLevel(level("白银", 2, 1000));
        MemberLevelSaveReqVO gold = level("黄金", 3, 5000);
        gold.setStatus(CommonStatusEnum.DISABLE.getStatus());
        levelService.createLevel(gold);

        assertThat(levelService.calculateLevel(0).getId()).isEqualTo(bronze);
        assertThat(levelService.calculateLevel(999).getId()).isEqualTo(bronze);
        assertThat(levelService.calculateLevel(1000).getId()).isEqualTo(silver);
        assertThat(levelService.calculateLevel(9999).getId()).isEqualTo(silver);
    }

    @Test
    void calculateLevel_没有满足的等级返回null() {
        levelService.createLevel(level("白银", 2, 1000));
        assertThat(levelService.calculateLevel(10)).isNull();
    }

    private static MemberLevelSaveReqVO level(String name, int level, int experience) {
        MemberLevelSaveReqVO reqVO = new MemberLevelSaveReqVO();
        reqVO.setName(name);
        reqVO.setLevel(level);
        reqVO.setExperience(experience);
        reqVO.setDiscountPercent(100);
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        return reqVO;
    }

    private void insertUser(Long levelId) {
        userMapper.insert(new MemberUserDO().setMobile("13900000001").setPassword("").setStatus(0)
                .setRegisterIp("127.0.0.1").setNickname("甲").setAvatar("").setPoint(0).setExperience(0)
                .setLevelId(levelId));
    }
```

同时写 `MemberLevelControllerTest extends BaseMemberWebIntegrationTest`：

```java
    @Test
    void 有权限的管理员可创建并分页查询() throws Exception {
        mockMvc.perform(asAdmin(post("/admin-api/member/level/create"), ADMIN_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"测试等级\",\"level\":91,\"experience\":910000,\"discountPercent\":90,\"status\":0}"))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(asAdmin(get("/admin-api/member/level/page").param("name", "测试等级"), ADMIN_ID))
                .andExpect(jsonPath("$.data.list[0].level").value(91));
    }

    @Test
    void 无权限的管理员返回403() throws Exception {
        mockMvc.perform(asAdmin(delete("/admin-api/member/level/delete").param("id", "1"), NO_PERM_ADMIN_ID))
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void 会员令牌访问管理端返回401() throws Exception {
        mockMvc.perform(asMember(get("/admin-api/member/level/page"), 90101L))
                .andExpect(jsonPath("$.code").value(401));
    }

    @AfterEach
    void cleanLevels() {
        jdbcTemplate.update("DELETE FROM member_level WHERE level >= 90");
    }
```

- [x] **Step 2: 运行确认失败**

Run: `mvn -o -q -pl ccloud-module-member/ccloud-module-member-biz -am test -Dtest='MemberLevelServiceImplTest,MemberLevelControllerTest' -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 编译失败（类不存在）。

- [x] **Step 3: 实现**。Mapper：

```java
@Mapper
public interface MemberLevelMapper extends BaseMapperX<MemberLevelDO> {

    default PageResult<MemberLevelDO> selectPage(MemberLevelPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MemberLevelDO>()
                .likeIfPresent(MemberLevelDO::getName, reqVO.getName())
                .eqIfPresent(MemberLevelDO::getStatus, reqVO.getStatus())
                .orderByAsc(MemberLevelDO::getLevel));
    }

    default List<MemberLevelDO> selectListByStatus(Integer status) {
        return selectList(new LambdaQueryWrapperX<MemberLevelDO>()
                .eq(MemberLevelDO::getStatus, status).orderByAsc(MemberLevelDO::getLevel));
    }
}
```

Service 配置部分的要点：

```java
    @Override
    public Long createLevel(MemberLevelSaveReqVO reqVO) {
        validateLevelForCreateOrUpdate(null, reqVO.getLevel(), reqVO.getExperience());
        MemberLevelDO level = BeanUtils.toBean(reqVO, MemberLevelDO.class);
        levelMapper.insert(level);
        return level.getId();
    }

    @Override
    public void updateLevel(MemberLevelSaveReqVO reqVO) {
        validateLevelForCreateOrUpdate(reqVO.getId(), reqVO.getLevel(), reqVO.getExperience());
        levelMapper.updateById(BeanUtils.toBean(reqVO, MemberLevelDO.class));
    }

    @Override
    public void deleteLevel(Long id) {
        validateLevelExists(id);
        if (userMapper.selectCountByLevelId(id) > 0) {
            throw exception(LEVEL_HAS_USER);
        }
        levelMapper.deleteById(id);
    }

    /**
     * 与其他全部等级（含停用的）比较：序号不能重复，序号越大门槛经验必须越高
     */
    private void validateLevelForCreateOrUpdate(Long id, Integer level, Integer experience) {
        if (id != null) {
            validateLevelExists(id);
        }
        for (MemberLevelDO other : levelMapper.selectList()) {
            if (other.getId().equals(id)) {
                continue;
            }
            if (other.getLevel().equals(level)) {
                throw exception(LEVEL_VALUE_EXISTS);
            }
            boolean higher = level > other.getLevel();
            if (higher ? experience <= other.getExperience() : experience >= other.getExperience()) {
                throw exception(LEVEL_EXPERIENCE_ORDER_INVALID);
            }
        }
    }

    @Override
    public MemberLevelDO calculateLevel(int experience) {
        MemberLevelDO matched = null;
        for (MemberLevelDO level : getEnableLevelList()) {
            if (level.getExperience() <= experience) {
                matched = level;
            }
        }
        return matched;
    }
```

`validateLevelExists` 不存在时抛 `LEVEL_NOT_EXISTS`。Controller 按上表实现，写法与 system 的 `PostController` 相同：`BeanUtils.toBean` 转 VO，`success(...)` 包装。

- [x] **Step 4: 运行确认通过**。Run: 同 Step 2。Expected: PASS。

- [x] **Step 5: 变异检查**，逐一改坏后跑 Step 2 的命令，确认有用例失败，然后还原并 `touch`：
  - 把 `experience <= other.getExperience()` 改为 `<`；
  - 删掉 `deleteLevel` 的在用检查；
  - 在 `calculateLevel` 中把 `<=` 改为 `<`。

- [x] **Step 6: 提交**

```bash
git add -A ccloud-module-member
git commit -m "feat(member): 会员等级配置管理"
```

---

### Task 3: 经验与等级变动、内部接口 MemberLevelApi / MemberExperienceApi、用户端等级

**Files:**
- Create: `{biz}/dal/dataobject/level/{MemberLevelRecordDO, MemberExperienceRecordDO}.java`，`{biz}/dal/mysql/level/{MemberLevelRecordMapper, MemberExperienceRecordMapper}.java`
- Modify: `{biz}/service/level/MemberLevelService.java`、`MemberLevelServiceImpl.java`（补经验与等级部分）
- Create: `{biz}/controller/admin/level/{MemberLevelRecordController, MemberExperienceRecordController}.java`，`vo/{MemberLevelRecordPageReqVO, MemberLevelRecordRespVO, MemberExperienceRecordPageReqVO, MemberExperienceRecordRespVO}.java`
- Create: `{biz}/controller/app/level/{AppMemberLevelController, AppMemberExperienceRecordController}.java`，`vo/{AppMemberLevelRespVO, AppMemberMyLevelRespVO, AppMemberExperienceRecordRespVO}.java`
- Create: `{biz}/api/level/{MemberLevelApiImpl, MemberExperienceApiImpl}.java`
- Modify: `{biz}/service/user/MemberUserServiceImpl.java`（注册时定初始等级）
- Modify: `{test}/api/RpcApiCoverageTest.java`（从 `PENDING_APIS` 移除 `MemberLevelApi`、`MemberExperienceApi`）
- Test: `{test}/service/level/MemberLevelServiceImplTest.java`（追加）、`{test}/service/level/MemberExperienceConcurrencyTest.java`、`{test}/controller/app/AppMemberLevelControllerTest.java`、`{test}/service/user/MemberUserServiceImplTest.java`（追加）

**Interfaces:**
- Consumes: `MemberUserMapper.selectByIdForUpdate`（Task 1）；Task 2 的配置部分。
- Produces: "共享约定"中 `MemberLevelService` 的经验与等级部分；以下接口：

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/admin-api/member/level-record/page` | `member:level-record:query`；筛选 `userId`、`levelId`、`createTime[]` |
| GET | `/admin-api/member/experience-record/page` | `member:experience-record:query`；筛选 `userId`、`bizType`、`title`（模糊）、`createTime[]` |
| GET | `/app-api/member/level/list` | 启用的等级，按 `level` 升序 |
| GET | `/app-api/member/level/get-my` | `{ level: AppMemberLevelRespVO 或 null, experience }` |
| GET | `/app-api/member/experience-record/page` | 本人经验记录，按编号倒序 |
| GET | `/rpc-api/member/level/get?id=` | `MemberLevelApi.getMemberLevel` |
| PUT | `/rpc-api/member/experience/add`、`/reduce` | `MemberExperienceApi` |

记录 DO：
- `MemberLevelRecordDO`（`member_level_record`）：`id, userId, levelId, level, discountPercent, experience, userExperience, remark, description`
- `MemberExperienceRecordDO`（`member_experience_record`）：`id, userId, bizType, bizId, title, description, experience, totalExperience`

`MemberExperienceRecordMapper` 需要：

```java
default MemberExperienceRecordDO selectByBiz(Long userId, Integer bizType, String bizId) {
    return selectOne(new LambdaQueryWrapperX<MemberExperienceRecordDO>()
            .eq(MemberExperienceRecordDO::getUserId, userId)
            .eq(MemberExperienceRecordDO::getBizType, bizType)
            .eq(MemberExperienceRecordDO::getBizId, bizId));
}
```

- [x] **Step 1: 写失败测试**，在 `MemberLevelServiceImplTest` 追加。测试前置用 Task 2 的 `level(...)` 建青铜（1, 0）、白银（2, 1000）、黄金（3, 5000）三级，插入一个经验 0、等级青铜的会员 `userId`。

```java
    @Test
    void addExperience_记录变动并升级_写等级变更记录() {
        levelService.addExperience(userId, 1200, ORDER_GIVE.getType(), "order-1");

        MemberUserDO user = userMapper.selectById(userId);
        assertThat(user.getExperience()).isEqualTo(1200);
        assertThat(user.getLevelId()).isEqualTo(silverId);
        MemberExperienceRecordDO record = experienceRecordMapper.selectByBiz(userId, ORDER_GIVE.getType(), "order-1");
        assertThat(record.getExperience()).isEqualTo(1200);
        assertThat(record.getTotalExperience()).isEqualTo(1200);
        assertThat(record.getTitle()).isEqualTo("下单奖励");
        assertThat(record.getDescription()).isEqualTo("下单获得 1200 经验");
        MemberLevelRecordDO levelRecord = levelRecordMapper.selectList().getFirst();
        assertThat(levelRecord.getLevelId()).isEqualTo(silverId);
        assertThat(levelRecord.getUserExperience()).isEqualTo(1200);
        assertThat(levelRecord.getDescription()).isEqualTo("升级为白银");
    }

    @Test
    void addExperience_同一业务编号只生效一次() {
        levelService.addExperience(userId, 100, ORDER_GIVE.getType(), "order-1");
        levelService.addExperience(userId, 100, ORDER_GIVE.getType(), "order-1");
        assertThat(userMapper.selectById(userId).getExperience()).isEqualTo(100);
        assertThat(experienceRecordMapper.selectCount()).isEqualTo(1);
    }

    @Test
    void addExperience_业务类型方向不符或数量不合法() {
        assertServiceException(() -> levelService.addExperience(userId, 10, ORDER_GIVE_CANCEL.getType(), "x"),
                EXPERIENCE_BIZ_TYPE_INVALID);
        assertServiceException(() -> levelService.reduceExperience(userId, 10, ORDER_GIVE.getType(), "x"),
                EXPERIENCE_BIZ_TYPE_INVALID);
        assertServiceException(() -> levelService.addExperience(userId, 10, 999, "x"), EXPERIENCE_BIZ_TYPE_INVALID);
        assertServiceException(() -> levelService.addExperience(userId, 0, ORDER_GIVE.getType(), "x"), BAD_REQUEST);
    }

    @Test
    void reduceExperience_不足时扣到0并降级_记录实际扣减量() {
        levelService.addExperience(userId, 1200, ORDER_GIVE.getType(), "order-1");
        levelService.reduceExperience(userId, 5000, ORDER_GIVE_CANCEL.getType(), "order-1");

        MemberUserDO user = userMapper.selectById(userId);
        assertThat(user.getExperience()).isZero();
        assertThat(user.getLevelId()).isEqualTo(bronzeId);
        MemberExperienceRecordDO record = experienceRecordMapper.selectByBiz(userId, ORDER_GIVE_CANCEL.getType(), "order-1");
        assertThat(record.getExperience()).isEqualTo(-1200);
        assertThat(record.getDescription()).isEqualTo("退单扣除 1200 经验");
        assertThat(levelRecordMapper.selectList()).extracting(MemberLevelRecordDO::getDescription)
                .containsExactly("升级为白银", "降级为青铜");
    }

    @Test
    void addExperience_等级未变时不写等级记录() {
        levelService.addExperience(userId, 10, ORDER_GIVE.getType(), "order-1");
        assertThat(levelRecordMapper.selectCount()).isZero();
    }

    @Test
    void addExperience_会员不存在() {
        assertServiceException(() -> levelService.addExperience(-1L, 10, ORDER_GIVE.getType(), "x"), USER_NOT_EXISTS);
    }

    @Test
    void updateUserLevel_经验调到门槛_写经验与等级记录() {
        levelService.updateUserLevel(userId, goldId, "活动赠送");

        MemberUserDO user = userMapper.selectById(userId);
        assertThat(user.getLevelId()).isEqualTo(goldId);
        assertThat(user.getExperience()).isEqualTo(5000);
        MemberExperienceRecordDO exp = experienceRecordMapper.selectList().getFirst();
        assertThat(exp.getBizType()).isEqualTo(MemberExperienceBizTypeEnum.ADMIN.getType());
        assertThat(exp.getExperience()).isEqualTo(5000);
        MemberLevelRecordDO record = levelRecordMapper.selectList().getFirst();
        assertThat(record.getRemark()).isEqualTo("活动赠送");
        assertThat(record.getDescription()).isEqualTo("升级为黄金");
    }

    @Test
    void updateUserLevel_选择当前等级时不做变更() {
        levelService.addExperience(userId, 300, ORDER_GIVE.getType(), "order-1");
        levelService.updateUserLevel(userId, bronzeId, "无变化");
        assertThat(userMapper.selectById(userId).getExperience()).isEqualTo(300);
        assertThat(levelRecordMapper.selectCount()).isZero();
    }

    @Test
    void updateUserLevel_等级停用或不存在() {
        levelMapper.updateById(new MemberLevelDO().setId(goldId).setStatus(CommonStatusEnum.DISABLE.getStatus()));
        assertServiceException(() -> levelService.updateUserLevel(userId, goldId, "x"), LEVEL_DISABLED);
        assertServiceException(() -> levelService.updateUserLevel(userId, -1L, "x"), LEVEL_NOT_EXISTS);
    }
```

`MemberExperienceConcurrencyTest extends BaseMemberDbUnitTest`，`@Import(MemberLevelServiceImpl.class)`。测试方法标注 `@Transactional(propagation = Propagation.NOT_SUPPORTED)`，结束时删除自己插入的数据：

```java
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void 并发扣经验与重复业务编号_结果与记录一致() throws Exception {
        // 前置：插入经验 100 的会员 userId，无等级配置
        ExecutorService pool = Executors.newFixedThreadPool(8);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            String bizId = "cancel-" + (i % 10);   // 10 个不同编号，每个提交两次
            futures.add(pool.submit(() -> TenantUtils.execute(TENANT_ID,
                    () -> levelService.reduceExperience(userId, 7, ORDER_GIVE_CANCEL.getType(), bizId))));
        }
        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertThat(userMapper.selectById(userId).getExperience()).isEqualTo(30);
        assertThat(experienceRecordMapper.selectList(MemberExperienceRecordDO::getUserId, userId)).hasSize(10)
                .extracting(MemberExperienceRecordDO::getTotalExperience).contains(30);
    }
```

`AppMemberLevelControllerTest extends BaseMemberWebIntegrationTest`：
- `get-my` 返回本人的等级与经验；
- 未登录返回 401；
- `/rpc-api/member/experience/add` 两次同一业务编号，经验只加一次；
- `/rpc-api/member/level/get?id=` 返回等级名称。

`MemberUserServiceImplTest` 追加：`createUserIfAbsent` 新会员的 `levelId` 等于 `MemberLevelService.calculateLevel(0)` 的结果。在该测试中用 `@MockitoBean MemberLevelService` 打桩，返回编号为 7 的等级，断言新会员 `levelId == 7`；打桩返回 null 时 `levelId` 为 null。

- [x] **Step 2: 运行确认失败**

Run: `mvn -o -q -pl ccloud-module-member/ccloud-module-member-biz -am test -Dtest='MemberLevelServiceImplTest,MemberExperienceConcurrencyTest,AppMemberLevelControllerTest,MemberUserServiceImplTest,RpcApiCoverageTest' -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 编译失败。

- [x] **Step 3: 实现经验与等级**（`MemberLevelServiceImpl`）

```java
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addExperience(Long userId, Integer experience, Integer bizType, String bizId) {
        MemberExperienceBizTypeEnum type = validateBizType(experience, bizType, true);
        changeExperience(userId, experience, type, bizId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reduceExperience(Long userId, Integer experience, Integer bizType, String bizId) {
        MemberExperienceBizTypeEnum type = validateBizType(experience, bizType, false);
        changeExperience(userId, -experience, type, bizId);
    }

    private static MemberExperienceBizTypeEnum validateBizType(Integer experience, Integer bizType, boolean add) {
        if (experience == null || experience <= 0) {
            throw invalidParamException("经验必须大于 0");
        }
        MemberExperienceBizTypeEnum type = MemberExperienceBizTypeEnum.valueOf(bizType);
        if (type == null || type.isAdd() != add) {
            throw exception(EXPERIENCE_BIZ_TYPE_INVALID);
        }
        return type;
    }

    /**
     * 锁定会员行后查重、改经验、写记录、重算等级。经验不足时扣到 0，记录实际变动量
     */
    private void changeExperience(Long userId, int delta, MemberExperienceBizTypeEnum type, String bizId) {
        MemberUserDO user = lockUser(userId);
        if (experienceRecordMapper.selectByBiz(userId, type.getType(), bizId) != null) {
            return;
        }
        int before = ObjectUtil.defaultIfNull(user.getExperience(), 0);
        int total = Math.max(0, before + delta);
        insertExperienceRecord(userId, type, bizId, total - before, total);
        MemberLevelDO level = calculateLevel(total);
        updateUserExperienceAndLevel(user, total, level, "");
    }

    private void insertExperienceRecord(Long userId, MemberExperienceBizTypeEnum type, String bizId,
                                        int experience, int total) {
        experienceRecordMapper.insert(new MemberExperienceRecordDO().setUserId(userId)
                .setBizType(type.getType()).setBizId(bizId).setTitle(type.getName())
                .setDescription(StrUtil.format(type.getDescription(), Math.abs(experience)))
                .setExperience(experience).setTotalExperience(total));
    }

    /**
     * 写入经验与等级；等级变化时写等级变更记录。等级为空时（没有满足的启用等级）清空会员等级
     */
    private void updateUserExperienceAndLevel(MemberUserDO user, int experience, MemberLevelDO level, String remark) {
        Long levelId = level == null ? null : level.getId();
        userMapper.update(null, new LambdaUpdateWrapper<MemberUserDO>()
                .eq(MemberUserDO::getId, user.getId())
                .set(MemberUserDO::getExperience, experience)
                .set(MemberUserDO::getLevelId, levelId));
        if (Objects.equals(levelId, user.getLevelId())) {
            return;
        }
        MemberLevelDO oldLevel = user.getLevelId() == null ? null : levelMapper.selectById(user.getLevelId());
        boolean upgrade = level != null && (oldLevel == null || level.getLevel() > oldLevel.getLevel());
        String levelName = level == null ? "无等级" : level.getName();
        levelRecordMapper.insert(new MemberLevelRecordDO().setUserId(user.getId())
                .setLevelId(levelId == null ? 0L : levelId)
                .setLevel(level == null ? 0 : level.getLevel())
                .setDiscountPercent(level == null ? 100 : level.getDiscountPercent())
                .setExperience(level == null ? 0 : level.getExperience())
                .setUserExperience(experience).setRemark(remark)
                .setDescription((upgrade ? "升级为" : "降级为") + levelName));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserLevel(Long userId, Long levelId, String reason) {
        MemberUserDO user = lockUser(userId);
        MemberLevelDO level = levelMapper.selectById(levelId);
        if (level == null) {
            throw exception(LEVEL_NOT_EXISTS);
        }
        if (!CommonStatusEnum.isEnable(level.getStatus())) {
            throw exception(LEVEL_DISABLED);
        }
        if (Objects.equals(user.getLevelId(), levelId)) {
            return;
        }
        int before = ObjectUtil.defaultIfNull(user.getExperience(), 0);
        if (level.getExperience() != before) {
            insertExperienceRecord(userId, MemberExperienceBizTypeEnum.ADMIN, IdUtil.fastSimpleUUID(),
                    level.getExperience() - before, level.getExperience());
        }
        updateUserExperienceAndLevel(user, level.getExperience(), level, StrUtil.nullToEmpty(reason));
    }

    private MemberUserDO lockUser(Long userId) {
        MemberUserDO user = userId == null ? null : userMapper.selectByIdForUpdate(userId);
        if (user == null) {
            throw exception(USER_NOT_EXISTS);
        }
        return user;
    }
```

注意：
- `ADMIN` 经验类型的描述模板是"管理员调整获得 {} 经验"，扣减时文案不准确。管理员调整的描述改为按正负号生成：`(experience >= 0 ? "管理员调整增加 " : "管理员调整扣减 ") + Math.abs(experience) + " 经验"`，在 `insertExperienceRecord` 中对 `ADMIN` 类型特判。
- 为此补一个测试：管理员把会员从黄金改到青铜，经验记录描述为"管理员调整扣减 5000 经验"。

- [x] **Step 4: 注册时定初始等级**。`MemberUserServiceImpl` 注入 `MemberLevelService`，`createUserIfAbsent` 在构造新会员时加：

```java
        MemberLevelDO level = levelService.calculateLevel(0);
        user.setPoint(0).setExperience(0).setLevelId(level == null ? null : level.getId());
```

- [x] **Step 5: 实现管理端记录分页、用户端等级接口、内部接口**。内部接口实现：

```java
@RestController
@Validated
public class MemberExperienceApiImpl implements MemberExperienceApi {

    @Resource
    private MemberLevelService levelService;

    @Override
    public CommonResult<Boolean> addExperience(Long userId, Integer experience, Integer bizType, String bizId) {
        levelService.addExperience(userId, experience, bizType, bizId);
        return success(true);
    }

    @Override
    public CommonResult<Boolean> reduceExperience(Long userId, Integer experience, Integer bizType, String bizId) {
        levelService.reduceExperience(userId, experience, bizType, bizId);
        return success(true);
    }
}
```

`MemberLevelApiImpl.getMemberLevel(id)` 返回 `BeanUtils.toBean(levelService.getLevel(id), MemberLevelRespDTO.class)`。最后从 `RpcApiCoverageTest.PENDING_APIS` 删除这两项。

- [x] **Step 6: 运行确认通过**。Run: 同 Step 2。Expected: PASS。然后跑会员模块全部测试：`mvn -o -q -pl ccloud-module-member/ccloud-module-member-biz -am test`，确认全部通过。

- [x] **Step 7: 变异检查**（每处改坏→运行 Step 2 命令→确认失败→还原并 `touch`）：
  - 去掉 `selectByBiz` 查重；
  - `Math.max(0, ...)` 改为不截断；
  - 去掉 `selectByIdForUpdate` 的 `.last("FOR UPDATE")`，并发测试应失败（可多跑两次确认）；
  - `updateUserLevel` 去掉停用检查；
  - `updateUserExperienceAndLevel` 中升降判断取反。

- [x] **Step 8: 提交**

```bash
git add -A ccloud-module-member
git commit -m "feat(member): 经验与等级变动、等级与经验记录、内部接口与用户端等级"
```

> 执行记录：经验与等级的用例单独放在 `MemberLevelExperienceTest`（需要三级等级的固定前置，与 Task 2 的配置用例冲突）；`MemberAuthServiceImplTest` 的 `@Import` 加入真实的 `MemberLevelServiceImpl`。

---

### Task 4: 积分

**Files:**
- Create: `{biz}/dal/dataobject/point/MemberPointRecordDO.java`、`{biz}/dal/mysql/point/MemberPointRecordMapper.java`
- Create: `{biz}/service/point/{MemberPointService, MemberPointServiceImpl}.java`
- Create: `{biz}/controller/admin/point/MemberPointRecordController.java`，`vo/{MemberPointRecordPageReqVO, MemberPointRecordRespVO}.java`
- Create: `{biz}/controller/app/point/AppMemberPointRecordController.java`，`vo/{AppMemberPointRecordPageReqVO, AppMemberPointRecordRespVO}.java`
- Create: `{biz}/api/point/MemberPointApiImpl.java`
- Modify: `{test}/api/RpcApiCoverageTest.java`（移除 `MemberPointApi`）
- Test: `{test}/service/point/MemberPointServiceImplTest.java`、`MemberPointConcurrencyTest.java`、`{test}/controller/app/AppMemberPointControllerTest.java`

**Interfaces:**
- Consumes: `MemberUserMapper.selectByIdForUpdate`。
- Produces: "共享约定"中 `MemberPointService`；以下接口：

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/admin-api/member/point/record/page` | `member:point-record:query`；筛选 `userId`、`bizType`、`title`（模糊）、`createTime[]` |
| GET | `/app-api/member/point/record/page` | 本人记录；`addStatus`：true 只看收入（`point > 0`），false 只看支出（`point < 0`），不传看全部；按编号倒序 |
| PUT | `/rpc-api/member/point/add`、`/reduce` | `MemberPointApi` |

`MemberPointRecordDO`（`member_point_record`）：`id, userId, bizId, bizType, title, description, point, totalPoint`。Mapper 提供与经验记录同形的 `selectByBiz(userId, bizType, bizId)`。

- [x] **Step 1: 写失败测试** `MemberPointServiceImplTest extends BaseMemberDbUnitTest`，`@Import(MemberPointServiceImpl.class)`；前置插入积分 100 的会员 `userId`。

```java
    @Test
    void addPoint_记录变动与余额() {
        pointService.addPoint(userId, 20, SIGN.getType(), "sign-1");
        assertThat(userMapper.selectById(userId).getPoint()).isEqualTo(120);
        MemberPointRecordDO record = pointRecordMapper.selectByBiz(userId, SIGN.getType(), "sign-1");
        assertThat(record.getPoint()).isEqualTo(20);
        assertThat(record.getTotalPoint()).isEqualTo(120);
        assertThat(record.getTitle()).isEqualTo("签到");
        assertThat(record.getDescription()).isEqualTo("签到获得 20 积分");
    }

    @Test
    void reducePoint_余额不足时拒绝且不写记录() {
        assertServiceException(() -> pointService.reducePoint(userId, 101, ORDER_USE.getType(), "o-1"),
                USER_POINT_NOT_ENOUGH);
        assertThat(userMapper.selectById(userId).getPoint()).isEqualTo(100);
        assertThat(pointRecordMapper.selectCount()).isZero();
    }

    @Test
    void reducePoint_恰好扣完() {
        pointService.reducePoint(userId, 100, ORDER_USE.getType(), "o-1");
        assertThat(userMapper.selectById(userId).getPoint()).isZero();
        assertThat(pointRecordMapper.selectByBiz(userId, ORDER_USE.getType(), "o-1").getPoint()).isEqualTo(-100);
    }

    @Test
    void 同一业务编号只生效一次_不同业务类型互不影响() {
        pointService.addPoint(userId, 10, ORDER_GIVE.getType(), "o-1");
        pointService.addPoint(userId, 10, ORDER_GIVE.getType(), "o-1");
        pointService.reducePoint(userId, 5, ORDER_USE.getType(), "o-1");
        assertThat(userMapper.selectById(userId).getPoint()).isEqualTo(105);
    }

    @Test
    void 业务类型方向不符或数量不合法() {
        assertServiceException(() -> pointService.addPoint(userId, 1, ORDER_USE.getType(), "x"), POINT_BIZ_TYPE_INVALID);
        assertServiceException(() -> pointService.reducePoint(userId, 1, SIGN.getType(), "x"), POINT_BIZ_TYPE_INVALID);
        assertServiceException(() -> pointService.addPoint(userId, 1, 999, "x"), POINT_BIZ_TYPE_INVALID);
        assertServiceException(() -> pointService.addPoint(userId, -1, SIGN.getType(), "x"), BAD_REQUEST);
    }

    @Test
    void updateUserPointByAdmin_正加负减_每次都生效_不能减到负数() {
        pointService.updateUserPointByAdmin(userId, 30);
        pointService.updateUserPointByAdmin(userId, 30);
        pointService.updateUserPointByAdmin(userId, -60);
        assertThat(userMapper.selectById(userId).getPoint()).isEqualTo(100);
        assertThat(pointRecordMapper.selectList()).extracting(MemberPointRecordDO::getDescription)
                .containsExactly("管理员增加 30 积分", "管理员增加 30 积分", "管理员扣减 60 积分");
        assertServiceException(() -> pointService.updateUserPointByAdmin(userId, -101), USER_POINT_NOT_ENOUGH);
        assertServiceException(() -> pointService.updateUserPointByAdmin(userId, 0), BAD_REQUEST);
    }

    @Test
    void getPointRecordPage_用户端按收入支出筛选_只看本人() {
        pointService.addPoint(userId, 10, SIGN.getType(), "s-1");
        pointService.reducePoint(userId, 5, ORDER_USE.getType(), "o-1");
        pointService.addPoint(otherUserId, 10, SIGN.getType(), "s-1");

        AppMemberPointRecordPageReqVO reqVO = new AppMemberPointRecordPageReqVO();
        reqVO.setAddStatus(true);
        assertThat(pointService.getPointRecordPage(userId, reqVO).getList()).extracting(MemberPointRecordDO::getPoint)
                .containsExactly(10);
        reqVO.setAddStatus(false);
        assertThat(pointService.getPointRecordPage(userId, reqVO).getList()).extracting(MemberPointRecordDO::getPoint)
                .containsExactly(-5);
        reqVO.setAddStatus(null);
        assertThat(pointService.getPointRecordPage(userId, reqVO).getTotal()).isEqualTo(2);
    }
```

`MemberPointConcurrencyTest`（写法同 Task 3 的并发测试）：积分 100 的会员，16 个线程各扣 10，编号互不相同。断言：
- 成功 10 次、失败 6 次，失败的错误码都是 `USER_POINT_NOT_ENOUGH`；
- 最终余额为 0；
- 记录 10 条，按编号排序后 `totalPoint` 依次为 90、80、…、0。

`AppMemberPointControllerTest`：
- 用户端分页只返回本人记录；
- 管理端分页无权限返回 403；
- `/rpc-api/member/point/reduce` 余额不足时返回 `code = 1004001003`。

- [x] **Step 2: 运行确认失败**

Run: `mvn -o -q -pl ccloud-module-member/ccloud-module-member-biz -am test -Dtest='MemberPointServiceImplTest,MemberPointConcurrencyTest,AppMemberPointControllerTest,RpcApiCoverageTest' -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 编译失败。

- [x] **Step 3: 实现**（`MemberPointServiceImpl`）

```java
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPoint(Long userId, Integer point, Integer bizType, String bizId) {
        changePoint(userId, point, validateBizType(point, bizType, true), bizId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reducePoint(Long userId, Integer point, Integer bizType, String bizId) {
        changePoint(userId, -point, validateBizType(point, bizType, false), bizId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserPointByAdmin(Long userId, Integer point) {
        if (point == null || point == 0) {
            throw invalidParamException("调整的积分不能为 0");
        }
        // 管理员每次调整都应生效，业务编号随机生成
        changePoint(userId, point, MemberPointBizTypeEnum.ADMIN, IdUtil.fastSimpleUUID());
    }

    private static MemberPointBizTypeEnum validateBizType(Integer point, Integer bizType, boolean add) {
        if (point == null || point <= 0) {
            throw invalidParamException("积分必须大于 0");
        }
        MemberPointBizTypeEnum type = MemberPointBizTypeEnum.valueOf(bizType);
        if (type == null || type.isAdd() != add) {
            throw exception(POINT_BIZ_TYPE_INVALID);
        }
        return type;
    }

    private void changePoint(Long userId, int delta, MemberPointBizTypeEnum type, String bizId) {
        MemberUserDO user = userMapper.selectByIdForUpdate(userId);
        if (user == null) {
            throw exception(USER_NOT_EXISTS);
        }
        if (pointRecordMapper.selectByBiz(userId, type.getType(), bizId) != null) {
            return;
        }
        int total = ObjectUtil.defaultIfNull(user.getPoint(), 0) + delta;
        if (total < 0) {
            throw exception(USER_POINT_NOT_ENOUGH);
        }
        userMapper.updateById(new MemberUserDO().setId(userId).setPoint(total));
        pointRecordMapper.insert(new MemberPointRecordDO().setUserId(userId).setBizType(type.getType())
                .setBizId(bizId).setTitle(type.getName()).setDescription(buildDescription(type, delta))
                .setPoint(delta).setTotalPoint(total));
    }

    private static String buildDescription(MemberPointBizTypeEnum type, int delta) {
        if (type == MemberPointBizTypeEnum.ADMIN) {
            return (delta > 0 ? "管理员增加 " : "管理员扣减 ") + Math.abs(delta) + " 积分";
        }
        return StrUtil.format(type.getDescription(), Math.abs(delta));
    }
```

用户端分页的收入 / 支出筛选：

```java
    default PageResult<MemberPointRecordDO> selectPage(Long userId, AppMemberPointRecordPageReqVO reqVO) {
        LambdaQueryWrapperX<MemberPointRecordDO> query = new LambdaQueryWrapperX<MemberPointRecordDO>()
                .eq(MemberPointRecordDO::getUserId, userId);
        if (Boolean.TRUE.equals(reqVO.getAddStatus())) {
            query.gt(MemberPointRecordDO::getPoint, 0);
        } else if (Boolean.FALSE.equals(reqVO.getAddStatus())) {
            query.lt(MemberPointRecordDO::getPoint, 0);
        }
        return selectPage(reqVO, query.orderByDesc(MemberPointRecordDO::getId));
    }
```

`MemberPointApiImpl` 两个方法直接委托给 `addPoint` / `reducePoint`，返回 `success(true)`。从 `PENDING_APIS` 删除 `MemberPointApi`。

- [x] **Step 4: 运行确认通过**。Run: 同 Step 2，再跑会员模块全部测试。Expected: PASS。

- [x] **Step 5: 变异检查**：
  - 去掉查重；
  - `total < 0` 改为 `total < -1`；
  - 去掉 `FOR UPDATE`（并发测试应失败）；
  - `type.isAdd() != add` 改为 `false`；
  - 收入筛选的 `gt` 改为 `ge`。

- [x] **Step 6: 提交**

```bash
git add -A ccloud-module-member
git commit -m "feat(member): 会员积分变动、积分记录与内部接口"
```

---

### Task 5: 签到

**Files:**
- Create: `{biz}/dal/dataobject/signin/{MemberSignInConfigDO, MemberSignInRecordDO}.java`，`{biz}/dal/mysql/signin/{MemberSignInConfigMapper, MemberSignInRecordMapper}.java`
- Create: `{biz}/service/signin/{MemberSignInConfigService, MemberSignInConfigServiceImpl, MemberSignInRecordService, MemberSignInRecordServiceImpl}.java`
- Create: `{biz}/controller/admin/signin/{MemberSignInConfigController, MemberSignInRecordController}.java` + `vo/`
- Create: `{biz}/controller/app/signin/{AppMemberSignInConfigController, AppMemberSignInRecordController}.java` + `vo/`
- Test: `{test}/service/signin/{MemberSignInConfigServiceImplTest, MemberSignInRecordServiceImplTest, MemberSignInConcurrencyTest}.java`、`{test}/controller/app/AppMemberSignInControllerTest.java`

**Interfaces:**
- Consumes: `MemberPointService.addPoint`（Task 4）、`MemberLevelService.addExperience`（Task 3）、`MemberUserMapper.selectByIdForUpdate`。
- Produces：

| 方法 | 路径 | 说明 |
|---|---|---|
| POST/PUT/DELETE/GET | `/admin-api/member/sign-in/config/{create,update,delete,get}` | `member:sign-in-config:*` |
| GET | `/admin-api/member/sign-in/config/list` | `member:sign-in-config:query`，全部规则按天数升序 |
| GET | `/admin-api/member/sign-in/record/page` | `member:sign-in-record:query`；筛选 `userId`、`day`、`createTime[]` |
| GET | `/app-api/member/sign-in/config/list` | 启用的规则，按天数升序 |
| GET | `/app-api/member/sign-in/record/get-summary` | `{ todaySignIn, continuousDay, totalDay }` |
| POST | `/app-api/member/sign-in/record/create` | 签到，返回本次记录（`day`、`point`、`experience`） |
| GET | `/app-api/member/sign-in/record/page` | 本人签到记录，按编号倒序 |

DO：
- `MemberSignInConfigDO`（`member_sign_in_config`）：`id, day, point, experience, status`
- `MemberSignInRecordDO`（`member_sign_in_record`）：`id, userId, day, point, experience`

规则保存校验：`day @NotNull @Min(1)`、`point @NotNull @Min(0)`、`experience @NotNull @Min(0)`、`status @InEnum`；天数重复报 `SIGN_IN_CONFIG_DAY_EXISTS`；修改或删除不存在的规则报 `SIGN_IN_CONFIG_NOT_EXISTS`。

- [x] **Step 1: 写失败测试** `MemberSignInRecordServiceImplTest extends BaseMemberDbUnitTest`：
  - `@Import({MemberSignInRecordServiceImpl.class, MemberSignInConfigServiceImpl.class, MemberPointServiceImpl.class, MemberLevelServiceImpl.class})`；
  - 前置：插入会员 `userId`（积分 0、经验 0）；规则为第 1 天 (2 积分, 1 经验)、第 3 天 (6, 3)、第 7 天 (14, 7)。
  - 辅助方法 `insertRecord(int day, LocalDateTime createTime)` 直接写入签到记录。`createTime` 非空时填充器不会覆盖。

```java
    @Test
    void 首次签到为第1天_发放积分与经验_以记录编号为业务编号() {
        MemberSignInRecordDO record = signInService.createSignRecord(userId);

        assertThat(record.getDay()).isEqualTo(1);
        assertThat(record.getPoint()).isEqualTo(2);
        assertThat(record.getExperience()).isEqualTo(1);
        MemberUserDO user = userMapper.selectById(userId);
        assertThat(user.getPoint()).isEqualTo(2);
        assertThat(user.getExperience()).isEqualTo(1);
        assertThat(pointRecordMapper.selectByBiz(userId, MemberPointBizTypeEnum.SIGN.getType(),
                record.getId().toString())).isNotNull();
        assertThat(experienceRecordMapper.selectByBiz(userId, MemberExperienceBizTypeEnum.SIGN_IN.getType(),
                record.getId().toString())).isNotNull();
    }

    @Test
    void 今天已签到时拒绝() {
        signInService.createSignRecord(userId);
        assertServiceException(() -> signInService.createSignRecord(userId), SIGN_IN_RECORD_TODAY_EXISTS);
    }

    @Test
    void 昨天签过则连续天数加1_规则不连续时取不超过当前天数的最大规则() {
        insertRecord(1, LocalDateTime.now().minusDays(1));
        MemberSignInRecordDO record = signInService.createSignRecord(userId);
        assertThat(record.getDay()).isEqualTo(2);
        assertThat(record.getPoint()).isEqualTo(2);   // 没有第 2 天规则，按第 1 天
    }

    @Test
    void 超过最大配置天数后一直按最大天数发放_天数照常累加() {
        insertRecord(9, LocalDateTime.now().minusDays(1));
        MemberSignInRecordDO record = signInService.createSignRecord(userId);
        assertThat(record.getDay()).isEqualTo(10);
        assertThat(record.getPoint()).isEqualTo(14);
        assertThat(record.getExperience()).isEqualTo(7);
    }

    @Test
    void 断签后从第1天开始() {
        insertRecord(5, LocalDateTime.now().minusDays(2));
        assertThat(signInService.createSignRecord(userId).getDay()).isEqualTo(1);
    }

    @Test
    void 停用的规则不参与计算_没有可用规则时奖励为0且不写积分经验记录() {
        configMapper.selectList().forEach(config -> configMapper.updateById(new MemberSignInConfigDO()
                .setId(config.getId()).setStatus(CommonStatusEnum.DISABLE.getStatus())));
        MemberSignInRecordDO record = signInService.createSignRecord(userId);
        assertThat(record.getPoint()).isZero();
        assertThat(record.getExperience()).isZero();
        assertThat(pointRecordMapper.selectCount()).isZero();
        assertThat(experienceRecordMapper.selectCount()).isZero();
    }

    @Test
    void 签到概况() {
        insertRecord(1, LocalDateTime.now().minusDays(3));
        insertRecord(1, LocalDateTime.now().minusDays(1));
        AppMemberSignInRecordSummaryRespVO before = signInService.getSignInRecordSummary(userId);
        assertThat(before.getTodaySignIn()).isFalse();
        assertThat(before.getContinuousDay()).isEqualTo(1);
        assertThat(before.getTotalDay()).isEqualTo(2);

        signInService.createSignRecord(userId);
        AppMemberSignInRecordSummaryRespVO after = signInService.getSignInRecordSummary(userId);
        assertThat(after.getTodaySignIn()).isTrue();
        assertThat(after.getContinuousDay()).isEqualTo(2);
        assertThat(after.getTotalDay()).isEqualTo(3);
    }

    @Test
    void 签到概况_最后一次在前天之前时连续天数为0() {
        insertRecord(4, LocalDateTime.now().minusDays(2));
        assertThat(signInService.getSignInRecordSummary(userId).getContinuousDay()).isZero();
    }
```

  `MemberSignInConfigServiceImplTest`：
  - 天数重复报错；
  - 修改为已被其他规则占用的天数报错；
  - 删除不存在的规则报错；
  - 用户端列表只含启用的规则，并按天数升序。

  `MemberSignInConcurrencyTest`（写法同前）：8 个线程同时为同一会员签到。断言：
  - 恰好 1 次成功，其余都是 `SIGN_IN_RECORD_TODAY_EXISTS`；
  - 签到记录 1 条；
  - 积分只加一次。

  `AppMemberSignInControllerTest`：
  - 签到后概况 `todaySignIn = true`；
  - 未登录签到返回 401；
  - 管理端签到规则接口无权限返回 403。

- [x] **Step 2: 运行确认失败**

Run: `mvn -o -q -pl ccloud-module-member/ccloud-module-member-biz -am test -Dtest='MemberSignIn*Test,AppMemberSignInControllerTest' -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 编译失败。

- [x] **Step 3: 实现**（`MemberSignInRecordServiceImpl`）

```java
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MemberSignInRecordDO createSignRecord(Long userId) {
        // 锁会员行：同一会员的并发签到排队执行，第二个请求能看到第一个写入的记录
        if (userMapper.selectByIdForUpdate(userId) == null) {
            throw exception(USER_NOT_EXISTS);
        }
        LocalDate today = LocalDate.now();
        MemberSignInRecordDO last = recordMapper.selectLastByUserId(userId);
        LocalDate lastDate = last == null ? null : last.getCreateTime().toLocalDate();
        if (today.equals(lastDate)) {
            throw exception(SIGN_IN_RECORD_TODAY_EXISTS);
        }
        int day = today.minusDays(1).equals(lastDate) ? last.getDay() + 1 : 1;
        MemberSignInConfigDO rule = findRule(day);
        MemberSignInRecordDO record = new MemberSignInRecordDO().setUserId(userId).setDay(day)
                .setPoint(rule == null ? 0 : rule.getPoint())
                .setExperience(rule == null ? 0 : rule.getExperience());
        recordMapper.insert(record);
        String bizId = record.getId().toString();
        if (record.getPoint() > 0) {
            pointService.addPoint(userId, record.getPoint(), MemberPointBizTypeEnum.SIGN.getType(), bizId);
        }
        if (record.getExperience() > 0) {
            levelService.addExperience(userId, record.getExperience(), MemberExperienceBizTypeEnum.SIGN_IN.getType(), bizId);
        }
        return record;
    }

    /**
     * 已启用规则中天数不超过连续天数的最大一条：超过最大配置天数后一直按最大天数发放，规则不连续时取前一条
     */
    private MemberSignInConfigDO findRule(int day) {
        MemberSignInConfigDO matched = null;
        for (MemberSignInConfigDO config : configService.getSignInConfigList(CommonStatusEnum.ENABLE.getStatus())) {
            if (config.getDay() <= day) {
                matched = config;
            }
        }
        return matched;
    }

    @Override
    public AppMemberSignInRecordSummaryRespVO getSignInRecordSummary(Long userId) {
        AppMemberSignInRecordSummaryRespVO summary = new AppMemberSignInRecordSummaryRespVO();
        summary.setTotalDay(recordMapper.selectCount(MemberSignInRecordDO::getUserId, userId).intValue());
        MemberSignInRecordDO last = recordMapper.selectLastByUserId(userId);
        LocalDate lastDate = last == null ? null : last.getCreateTime().toLocalDate();
        LocalDate today = LocalDate.now();
        summary.setTodaySignIn(today.equals(lastDate));
        boolean continuing = today.equals(lastDate) || today.minusDays(1).equals(lastDate);
        summary.setContinuousDay(continuing ? last.getDay() : 0);
        return summary;
    }
```

`selectLastByUserId`：`selectOne(new LambdaQueryWrapperX<MemberSignInRecordDO>().eq(userId).orderByDesc(MemberSignInRecordDO::getCreateTime).orderByDesc(MemberSignInRecordDO::getId).last("LIMIT 1"))`。`getSignInConfigList(Integer status)`：status 为 null 时返回全部，均按 `day` 升序。

- [x] **Step 4: 运行确认通过**。Run: 同 Step 2，再跑会员模块全部测试。Expected: PASS。

- [x] **Step 5: 变异检查**：
  - `config.getDay() <= day` 改为 `==`；
  - `minusDays(1)` 改为 `minusDays(2)`；
  - 去掉会员行锁（并发测试应失败）；
  - 概况中 `continuing` 恒为 true。

- [x] **Step 6: 提交**

```bash
git add -A ccloud-module-member
git commit -m "feat(member): 签到规则与签到，连续签到奖励积分与经验"
```

> 执行记录：签到规则改为物理删除（`MemberSignInConfigMapper.deleteByIdPhysically`）。唯一索引 `uk_day` 含 `deleted` 列，逻辑删除后同一天数的规则第二次删除会撞唯一索引；已补"反复删除后重建"与"物理删除受租户隔离"两个用例。

---

### Task 6: 收件地址与内部接口 MemberAddressApi

**Files:**
- Create: `{biz}/dal/dataobject/address/MemberAddressDO.java`、`{biz}/dal/mysql/address/MemberAddressMapper.java`
- Create: `{biz}/service/address/{MemberAddressService, MemberAddressServiceImpl}.java`
- Create: `{biz}/controller/app/address/AppMemberAddressController.java`，`vo/{AppMemberAddressCreateReqVO, AppMemberAddressUpdateReqVO, AppMemberAddressRespVO}.java`
- Create: `{biz}/api/address/MemberAddressApiImpl.java`
- Modify: `{test}/api/RpcApiCoverageTest.java`（`PENDING_APIS` 清空为 `Map.of()`）
- Test: `{test}/service/address/MemberAddressServiceImplTest.java`、`{test}/controller/app/AppMemberAddressControllerTest.java`

**Interfaces:**
- Produces：

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/app-api/member/address/create` | 返回编号 |
| PUT | `/app-api/member/address/update` | |
| DELETE | `/app-api/member/address/delete?id=` | |
| GET | `/app-api/member/address/get?id=` | 他人地址返回 `ADDRESS_NOT_EXISTS` |
| GET | `/app-api/member/address/list` | 默认地址在前，其余按编号倒序 |
| GET | `/app-api/member/address/get-default` | 无默认地址时 `data` 为 null |
| GET | `/rpc-api/member/address/get`、`/get-default` | `MemberAddressApi` |

`MemberAddressDO`（`member_address`）：`id, userId, name, mobile, areaId, detailAddress, defaultStatus`（`Boolean`）。

创建 VO 的字段与校验：
- `name`：`@NotBlank`，最长 10；
- `mobile`：`@NotBlank @Mobile`；
- `areaId`：`@NotNull`，类型 `Long`；
- `detailAddress`：`@NotBlank`，最长 250；
- `defaultStatus`：`@NotNull`。

修改 VO 在此基础上多一个 `@NotNull id`。

- [x] **Step 1: 写失败测试** `MemberAddressServiceImplTest`（常量 `MemberAddressServiceImpl.MAX_COUNT = 20`）

```java
    @Test
    void 第一个地址自动设为默认_即使请求为非默认() {
        Long id = addressService.createAddress(USER, reqVO(false));
        assertThat(addressMapper.selectById(id).getDefaultStatus()).isTrue();
    }

    @Test
    void 设新的默认地址时取消原默认() {
        Long first = addressService.createAddress(USER, reqVO(true));
        Long second = addressService.createAddress(USER, reqVO(true));
        assertThat(addressMapper.selectById(first).getDefaultStatus()).isFalse();
        assertThat(addressService.getDefaultAddress(USER).getId()).isEqualTo(second);
    }

    @Test
    void 修改为默认时同样取消原默认() {
        Long first = addressService.createAddress(USER, reqVO(true));
        Long second = addressService.createAddress(USER, reqVO(false));
        AppMemberAddressUpdateReqVO update = BeanUtils.toBean(reqVO(true), AppMemberAddressUpdateReqVO.class);
        update.setId(second);
        addressService.updateAddress(USER, update);
        assertThat(addressMapper.selectById(first).getDefaultStatus()).isFalse();
        assertThat(addressMapper.selectById(second).getDefaultStatus()).isTrue();
    }

    @Test
    void 删除默认地址后不自动另选() {
        Long first = addressService.createAddress(USER, reqVO(true));
        addressService.createAddress(USER, reqVO(false));
        addressService.deleteAddress(USER, first);
        assertThat(addressService.getDefaultAddress(USER)).isNull();
    }

    @Test
    void 超过20个时拒绝() {
        for (int i = 0; i < 20; i++) {
            addressService.createAddress(USER, reqVO(false));
        }
        assertServiceException(() -> addressService.createAddress(USER, reqVO(false)), ADDRESS_COUNT_EXCEEDED);
    }

    @Test
    void 不能访问他人地址() {
        Long id = addressService.createAddress(USER, reqVO(true));
        assertThat(addressService.getAddress(OTHER, id)).isNull();
        AppMemberAddressUpdateReqVO update = BeanUtils.toBean(reqVO(true), AppMemberAddressUpdateReqVO.class);
        update.setId(id);
        assertServiceException(() -> addressService.updateAddress(OTHER, update), ADDRESS_NOT_EXISTS);
        assertServiceException(() -> addressService.deleteAddress(OTHER, id), ADDRESS_NOT_EXISTS);
        assertThat(addressMapper.selectById(id)).isNotNull();
    }
```

`AppMemberAddressControllerTest`：
- 创建后列表可见；
- 以另一会员身份 `get` 返回 `code = 1004004000`；
- `/rpc-api/member/address/get` 传错 `userId` 返回 `data = null`；
- 未登录返回 401。

- [x] **Step 2: 运行确认失败**

Run: `mvn -o -q -pl ccloud-module-member/ccloud-module-member-biz -am test -Dtest='MemberAddressServiceImplTest,AppMemberAddressControllerTest,RpcApiCoverageTest' -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 编译失败。

- [x] **Step 3: 实现**

```java
    public static final int MAX_COUNT = 20;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAddress(Long userId, AppMemberAddressCreateReqVO reqVO) {
        long count = addressMapper.selectCount(MemberAddressDO::getUserId, userId);
        if (count >= MAX_COUNT) {
            throw exception(ADDRESS_COUNT_EXCEEDED, MAX_COUNT);
        }
        boolean isDefault = count == 0 || Boolean.TRUE.equals(reqVO.getDefaultStatus());
        if (isDefault) {
            addressMapper.updateDefaultFalseByUserId(userId);
        }
        MemberAddressDO address = BeanUtils.toBean(reqVO, MemberAddressDO.class).setUserId(userId).setDefaultStatus(isDefault);
        addressMapper.insert(address);
        return address.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAddress(Long userId, AppMemberAddressUpdateReqVO reqVO) {
        validateAddressExists(userId, reqVO.getId());
        if (Boolean.TRUE.equals(reqVO.getDefaultStatus())) {
            addressMapper.updateDefaultFalseByUserId(userId);
        }
        addressMapper.updateById(BeanUtils.toBean(reqVO, MemberAddressDO.class).setUserId(userId));
    }

    @Override
    public MemberAddressDO getAddress(Long userId, Long id) {
        return addressMapper.selectOne(MemberAddressDO::getId, id, MemberAddressDO::getUserId, userId);
    }
```

Mapper 新增：

```java
    default void updateDefaultFalseByUserId(Long userId) {
        update(new MemberAddressDO().setDefaultStatus(false), new LambdaQueryWrapperX<MemberAddressDO>()
                .eq(MemberAddressDO::getUserId, userId).eq(MemberAddressDO::getDefaultStatus, true));
    }
```

`validateAddressExists(userId, id)` 通过 `getAddress` 查不到时抛 `ADDRESS_NOT_EXISTS`。`MemberAddressApiImpl.getAddress(id, userId)` 与 `getDefaultAddress(userId)` 转成 `MemberAddressRespDTO` 返回。最后 `PENDING_APIS` 改为 `Map.of()`。

- [x] **Step 4: 运行确认通过**，再跑会员模块全部测试。Expected: PASS。

- [x] **Step 5: 变异检查**：
  - 去掉 `count == 0 ||`；
  - `getAddress` 去掉 `userId` 条件；
  - `>=` 改为 `>`。

- [x] **Step 6: 提交**

```bash
git add -A ccloud-module-member
git commit -m "feat(member): 会员收件地址与内部接口"
```

---

### Task 7: 标签

**Files:**
- Create: `{biz}/dal/dataobject/tag/MemberTagDO.java`、`{biz}/dal/mysql/tag/MemberTagMapper.java`
- Create: `{biz}/service/tag/{MemberTagService, MemberTagServiceImpl}.java`
- Create: `{biz}/controller/admin/tag/MemberTagController.java`，`vo/{MemberTagSaveReqVO, MemberTagPageReqVO, MemberTagRespVO}.java`
- Test: `{test}/service/tag/MemberTagServiceImplTest.java`、`{test}/controller/admin/MemberTagControllerTest.java`

**Interfaces:**
- Consumes: `MemberUserMapper.selectCountByTagId`（Task 1）。
- Produces: `MemberTagService.validateTagList(Collection<Long>)`、`getTagList(Collection<Long> ids)`；以下接口：
  - `/admin-api/member/tag/{create, update, delete, get, page}`，权限 `member:tag:*`；
  - `/admin-api/member/tag/list-all-simple`，登录即可。

`MemberTagDO`（`member_tag`）：`id, name`。保存 VO：`name @NotBlank`，最长 50。分页 VO：`name`（模糊）、`createTime[]`。

- [x] **Step 1: 写失败测试**：

```java
    @Test
    void createTag_名称重复() {
        tagService.createTag(save(null, "高价值"));
        assertServiceException(() -> tagService.createTag(save(null, "高价值")), TAG_NAME_EXISTS);
    }

    @Test
    void updateTag_保持自身名称不算重复_改成他人名称报错() {
        Long a = tagService.createTag(save(null, "A"));
        tagService.createTag(save(null, "B"));
        tagService.updateTag(save(a, "A"));
        assertServiceException(() -> tagService.updateTag(save(a, "B")), TAG_NAME_EXISTS);
    }

    @Test
    void deleteTag_有会员在用时拒绝_逗号列表中的任一位置都能识别() {
        Long a = tagService.createTag(save(null, "A"));
        Long b = tagService.createTag(save(null, "B"));
        insertUserWithTags(a + "," + b);
        assertServiceException(() -> tagService.deleteTag(b), TAG_HAS_USER);
        assertServiceException(() -> tagService.deleteTag(a), TAG_HAS_USER);
    }

    @Test
    void deleteTag_编号是其他编号子串时不误判() {
        Long a = tagService.createTag(save(null, "A"));
        insertUserWithTags(a + "0");       // 如 1 与 10
        tagService.deleteTag(a);
        assertThat(tagMapper.selectById(a)).isNull();
    }

    @Test
    void validateTagList_任一不存在即报错_空集合通过() {
        Long a = tagService.createTag(save(null, "A"));
        tagService.validateTagList(List.of());
        tagService.validateTagList(List.of(a));
        assertServiceException(() -> tagService.validateTagList(List.of(a, -1L)), TAG_NOT_EXISTS);
    }
```

`MemberTagControllerTest`：
- 有权限可以创建；
- 无权限删除返回 403；
- `list-all-simple` 登录即可访问。

- [x] **Step 2: 运行确认失败**。Run: `-Dtest='MemberTagServiceImplTest,MemberTagControllerTest'`。Expected: 编译失败。
- [x] **Step 3: 实现**：按 system `PostServiceImpl` 的写法做名称查重，删除前调用 `userMapper.selectCountByTagId(id) > 0` 报 `TAG_HAS_USER`。`validateTagList` 用 `selectByIds` 比对数量，逐个检查存在性。
- [x] **Step 4: 运行确认通过**，再跑会员模块全部测试。
- [x] **Step 5: 变异检查**：
  - 删除前的在用检查去掉；
  - `validateTagList` 只检查第一个编号。
- [x] **Step 6: 提交** `git commit -m "feat(member): 会员标签管理"`

---

### Task 8: 分组

**Files:**
- Create: `{biz}/dal/dataobject/group/MemberGroupDO.java`、`{biz}/dal/mysql/group/MemberGroupMapper.java`
- Create: `{biz}/service/group/{MemberGroupService, MemberGroupServiceImpl}.java`
- Create: `{biz}/controller/admin/group/MemberGroupController.java`，`vo/{MemberGroupSaveReqVO, MemberGroupPageReqVO, MemberGroupRespVO, MemberGroupSimpleRespVO}.java`
- Test: `{test}/service/group/MemberGroupServiceImplTest.java`、`{test}/controller/admin/MemberGroupControllerTest.java`

**Interfaces:**
- Consumes: `MemberUserMapper.selectCountByGroupId`。
- Produces: `MemberGroupService.validateGroupEnabled(Long id)`、`getGroupList(Collection<Long> ids)`；以下接口：
  - `/admin-api/member/group/{create, update, delete, get, page}`，权限 `member:group:*`；
  - `/admin-api/member/group/list-all-simple`：登录即可，只返回启用的分组。

`MemberGroupDO`（`member_group`）：`id, name, remark, status`。

保存 VO：
- `name`：`@NotBlank`，最长 50；
- `remark`：最长 500；
- `status`：`@NotNull @InEnum(CommonStatusEnum.class)`。

分页 VO：`name`（模糊）、`status`、`createTime[]`。

- [x] **Step 1: 写失败测试**：
  - 名称重复（新增与修改）；
  - 有会员在用时删除报 `GROUP_HAS_USER`；
  - `validateGroupEnabled`：不存在报 `GROUP_NOT_EXISTS`，停用报 `GROUP_DISABLED`，启用通过；
  - `list-all-simple` 不含停用分组；
  - 无权限删除返回 403。

```java
    @Test
    void validateGroupEnabled() {
        Long enabled = groupService.createGroup(save(null, "普通", CommonStatusEnum.ENABLE));
        Long disabled = groupService.createGroup(save(null, "冻结", CommonStatusEnum.DISABLE));
        groupService.validateGroupEnabled(enabled);
        assertServiceException(() -> groupService.validateGroupEnabled(disabled), GROUP_DISABLED);
        assertServiceException(() -> groupService.validateGroupEnabled(-1L), GROUP_NOT_EXISTS);
    }

    @Test
    void deleteGroup_有会员在用时拒绝() {
        Long id = groupService.createGroup(save(null, "普通", CommonStatusEnum.ENABLE));
        insertUserWithGroup(id);
        assertServiceException(() -> groupService.deleteGroup(id), GROUP_HAS_USER);
    }
```

- [x] **Step 2: 运行确认失败**。Run: `-Dtest='MemberGroupServiceImplTest,MemberGroupControllerTest'`。
- [x] **Step 3: 实现**，写法同 Task 7，多一个状态字段。
- [x] **Step 4: 运行确认通过**，再跑会员模块全部测试。
- [x] **Step 5: 变异检查**：
  - 去掉停用检查；
  - 去掉在用检查。
- [x] **Step 6: 提交** `git commit -m "feat(member): 会员分组管理"`

---

### Task 9: 会员配置

**Files:**
- Create: `{biz}/dal/dataobject/config/MemberConfigDO.java`、`{biz}/dal/mysql/config/MemberConfigMapper.java`
- Create: `{biz}/service/config/{MemberConfigService, MemberConfigServiceImpl}.java`
- Create: `{biz}/controller/admin/config/MemberConfigController.java`，`vo/{MemberConfigSaveReqVO, MemberConfigRespVO}.java`
- Test: `{test}/service/config/MemberConfigServiceImplTest.java`、`{test}/controller/admin/MemberConfigControllerTest.java`

**Interfaces:**
- Produces:
  - `GET /admin-api/member/config/get`（`member:config:query`）：没有配置时 `data` 为 null；
  - `PUT /admin-api/member/config/save`（`member:config:update`）。

`MemberConfigDO`（`member_config`）：`id, pointTradeDeductEnable, pointTradeDeductUnitPrice, pointTradeDeductMaxPrice, pointTradeGivePoint`。

保存 VO：
- `pointTradeDeductEnable`：`@NotNull Boolean`；
- `pointTradeDeductUnitPrice`：`@NotNull @Min(0)`；
- `pointTradeDeductMaxPrice`：`@Min(0)`，可空；
- `pointTradeGivePoint`：`@Min(0)`，可空。

- [x] **Step 1: 写失败测试**

```java
    @Test
    void saveConfig_不存在时新增_存在时修改_每个租户一条() {
        assertThat(configService.getConfig()).isNull();
        configService.saveConfig(save(true, 1));
        configService.saveConfig(save(false, 2));
        assertThat(configMapper.selectCount()).isEqualTo(1);
        assertThat(configService.getConfig().getPointTradeDeductUnitPrice()).isEqualTo(2);
    }

    @Test
    void getConfig_租户隔离() {
        configService.saveConfig(save(true, 1));
        assertThat(TenantUtils.execute(TENANT_ID + 1000, () -> configService.getConfig())).isNull();
    }
```

  Controller 测试：
  - 无权限保存返回 403；
  - 有权限保存后再读取，值一致。
- [x] **Step 2: 运行确认失败**。
- [x] **Step 3: 实现**：`getConfig()` 为 `configMapper.selectOne(new LambdaQueryWrapperX<>())`（租户拦截器自动加租户条件）；`saveConfig` 查到则按编号更新，否则新增。
- [x] **Step 4: 运行确认通过**，再跑会员模块全部测试。
- [x] **Step 5: 提交** `git commit -m "feat(member): 会员配置"`

---

### Task 10: 管理端会员用户

**Files:**
- Create: `{biz}/controller/admin/user/MemberUserController.java`，`vo/{MemberUserPageReqVO, MemberUserUpdateReqVO, MemberUserUpdatePointReqVO, MemberUserUpdateLevelReqVO, MemberUserRespVO}.java`
- Modify: `{biz}/service/user/MemberUserService.java`、`MemberUserServiceImpl.java`
- Modify: `{biz}/dal/mysql/user/MemberUserMapper.java`（分页查询）
- Test: `{test}/service/user/MemberUserServiceImplTest.java`（追加）、`{test}/controller/admin/MemberUserControllerTest.java`

**Interfaces:**
- Consumes:
  - `MemberTagService.validateTagList`（Task 7）
  - `MemberGroupService.validateGroupEnabled`（Task 8）
  - `MemberPointService.updateUserPointByAdmin`（Task 4）
  - `MemberLevelService.updateUserLevel`（Task 3）
  - `OAuth2TokenApi.removeAccessTokensByUser`（system-api，已在 `RpcConfiguration` 注册）
- Produces:

| 方法 | 路径 | 权限 |
|---|---|---|
| GET | `/admin-api/member/user/page` | `member:user:query` |
| GET | `/admin-api/member/user/get?id=` | `member:user:query` |
| PUT | `/admin-api/member/user/update` | `member:user:update` |
| PUT | `/admin-api/member/user/update-point` | `member:user:update-point`，`{id, point}` |
| PUT | `/admin-api/member/user/update-level` | `member:user:update-level`，`{id, levelId, reason}` |
| DELETE | `/admin-api/member/user/delete?id=` | `member:user:delete` |

新增服务方法：

```java
PageResult<MemberUserDO> getUserPage(MemberUserPageReqVO reqVO);
void updateUserByAdmin(MemberUserUpdateReqVO reqVO);
void deleteUser(Long id);
```

VO：
- `MemberUserPageReqVO`：`mobile`（模糊）、`nickname`（模糊）、`createTime[]`、`loginDate[]`、`levelId`、`groupId`、`tagIds`（`List<Long>`，命中任一即可）。
- `MemberUserUpdateReqVO`：
  - `id @NotNull`；
  - `nickname`：`@NotBlank`，最长 30；
  - `avatar`：最长 512；
  - `status`：`@NotNull @InEnum`；
  - `sex`、`birthday`；
  - `mark`：最长 255；
  - `tagIds`：`List<Long>`；
  - `groupId`。
- `MemberUserRespVO`：会员基本信息（不含密码）加 `point`、`experience`、`levelId`、`levelName`、`groupId`、`groupName`、`tagIds`（`List<Long>`）。
- `MemberUserUpdatePointReqVO`：`id @NotNull`、`point @NotNull`。
- `MemberUserUpdateLevelReqVO`：`id @NotNull`、`levelId @NotNull`、`reason`（`@NotBlank`，最长 255）。

- [x] **Step 1: 写失败测试**（`MemberUserServiceImplTest` 追加；`@MockitoBean` 注入 `MemberTagService`、`MemberGroupService`、`OAuth2TokenApi`）

```java
    @Test
    void updateUserByAdmin_保存标签为逗号列表_校验标签与分组() {
        MemberUserDO user = userService.createUserIfAbsent("13911119001", "10.0.0.1", null);
        MemberUserUpdateReqVO reqVO = updateReqVO(user.getId(), CommonStatusEnum.ENABLE);
        reqVO.setTagIds(List.of(3L, 5L));
        reqVO.setGroupId(9L);

        userService.updateUserByAdmin(reqVO);

        assertThat(userMapper.selectById(user.getId()).getTagIds()).isEqualTo("3,5");
        verify(tagService).validateTagList(List.of(3L, 5L));
        verify(groupService).validateGroupEnabled(9L);
        verify(oauth2TokenApi, never()).removeAccessTokensByUser(anyLong(), anyInt());
    }

    @Test
    void updateUserByAdmin_清空标签与分组() {
        MemberUserDO user = userService.createUserIfAbsent("13911119002", "10.0.0.1", null);
        userMapper.updateById(new MemberUserDO().setId(user.getId()).setTagIds("1").setGroupId(2L));
        userService.updateUserByAdmin(updateReqVO(user.getId(), CommonStatusEnum.ENABLE));
        MemberUserDO saved = userMapper.selectById(user.getId());
        assertThat(saved.getTagIds()).isNull();
        assertThat(saved.getGroupId()).isNull();
    }

    @Test
    void updateUserByAdmin_禁用时作废令牌() {
        MemberUserDO user = userService.createUserIfAbsent("13911119003", "10.0.0.1", null);
        when(oauth2TokenApi.removeAccessTokensByUser(anyLong(), anyInt())).thenReturn(CommonResult.success(true));
        userService.updateUserByAdmin(updateReqVO(user.getId(), CommonStatusEnum.DISABLE));
        verify(oauth2TokenApi).removeAccessTokensByUser(user.getId(), UserTypeEnum.MEMBER.getValue());
    }

    @Test
    void deleteUser_删除并作废令牌() {
        MemberUserDO user = userService.createUserIfAbsent("13911119004", "10.0.0.1", null);
        when(oauth2TokenApi.removeAccessTokensByUser(anyLong(), anyInt())).thenReturn(CommonResult.success(true));
        userService.deleteUser(user.getId());
        assertThat(userMapper.selectById(user.getId())).isNull();
        verify(oauth2TokenApi).removeAccessTokensByUser(user.getId(), UserTypeEnum.MEMBER.getValue());
        assertServiceException(() -> userService.deleteUser(-1L), USER_NOT_EXISTS);
    }

    @Test
    void getUserPage_按标签任一命中_按等级分组与手机号筛选() {
        MemberUserDO a = userService.createUserIfAbsent("13911119005", "10.0.0.1", null);
        MemberUserDO b = userService.createUserIfAbsent("13911119006", "10.0.0.1", null);
        userMapper.updateById(new MemberUserDO().setId(a.getId()).setTagIds("1,12").setGroupId(7L));
        userMapper.updateById(new MemberUserDO().setId(b.getId()).setTagIds("2"));

        MemberUserPageReqVO reqVO = new MemberUserPageReqVO();
        reqVO.setTagIds(List.of(12L, 99L));
        assertThat(userService.getUserPage(reqVO).getList()).extracting(MemberUserDO::getId).containsExactly(a.getId());
        reqVO.setTagIds(List.of(2L));
        assertThat(userService.getUserPage(reqVO).getList()).extracting(MemberUserDO::getId).containsExactly(b.getId());
        reqVO.setTagIds(null);
        reqVO.setGroupId(7L);
        assertThat(userService.getUserPage(reqVO).getList()).extracting(MemberUserDO::getId).containsExactly(a.getId());
        reqVO.setGroupId(null);
        reqVO.setMobile("9006");
        assertThat(userService.getUserPage(reqVO).getList()).extracting(MemberUserDO::getId).containsExactly(b.getId());
    }
```

`MemberUserControllerTest`（`extends BaseMemberWebIntegrationTest`；前置用 SQL 插入会员 90201，积分 100，以及启用的等级 id 9001、门槛 0、序号 90）：
- `update-point` 加 50 后 `get` 返回 `point = 150`；
- `update-level` 后 `levelName` 为该等级名称；
- 无 `member:user:update-point` 权限（`NO_PERM_ADMIN_ID`）返回 403；
- 会员身份访问返回 401；
- `get` 响应不含 `password`。

- [x] **Step 2: 运行确认失败**

Run: `-Dtest='MemberUserServiceImplTest,MemberUserControllerTest'`。Expected: 编译失败。

- [x] **Step 3: 实现**

```java
    @Override
    public void updateUserByAdmin(MemberUserUpdateReqVO reqVO) {
        validateUserExists(reqVO.getId());
        if (CollUtil.isNotEmpty(reqVO.getTagIds())) {
            tagService.validateTagList(reqVO.getTagIds());
        }
        if (reqVO.getGroupId() != null) {
            groupService.validateGroupEnabled(reqVO.getGroupId());
        }
        // 标签、分组允许清空，用 UpdateWrapper 显式写入 null
        memberUserMapper.update(null, new LambdaUpdateWrapper<MemberUserDO>()
                .eq(MemberUserDO::getId, reqVO.getId())
                .set(MemberUserDO::getNickname, reqVO.getNickname())
                .set(MemberUserDO::getAvatar, StrUtil.nullToEmpty(reqVO.getAvatar()))
                .set(MemberUserDO::getStatus, reqVO.getStatus())
                .set(MemberUserDO::getSex, reqVO.getSex())
                .set(MemberUserDO::getBirthday, reqVO.getBirthday())
                .set(MemberUserDO::getMark, reqVO.getMark())
                .set(MemberUserDO::getTagIds, CollUtil.isEmpty(reqVO.getTagIds()) ? null : CollUtil.join(reqVO.getTagIds(), ","))
                .set(MemberUserDO::getGroupId, reqVO.getGroupId()));
        if (CommonStatusEnum.isDisable(reqVO.getStatus())) {
            removeUserTokens(reqVO.getId());
        }
    }

    @Override
    public void deleteUser(Long id) {
        validateUserExists(id);
        memberUserMapper.deleteById(id);
        removeUserTokens(id);
    }

    private void removeUserTokens(Long id) {
        oauth2TokenApi.removeAccessTokensByUser(id, UserTypeEnum.MEMBER.getValue()).checkError();
    }
```

分页查询（`MemberUserMapper`）：

```java
    default PageResult<MemberUserDO> selectPage(MemberUserPageReqVO reqVO) {
        LambdaQueryWrapperX<MemberUserDO> query = new LambdaQueryWrapperX<MemberUserDO>()
                .likeIfPresent(MemberUserDO::getMobile, reqVO.getMobile())
                .likeIfPresent(MemberUserDO::getNickname, reqVO.getNickname())
                .betweenIfPresent(MemberUserDO::getCreateTime, reqVO.getCreateTime())
                .betweenIfPresent(MemberUserDO::getLoginDate, reqVO.getLoginDate())
                .eqIfPresent(MemberUserDO::getLevelId, reqVO.getLevelId())
                .eqIfPresent(MemberUserDO::getGroupId, reqVO.getGroupId());
        List<Long> tagIds = reqVO.getTagIds();
        if (CollUtil.isNotEmpty(tagIds)) {
            // 命中任一标签即可：(FIND_IN_SET(a, tag_ids) OR FIND_IN_SET(b, tag_ids) ...)
            query.and(w -> {
                for (int i = 0; i < tagIds.size(); i++) {
                    if (i > 0) {
                        w.or();
                    }
                    w.apply("FIND_IN_SET({0}, tag_ids)", tagIds.get(i));
                }
            });
        }
        return selectPage(reqVO, query.orderByDesc(MemberUserDO::getId));
    }
```

Controller 的 `page` 与 `get`：
- 用 `levelService.getLevelList(ids)`、`groupService.getGroupList(ids)` 批量取名称填入 `levelName`、`groupName`；
- `tagIds` 由 `StrUtils.splitToLong(user.getTagIds(), ",")` 转换，空值转成空列表。

`update-point` 调用 `pointService.updateUserPointByAdmin(id, point)`，`update-level` 调用 `levelService.updateUserLevel(id, levelId, reason)`。

- [x] **Step 4: 运行确认通过**，再跑会员模块全部测试。Expected: PASS。
- [x] **Step 5: 变异检查**：
  - 去掉禁用时的作废令牌；
  - 去掉删除时的作废令牌；
  - 标签筛选的 `or()` 改为 `and`。
- [x] **Step 6: 提交** `git commit -m "feat(member): 管理端会员用户管理，禁用与删除时作废会员令牌"`

---

### Task 11: 端到端验收与文档

**Files:**
- Modify: `scripts/verify-e2e.sh`（在"文档、链路追踪…"一节之前插入"会员运营"一节）
- Modify: `docs/模块说明.md`、`docs/接口鉴权说明.md`
- Modify: `scripts/verify-docs.sh`（仅当新增文档引用需要登记时）

**Interfaces:**
- Consumes: Task 1～10 交付的全部接口；docker 环境中 member 服务端口 `127.0.0.1:${MEMBER_PORT}`（内部接口直连，不经网关）。

- [x] **Step 1: 在 `verify-e2e.sh` 插入**（`$MT` 为 C 端 App 令牌；此时 `$TOKEN` 已登出，重新登录取管理员令牌）：

```bash
echo "==> 会员运营"
AT=$(admin_login admin admin123 | jq -r '.data.accessToken')
MEMBER_ID=$(curl -s "$GW/admin-api/member/user/page?pageNo=1&pageSize=1&mobile=13800138000" -H "Authorization: Bearer $AT" \
  | jq -r '.data.list[0].id // empty')
[ -n "$MEMBER_ID" ] || die "管理端查不到演示会员"; ok "管理端查询会员"
P0=$(curl -s "$GW/admin-api/member/user/get?id=$MEMBER_ID" -H "Authorization: Bearer $AT" | jq -r '.data.point')
R=$(curl -s -X PUT "$GW/admin-api/member/user/update-point" -H "Authorization: Bearer $AT" -H "$JSON" \
  -d "{\"id\":$MEMBER_ID,\"point\":10}" | jq -r '.code')
[ "$R" = "0" ] || die "管理员加积分返回 $R"
P1=$(curl -s "$GW/admin-api/member/user/get?id=$MEMBER_ID" -H "Authorization: Bearer $AT" | jq -r '.data.point')
[ "$P1" = "$((P0 + 10))" ] || die "加积分后为 $P1，期望 $((P0 + 10))"; ok "管理员修改积分"
R=$(curl -s -X PUT "$GW/admin-api/member/user/update-level" -H "Authorization: Bearer $AT" -H "$JSON" \
  -d "{\"id\":$MEMBER_ID,\"levelId\":2,\"reason\":\"e2e\"}" | jq -r '.code')
[ "$R" = "0" ] || die "管理员修改等级返回 $R"
[ "$(curl -s "$GW/admin-api/member/user/get?id=$MEMBER_ID" -H "Authorization: Bearer $AT" | jq -r '.data.levelName')" = "白银会员" ] \
  || die "修改等级后等级名称不是白银会员"; ok "管理员修改等级"
# 同一天重复执行脚本时已签到，两种结果都接受
R=$(curl -s -X POST "$GW/app-api/member/sign-in/record/create" -H "Authorization: Bearer $MT" | jq -r '.code')
[ "$R" = "0" ] || [ "$R" = "1004010000" ] || die "会员签到返回 $R"
curl -s "$GW/app-api/member/sign-in/record/get-summary" -H "Authorization: Bearer $MT" \
  | jq -e '.data.todaySignIn == true' >/dev/null || die "签到后概况显示今日未签到"; ok "会员签到"
curl -s "$GW/app-api/member/point/record/page?pageNo=1&pageSize=10" -H "Authorization: Bearer $MT" \
  | jq -e '.data.total > 0' >/dev/null || die "会员积分记录为空"; ok "会员查询积分记录"
curl -s "$GW/admin-api/member/level-record/page?pageNo=1&pageSize=10&userId=$MEMBER_ID" -H "Authorization: Bearer $AT" \
  | jq -e '.data.total > 0' >/dev/null || die "等级变更记录为空"; ok "管理端查询等级变更记录"
ADDR=$(curl -s -X POST "$GW/app-api/member/address/create" -H "Authorization: Bearer $MT" -H "$JSON" \
  -d '{"name":"张三","mobile":"13800138000","areaId":440305,"detailAddress":"科技园 1 号","defaultStatus":true}' | jq -r '.data // empty')
[ -n "$ADDR" ] || die "新增收件地址失败"
curl -s "$GW/app-api/member/address/list" -H "Authorization: Bearer $MT" \
  | jq -e --argjson id "$ADDR" 'any(.data[]; .id == $id)' >/dev/null || die "收件地址列表中没有新地址"
curl -s -X DELETE "$GW/app-api/member/address/delete?id=$ADDR" -H "Authorization: Bearer $MT" >/dev/null
ok "会员收件地址"
MS="http://127.0.0.1:${MEMBER_PORT}"
BIZ="e2e-$(date +%s)"
BEFORE=$(curl -s "$GW/admin-api/member/user/get?id=$MEMBER_ID" -H "Authorization: Bearer $AT" | jq -r '.data.point')
for _ in 1 2; do
  R=$(curl -s -X PUT "$MS/rpc-api/member/point/add?userId=$MEMBER_ID&point=5&bizType=10&bizId=$BIZ" -H "$TENANT" | jq -r '.code')
  [ "$R" = "0" ] || die "内部接口加积分返回 $R"
done
AFTER=$(curl -s "$GW/admin-api/member/user/get?id=$MEMBER_ID" -H "Authorization: Bearer $AT" | jq -r '.data.point')
[ "$AFTER" = "$((BEFORE + 5))" ] || die "同一业务编号加积分两次后为 $AFTER，期望 $((BEFORE + 5))"
ok "内部接口加积分，同一业务编号只生效一次"
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X PUT "$GW/rpc-api/member/point/add?userId=$MEMBER_ID&point=5&bizType=10&bizId=x")
[ "$CODE" = "403" ] || die "经网关可访问会员内部接口，返回 $CODE"; ok "会员内部接口经网关已阻断"
```

同时把脚本首行注释改为"P1 与 P2 端到端验收"，结尾的输出改为"端到端验收全部通过"。

- [x] **Step 2: 更新文档**：
  - `docs/模块说明.md`：member 行职责改为"会员用户与 C 端认证；收件地址、标签、分组、等级与经验、积分、签到、会员配置"，删除"后续阶段"表中的"`ccloud-module-member` 运营功能"一行。
  - `docs/接口鉴权说明.md`：用户端 app-api 表格补充地址、积分记录、等级、签到接口各一行（按功能合并，例如 "`/app-api/member/address/*` 收件地址增删改查与默认地址"）。

- [x] **Step 3: 全量测试**

Run: `mvn -o test`
Expected: BUILD SUCCESS，member 模块测试数明显增加，其他模块与改动前一致（system 419、gateway 56、monitor 7）。

- [x] **Step 4: 全量验收**。需先确认本机 3306、8848 等端口的占用情况，按 P1 的做法用端口覆盖变量：

```bash
MYSQL_PORT=13306 REDIS_PORT=16379 NACOS_PORT=18848 NACOS_GRPC_PORT=19848 NACOS_CONSOLE_PORT=18849 scripts/verify-all.sh
```

Expected: 7 个步骤全部通过，e2e 输出"会员运营"一节全部 OK。数据卷里是旧的初始化数据（演示会员无等级）时，先 `scripts/docker-down.sh --purge` 再执行。

- [x] **Step 5: 提交**

```bash
git add scripts docs
git commit -m "test: 会员运营端到端验收与文档"
```

> 执行记录：按用户要求，部署与验收改为连接本机已有的中间件（mysql、redis、nacos 3.1.2、zipkin，以及为 ccloud 新装的 xxl-job-admin），ccloud 只启动自己的 4 个应用；Redis 用 1 号库，Nacos 用 `ccloud` 命名空间，Nacos 账号在 `~/.ccloud/middleware.env`。验收中发现并修复两处脚本问题：载入 .env 的循环在最后一个键被环境变量覆盖时让 `set -e` 静默退出；监控面板在服务刚重启时尚未标为 UP，改为最多等待 60 秒。
