package com.ccloud.module.open.job.notify;

import com.ccloud.module.open.service.notify.OpenNotifyService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 到期回调的发送与重试，需在 XXL-Job 为本服务登记（每 15 秒执行）
 */
@Component
public class OpenNotifyRetryJob {

    static final int BATCH_SIZE = 100;

    @Resource
    private OpenNotifyService openNotifyService;

    @XxlJob("openNotifyRetryJob")
    public void execute() {
        int count = openNotifyService.executeDueNotifies(BATCH_SIZE);
        XxlJobHelper.handleSuccess(String.format("处理到期回调 %d 条", count));
    }
}
