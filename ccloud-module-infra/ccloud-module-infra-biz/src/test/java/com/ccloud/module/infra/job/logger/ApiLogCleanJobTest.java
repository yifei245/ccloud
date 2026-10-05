package com.ccloud.module.infra.job.logger;

import com.ccloud.framework.test.core.ut.BaseMockitoUnitTest;
import com.ccloud.module.infra.framework.config.InfraProperties;
import com.ccloud.module.infra.service.logger.ApiAccessLogService;
import com.ccloud.module.infra.service.logger.ApiErrorLogService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ApiLogCleanJobTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ApiLogCleanJob job;

    @Mock
    private ApiAccessLogService apiAccessLogService;
    @Mock
    private ApiErrorLogService apiErrorLogService;
    @Spy
    private InfraProperties infraProperties = new InfraProperties();

    @Test
    void cleanAccessLog_usesAccessRetainDays() {
        infraProperties.setAccessRetainDays(7);
        infraProperties.setErrorRetainDays(99);

        job.cleanAccessLog();

        verify(apiAccessLogService).cleanApiAccessLog(7, 10_000);
        verifyNoInteractions(apiErrorLogService);
    }

    @Test
    void cleanErrorLog_usesErrorRetainDays() {
        infraProperties.setAccessRetainDays(99);
        infraProperties.setErrorRetainDays(15);

        job.cleanErrorLog();

        verify(apiErrorLogService).cleanApiErrorLog(15, 10_000);
        verifyNoInteractions(apiAccessLogService);
    }
}
