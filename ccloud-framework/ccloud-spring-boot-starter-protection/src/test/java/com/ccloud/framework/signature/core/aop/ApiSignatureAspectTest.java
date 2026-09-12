package com.ccloud.framework.signature.core.aop;

import com.ccloud.framework.common.exception.ServiceException;
import com.ccloud.framework.signature.core.ApiSignatureFrameworkService;
import com.ccloud.framework.signature.core.ApiSignatureVerifier;
import com.ccloud.framework.signature.core.annotation.ApiSignature;
import com.ccloud.framework.signature.core.redis.ApiSignatureRedisDAO;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.annotation.Annotation;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ApiSignatureAspectTest {

    private static final String APP_KEY = "ccloud-open-demo";
    private static final String APP_SECRET = "demo-app-secret";

    private ApiSignatureVerifier verifier;
    private ApiSignatureRedisDAO redisDAO;
    private ApiSignatureFrameworkService frameworkService;
    private ApiSignatureAspect aspect;
    private ProceedingJoinPoint joinPoint;

    private final ApiSignature annotation = new ApiSignature() {
        @Override
        public Class<? extends Annotation> annotationType() {
            return ApiSignature.class;
        }

        @Override
        public int timeout() {
            return 5;
        }

        @Override
        public TimeUnit timeUnit() {
            return TimeUnit.MINUTES;
        }

        @Override
        public String message() {
            return "签名校验失败";
        }
    };

    @BeforeEach
    void setUp() throws Throwable {
        verifier = new ApiSignatureVerifier(Duration.ofMinutes(5));
        redisDAO = mock(ApiSignatureRedisDAO.class);
        frameworkService = mock(ApiSignatureFrameworkService.class);
        aspect = new ApiSignatureAspect(verifier, redisDAO, frameworkService);

        joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenReturn("ok");
        when(frameworkService.getAppSecret(APP_KEY)).thenReturn(APP_SECRET);
        when(redisDAO.tryAcquireNonce(anyString(), anyString(), any())).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    /**
     * 构造一个带完整签名头的请求并绑定到当前线程
     */
    private void bindRequest(String appKey, String nonce, Long timestamp, String signOverride) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/open-api/system/user/get");
        request.setParameter("id", "1");
        long ts = timestamp != null ? timestamp : System.currentTimeMillis();
        String sign = signOverride != null ? signOverride
                : verifier.sign(Map.of("id", "1"), null, ts, nonce, APP_SECRET);
        if (appKey != null) {
            request.addHeader(ApiSignatureAspect.HEADER_APP_KEY, appKey);
        }
        request.addHeader(ApiSignatureAspect.HEADER_NONCE, nonce);
        request.addHeader(ApiSignatureAspect.HEADER_TIMESTAMP, String.valueOf(ts));
        request.addHeader(ApiSignatureAspect.HEADER_SIGN, sign);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    @DisplayName("三步校验全通过时放行")
    void validRequest_shouldPass() throws Throwable {
        bindRequest(APP_KEY, "nonce-1", null, null);
        assertThat(aspect.around(joinPoint, annotation)).isEqualTo("ok");
        verify(joinPoint).proceed();
    }

    @Test
    @DisplayName("nonce 被重复使用时必须拒绝——这是防重放的最后一道闸")
    void replayedNonce_shouldBeRejected() throws Throwable {
        bindRequest(APP_KEY, "nonce-used", null, null);
        when(redisDAO.tryAcquireNonce(anyString(), anyString(), any())).thenReturn(false);

        assertThatThrownBy(() -> aspect.around(joinPoint, annotation))
                .isInstanceOf(ServiceException.class);
        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("签名错误时拒绝，且不消耗 nonce")
    void badSignature_shouldBeRejectedBeforeConsumingNonce() throws Throwable {
        bindRequest(APP_KEY, "nonce-2", null, "forged-signature");

        assertThatThrownBy(() -> aspect.around(joinPoint, annotation))
                .isInstanceOf(ServiceException.class);
        verify(redisDAO, never()).tryAcquireNonce(anyString(), anyString(), any());
        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("appKey 不存在或已停用时拒绝")
    void unknownAppKey_shouldBeRejected() throws Throwable {
        bindRequest("not-exist", "nonce-3", null, null);
        when(frameworkService.getAppSecret("not-exist")).thenReturn(null);

        assertThatThrownBy(() -> aspect.around(joinPoint, annotation))
                .isInstanceOf(ServiceException.class);
        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("缺少签名头时拒绝")
    void missingHeaders_shouldBeRejected() throws Throwable {
        bindRequest(null, "nonce-4", null, null);

        assertThatThrownBy(() -> aspect.around(joinPoint, annotation))
                .isInstanceOf(ServiceException.class);
        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("时间戳过期时拒绝")
    void staleTimestamp_shouldBeRejected() throws Throwable {
        bindRequest(APP_KEY, "nonce-5", System.currentTimeMillis() - Duration.ofMinutes(6).toMillis(), null);

        assertThatThrownBy(() -> aspect.around(joinPoint, annotation))
                .isInstanceOf(ServiceException.class);
        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("各失败分支的错误信息必须一致，避免攻击者据此枚举有效的 appKey")
    void allFailures_shouldReturnIndistinguishableMessage() throws Throwable {
        bindRequest("not-exist", "n", null, null);
        when(frameworkService.getAppSecret("not-exist")).thenReturn(null);
        String unknownApp = catchMessage();

        bindRequest(APP_KEY, "n", null, "forged");
        String badSign = catchMessage();

        assertThat(unknownApp).isEqualTo(badSign);
    }

    private String catchMessage() {
        try {
            aspect.around(joinPoint, annotation);
            return null;
        } catch (Throwable e) {
            return e.getMessage();
        }
    }
}
