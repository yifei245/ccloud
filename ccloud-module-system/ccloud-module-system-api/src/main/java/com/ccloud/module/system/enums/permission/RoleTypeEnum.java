package com.ccloud.module.system.enums.permission;

import com.ccloud.framework.common.core.IntArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;

/**
 * 角色类型。内置角色不可删除、不可修改标识
 */
@Getter
@AllArgsConstructor
public enum RoleTypeEnum implements IntArrayValuable {

    SYSTEM(1, "内置"),
    CUSTOM(2, "自定义");

    private static final int[] ARRAYS = Arrays.stream(values()).mapToInt(RoleTypeEnum::getType).toArray();

    private final Integer type;
    private final String name;

    @Override
    public int[] array() {
        return ARRAYS;
    }

    public static RoleTypeEnum valueOf(Integer value) {
        return Arrays.stream(values()).filter(e -> Objects.equals(e.type, value)).findFirst().orElse(null);
    }
}
