package com.ccloud.framework.signature.core.aop;

import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.exception.ServiceException;
import com.ccloud.framework.common.exception.enums.GlobalErrorCodeConstants;
import com.ccloud.framework.common.util.servlet.ServletUtils;
import com.ccloud.framework.signature.core.ApiSignatureFrameworkService;
import com.ccloud.framework.signature.core.ApiSignatureVerifier;
import com.ccloud.framework.signature.core.annotation.ApiSignature;
import com.ccloud.framework.signature.core.redis.ApiSignatureRedisDAO;
import com.ccloud.framework.web.core.util.WebFrameworkUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.time.Duration;
import java.util.Map;

/**
 * {@link ApiSignature} 的切面实现。
 *
 * <p>校验分三步，缺一不可：
 * <ol>
 *   <li>应用存在且密钥有效——确认调用方身份；</li>
 *   <li>签名正确——确认报文没被篡改；</li>
 *   <li>nonce 未被使用过——确认这不是一次重放。</li>
 * </ol>
 * 只做前两步的话，攻击者抓到一个合法请求就能在有效期内无限重发。
 */
@Aspect
@Slf4j
@RequiredArgsConstructor
public class ApiSignatureAspect {

    public static final String HEADER_APP_KEY = "X-App-Key";
    public static final String HEADER_TIMESTAMP = "X-Timestamp";
    public static final String HEADER_NONCE = "X-Nonce";
    public static final String HEADER_SIGN = "X-Sign";

    private final ApiSignatureVerifier verifier;
    private final ApiSignatureRedisDAO signatureRedisDAO;
    private final ApiSignatureFrameworkService signatureFrameworkService;

    @Around("@annotation(apiSignature)")
    public Object around(ProceedingJoinPoint joinPoint, ApiSignature apiSignature) throws Throwable {
        verifySignature(apiSignature);
        return joinPoint.proceed();
    }

    private void verifySignature(ApiSignature apiSignature) {
        HttpServletRequest request = WebFrameworkUtils.getRequest();
        if (request == null) {
            throw fail(apiSignature);
        }

        String appKey = request.getHeader(HEADER_APP_KEY);
        String nonce = request.getHeader(HEADER_NONCE);
        String sign = request.getHeader(HEADER_SIGN);
        String timestampText = request.getHeader(HEADER_TIMESTAMP);
        if (StrUtil.hasBlank(appKey, nonce, sign, timestampText)) {
            log.info("[verifySignature] 缺少签名相关请求头");
            throw fail(apiSignature);
        }

        long timestamp;
        try {
            timestamp = Long.parseLong(timestampText);
        } catch (NumberFormatException e) {
            throw fail(apiSignature);
        }

        String appSecret = signatureFrameworkService.getAppSecret(appKey);
        if (StrUtil.isBlank(appSecret)) {
            log.info("[verifySignature] 应用 {} 不存在或已停用", appKey);
            throw fail(apiSignature);
        }

        Map<String, String> params = ServletUtils.getParamMap(request);
        String body = ServletUtils.getBody(request);
        if (!verifier.verify(params, body, timestamp, nonce, sign, appSecret)) {
            log.info("[verifySignature] 应用 {} 的签名校验失败", appKey);
            throw fail(apiSignature);
        }

        // nonce 的保留时长与签名有效期一致：nonce 若先过期，
        // 攻击者就能在签名仍有效的窗口内用同一个 nonce 重放
        Duration timeout = Duration.ofMillis(apiSignature.timeUnit().toMillis(apiSignature.timeout()));
        if (!signatureRedisDAO.tryAcquireNonce(appKey, nonce, timeout)) {
            log.warn("[verifySignature] 应用 {} 的 nonce {} 被重复使用，疑似重放攻击", appKey, nonce);
            throw fail(apiSignature);
        }
    }

    /**
     * 所有失败分支返回同一个错误，不告诉调用方是"应用不存在"还是"签名错误"——
     * 差异化的提示会帮助攻击者枚举有效的 appKey
     */
    private static ServiceException fail(ApiSignature apiSignature) {
        return new ServiceException(GlobalErrorCodeConstants.UNAUTHORIZED.getCode(), apiSignature.message());
    }
}
