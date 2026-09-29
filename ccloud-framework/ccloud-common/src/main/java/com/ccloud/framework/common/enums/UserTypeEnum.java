package com.ccloud.framework.common.enums;

import com.ccloud.framework.common.core.IntArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;

/**
 * 用户类型。ccloud 把 C 端会员与管理端用户放在两套独立体系里，
 * 同一手机号在两边是互不相干的两条记录，令牌也按该字段隔离。
 */
@Getter
@AllArgsConstructor
public enum UserTypeEnum implements IntArrayValuable {

    MEMBER(1, "会员"),
    ADMIN(2, "管理员");

    public static final String DICT_TYPE = "user_type";

    private static final int[] ARRAYS = Arrays.stream(values()).mapToInt(UserTypeEnum::getValue).toArray();

    private final Integer value;
    private final String name;

    @Override
    public int[] array() {
        return ARRAYS;
    }

    public static UserTypeEnum valueOf(Integer value) {
        return Arrays.stream(values())
                .filter(e -> Objects.equals(e.getValue(), value))
                .findFirst()
                .orElse(null);
    }

    public static boolean isMember(Integer value) {
        return Objects.equals(MEMBER.value, value);
    }

    public static boolean isAdmin(Integer value) {
        return Objects.equals(ADMIN.value, value);
    }
}
