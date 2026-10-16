package com.ccloud.module.pay.service.order;

import com.ccloud.framework.common.pojo.PageParam;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.pay.api.order.dto.PayOrderCreateReqDTO;
import com.ccloud.module.pay.controller.admin.order.vo.PayOrderPageReqVO;
import com.ccloud.module.pay.controller.admin.order.vo.PayOrderSubmitReqVO;
import com.ccloud.module.pay.controller.admin.order.vo.PayOrderSubmitRespVO;
import com.ccloud.module.pay.dal.dataobject.app.PayAppDO;
import com.ccloud.module.pay.dal.dataobject.channel.PayChannelDO;
import com.ccloud.module.pay.dal.dataobject.order.PayOrderDO;
import com.ccloud.module.pay.dal.dataobject.order.PayOrderExtensionDO;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayOrderResult;

import java.util.List;

/**
 * 支付单 Service
 */
public interface PayOrderService {

    /**
     * 创建支付单，按 appKey 找应用。同一商户订单号重复创建：金额一致返回原单，不一致抛 PAY_ORDER_EXISTS_PRICE_MISMATCH
     */
    Long createOrder(PayOrderCreateReqDTO reqDTO);

    /**
     * 创建支付单，应用已校验（开放接口、钱包充值用）
     */
    Long createOrder(PayAppDO app, PayOrderCreateReqDTO reqDTO);

    PayOrderDO getOrder(Long id);

    PayOrderDO getOrder(Long appId, String merchantOrderId);

    PayOrderExtensionDO getOrderExtension(Long id);

    /**
     * 按外部订单号忽略租户查询支付尝试，Mock 确认页用
     */
    PayOrderExtensionDO getOrderExtensionByNoIgnoreTenant(String no);

    List<PayOrderExtensionDO> getOrderExtensionListByOrderId(Long orderId);

    PageResult<PayOrderDO> getOrderPage(PayOrderPageReqVO reqVO);

    /**
     * 异常支付分页：支付尝试已成功，但不是订单记录的成功尝试（重复支付，或订单关闭后才付款成功），需人工处理
     */
    PageResult<PayOrderExtensionDO> getAbnormalExtensionPage(PageParam pageParam);

    /**
     * 发起一次支付尝试。本方法不开事务：渠道调用须在事务之外
     */
    PayOrderSubmitRespVO submitOrder(Long orderId, PayOrderSubmitReqVO reqVO, String userIp);

    /**
     * 处理渠道的支付结果，回调、同步结果、补查共用，幂等
     */
    void notifyOrder(PayChannelDO channel, PayOrderResult result);

    /**
     * 补查任务：向渠道查询等待中的支付尝试，返回处理条数
     */
    int syncOrder(int limit);

    /**
     * 关单任务：关闭已超时的待支付订单，返回关闭条数
     */
    int expireOrder(int limit);
}
