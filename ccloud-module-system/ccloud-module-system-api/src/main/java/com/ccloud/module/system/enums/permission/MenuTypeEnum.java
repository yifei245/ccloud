package com.ccloud.module.system.enums.permission;

import com.ccloud.framework.common.core.IntArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;

/**
 * 菜单类型
 */
@Getter
@AllArgsConstructor
public enum MenuTypeEnum implements IntArrayValuable {

    DIR(1, "目录"),
    MENU(2, "菜单"),
    BUTTON(3, "按钮");

    private static final int[] ARRAYS = Arrays.stream(values()).mapToInt(MenuTypeEnum::getType).toArray();

    private final Integer type;
    private final String name;

    @Override
    public int[] array() {
        return ARRAYS;
    }

    public static MenuTypeEnum valueOf(Integer value) {
        return Arrays.stream(values()).filter(e -> Objects.equals(e.type, value)).findFirst().orElse(null);
    }
}
