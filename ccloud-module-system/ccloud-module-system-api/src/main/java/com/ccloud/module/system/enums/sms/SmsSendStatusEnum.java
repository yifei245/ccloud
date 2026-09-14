package com.ccloud.module.system.enums.sms;

import com.ccloud.framework.common.core.IntArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;

/**
 * 短信发送状态
 */
@Getter
@AllArgsConstructor
public enum SmsSendStatusEnum implements IntArrayValuable {

    INIT(0, "初始化"),
    SUCCESS(10, "发送成功"),
    FAILURE(20, "发送失败"),
    IGNORE(30, "不发送");

    private static final int[] ARRAYS = Arrays.stream(values()).mapToInt(SmsSendStatusEnum::getStatus).toArray();

    private final Integer status;
    private final String name;

    @Override
    public int[] array() {
        return ARRAYS;
    }

    public static SmsSendStatusEnum valueOf(Integer value) {
        return Arrays.stream(values()).filter(e -> Objects.equals(e.status, value)).findFirst().orElse(null);
    }
}
