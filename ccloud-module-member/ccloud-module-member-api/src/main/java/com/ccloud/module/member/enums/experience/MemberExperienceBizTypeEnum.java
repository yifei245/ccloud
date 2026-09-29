package com.ccloud.module.member.enums.experience;

import com.ccloud.framework.common.core.IntArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;

/**
 * 经验业务类型，与字典 member_experience_biz_type 一致
 */
@Getter
@AllArgsConstructor
public enum MemberExperienceBizTypeEnum implements IntArrayValuable {

    ADMIN(0, "管理员调整", "管理员调整获得 {} 经验", true),
    INVITE_REGISTER(1, "邀新奖励", "邀请好友获得 {} 经验", true),
    ORDER_GIVE(2, "下单奖励", "下单获得 {} 经验", true),
    ORDER_GIVE_CANCEL(3, "退单扣除", "退单扣除 {} 经验", false),
    SIGN_IN(4, "签到奖励", "签到获得 {} 经验", true),
    LOTTERY(5, "抽奖奖励", "抽奖获得 {} 经验", true);

    private static final int[] ARRAYS = Arrays.stream(values()).mapToInt(MemberExperienceBizTypeEnum::getType).toArray();

    private final Integer type;
    private final String name;
    /**
     * 经验记录描述模板，{} 为变动的经验值
     */
    private final String description;
    /**
     * 是否增加经验：否则为扣减
     */
    private final boolean add;

    @Override
    public int[] array() {
        return ARRAYS;
    }

    public static MemberExperienceBizTypeEnum valueOf(Integer type) {
        return Arrays.stream(values()).filter(e -> Objects.equals(e.type, type)).findFirst().orElse(null);
    }
}
