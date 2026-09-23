package com.ccloud.module.system.dal.dataobject.sms;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 短信渠道，全局表（各租户共用）
 */
@TableName("system_sms_channel")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, exclude = "apiSecret")
@Accessors(chain = true)
public class SmsChannelDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 短信签名
     */
    private String signature;
    /**
     * 渠道编码，见 SmsChannelEnum
     */
    private String code;
    /**
     * 状态，见 CommonStatusEnum
     */
    private Integer status;
    private String remark;
    private String apiKey;
    private String apiSecret;
    /**
     * 短信发送回调地址
     */
    private String callbackUrl;
}
