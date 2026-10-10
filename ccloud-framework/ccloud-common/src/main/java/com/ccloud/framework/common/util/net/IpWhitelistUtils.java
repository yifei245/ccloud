package com.ccloud.framework.common.util.net;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

/**
 * IP 白名单：逗号分隔的 IPv4 / IPv6 地址或 CIDR。只接受字面量地址，绝不做 DNS 解析
 * （域名白名单会让匹配结果随 DNS 变化，解析本身也会阻塞网关的事件循环线程）
 */
public final class IpWhitelistUtils {

    private static final Pattern LITERAL = Pattern.compile("[0-9a-fA-F:.]+");

    private IpWhitelistUtils() {
    }

    public static void validate(String whitelist) {
        if (whitelist == null || whitelist.isBlank()) {
            return;
        }
        for (String entry : whitelist.split(",")) {
            String trimmed = entry.trim();
            if (parse(trimmed) == null) {
                throw new IllegalArgumentException(trimmed);
            }
        }
    }

    public static boolean matches(String whitelist, String ip) {
        if (whitelist == null || whitelist.isBlank()) {
            return true;
        }
        byte[] target = toBytes(ip);
        if (target == null) {
            return false;
        }
        for (String entry : whitelist.split(",")) {
            Range range = parse(entry.trim());
            if (range != null && range.contains(target)) {
                return true;
            }
        }
        return false;
    }

    private record Range(byte[] network, int prefix) {
        boolean contains(byte[] address) {
            if (address.length != network.length) {
                return false;
            }
            int full = prefix / 8;
            for (int i = 0; i < full; i++) {
                if (address[i] != network[i]) {
                    return false;
                }
            }
            int rest = prefix % 8;
            if (rest == 0) {
                return true;
            }
            int mask = (0xFF << (8 - rest)) & 0xFF;
            return (address[full] & mask) == (network[full] & mask);
        }
    }

    private static Range parse(String entry) {
        if (entry.isEmpty()) {
            return null;
        }
        String[] parts = entry.split("/", -1);
        if (parts.length > 2) {
            return null;
        }
        byte[] network = toBytes(parts[0]);
        if (network == null) {
            return null;
        }
        int max = network.length * 8;
        int prefix = max;
        if (parts.length == 2) {
            try {
                prefix = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                return null;
            }
            if (prefix < 0 || prefix > max) {
                return null;
            }
        }
        return new Range(network, prefix);
    }

    /**
     * 字面量地址转字节；IPv4 映射的 IPv6 地址（::ffff:a.b.c.d）按 IPv4 处理
     */
    private static byte[] toBytes(String ip) {
        if (ip == null || !LITERAL.matcher(ip).matches()) {
            return null;
        }
        try {
            InetAddress address = InetAddress.getByName(ip);   // 已限定为字面量，不会触发 DNS
            byte[] bytes = address.getAddress();
            if (address instanceof Inet6Address && isIpv4Mapped(bytes)) {
                return new byte[]{bytes[12], bytes[13], bytes[14], bytes[15]};
            }
            return bytes;
        } catch (UnknownHostException e) {
            return null;
        }
    }

    private static boolean isIpv4Mapped(byte[] b) {
        for (int i = 0; i < 10; i++) {
            if (b[i] != 0) {
                return false;
            }
        }
        return b[10] == (byte) 0xFF && b[11] == (byte) 0xFF;
    }
}
