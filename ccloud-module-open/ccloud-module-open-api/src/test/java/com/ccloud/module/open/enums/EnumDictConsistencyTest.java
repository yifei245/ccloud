package com.ccloud.module.open.enums;

import com.ccloud.module.open.enums.notify.OpenNotifyStatusEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 枚举值须与字典初始化数据一致：后台展示、导出按字典翻译，枚举多一个值就会显示成数字
 */
class EnumDictConsistencyTest {

    private static <E extends Enum<E>> Set<String> values(Class<E> type, Function<E, Object> getter) {
        return Arrays.stream(type.getEnumConstants()).map(getter).map(String::valueOf)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Test
    @DisplayName("回调状态与字典 open_notify_status 一致")
    void notifyStatus() {
        assertThat(values(OpenNotifyStatusEnum.class, OpenNotifyStatusEnum::getStatus))
                .isNotEmpty()
                .isEqualTo(new TreeSet<>(SqlFixture.dictValues(OpenNotifyStatusEnum.DICT_TYPE)));
    }
}
