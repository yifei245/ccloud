package com.ccloud.framework.common.util.net;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class IpWhitelistUtilsTest {

    @Test
    void 空白名单不限制() {
        assertThat(IpWhitelistUtils.matches(null, "1.2.3.4")).isTrue();
        assertThat(IpWhitelistUtils.matches(" ", "1.2.3.4")).isTrue();
    }

    @Test
    void 精确地址与CIDR() {
        String list = "10.0.0.1, 192.168.1.0/24,2001:db8::/32";
        assertThat(IpWhitelistUtils.matches(list, "10.0.0.1")).isTrue();
        assertThat(IpWhitelistUtils.matches(list, "10.0.0.2")).isFalse();
        assertThat(IpWhitelistUtils.matches(list, "192.168.1.200")).isTrue();
        assertThat(IpWhitelistUtils.matches(list, "192.168.2.1")).isFalse();
        assertThat(IpWhitelistUtils.matches(list, "2001:db8:1::5")).isTrue();
        assertThat(IpWhitelistUtils.matches(list, "2001:db9::5")).isFalse();
    }

    @Test
    void 前缀长度不是8的倍数时按位匹配() {
        assertThat(IpWhitelistUtils.matches("10.0.16.0/20", "10.0.31.255")).isTrue();
        assertThat(IpWhitelistUtils.matches("10.0.16.0/20", "10.0.32.0")).isFalse();
        assertThat(IpWhitelistUtils.matches("10.0.16.0/20", "10.0.15.255")).isFalse();
        assertThat(IpWhitelistUtils.matches("2001:db8::/33", "2001:db8:7fff::1")).isTrue();
        assertThat(IpWhitelistUtils.matches("2001:db8::/33", "2001:db8:8000::1")).isFalse();
        assertThat(IpWhitelistUtils.matches("0.0.0.0/0", "8.8.8.8")).isTrue();
    }

    @Test
    void IPv4映射的IPv6地址按IPv4匹配() {
        assertThat(IpWhitelistUtils.matches("10.0.0.1", "::ffff:10.0.0.1")).isTrue();
    }

    @Test
    void 调用方地址不合法时不匹配() {
        assertThat(IpWhitelistUtils.matches("10.0.0.1", "unknown")).isFalse();
        assertThat(IpWhitelistUtils.matches("10.0.0.1", null)).isFalse();
    }

    @Test
    void 格式校验_拒绝域名与错误的前缀长度_不做DNS解析() {
        IpWhitelistUtils.validate("10.0.0.1,192.168.0.0/16,::1");
        assertThatThrownBy(() -> IpWhitelistUtils.validate("example.com"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("example.com");
        assertThatThrownBy(() -> IpWhitelistUtils.validate("10.0.0.0/33"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("10.0.0.0/33");
        assertThatThrownBy(() -> IpWhitelistUtils.validate("10.0.0.1,,10.0.0.2"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 非字面量地址一律拒绝_不走DNS也不接受遗留缩写() {
        for (String bad : new String[]{"abc", "dead.beef", "a.b", "be", "face", "1", "10.1", "1.2.3", "1.2.3.4.5"}) {
            assertThatThrownBy(() -> IpWhitelistUtils.validate(bad))
                    .as(bad).isInstanceOf(IllegalArgumentException.class).hasMessage(bad);
            assertThat(IpWhitelistUtils.matches(bad, "0.0.0.1")).as(bad).isFalse();
            assertThat(IpWhitelistUtils.matches("10.0.0.1", bad)).as(bad).isFalse();
        }
    }

    @Test
    void 校验不做DNS解析_快速失败() {
        assertTimeoutPreemptively(Duration.ofMillis(500), () ->
                assertThatThrownBy(() -> IpWhitelistUtils.validate("abc")).isInstanceOf(IllegalArgumentException.class));
    }

    @Test
    void 带zone的IPv6地址一律拒绝_不抛出非预期异常() {
        assertThat(IpWhitelistUtils.matches("10.0.0.1", "fe80::1%eth0")).isFalse();
        assertThat(IpWhitelistUtils.matches("fe80::1", "fe80::1%1")).isFalse();
        for (String bad : new String[]{"fe80::1%eth0", "fe80::1%1"}) {
            assertThatThrownBy(() -> IpWhitelistUtils.validate(bad))
                    .isInstanceOf(IllegalArgumentException.class).hasMessage(bad);
        }
    }
}
