package com.ccloud.framework.tenant.core.db;

import com.ccloud.framework.tenant.config.TenantProperties;
import com.ccloud.framework.tenant.core.context.TenantContextHolder;
import net.sf.jsqlparser.expression.LongValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TenantLineHandlerImplTest {

    private TenantLineHandlerImpl handler;

    @BeforeEach
    void setUp() {
        TenantProperties properties = new TenantProperties();
        properties.setIgnoreTables(Set.of("system_tenant", "system_menu"));
        handler = new TenantLineHandlerImpl(properties);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("普通表应被拼接 tenant_id 条件")
    void ignoreTable_shouldReturnFalseForNormalTable() {
        TenantContextHolder.setTenantId(1L);
        assertThat(handler.ignoreTable("system_users")).isFalse();
    }

    @Test
    @DisplayName("忽略清单中的表不拼条件")
    void ignoreTable_shouldReturnTrueForConfiguredTables() {
        TenantContextHolder.setTenantId(1L);
        assertThat(handler.ignoreTable("system_tenant")).isTrue();
        assertThat(handler.ignoreTable("system_menu")).isTrue();
    }

    @Test
    @DisplayName("表名匹配应忽略大小写，避免因 SQL 书写风格漏掉隔离")
    void ignoreTable_shouldBeCaseInsensitive() {
        TenantContextHolder.setTenantId(1L);
        assertThat(handler.ignoreTable("SYSTEM_TENANT")).isTrue();
    }

    @Test
    @DisplayName("显式设置 ignore 时所有表都不拼条件")
    void ignoreTable_shouldRespectExplicitIgnoreFlag() {
        TenantContextHolder.setTenantId(1L);
        TenantContextHolder.setIgnore(true);
        assertThat(handler.ignoreTable("system_users")).isTrue();
    }

    @Test
    @DisplayName("上下文中没有租户时不拼条件，否则会生成 tenant_id = null 查不出任何数据")
    void ignoreTable_shouldReturnTrueWhenNoTenantInContext() {
        assertThat(handler.ignoreTable("system_users")).isTrue();
    }

    @Test
    @DisplayName("租户编号应作为字面量值参与 SQL 拼接")
    void getTenantId_shouldReturnCurrentTenantAsLiteral() {
        TenantContextHolder.setTenantId(99L);
        assertThat(handler.getTenantId()).isInstanceOf(LongValue.class)
                .extracting(e -> ((LongValue) e).getValue()).isEqualTo(99L);
    }

    @Test
    @DisplayName("租户列名固定为 tenant_id")
    void getTenantIdColumn_shouldBeTenantId() {
        assertThat(handler.getTenantIdColumn()).isEqualTo("tenant_id");
    }
}
