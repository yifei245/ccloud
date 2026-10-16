package com.ccloud.module.pay.framework.pay.core.client.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 统一转账请求
 */
@Data
@Accessors(chain = true)
public class PayTransferUnifiedReq {

    /** 转账单号 */
    private String outTransferNo;
    private String subject;
    /** 单位分 */
    private Integer price;
    /** 收款账号 */
    private String userAccount;
    private String userName;
    private String notifyUrl;

}
