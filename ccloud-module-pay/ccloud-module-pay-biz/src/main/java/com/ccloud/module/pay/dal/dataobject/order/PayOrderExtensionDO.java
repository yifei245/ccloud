package com.ccloud.module.pay.dal.dataobject.order;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.ccloud.framework.mybatis.core.dataobject.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * 支付尝试。订单每发起一次支付生成一条，传给渠道的商户单号即 {@link #no}。按租户隔离
 */
@TableName(value = "pay_order_extension", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class PayOrderExtensionDO extends TenantBaseDO {

    /**
     * 支付尝试编号
     */
    @TableId
    private Long id;
    /**
     * 外部订单号，传给渠道
     */
    private String no;
    /**
     * 支付单编号
     */
    private Long orderId;
    /**
     * 渠道编号
     */
    private Long channelId;
    /**
     * 渠道编码
     */
    private String channelCode;
    /**
     * 支付方式
     */
    private String payMethod;
    /**
     * 用户 IP
     */
    private String userIp;
    /**
     * 状态，见 PayOrderStatusEnum（等待、成功、关闭）
     */
    private Integer status;
    /**
     * 渠道额外参数
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, String> channelExtras;
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
