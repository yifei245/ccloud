package com.ccloud.module.pay.job.order;

import com.ccloud.module.pay.framework.config.PayProperties;
import com.ccloud.module.pay.service.order.PayOrderService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 关闭超时未支付的订单（先向渠道确认未支付），需在 XXL-Job 为本服务登记（每分钟执行）
 */
@Component
public class PayOrderExpireJob {

    @Resource
    private PayOrderService orderService;
    @Resource
    private PayProperties payProperties;

    @XxlJob("payOrderExpireJob")
    public void execute() {
        int count = orderService.expireOrder(payProperties.getJob().getBatchSize());
        XxlJobHelper.handleSuccess(String.format("关闭超时订单 %d 笔", count));
    }
}
