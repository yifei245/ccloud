package com.ccloud.module.pay.dal.dataobject.wallet;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 钱包充值套餐。按租户隔离
 */
@TableName("pay_wallet_recharge_package")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class PayWalletRechargePackageDO extends TenantBaseDO {

    /**
     * 套餐编号
     */
    @TableId
    private Long id;
    /**
     * 套餐名
     */
    private String name;
    /**
     * 支付金额，单位分
     */
    private Integer payPrice;
    /**
     * 赠送金额，单位分
     */
    private Integer bonusPrice;
    /**
     * 状态，见 CommonStatusEnum：0 开启 1 关闭
     */
    private Integer status;
}
