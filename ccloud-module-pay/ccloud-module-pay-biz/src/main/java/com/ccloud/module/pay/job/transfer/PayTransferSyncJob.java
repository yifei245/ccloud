package com.ccloud.module.pay.job.transfer;

import com.ccloud.module.pay.framework.config.PayProperties;
import com.ccloud.module.pay.service.transfer.PayTransferService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 向渠道补查等待中的转账，需在 XXL-Job 为本服务登记（每分钟执行）
 */
@Component
public class PayTransferSyncJob {

    @Resource
    private PayTransferService transferService;
    @Resource
    private PayProperties payProperties;

    @XxlJob("payTransferSyncJob")
    public void execute() {
        int count = transferService.syncTransfer(payProperties.getJob().getBatchSize());
        XxlJobHelper.handleSuccess(String.format("补查等待中的转账 %d 笔", count));
    }
}
