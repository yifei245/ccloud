package com.ccloud.framework.redis.config;

import lombok.Data;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 缓存值必须原样还原：Long 被还原成 String 时，Set#contains(1L) 会静默返回 false，
 * 权限、角色这类缓存将全部失效且没有任何报错。
 */
class RedisSerializerRoundTripTest {

    private final RedisSerializer<Object> serializer = CcloudRedisAutoConfiguration.buildRedisSerializer();

    @Data
    public static class Demo {
        private Long id;
        private String name;
        private Set<Long> menuIds;
        private LocalDateTime createTime;
        private Boolean deleted;
    }

    @SuppressWarnings("unchecked")
    private <T> T roundTrip(Object value) {
        return (T) serializer.deserialize(serializer.serialize(value));
    }

    @Test
    void Long集合应还原为Long() {
        Set<Long> set = roundTrip(new LinkedHashSet<>(List.of(1L, 9007199254740993L)));
        assertThat(set).containsExactly(1L, 9007199254740993L);
        List<Long> list = roundTrip(new ArrayList<>(List.of(3L)));
        assertThat(list).containsExactly(3L);
    }

    @Test
    void 单个Long应还原为Long() {
        Object value = roundTrip(5L);
        assertThat(value).isEqualTo(5L);
    }

    @Test
    void 对象字段应完整还原() {
        Demo demo = new Demo();
        demo.setId(9007199254740993L);
        demo.setName("超级管理员");
        demo.setMenuIds(new HashSet<>(Set.of(1L, 2L)));
        demo.setCreateTime(LocalDateTime.of(2026, 9, 17, 10, 30, 15));
        demo.setDeleted(false);

        Demo copy = roundTrip(demo);

        assertThat(copy).isEqualTo(demo);
        assertThat(copy.getMenuIds()).contains(1L);
    }

    @Test
    void 对象列表应还原为对象() {
        Demo demo = new Demo();
        demo.setId(1L);
        List<Demo> list = roundTrip(new ArrayList<>(List.of(demo)));
        assertThat(list.get(0)).isInstanceOf(Demo.class);
        assertThat(list.get(0).getId()).isEqualTo(1L);
    }
}
