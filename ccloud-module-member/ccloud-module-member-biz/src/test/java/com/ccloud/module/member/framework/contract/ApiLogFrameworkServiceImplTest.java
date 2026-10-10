package com.ccloud.module.member.framework.contract;

import com.ccloud.framework.apilog.core.service.ApiAccessLog;
import com.ccloud.framework.apilog.core.service.ApiErrorLog;
import com.ccloud.framework.common.util.json.JsonUtils;
import com.ccloud.framework.mq.outbox.core.MessageSender;
import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.module.infra.enums.logger.ApiLogTopics;
import com.ccloud.module.open.enums.logger.OpenApiLogTopics;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ApiLogFrameworkServiceImplTest {

    private final MessageSender messageSender = mock(MessageSender.class);
    private final ApiLogFrameworkServiceImpl service = new ApiLogFrameworkServiceImpl(messageSender);

    @Test
    void 访问日志与异常日志异步发到各自的主题_消息体为日志JSON() {
        ApiAccessLog accessLog = new ApiAccessLog();
        accessLog.setApplicationName("ut");
        accessLog.setTenantId(3L);
        ApiErrorLog errorLog = new ApiErrorLog();
        errorLog.setExceptionName("java.lang.IllegalStateException");

        service.createApiAccessLogAsync(accessLog);
        service.createApiErrorLogAsync(errorLog);

        ArgumentCaptor<String> accessPayload = ArgumentCaptor.forClass(String.class);
        verify(messageSender, timeout(3000)).send(eq(ApiLogTopics.ACCESS_LOG), any(), accessPayload.capture());
        verify(messageSender, timeout(3000)).send(eq(ApiLogTopics.ERROR_LOG), any(), contains("IllegalStateException"));
        ApiAccessLog sent = JsonUtils.parseObject(accessPayload.getValue(), ApiAccessLog.class);
        assertThat(sent.getApplicationName()).isEqualTo("ut");
        assertThat(sent.getTenantId()).isEqualTo(3L);
    }

    @Test
    void 发送失败不影响调用方_后续日志照常发送() {
        doThrow(new IllegalStateException("redis down")).doNothing().when(messageSender).send(any(), any(), any());

        service.createApiAccessLogAsync(new ApiAccessLog());
        service.createApiAccessLogAsync(new ApiAccessLog());

        verify(messageSender, timeout(3000).times(2)).send(eq(ApiLogTopics.ACCESS_LOG), any(), any());
    }

    @Test
    void 开放应用的开放接口调用另发一份到开放平台调用日志() {
        ApiAccessLog accessLog = new ApiAccessLog();
        accessLog.setUserId(9L);
        accessLog.setUserType(UserTypeEnum.OPEN_APP.getValue());
        accessLog.setRequestUrl("/open-api/member/user/get");

        service.createApiAccessLogAsync(accessLog);

        verify(messageSender, timeout(2000)).send(eq(ApiLogTopics.ACCESS_LOG), anyString(), anyString());
        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(messageSender, timeout(2000)).send(eq(OpenApiLogTopics.API_LOG), anyString(), payload.capture());
        ApiAccessLog sent = JsonUtils.parseObject(payload.getValue(), ApiAccessLog.class);
        assertThat(sent.getUserId()).isEqualTo(9L);
        assertThat(sent.getUserType()).isEqualTo(3);
    }

    @Test
    void 管理员访问管理端接口只发访问日志() {
        ApiAccessLog accessLog = new ApiAccessLog();
        accessLog.setUserId(1L);
        accessLog.setUserType(UserTypeEnum.ADMIN.getValue());
        accessLog.setRequestUrl("/admin-api/member/user/page");

        service.createApiAccessLogAsync(accessLog);

        verify(messageSender, timeout(2000)).send(eq(ApiLogTopics.ACCESS_LOG), anyString(), anyString());
        verify(messageSender, after(300).never()).send(eq(OpenApiLogTopics.API_LOG), anyString(), anyString());
    }

    @Test
    void 开放应用类型但地址不是开放接口只发访问日志() {
        ApiAccessLog accessLog = new ApiAccessLog();
        accessLog.setUserId(9L);
        accessLog.setUserType(UserTypeEnum.OPEN_APP.getValue());
        accessLog.setRequestUrl("/app-api/member/user/get");

        service.createApiAccessLogAsync(accessLog);

        verify(messageSender, timeout(2000)).send(eq(ApiLogTopics.ACCESS_LOG), anyString(), anyString());
        verify(messageSender, after(300).never()).send(eq(OpenApiLogTopics.API_LOG), anyString(), anyString());
    }

    @Test
    void 非开放应用身份即使地址是开放接口也只发访问日志() {
        ApiAccessLog accessLog = new ApiAccessLog();
        accessLog.setUserId(1L);
        accessLog.setUserType(UserTypeEnum.ADMIN.getValue());
        accessLog.setRequestUrl("/open-api/member/user/get");

        service.createApiAccessLogAsync(accessLog);

        verify(messageSender, timeout(2000)).send(eq(ApiLogTopics.ACCESS_LOG), anyString(), anyString());
        verify(messageSender, after(300).never()).send(eq(OpenApiLogTopics.API_LOG), anyString(), anyString());
    }
}
