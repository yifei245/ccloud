package com.ccloud.module.system.enums.logger;

import com.ccloud.framework.common.core.IntArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;

/**
 * 登录结果
 */
@Getter
@AllArgsConstructor
public enum LoginResultEnum implements IntArrayValuable {

    SUCCESS(0, "成功"),
    BAD_CREDENTIALS(10, "账号或密码不正确"),
    USER_DISABLED(20, "用户被禁用"),
    CAPTCHA_NOT_FOUND(30, "验证码不存在"),
    CAPTCHA_CODE_ERROR(31, "验证码不正确"),
    UNKNOWN_ERROR(100, "未知异常");

    private static final int[] ARRAYS = Arrays.stream(values()).mapToInt(LoginResultEnum::getResult).toArray();

    private final Integer result;
    private final String name;

    @Override
    public int[] array() {
        return ARRAYS;
    }

    public static LoginResultEnum valueOf(Integer value) {
        return Arrays.stream(values()).filter(e -> Objects.equals(e.result, value)).findFirst().orElse(null);
    }
}
