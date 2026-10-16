package com.ccloud.module.pay.dal.dataobject.wallet;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 会员钱包。余额只能经 PayWalletMapper#updateBalance 原子变更。按租户隔离
 */
@TableName("pay_wallet")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class PayWalletDO extends TenantBaseDO {

    /**
     * 钱包编号
     */
    @TableId
    private Long id;
    /**
     * 用户编号
     */
    private Long userId;
    /**
     * 用户类型，见 UserTypeEnum
     */
    private Integer userType;
    /**
     * 余额，单位分
     */
    private Integer balance;
    /**
     * 累计支出，单位分
     */
    private Integer totalExpense;
    /**
     * 累计充值，单位分
     */
    private Integer totalRecharge;
    /**
     * 冻结金额，单位分
     */
    private Integer freezePrice;
}
