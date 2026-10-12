package com.ccloud.module.open.framework.notify;

import org.junit.jupiter.api.Test;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 直接校验解析结果。IPv6 地址用 {@link Inet6Address#getByAddress(String, byte[], int)} 构造：
 * 系统解析器对 AAAA 记录返回的就是这种对象，::ffff:a.b.c.d 也不会被 JDK 转成 IPv4
 */
class RestrictedDnsResolverTest {

    /**
     * 不允许内网时拒绝：回环、私有、运营商级 NAT、组播，以及内嵌这些 IPv4 的各类 IPv6 写法
     */
    private static final List<String> PRIVATE = List.of(
            "7f000001",                                 // 127.0.0.1
            "0a000001",                                 // 10.0.0.1
            "ac100001",                                 // 172.16.0.1
            "c0a80101",                                 // 192.168.1.1
            "64400001",                                 // 100.64.0.1
            "00000000",                                 // 0.0.0.0
            "e0000001",                                 // 224.0.0.1
            "00000000000000000000000000000001",         // ::1
            "00000000000000000000000000000000",         // ::
            "00000000000000000000ffff7f000001",         // ::ffff:127.0.0.1（IPv4 映射）
            "00000000000000000000ffff0a000001",         // ::ffff:10.0.0.1
            "0000000000000000ffff00007f000001",         // ::ffff:0:127.0.0.1（SIIT）
            "0000000000000000000000007f000001",         // ::127.0.0.1（IPv4 兼容）
            "0064ff9b00000000000000000a000001",         // 64:ff9b::10.0.0.1（NAT64）
            "20027f00000100000000000000000001",         // 2002:7f00:1::1（6to4 127.0.0.1）
            "fe800000000000000000000000000001",         // fe80::1
            "fc000000000000000000000000000001",         // fc00::1
            "ff020000000000000000000000000001");        // ff02::1

    /**
     * 任何配置下都拒绝：云元数据地址（含内嵌写法）与无法取出目标的转换前缀
     */
    private static final List<String> ALWAYS = List.of(
            "a9fea9fe",                                 // 169.254.169.254
            "a9fe0017",                                 // 169.254.0.23 腾讯云
            "a9feaa02",                                 // 169.254.170.2 AWS ECS
            "a9fe0001",                                 // 169.254.0.1 链路本地
            "646464c8",                                 // 100.100.100.200 阿里云
            "00000000000000000000ffffa9fea9fe",         // ::ffff:169.254.169.254
            "00000000000000000000ffff646464c8",         // ::ffff:100.100.100.200
            "0000000000000000ffff0000a9fea9fe",         // ::ffff:0:169.254.169.254（SIIT）
            "000000000000000000000000a9fea9fe",         // ::169.254.169.254（IPv4 兼容）
            "0064ff9b0000000000000000a9fea9fe",         // 64:ff9b::169.254.169.254
            "2002a9fea9fe00000000000000000001",         // 2002:a9fe:a9fe::1（6to4）
            "fd000ec2000000000000000000000254",         // fd00:ec2::254
            "0064ff9b000100000000000000000001",         // 64:ff9b:1::1（本地 NAT64）
            "0064ff9b00010a0000000000000a0001",         // 64:ff9b:1:a00::a:1
            "20010000000000000000000080ffffff");        // 2001:0::…（Teredo）

    private static final List<String> PUBLIC = List.of(
            "08080808",                                 // 8.8.8.8
            "00000000000000000000ffff08080808",         // ::ffff:8.8.8.8
            "0064ff9b000000000000000008080808",         // 64:ff9b::8.8.8.8
            "20020808080800000000000000000001",         // 2002:808:808::1
            "26064700000000000000000000001111");        // 2606:4700::1111

    @Test
    void 不允许内网时拒绝内网地址及其内嵌写法() {
        for (String hex : PRIVATE) {
            assertRejected(hex, false);
        }
    }

    @Test
    void 元数据地址与不可判断的转换前缀在任何配置下都拒绝() {
        for (String hex : ALWAYS) {
            assertRejected(hex, false);
            assertRejected(hex, true);
        }
    }

    @Test
    void 允许内网时放行内网地址() {
        for (String hex : PRIVATE) {
            assertThatCode(() -> RestrictedDnsResolver.check(address(hex), true)).as(hex).doesNotThrowAnyException();
        }
    }

    @Test
    void 公网地址放行() {
        for (String hex : PUBLIC) {
            assertThatCode(() -> RestrictedDnsResolver.check(address(hex), false)).as(hex).doesNotThrowAnyException();
        }
    }

    @Test
    void 构造的IPv4映射地址确为IPv6对象() throws UnknownHostException {
        assertThat(address("00000000000000000000ffff7f000001")).isInstanceOf(Inet6Address.class);
    }

    private static void assertRejected(String hex, boolean allowPrivate) {
        assertThatThrownBy(() -> RestrictedDnsResolver.check(address(hex), allowPrivate))
                .as("%s allowPrivate=%s", hex, allowPrivate)
                .isInstanceOf(UnknownHostException.class)
                .hasMessageContaining("受限地址");
    }

    private static InetAddress address(String hex) throws UnknownHostException {
        byte[] bytes = HexFormat.of().parseHex(hex);
        return bytes.length == 4 ? InetAddress.getByAddress(bytes) : Inet6Address.getByAddress(null, bytes, -1);
    }
}
