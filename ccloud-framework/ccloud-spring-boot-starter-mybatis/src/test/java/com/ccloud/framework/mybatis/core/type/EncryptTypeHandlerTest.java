package com.ccloud.framework.mybatis.core.type;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncryptTypeHandlerTest {

    private static final String KEY = "ccloud1234567890";

    @BeforeEach
    void setUp() {
        EncryptTypeHandler.setEncryptKey(KEY);
    }

    @AfterEach
    void tearDown() {
        // 密钥是静态状态，恢复原值，避免影响同一 JVM 中的其他测试
        EncryptTypeHandler.setEncryptKey(KEY);
    }

    @Test
    @DisplayName("加密后再解密应还原为原文")
    void encryptThenDecrypt_shouldRoundTrip() {
        String plain = "13800138000";
        String cipher = EncryptTypeHandler.encrypt(plain);
        assertThat(cipher).isNotEqualTo(plain);
        assertThat(EncryptTypeHandler.decrypt(cipher)).isEqualTo(plain);
    }

    @Test
    @DisplayName("同一明文两次加密结果必须一致，否则加密列无法做等值查询")
    void encrypt_shouldBeDeterministic() {
        assertThat(EncryptTypeHandler.encrypt("abc")).isEqualTo(EncryptTypeHandler.encrypt("abc"));
    }

    @Test
    @DisplayName("不同明文的密文应不同")
    void encrypt_shouldDifferForDifferentInput() {
        assertThat(EncryptTypeHandler.encrypt("abc")).isNotEqualTo(EncryptTypeHandler.encrypt("abd"));
    }

    @Test
    @DisplayName("null 应原样返回，不参与加解密")
    void shouldPassThroughNull() {
        assertThat(EncryptTypeHandler.encrypt(null)).isNull();
        assertThat(EncryptTypeHandler.decrypt(null)).isNull();
    }

    @Test
    @DisplayName("未配置密钥时应给出明确的错误提示，而不是抛出难以理解的底层异常")
    void shouldFailFastWithClearMessageWhenKeyMissing() {
        EncryptTypeHandler.setEncryptKey(null);
        assertThatThrownBy(() -> EncryptTypeHandler.encrypt("abc"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ccloud.mybatis.encrypt-key");
    }

    @Test
    @DisplayName("用另一个密钥加密的密文无法解密时，应提示加密密钥与数据不一致")
    void decrypt_shouldFailWithClearMessageWhenKeyMismatch() {
        EncryptTypeHandler.setEncryptKey("other-key-123456");
        String cipherOfOtherKey = EncryptTypeHandler.encrypt("ccloud-open-demo-secret");
        EncryptTypeHandler.setEncryptKey(KEY);

        assertThatThrownBy(() -> EncryptTypeHandler.decrypt(cipherOfOtherKey))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("加密密钥与数据不一致")
                .hasMessageContaining("CCLOUD_ENCRYPT_KEY")
                .hasCauseInstanceOf(RuntimeException.class);
    }
}
