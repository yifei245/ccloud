package com.ccloud.module.system.enums.notice;

import com.ccloud.framework.common.core.IntArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 通知公告类型，与字典 system_notice_type 一致
 */
@Getter
@AllArgsConstructor
public enum NoticeTypeEnum implements IntArrayValuable {

    NOTICE(1, "通知"),
    ANNOUNCEMENT(2, "公告");

    private static final int[] ARRAYS = Arrays.stream(values()).mapToInt(NoticeTypeEnum::getType).toArray();

    private final Integer type;
    private final String name;

    @Override
    public int[] array() {
        return ARRAYS;
    }
}
