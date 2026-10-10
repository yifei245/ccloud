package com.ccloud.module.open.job.notify;

import com.ccloud.framework.test.core.ut.BaseMockitoUnitTest;
import com.ccloud.module.open.service.notify.OpenNotifyService;
import com.xxl.job.core.context.XxlJobHelper;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class OpenNotifyRetryJobTest extends BaseMockitoUnitTest {

    @InjectMocks
    private OpenNotifyRetryJob job;

    @Mock
    private OpenNotifyService openNotifyService;

    @Test
    void execute_processesDueNotifiesInBatch_andReportsCount() {
        when(openNotifyService.executeDueNotifies(100)).thenReturn(4);

        try (MockedStatic<XxlJobHelper> helper = mockStatic(XxlJobHelper.class)) {
            job.execute();

            helper.verify(() -> XxlJobHelper.handleSuccess("处理到期回调 4 条"));
        }
    }
}
