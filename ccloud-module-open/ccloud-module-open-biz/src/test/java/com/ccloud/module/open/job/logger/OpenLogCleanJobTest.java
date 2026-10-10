package com.ccloud.module.open.job.logger;

import com.ccloud.framework.test.core.ut.BaseMockitoUnitTest;
import com.ccloud.module.open.framework.config.OpenProperties;
import com.ccloud.module.open.service.logger.OpenApiLogService;
import com.xxl.job.core.context.XxlJobHelper;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;

import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpenLogCleanJobTest extends BaseMockitoUnitTest {

    @InjectMocks
    private OpenLogCleanJob job;

    @Mock
    private OpenApiLogService openApiLogService;
    @Spy
    private OpenProperties openProperties = new OpenProperties();

    @Test
    void clean_usesApiLogRetainDays_andReportsCount() {
        openProperties.getApiLog().setRetainDays(7);
        when(openApiLogService.cleanApiLog(7, 10_000)).thenReturn(5);

        try (MockedStatic<XxlJobHelper> helper = mockStatic(XxlJobHelper.class)) {
            job.clean();

            verify(openApiLogService).cleanApiLog(7, 10_000);
            helper.verify(() -> XxlJobHelper.handleSuccess("删除 7 天前的调用日志 5 条"));
        }
    }
}
