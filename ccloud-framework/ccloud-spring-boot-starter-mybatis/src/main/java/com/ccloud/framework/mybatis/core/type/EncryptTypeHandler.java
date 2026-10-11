package com.ccloud.framework.mybatis.core.type;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.symmetric.AES;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.nio.charset.StandardCharsets;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 数据库字段加密处理器。
 *
 * <p>用法：{@code @TableField(typeHandler = EncryptTypeHandler.class)}。
 *
 * <p>刻意采用 AES-ECB：同一明文必须产生同一密文，否则加密列无法做等值查询
 * （按手机号查用户就不可能实现）。代价是失去语义安全性——相同明文的密文可被识别。
 * 因此只用于"需要可检索的敏感字段"，密码一律走 BCrypt 单向哈希，不用本处理器。
 */
public class EncryptTypeHandler extends BaseTypeHandler<String> {

    private static volatile AES aes;
    private static volatile String encryptKey;

    /**
     * 由自动配置在启动时注入密钥
     */
    public static void setEncryptKey(String key) {
        encryptKey = key;
        aes = null;
    }

    private static AES getAes() {
        if (StrUtil.isBlank(encryptKey)) {
            throw new IllegalStateException(
                    "数据库字段加密密钥未配置，请设置 ccloud.mybatis.encrypt-key（16/24/32 位字符串）");
        }
        if (aes == null) {
            synchronized (EncryptTypeHandler.class) {
                if (aes == null) {
                    aes = cn.hutool.crypto.SecureUtil.aes(encryptKey.getBytes(StandardCharsets.UTF_8));
                }
            }
        }
        return aes;
    }

    public static String encrypt(String plain) {
        return plain == null ? null : getAes().encryptHex(plain);
    }

    public static String decrypt(String cipher) {
        if (cipher == null) {
            return null;
        }
        AES current = getAes();
        try {
            return current.decryptStr(cipher);
        } catch (RuntimeException e) {
            // 最常见的原因：库里的密文是用另一个密钥加密的（例如导入了用开发默认密钥加密的初始化数据后更换了密钥）
            throw new IllegalStateException(
                    "数据库加密字段解密失败：加密密钥与数据不一致（ccloud.mybatis.encrypt-key / CCLOUD_ENCRYPT_KEY）", e);
        }
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, encrypt(parameter));
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return decrypt(rs.getString(columnName));
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return decrypt(rs.getString(columnIndex));
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return decrypt(cs.getString(columnIndex));
    }
}
