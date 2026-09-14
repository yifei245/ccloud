package com.ccloud.module.system.enums.sms;

import com.ccloud.framework.common.core.IntArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;

/**
 * 短信模板类型
 */
@Getter
@AllArgsConstructor
public enum SmsTemplateTypeEnum implements IntArrayValuable {

    VERIFICATION_CODE(1, "验证码"),
    NOTICE(2, "通知"),
    PROMOTION(3, "营销");

    private static final int[] ARRAYS = Arrays.stream(values()).mapToInt(SmsTemplateTypeEnum::getType).toArray();

    private final Integer type;
    private final String name;

    @Override
    public int[] array() {
        return ARRAYS;
    }

    public static SmsTemplateTypeEnum valueOf(Integer value) {
        return Arrays.stream(values()).filter(e -> Objects.equals(e.type, value)).findFirst().orElse(null);
    }
}
