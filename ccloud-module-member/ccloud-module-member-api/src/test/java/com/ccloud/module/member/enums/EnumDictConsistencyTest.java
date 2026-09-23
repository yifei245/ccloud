package com.ccloud.module.member.enums;

import com.ccloud.module.member.enums.experience.MemberExperienceBizTypeEnum;
import com.ccloud.module.member.enums.point.MemberPointBizTypeEnum;
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
    @DisplayName("积分业务类型与字典 member_point_biz_type 一致")
    void pointBizType() {
        assertThat(values(MemberPointBizTypeEnum.class, MemberPointBizTypeEnum::getType))
                .isEqualTo(new TreeSet<>(SqlFixture.dictValues("member_point_biz_type")));
    }

    @Test
    @DisplayName("经验业务类型与字典 member_experience_biz_type 一致")
    void experienceBizType() {
        assertThat(values(MemberExperienceBizTypeEnum.class, MemberExperienceBizTypeEnum::getType))
                .isEqualTo(new TreeSet<>(SqlFixture.dictValues("member_experience_biz_type")));
    }
}
