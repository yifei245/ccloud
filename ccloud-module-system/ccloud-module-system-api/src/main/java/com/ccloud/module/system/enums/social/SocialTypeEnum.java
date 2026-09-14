package com.ccloud.module.system.enums.social;

import com.ccloud.framework.common.core.IntArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;

/**
 * 社交平台类型
 */
@Getter
@AllArgsConstructor
public enum SocialTypeEnum implements IntArrayValuable {

    DINGTALK(20, "钉钉", "DINGTALK"),
    WECHAT_ENTERPRISE(30, "企业微信", "WECHAT_ENTERPRISE"),
    WECHAT_MP(31, "微信公众号", "WECHAT_MP"),
    WECHAT_OPEN(32, "微信开放平台", "WECHAT_OPEN"),
    WECHAT_MINI_PROGRAM(34, "微信小程序", "WECHAT_MINI_PROGRAM");

    private static final int[] ARRAYS = Arrays.stream(values()).mapToInt(SocialTypeEnum::getType).toArray();

    private final Integer type;
    private final String name;

    /**
     * JustAuth 中的平台标识
     */
    private final String source;

    @Override
    public int[] array() {
        return ARRAYS;
    }

    public static SocialTypeEnum valueOf(Integer value) {
        return Arrays.stream(values()).filter(e -> Objects.equals(e.type, value)).findFirst().orElse(null);
    }
}
