package com.ccloud.framework.common.exception;

import com.ccloud.framework.common.exception.util.ServiceExceptionUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceExceptionUtilTest {

    @Test
    @DisplayName("exception 应基于 ErrorCode 构建异常并格式化占位符")
    void exception_shouldBuildFromErrorCodeWithParams() {
        ErrorCode ec = new ErrorCode(1_001_000_003, "部门【{}】下存在 {} 个用户");
        ServiceException e = ServiceExceptionUtil.exception(ec, "研发部", 3);
        assertThat(e.getCode()).isEqualTo(1_001_000_003);
        assertThat(e.getMessage()).isEqualTo("部门【研发部】下存在 3 个用户");
    }

    @Test
    @DisplayName("注册的文案覆盖应优先于 ErrorCode 自带文案")
    void exception_registeredMessageShouldOverrideDefault() {
        ErrorCode ec = new ErrorCode(1_001_999_001, "默认文案");
        ServiceExceptionUtil.putMessage(1_001_999_001, "覆盖后的文案");
        try {
            assertThat(ServiceExceptionUtil.exception(ec).getMessage()).isEqualTo("覆盖后的文案");
        } finally {
            ServiceExceptionUtil.clearMessages();
        }
    }

    @Test
    @DisplayName("invalidParamException 应返回 400 错误码")
    void invalidParamException_shouldUseBadRequestCode() {
        ServiceException e = ServiceExceptionUtil.invalidParamException("手机号 {} 不合法", "123");
        assertThat(e.getCode()).isEqualTo(400);
        assertThat(e.getMessage()).isEqualTo("手机号 123 不合法");
    }

    @Test
    @DisplayName("ErrorCode 相同 code 应相等，可安全用作 Map 键")
    void errorCode_shouldBeValueObject() {
        assertThat(new ErrorCode(1, "a")).isEqualTo(new ErrorCode(1, "a"));
        assertThat(new ErrorCode(1, "a")).hasSameHashCodeAs(new ErrorCode(1, "a"));
        assertThat(new ErrorCode(1, "a")).isNotEqualTo(new ErrorCode(2, "a"));
    }
}
