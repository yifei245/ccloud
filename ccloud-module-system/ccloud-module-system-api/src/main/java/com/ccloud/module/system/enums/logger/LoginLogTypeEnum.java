package com.ccloud.module.system.enums.logger;

import com.ccloud.framework.common.core.IntArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;

/**
 * 登录日志类型
 */
@Getter
@AllArgsConstructor
public enum LoginLogTypeEnum implements IntArrayValuable {

    LOGIN_USERNAME(100, "账号登录"),
    LOGIN_SOCIAL(101, "社交登录"),
    LOGIN_MOBILE(103, "短信登录"),
    LOGOUT_SELF(200, "主动登出"),
    LOGOUT_DELETE(202, "强制登出");

    private static final int[] ARRAYS = Arrays.stream(values()).mapToInt(LoginLogTypeEnum::getType).toArray();

    private final Integer type;
    private final String name;

    @Override
    public int[] array() {
        return ARRAYS;
    }

    public static LoginLogTypeEnum valueOf(Integer value) {
        return Arrays.stream(values()).filter(e -> Objects.equals(e.type, value)).findFirst().orElse(null);
    }
}
