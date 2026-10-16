package com.ccloud.module.pay.job.order;

import com.ccloud.module.pay.framework.config.PayProperties;
import com.ccloud.module.pay.service.order.PayOrderService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 向渠道补查等待中的支付尝试，需在 XXL-Job 为本服务登记（每分钟执行）
 */
@Component
public class PayOrderSyncJob {

    @Resource
    private PayOrderService orderService;
    @Resource
    private PayProperties payProperties;

    @XxlJob("payOrderSyncJob")
    public void execute() {
        int count = orderService.syncOrder(payProperties.getJob().getBatchSize());
        XxlJobHelper.handleSuccess(String.format("补查等待中的支付尝试 %d 笔", count));
    }
}
