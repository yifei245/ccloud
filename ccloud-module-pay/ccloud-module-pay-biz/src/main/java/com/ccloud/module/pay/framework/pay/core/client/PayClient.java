package com.ccloud.module.pay.framework.pay.core.client;

import com.ccloud.module.pay.framework.pay.core.client.dto.PayNotifyRequest;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayOrderResult;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayOrderUnifiedReq;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayRefundResult;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayRefundUnifiedReq;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayTransferResult;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayTransferUnifiedReq;

/**
 * 支付渠道客户端：统一封装下单、查询、回调解析、退款、转账。
 * 实现类须无状态或仅持有配置，异常由调用方处理（渠道调用不在数据库事务内）
 */
public interface PayClient {

    Long getChannelId();

    PayOrderResult unifiedOrder(PayOrderUnifiedReq req);

    PayOrderResult getOrder(String outTradeNo);

    /** 验签并解析支付回调，验签或解析失败抛 {@link PayNotifyVerifyException} */
    PayOrderResult parseOrderNotify(PayNotifyRequest request);

    PayRefundResult unifiedRefund(PayRefundUnifiedReq req);

    PayRefundResult getRefund(String outTradeNo, String outRefundNo);

    /** 验签并解析退款回调，验签或解析失败抛 {@link PayNotifyVerifyException} */
    PayRefundResult parseRefundNotify(PayNotifyRequest request);

    PayTransferResult unifiedTransfer(PayTransferUnifiedReq req);

    PayTransferResult getTransfer(String outTransferNo);

    /** 验签并解析转账回调，验签或解析失败抛 {@link PayNotifyVerifyException} */
    PayTransferResult parseTransferNotify(PayNotifyRequest request);

    /** 回调处理成功后应答渠道的响应体 */
    default String notifyAckBody() {
        return "success";
    }

}
