package com.ccloud.framework.test.core.ut;

import org.junit.jupiter.api.Test;

import java.net.ServerSocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalRedisTest {

    @Test
    void 拒绝清空0号库_不会连接Redis() throws Exception {
        // 用一个不响应的端口：若先连接再判断，会报连接失败而不是这里期望的拒绝
        try (ServerSocket silent = new ServerSocket(0)) {
            assertThatThrownBy(() -> LocalRedis.flushTestDatabase("127.0.0.1", silent.getLocalPort(), "", 0))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("0 号库");
        }
    }

    @Test
    void 测试默认使用非0号库() {
        assertThat(LocalRedis.database()).isNotZero();
    }
}
