package com.ccloud.framework.redis.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.config.Config;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 配置文件常写 {@code password: ${REDIS_PASSWORD:}}：未设置环境变量时绑定为空串。
 * Lettuce 把空串当作无密码，Redisson 却会发送 {@code AUTH ""}，无密码的 Redis 直接拒绝，服务启动失败
 */
class BlankRedisPasswordCustomizerTest {

    private final CcloudRedisAutoConfiguration configuration = new CcloudRedisAutoConfiguration();

    @Test
    @DisplayName("单机模式的空串密码与用户名应清空为 null")
    void blankPasswordShouldBeClearedForSingleServer() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379").setPassword("").setUsername(" ");

        configuration.blankRedisPasswordCustomizer().customize(config);

        assertThat(config.useSingleServer().getPassword()).isNull();
        assertThat(config.useSingleServer().getUsername()).isNull();
    }

    @Test
    @DisplayName("配置了密码时应原样保留")
    void nonBlankPasswordShouldBeKept() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379").setPassword("secret").setUsername("app");

        configuration.blankRedisPasswordCustomizer().customize(config);

        assertThat(config.useSingleServer().getPassword()).isEqualTo("secret");
        assertThat(config.useSingleServer().getUsername()).isEqualTo("app");
    }

    @Test
    @DisplayName("集群与哨兵模式同样处理，且不改变部署模式")
    void clusterAndSentinelShouldBeHandled() {
        Config cluster = new Config();
        cluster.useClusterServers().addNodeAddress("redis://127.0.0.1:7000").setPassword("");
        configuration.blankRedisPasswordCustomizer().customize(cluster);
        assertThat(cluster.isClusterConfig()).isTrue();
        assertThat(cluster.useClusterServers().getPassword()).isNull();

        Config sentinel = new Config();
        sentinel.useSentinelServers().addSentinelAddress("redis://127.0.0.1:26379").setMasterName("m").setPassword("");
        configuration.blankRedisPasswordCustomizer().customize(sentinel);
        assertThat(sentinel.isSentinelConfig()).isTrue();
        assertThat(sentinel.useSentinelServers().getPassword()).isNull();
    }
}
