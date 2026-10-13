package com.ccloud.module.pay.dal.dataobject.channel;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.TenantBaseDO;
import com.ccloud.framework.mybatis.core.type.EncryptTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 支付渠道。按租户隔离；同一应用下每个渠道编码最多一条
 */
@TableName(value = "pay_channel", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, exclude = "config")
@Accessors(chain = true)
public class PayChannelDO extends TenantBaseDO {

    /**
     * 渠道编号
     */
    @TableId
    private Long id;
    /**
     * 渠道编码，如 wx_lite / alipay_pc / wallet / mock
     */
    private String code;
    /**
     * 状态，见 CommonStatusEnum
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;
    /**
     * 渠道费率，单位百分比
     */
    private Double feeRate;
    /**
     * 应用编号
     */
    private Long appId;
    /**
     * 渠道配置 JSON。库中为密文；读取后为原文，含商户密钥，只在服务内使用，回显前须经 PayClientFactory.maskConfig 脱敏
     */
    @TableField(typeHandler = EncryptTypeHandler.class)
    private String config;
}
