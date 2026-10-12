package com.ccloud.module.open.framework.notify;

import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.SystemDefaultDnsResolver;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * 只放行公网地址的 DNS 解析器。解析结果即连接地址，校验与连接用的是同一组地址，DNS 重绑定无法绕过。
 * 任何一个解析结果受限即整体拒绝：只挑合法地址连接，攻击者仍可让部分请求打到内网
 */
public class RestrictedDnsResolver implements DnsResolver {

    /**
     * AWS 的 IPv6 元数据地址 fd00:ec2::254
     */
    private static final byte[] AWS_METADATA_V6 = {(byte) 0xFD, 0, 0x0E, (byte) 0xC2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0x02, 0x54};

    private final boolean allowPrivate;

    public RestrictedDnsResolver(boolean allowPrivate) {
        this.allowPrivate = allowPrivate;
    }

    @Override
    public InetAddress[] resolve(String host) throws UnknownHostException {
        InetAddress[] addresses = SystemDefaultDnsResolver.INSTANCE.resolve(host);
        for (InetAddress address : addresses) {
            check(address, allowPrivate);
        }
        return addresses;
    }

    @Override
    public String resolveCanonicalHostname(String host) throws UnknownHostException {
        return SystemDefaultDnsResolver.INSTANCE.resolveCanonicalHostname(host);
    }

    static void check(InetAddress address, boolean allowPrivate) throws UnknownHostException {
        if (isRestricted(address.getAddress(), allowPrivate)) {
            throw new UnknownHostException("回调地址解析到受限地址：" + address.getHostAddress());
        }
    }

    private static boolean isRestricted(byte[] b, boolean allowPrivate) {
        if (b.length == 4) {
            return isMetadataV4(b) || (!allowPrivate && isPrivateV4(b));
        }
        // 无法可靠取出目标 IPv4 的转换前缀，任何配置下都拒绝：
        // Teredo（2001:0::/32）客户端地址经混淆且隧道已基本停用；本地 NAT64（64:ff9b:1::/48）的内嵌位置随运营方前缀长度而变
        if (startsWith(b, 0x20, 0x01, 0x00, 0x00) || startsWith(b, 0x00, 0x64, 0xFF, 0x9B, 0x00, 0x01)) {
            return true;
        }
        byte[] v4 = embeddedIpv4(b);
        if (v4 != null) {
            return isRestricted(v4, allowPrivate);
        }
        return Arrays.equals(b, AWS_METADATA_V6) || (!allowPrivate && isPrivateV6(b));
    }

    /**
     * 取出内嵌在 IPv6 地址中的 IPv4，系统解析器对 AAAA 记录返回的这类地址最终会连到该 IPv4：
     * ::ffff:0:0/96（IPv4 映射）、::ffff:0:0:0/96（SIIT）、::/96（IPv4 兼容，含 :: 与 ::1）、
     * 64:ff9b::/96（NAT64 知名前缀）、2002::/16（6to4，字节 2-5）。不是这些前缀时返回 null
     */
    private static byte[] embeddedIpv4(byte[] b) {
        if (allZero(b, 0, 10) && (b[10] & b[11] & 0xFF) == 0xFF) {
            return Arrays.copyOfRange(b, 12, 16);
        }
        if (allZero(b, 0, 8) && (b[8] & b[9] & 0xFF) == 0xFF && allZero(b, 10, 12)) {
            return Arrays.copyOfRange(b, 12, 16);
        }
        if (allZero(b, 0, 12)) {
            return Arrays.copyOfRange(b, 12, 16);
        }
        if (startsWith(b, 0x00, 0x64, 0xFF, 0x9B) && allZero(b, 4, 12)) {
            return Arrays.copyOfRange(b, 12, 16);
        }
        if (startsWith(b, 0x20, 0x02)) {
            return Arrays.copyOfRange(b, 2, 6);
        }
        return null;
    }

    /**
     * 云厂商实例元数据地址，任何配置下都拒绝：整个 169.254.0.0/16（AWS 169.254.169.254、ECS 169.254.170.2、
     * 腾讯云 169.254.0.23 等）与阿里云 100.100.100.200
     */
    private static boolean isMetadataV4(byte[] b) {
        int first = b[0] & 0xFF;
        int second = b[1] & 0xFF;
        return (first == 169 && second == 254)
                || (first == 100 && second == 100 && (b[2] & 0xFF) == 100 && (b[3] & 0xFF) == 200);
    }

    private static boolean isPrivateV4(byte[] b) {
        int first = b[0] & 0xFF;
        int second = b[1] & 0xFF;
        return first == 0 || first == 10 || first == 127                        // 本网、私有、回环
                || (first == 100 && second >= 64 && second <= 127)              // 100.64.0.0/10 运营商级 NAT
                || (first == 172 && second >= 16 && second <= 31)               // 172.16.0.0/12
                || (first == 192 && second == 168)                              // 192.168.0.0/16
                || first >= 224;                                                // 组播、保留、广播
    }

    private static boolean isPrivateV6(byte[] b) {
        int first = b[0] & 0xFF;
        return first == 0xFF                                                    // ff00::/8 组播
                || (first & 0xFE) == 0xFC                                       // fc00::/7 唯一本地地址
                || (first == 0xFE && (b[1] & 0xC0) == 0x80)                     // fe80::/10 链路本地
                || (first == 0xFE && (b[1] & 0xC0) == 0xC0);                    // fec0::/10 站点本地（已废弃）
    }

    private static boolean allZero(byte[] b, int from, int to) {
        for (int i = from; i < to; i++) {
            if (b[i] != 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean startsWith(byte[] b, int... prefix) {
        for (int i = 0; i < prefix.length; i++) {
            if ((b[i] & 0xFF) != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
