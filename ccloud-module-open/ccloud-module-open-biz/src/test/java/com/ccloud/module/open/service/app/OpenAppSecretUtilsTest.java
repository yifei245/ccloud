package com.ccloud.module.open.service.app;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAppSecretUtilsTest {

    @Test
    void appKey为16位小写字母数字_密钥为32位字母数字_每次不同() {
        assertThat(OpenAppSecretUtils.generateAppKey()).matches("[a-z0-9]{16}");
        assertThat(OpenAppSecretUtils.generateSecret()).matches("[A-Za-z0-9]{32}");
        assertThat(OpenAppSecretUtils.generateSecret()).isNotEqualTo(OpenAppSecretUtils.generateSecret());
    }

    @Test
    void 掩码保留前后各4位() {
        assertThat(OpenAppSecretUtils.mask("abcd1234efgh5678ijkl9012mnop3456")).isEqualTo("abcd****3456");
        assertThat(OpenAppSecretUtils.mask("short")).isEqualTo("****");
        assertThat(OpenAppSecretUtils.mask(null)).isNull();
    }
}
