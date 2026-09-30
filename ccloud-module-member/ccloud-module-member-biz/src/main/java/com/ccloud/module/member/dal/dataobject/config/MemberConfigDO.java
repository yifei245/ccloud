package com.ccloud.module.member.dal.dataobject.config;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 会员配置，每个租户一条。积分抵扣相关配置由商城下单时使用
 */
@TableName("member_config")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class MemberConfigDO extends TenantBaseDO {

    @TableId
    private Long id;
    /**
     * 是否开启积分抵扣
     */
    private Boolean pointTradeDeductEnable;
    /**
     * 1 积分抵扣多少分，单位：分
     */
    private Integer pointTradeDeductUnitPrice;
    /**
     * 单笔订单最多抵扣的积分数，为空表示不限
     */
    private Integer pointTradeDeductMaxPrice;
    /**
     * 每消费 1 元赠送的积分，为空表示不赠送
     */
    private Long pointTradeGivePoint;
}
