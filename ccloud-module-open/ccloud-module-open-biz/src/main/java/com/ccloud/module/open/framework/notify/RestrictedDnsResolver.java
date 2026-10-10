package com.ccloud.module.open.framework.notify;

import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.SystemDefaultDnsResolver;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 只放行公网地址的 DNS 解析器。解析结果即连接地址，校验与连接用的是同一组地址，DNS 重绑定无法绕过。
 * 任何一个解析结果受限即整体拒绝：只挑合法地址连接，攻击者仍可让部分请求打到内网
 */
public class RestrictedDnsResolver implements DnsResolver {

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
        InetAddress target = embeddedIpv4(address);
        if (isMetadata(target) || (!allowPrivate && isPrivate(target))) {
            throw new UnknownHostException("回调地址解析到受限地址：" + address.getHostAddress());
        }
    }

    /**
     * 内嵌 IPv4 的 IPv6 地址（::a.b.c.d IPv4 兼容地址、64:ff9b::a.b.c.d NAT64 地址）按内嵌的 IPv4 判断，
     * 否则 [::7f00:1]、[64:ff9b::a9fe:a9fe] 这类写法可绕过回环与元数据地址的拦截。
     * IPv4 映射地址（::ffff:a.b.c.d）由 JDK 直接解析为 IPv4，无需处理
     */
    private static InetAddress embeddedIpv4(InetAddress address) throws UnknownHostException {
        byte[] b = address.getAddress();
        if (b.length != 16) {
            return address;
        }
        boolean compatible = true;
        for (int i = 0; i < 12; i++) {
            compatible &= b[i] == 0;
        }
        boolean nat64 = b[0] == 0 && b[1] == 0x64 && (b[2] & 0xFF) == 0xFF && (b[3] & 0xFF) == 0x9B;
        for (int i = 4; nat64 && i < 12; i++) {
            nat64 = b[i] == 0;
        }
        return compatible || nat64 ? InetAddress.getByAddress(new byte[]{b[12], b[13], b[14], b[15]}) : address;
    }

    private static boolean isMetadata(InetAddress address) {
        String ip = address.getHostAddress();
        return "169.254.169.254".equals(ip) || ip.startsWith("fd00:ec2:0:0:0:0:0:254");
    }

    private static boolean isPrivate(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) {
            return true;
        }
        byte[] b = address.getAddress();
        if (b.length == 4) {
            int first = b[0] & 0xFF;
            int second = b[1] & 0xFF;
            return (first == 100 && second >= 64 && second <= 127)   // 100.64.0.0/10 运营商级 NAT
                    || first == 0 || first >= 240;                   // 0.0.0.0/8、保留地址
        }
        return (b[0] & 0xFE) == 0xFC;                                // fc00::/7 唯一本地地址
    }
}
