package com.ccloud.framework.web.core.handler;

import com.ccloud.framework.apilog.core.service.ApiErrorLog;
import com.ccloud.framework.apilog.core.service.ApiErrorLogFrameworkService;
import com.ccloud.framework.common.exception.ServiceException;
import com.ccloud.framework.common.pojo.CommonResult;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler("ccloud-test-server", null);
        request = new MockHttpServletRequest("GET", "/admin-api/system/user/get");
    }

    @Test
    @DisplayName("ServiceException 应转成对应 code 的 CommonResult")
    void serviceException_shouldPreserveCodeAndMessage() {
        CommonResult<?> r = handler.serviceExceptionHandler(new ServiceException(1_001_000_001, "用户不存在"));
        assertThat(r.getCode()).isEqualTo(1_001_000_001);
        assertThat(r.getMsg()).isEqualTo("用户不存在");
    }

    @Test
    @DisplayName("MethodArgumentNotValidException 应返回 400 并带上字段提示")
    void methodArgumentNotValid_shouldReturnBadRequestWithFieldMessage() throws NoSuchMethodException {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "form");
        bindingResult.rejectValue(null, "mobile", "手机号格式不正确");
        MethodParameter parameter = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("dummyMethod", String.class), 0);
        CommonResult<?> r = handler.methodArgumentNotValidExceptionHandler(
                new MethodArgumentNotValidException(parameter, bindingResult));
        assertThat(r.getCode()).isEqualTo(400);
        assertThat(r.getMsg()).contains("手机号格式不正确");
    }

    @Test
    @DisplayName("BindException 应返回 400")
    void bindException_shouldReturnBadRequest() {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "form");
        bindingResult.rejectValue(null, "name", "名称不能为空");
        CommonResult<?> r = handler.bindExceptionHandler(new BindException(bindingResult));
        assertThat(r.getCode()).isEqualTo(400);
        assertThat(r.getMsg()).contains("名称不能为空");
    }

    @Test
    @DisplayName("缺少必填请求参数应返回 400 且指出参数名")
    void missingParameter_shouldReturnBadRequestNamingTheParameter() {
        CommonResult<?> r = handler.missingServletRequestParameterExceptionHandler(
                new MissingServletRequestParameterException("id", "Long"));
        assertThat(r.getCode()).isEqualTo(400);
        assertThat(r.getMsg()).contains("id");
    }

    @Test
    @DisplayName("ConstraintViolationException 应返回 400")
    void constraintViolation_shouldReturnBadRequest() {
        CommonResult<?> r = handler.constraintViolationExceptionHandler(
                new ConstraintViolationException("参数不合法", Set.of()));
        assertThat(r.getCode()).isEqualTo(400);
    }

    @Test
    @DisplayName("请求方法不支持应返回 405")
    void methodNotSupported_shouldReturn405() {
        CommonResult<?> r = handler.httpRequestMethodNotSupportedExceptionHandler(
                new HttpRequestMethodNotSupportedException("POST"));
        assertThat(r.getCode()).isEqualTo(405);
    }

    @Test
    @DisplayName("找不到处理器应返回 404")
    void noHandlerFound_shouldReturn404() {
        CommonResult<?> r = handler.noHandlerFoundExceptionHandler(
                new NoHandlerFoundException("GET", "/nope", org.springframework.http.HttpHeaders.EMPTY));
        assertThat(r.getCode()).isEqualTo(404);
    }

    @Test
    @DisplayName("未知异常应返回 500，且不把原始异常信息泄漏给调用方")
    void unknownException_shouldReturn500WithoutLeakingDetails() {
        CommonResult<?> r = handler.defaultExceptionHandler(request,
                new IllegalStateException("jdbc:mysql://10.0.0.1/ccloud?password=Sup3rSecret"));
        assertThat(r.getCode()).isEqualTo(500);
        assertThat(r.getMsg()).doesNotContain("Sup3rSecret").doesNotContain("jdbc:mysql");
        assertThat(r.getMsg()).isEqualTo("系统异常");
    }

    @Test
    @DisplayName("allExceptionHandler 应按异常类型分派，ServiceException 不被当作未知异常")
    void allExceptionHandler_shouldDispatchByType() {
        CommonResult<?> r = handler.allExceptionHandler(request, new ServiceException(1_001_000_002, "越权"));
        assertThat(r.getCode()).isEqualTo(1_001_000_002);
    }

    @Test
    @DisplayName("异常日志里的请求参数要脱敏，并记录请求头中的租户编号")
    void unknownException_shouldMaskRequestParamsAndRecordTenantId() {
        ApiErrorLogFrameworkService errorLogService = mock(ApiErrorLogFrameworkService.class);
        GlobalExceptionHandler logging = new GlobalExceptionHandler("ccloud-test-server", errorLogService);
        MockHttpServletRequest post = new MockHttpServletRequest("POST", "/admin-api/system/auth/login");
        post.setContentType("application/json");
        post.setContent("{\"username\":\"admin\",\"password\":\"admin123\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        post.addHeader("X-Tenant-Id", "7");

        logging.defaultExceptionHandler(post, new IllegalStateException("boom"));

        ArgumentCaptor<ApiErrorLog> captor = ArgumentCaptor.forClass(ApiErrorLog.class);
        verify(errorLogService).createApiErrorLogAsync(captor.capture());
        assertThat(captor.getValue().getRequestParams()).contains("\"username\":\"admin\"").doesNotContain("admin123");
        assertThat(captor.getValue().getTenantId()).isEqualTo(7L);
    }

    @SuppressWarnings("unused")
    private void dummyMethod(String arg) {
    }
}
