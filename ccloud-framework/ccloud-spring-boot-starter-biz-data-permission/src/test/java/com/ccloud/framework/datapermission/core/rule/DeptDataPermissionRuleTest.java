package com.ccloud.framework.datapermission.core.rule;

import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.datapermission.core.rule.dept.DeptDataPermissionRespDTO;
import com.ccloud.framework.datapermission.core.rule.dept.DeptDataPermissionFrameworkService;
import com.ccloud.framework.datapermission.core.rule.dept.DeptDataPermissionRule;
import com.ccloud.framework.security.core.LoginUser;
import com.ccloud.framework.security.core.util.SecurityFrameworkUtils;
import net.sf.jsqlparser.expression.Expression;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeptDataPermissionRuleTest {

    private DeptDataPermissionFrameworkService service;
    private DeptDataPermissionRule rule;

    @BeforeEach
    void setUp() {
        service = mock(DeptDataPermissionFrameworkService.class);
        rule = new DeptDataPermissionRule(service);
        rule.addDeptColumn("system_users", "dept_id");
        rule.addUserColumn("system_users", "id");
        login(100L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void login(Long userId) {
        LoginUser user = new LoginUser();
        user.setId(userId);
        user.setUserType(UserTypeEnum.ADMIN.getValue());
        user.setTenantId(1L);
        user.setExpiresTime(LocalDateTime.now().plusHours(1));
        SecurityFrameworkUtils.setLoginUser(user, null);
    }

    private String expressionOf(String table) {
        Expression expression = rule.getExpression(table, () -> table);
        return expression == null ? null : expression.toString();
    }

    @Test
    @DisplayName("拥有全部数据权限时不追加任何条件")
    void allPermission_shouldNotAppendCondition() {
        when(service.getDeptDataPermission(anyLong()))
                .thenReturn(new DeptDataPermissionRespDTO().setAll(true));
        assertThat(expressionOf("system_users")).isNull();
    }

    @Test
    @DisplayName("指定部门权限应生成 dept_id IN 条件")
    void deptPermission_shouldGenerateInCondition() {
        when(service.getDeptDataPermission(anyLong()))
                .thenReturn(new DeptDataPermissionRespDTO().setDeptIds(Set.of(10L)));
        assertThat(expressionOf("system_users")).isEqualTo("system_users.dept_id IN (10)");
    }

    @Test
    @DisplayName("仅本人权限应生成当前用户编号的等值条件")
    void selfPermission_shouldGenerateEqualsCondition() {
        when(service.getDeptDataPermission(anyLong()))
                .thenReturn(new DeptDataPermissionRespDTO().setSelf(true));
        assertThat(expressionOf("system_users")).isEqualTo("system_users.id = 100");
    }

    @Test
    @DisplayName("部门与本人权限并存时应生成 OR 条件，二者取并集")
    void deptAndSelf_shouldGenerateOrCondition() {
        when(service.getDeptDataPermission(anyLong()))
                .thenReturn(new DeptDataPermissionRespDTO().setDeptIds(Set.of(10L)).setSelf(true));
        assertThat(expressionOf("system_users"))
                .isEqualTo("(system_users.dept_id IN (10) OR system_users.id = 100)");
    }

    @Test
    @DisplayName("没有任何数据权限时应生成恒假条件，查不到数据而不是查到全部")
    void noPermission_shouldGenerateAlwaysFalseCondition() {
        when(service.getDeptDataPermission(anyLong())).thenReturn(new DeptDataPermissionRespDTO());
        assertThat(expressionOf("system_users")).isEqualTo("system_users.dept_id IS NULL");
    }

    @Test
    @DisplayName("未注册字段的表不追加条件，避免误伤与该维度无关的表")
    void unregisteredTable_shouldNotAppendCondition() {
        when(service.getDeptDataPermission(anyLong()))
                .thenReturn(new DeptDataPermissionRespDTO().setDeptIds(Set.of(10L)));
        assertThat(expressionOf("infra_config")).isNull();
    }

    @Test
    @DisplayName("未登录时不追加条件，定时任务等无身份场景不应被数据权限拦截")
    void notLoggedIn_shouldNotAppendCondition() {
        SecurityContextHolder.clearContext();
        assertThat(expressionOf("system_users")).isNull();
    }

    @Test
    @DisplayName("同一请求内重复取权限应复用结果，不重复发起 RPC")
    void permissionShouldBeResolvedOncePerRequest() {
        when(service.getDeptDataPermission(anyLong()))
                .thenReturn(new DeptDataPermissionRespDTO().setDeptIds(Set.of(10L)));
        expressionOf("system_users");
        expressionOf("system_users");
        org.mockito.Mockito.verify(service, org.mockito.Mockito.times(1)).getDeptDataPermission(100L);
    }
}
