package com.ccloud.module.pay.dal.dataobject.transfer;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.ccloud.framework.mybatis.core.dataobject.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 转账单。同一应用内商户转账单号唯一，用于佣金提现、钱包提现等出款。按租户隔离
 */
@TableName(value = "pay_transfer", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class PayTransferDO extends TenantBaseDO {

    /**
     * 转账单编号
     */
    @TableId
    private Long id;
    /**
     * 外部转账单号，传给渠道
     */
    private String no;
    /**
     * 应用编号
     */
    private Long appId;
    /**
     * 渠道编号
     */
    private Long channelId;
    /**
     * 渠道编码
     */
    private String channelCode;
    /**
     * 商户转账单号
     */
    private String merchantTransferId;
    /**
     * 转账标题
     */
    private String subject;
    /**
     * 转账金额，单位分
     */
    private Integer price;
    /**
     * 收款账号，钱包转账时为会员编号
     */
    private String userAccount;
    /**
     * 收款人姓名
     */
    private String userName;
    /**
     * 转账状态，见 PayTransferStatusEnum
     */
    private Integer status;
    /**
     * 转账成功时间
     */
    private LocalDateTime successTime;
    /**
     * 用户 IP
     */
    private String userIp;
    /**
     * 渠道额外参数
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, String> channelExtras;
    /**
     * 渠道转账单号
     */
    private String channelTransferNo;
    /**
     * 渠道错误码
     */
    private String channelErrorCode;
    /**
     * 渠道错误信息
     */
    private String channelErrorMsg;
    /**
     * 渠道回调原始报文
     */
    private String channelNotifyData;
}
