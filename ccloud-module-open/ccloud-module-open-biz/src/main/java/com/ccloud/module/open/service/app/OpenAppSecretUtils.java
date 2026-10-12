package com.ccloud.module.open.service.app;

import java.security.SecureRandom;

/**
 * 应用标识与密钥的生成、掩码。用 SecureRandom：密钥可被预测就等于可被伪造签名
 */
public final class OpenAppSecretUtils {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String LOWER_DIGITS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final String ALNUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private OpenAppSecretUtils() {
    }

    public static String generateAppKey() {
        return random(LOWER_DIGITS, 16);
    }

    public static String generateSecret() {
        return random(ALNUM, 32);
    }

    /**
     * 列表、详情中展示的密钥：前 4 位 + **** + 后 4 位；长度不足 12 位时整体隐藏
     */
    public static String mask(String secret) {
        if (secret == null) {
            return null;
        }
        if (secret.length() < 12) {
            return "****";
        }
        return secret.substring(0, 4) + "****" + secret.substring(secret.length() - 4);
    }

    private static String random(String chars, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
