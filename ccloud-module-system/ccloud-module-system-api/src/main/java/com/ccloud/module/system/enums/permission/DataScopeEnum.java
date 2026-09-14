package com.ccloud.module.system.enums.permission;

import com.ccloud.framework.common.core.IntArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;

/**
 * 数据范围。用户有多个角色时取其中最宽的一个：全部 > 指定部门 > 本部门及以下 > 本部门 > 仅本人
 */
@Getter
@AllArgsConstructor
public enum DataScopeEnum implements IntArrayValuable {

    ALL(1, "全部数据权限"),
    DEPT_CUSTOM(2, "指定部门数据权限"),
    DEPT_ONLY(3, "本部门数据权限"),
    DEPT_AND_CHILD(4, "本部门及以下数据权限"),
    SELF(5, "仅本人数据权限");

    private static final int[] ARRAYS = Arrays.stream(values()).mapToInt(DataScopeEnum::getScope).toArray();

    private final Integer scope;
    private final String name;

    @Override
    public int[] array() {
        return ARRAYS;
    }

    public static DataScopeEnum valueOf(Integer value) {
        return Arrays.stream(values()).filter(e -> Objects.equals(e.scope, value)).findFirst().orElse(null);
    }
}
