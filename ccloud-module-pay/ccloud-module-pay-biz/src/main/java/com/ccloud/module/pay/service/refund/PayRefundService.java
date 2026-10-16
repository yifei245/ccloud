package com.ccloud.module.pay.service.refund;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.pay.api.refund.dto.PayRefundCreateReqDTO;
import com.ccloud.module.pay.controller.admin.refund.vo.PayRefundPageReqVO;
import com.ccloud.module.pay.dal.dataobject.app.PayAppDO;
import com.ccloud.module.pay.dal.dataobject.channel.PayChannelDO;
import com.ccloud.module.pay.dal.dataobject.refund.PayRefundDO;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayRefundResult;

/**
 * 退款单 Service
 */
public interface PayRefundService {

    /**
     * 创建退款单，按 appKey 找应用。同一商户退款单号重复创建：订单与金额一致返回原单，否则抛 PAY_REFUND_EXISTS_MISMATCH。
     * 创建时在支付单上预占退款额度；渠道调用在事务提交之后进行（有外层事务时推迟到外层提交之后）
     */
    Long createRefund(PayRefundCreateReqDTO reqDTO);

    /**
     * 创建退款单，应用已校验（开放接口、钱包充值退款用）
     */
    Long createRefund(PayAppDO app, PayRefundCreateReqDTO reqDTO);

    PayRefundDO getRefund(Long id);

    PayRefundDO getRefund(Long appId, String merchantRefundId);

    PageResult<PayRefundDO> getRefundPage(PayRefundPageReqVO reqVO);

    /**
     * 处理渠道的退款结果，回调、同步结果、补查共用，幂等。回调须来自发起该退款的渠道
     */
    void notifyRefund(PayChannelDO channel, PayRefundResult result);

    /**
     * 补查任务：向渠道查询等待中的退款（仍等待则按同一退款单号重发），返回处理条数
     */
    int syncRefund(int limit);
}
