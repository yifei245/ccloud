package com.ccloud.module.system.enums.mail;

import com.ccloud.framework.common.core.IntArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 邮件发送状态
 */
@Getter
@AllArgsConstructor
public enum MailSendStatusEnum implements IntArrayValuable {

    INIT(0, "初始化"),
    SUCCESS(10, "发送成功"),
    FAILURE(20, "发送失败"),
    IGNORE(30, "不发送");

    private static final int[] ARRAYS = Arrays.stream(values()).mapToInt(MailSendStatusEnum::getStatus).toArray();

    private final Integer status;
    private final String name;

    @Override
    public int[] array() {
        return ARRAYS;
    }
}
