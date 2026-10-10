package com.ccloud.module.infra.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 参数配置、文件配置、消息死信管理的是全局或跨租户的数据，只能由系统租户（套餐 0）使用。
 * 普通套餐与非系统租户的角色一旦含这些菜单，租户管理员就能改全局参数、看其他租户的死信
 */
class SystemTenantMenuSeedTest {

    /**
     * 文件配置 126–130、参数配置 131–136、消息死信 166–168
     */
    private static final Set<Long> SYSTEM_ONLY_MENU_IDS = LongStream.concat(
            LongStream.rangeClosed(126, 136), LongStream.rangeClosed(166, 168))
            .boxed().collect(Collectors.toCollection(TreeSet::new));

    private static final long SYSTEM_TENANT_ID = 1L;

    @Test
    @DisplayName("普通套餐不得含全局 / 跨租户的 infra 管理菜单")
    void normalPackageShouldNotContainSystemOnlyMenus() {
        Set<Long> menuIds = normalPackageMenuIds();

        assertThat(SYSTEM_ONLY_MENU_IDS).hasSize(14);
        assertThat(menuIds).as("普通套餐菜单").isNotEmpty().doesNotContainAnyElementsOf(SYSTEM_ONLY_MENU_IDS);
        // 文件列表与 API 日志仍按租户隔离，普通租户可用
        assertThat(menuIds).contains(121L, 122L, 123L, 124L, 125L);
    }

    @Test
    @DisplayName("普通套餐不含接口目录的增删改，含开放应用授权与测试回调按钮")
    void 普通套餐不含接口目录的增删改_含开放应用授权与测试回调按钮() {
        Set<Long> menuIds = normalPackageMenuIds();
        assertThat(menuIds).doesNotContain(215L, 216L, 217L).contains(214L, 493L, 494L);
    }

    private static Set<Long> normalPackageMenuIds() {
        Matcher matcher = Pattern.compile("\\(1, '普通套餐', [^\\n]*'\\[([0-9,]+)]'\\)")
                .matcher(SqlFixture.read("01-tenant-and-user.sql"));
        assertThat(matcher.find()).as("01-tenant-and-user.sql 中应有普通套餐").isTrue();
        return Arrays.stream(matcher.group(1).split(",")).map(Long::valueOf).collect(Collectors.toSet());
    }

    @Test
    @DisplayName("非系统租户的角色不得被授予全局 / 跨租户的 infra 管理菜单")
    void nonSystemTenantRolesShouldNotContainSystemOnlyMenus() {
        // 行格式：(id, role_id, menu_id, tenant_id, 'creator')
        Matcher matcher = Pattern.compile("\\(\\d+, \\d+, (\\d+), (\\d+), '[^']*'\\)")
                .matcher(SqlFixture.read("03-role.sql"));
        List<String> violations = new java.util.ArrayList<>();
        int rows = 0;
        while (matcher.find()) {
            rows++;
            long menuId = Long.parseLong(matcher.group(1));
            long tenantId = Long.parseLong(matcher.group(2));
            if (tenantId != SYSTEM_TENANT_ID && SYSTEM_ONLY_MENU_IDS.contains(menuId)) {
                violations.add(matcher.group());
            }
        }
        assertThat(rows).as("03-role.sql 中的角色菜单行").isPositive();
        assertThat(violations).as("非系统租户的角色菜单").isEmpty();
    }
}
