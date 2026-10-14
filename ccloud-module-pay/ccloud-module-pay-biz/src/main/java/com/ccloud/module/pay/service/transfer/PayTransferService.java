package com.ccloud.module.pay.service.transfer;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.pay.api.transfer.dto.PayTransferCreateReqDTO;
import com.ccloud.module.pay.controller.admin.transfer.vo.PayTransferPageReqVO;
import com.ccloud.module.pay.dal.dataobject.channel.PayChannelDO;
import com.ccloud.module.pay.dal.dataobject.transfer.PayTransferDO;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayTransferResult;

/**
 * 转账单 Service
 */
public interface PayTransferService {

    /**
     * 创建转账单并向渠道发起转账。同一商户转账单号重复创建：金额与收款账号一致返回原单，否则抛 PAY_TRANSFER_EXISTS_MISMATCH。
     * 渠道调用不放在调用方的事务内：有外层事务时推迟到其提交之后，没有则立即进行
     */
    Long createTransfer(PayTransferCreateReqDTO reqDTO);

    PayTransferDO getTransfer(Long id);

    PageResult<PayTransferDO> getTransferPage(PayTransferPageReqVO reqVO);

    /**
     * 处理渠道的转账结果，回调、同步结果、补查共用，幂等。回调须来自发起该转账的渠道
     */
    void notifyTransfer(PayChannelDO channel, PayTransferResult result);

    /**
     * 补查任务：等待中的转账按同一转账单号重发，进行中的向渠道查询结果，返回处理条数
     */
    int syncTransfer(int limit);
}
