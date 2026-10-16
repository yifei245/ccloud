package com.ccloud.module.pay.job.refund;

import com.ccloud.module.pay.framework.config.PayProperties;
import com.ccloud.module.pay.service.refund.PayRefundService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 向渠道补查等待中的退款，需在 XXL-Job 为本服务登记（每分钟执行）
 */
@Component
public class PayRefundSyncJob {

    @Resource
    private PayRefundService refundService;
    @Resource
    private PayProperties payProperties;

    @XxlJob("payRefundSyncJob")
    public void execute() {
        int count = refundService.syncRefund(payProperties.getJob().getBatchSize());
        XxlJobHelper.handleSuccess(String.format("补查等待中的退款 %d 笔", count));
    }
}
