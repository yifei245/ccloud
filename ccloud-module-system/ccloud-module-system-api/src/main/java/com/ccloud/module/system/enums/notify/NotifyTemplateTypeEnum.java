package com.ccloud.module.system.enums.notify;

import com.ccloud.framework.common.core.IntArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 站内信模板类型，与字典 system_notify_template_type 一致
 */
@Getter
@AllArgsConstructor
public enum NotifyTemplateTypeEnum implements IntArrayValuable {

    NOTICE(1, "通知公告"),
    SYSTEM_MESSAGE(2, "系统消息");

    private static final int[] ARRAYS = Arrays.stream(values()).mapToInt(NotifyTemplateTypeEnum::getType).toArray();

    private final Integer type;
    private final String name;

    @Override
    public int[] array() {
        return ARRAYS;
    }
}
