package com.ccloud.framework.common.util.crypto;

import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;

import java.nio.charset.StandardCharsets;

/**
 * AES 加解密。用于数据库敏感字段（手机号、密钥等）的落库加密。
 *
 * <p>刻意使用 ECB 模式：同一明文必须产生同一密文，否则加密列无法做等值查询。
 * 代价是丢失语义安全性——因此仅用于"需要可查询的敏感字段"，
 * 不用于密码（密码走 BCrypt 单向哈希）。
 */
public class AesUtils {

    /**
     * 用 16 位密钥构建 AES 实例
     *
     * @param key 密钥，长度必须为 16 / 24 / 32 字节
     */
    public static AES build(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("AES 密钥不能为空，请配置 ccloud.mybatis.encrypt-key");
        }
        return SecureUtil.aes(key.getBytes(StandardCharsets.UTF_8));
    }

    public static String encrypt(String plain, String key) {
        return plain == null ? null : build(key).encryptHex(plain);
    }

    public static String decrypt(String cipher, String key) {
        return cipher == null ? null : build(key).decryptStr(cipher);
    }

    /**
     * 生成一个随机的 16 字节密钥（32 位 hex），用于初始化配置
     */
    public static String generateKey() {
        return HexUtil.encodeHexStr(SecureUtil.generateKey("AES", 128).getEncoded());
    }
}
