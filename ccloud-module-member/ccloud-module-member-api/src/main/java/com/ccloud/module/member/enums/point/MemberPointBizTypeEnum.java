package com.ccloud.module.member.enums.point;

import com.ccloud.framework.common.core.IntArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;

/**
 * 积分业务类型，与字典 member_point_biz_type 一致
 */
@Getter
@AllArgsConstructor
public enum MemberPointBizTypeEnum implements IntArrayValuable {

    SIGN(1, "签到", "签到获得 {} 积分", true),
    ADMIN(2, "管理员修改", "管理员修改 {} 积分", true),
    ORDER_GIVE(10, "订单奖励", "下单获得 {} 积分", true),
    ORDER_GIVE_CANCEL(11, "订单取消", "订单取消，退还 {} 积分", false),
    ORDER_USE(12, "订单积分抵扣", "下单使用 {} 积分", false),
    AFTER_SALE_REFUND_USED(13, "售后退款", "售后退款，退还 {} 积分", true);

    private static final int[] ARRAYS = Arrays.stream(values()).mapToInt(MemberPointBizTypeEnum::getType).toArray();

    private final Integer type;
    private final String name;
    /**
     * 积分记录描述模板，{} 为变动的积分数
     */
    private final String description;
    /**
     * 是否增加积分：否则为扣减
     */
    private final boolean add;

    @Override
    public int[] array() {
        return ARRAYS;
    }

    public static MemberPointBizTypeEnum valueOf(Integer type) {
        return Arrays.stream(values()).filter(e -> Objects.equals(e.type, type)).findFirst().orElse(null);
    }
}
