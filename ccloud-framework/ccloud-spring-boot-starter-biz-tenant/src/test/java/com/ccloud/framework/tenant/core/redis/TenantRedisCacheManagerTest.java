package com.ccloud.framework.tenant.core.redis;

import com.ccloud.framework.tenant.core.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TenantRedisCacheManagerTest {

    private final TenantRedisCacheManager manager = new TenantRedisCacheManager(
            mock(RedisCacheWriter.class), RedisCacheConfiguration.defaultCacheConfig(),
            Set.of("permission_menu_ids", "dict_data_list"));

    @AfterEach
    void clear() {
        TenantContextHolder.clear();
    }

    @Test
    void 租户数据的缓存名带租户前缀() {
        TenantContextHolder.setTenantId(7L);
        assertThat(manager.getCache("role#30m").getName()).isEqualTo("7:role#30m");
    }

    /**
     * 全局表（菜单、字典）的缓存若按租户隔离，系统租户修改后只能清掉自己那份，其他租户会持续读到旧数据
     */
    @Test
    void 忽略的缓存为全局共享_不论是否声明过期时间() {
        TenantContextHolder.setTenantId(7L);
        assertThat(manager.getCache("permission_menu_ids#30m").getName()).isEqualTo("permission_menu_ids#30m");
        assertThat(manager.getCache("dict_data_list").getName()).isEqualTo("dict_data_list");

        TenantContextHolder.setTenantId(8L);
        assertThat(manager.getCache("permission_menu_ids#30m"))
                .isSameAs(manager.getCache("permission_menu_ids#30m"));
    }

    @Test
    void 无租户上下文时不加前缀() {
        assertThat(manager.getCache("role#30m").getName()).isEqualTo("role#30m");
    }
}
