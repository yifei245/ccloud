package com.ccloud.module.pay.dal.dataobject.wallet;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 钱包流水。同一 (钱包, 业务类型, 业务编号) 只有一条。按租户隔离
 */
@TableName("pay_wallet_transaction")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class PayWalletTransactionDO extends TenantBaseDO {

    /**
     * 流水编号
     */
    @TableId
    private Long id;
    /**
     * 流水号
     */
    private String no;
    /**
     * 钱包编号
     */
    private Long walletId;
    /**
     * 业务类型，见 PayWalletBizTypeEnum
     */
    private Integer bizType;
    /**
     * 业务编号
     */
    private String bizId;
    /**
     * 流水说明
     */
    private String title;
    /**
     * 变动金额，单位分，正数入账负数出账
     */
    private Integer price;
    /**
     * 变动后余额，单位分
     */
    private Integer balance;
}
