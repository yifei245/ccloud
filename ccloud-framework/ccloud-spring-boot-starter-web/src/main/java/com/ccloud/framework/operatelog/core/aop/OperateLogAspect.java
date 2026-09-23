package com.ccloud.framework.operatelog.core.aop;

import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.util.json.JsonUtils;
import com.ccloud.framework.common.util.monitor.TracerUtils;
import com.ccloud.framework.common.util.servlet.ServletUtils;
import com.ccloud.framework.operatelog.core.annotation.OperateLog;
import com.ccloud.framework.operatelog.core.service.OperateLogFrameworkService;
import com.ccloud.framework.operatelog.core.service.OperateLogRecord;
import com.ccloud.framework.operatelog.core.util.DesensitizeUtils;
import com.ccloud.framework.web.core.util.WebFrameworkUtils;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link OperateLog} 切面：方法执行后（无论成败）记录一条操作日志。记录失败只打日志，不影响业务
 */
@Aspect
@Slf4j
public class OperateLogAspect {

    private static final int ACTION_MAX_LENGTH = 2000;
    private static final int EXTRA_MAX_LENGTH = 2000;

    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    private final OperateLogFrameworkService operateLogFrameworkService;

    public OperateLogAspect(OperateLogFrameworkService operateLogFrameworkService) {
        this.operateLogFrameworkService = operateLogFrameworkService;
    }

    @Around("@annotation(operateLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperateLog operateLog) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = null;
        Throwable error = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            try {
                operateLogFrameworkService.createOperateLog(
                        buildRecord(joinPoint, operateLog, result, error, System.currentTimeMillis() - start));
            } catch (Throwable e) {
                log.error("[around] 记录操作日志失败：{}#{}", operateLog.type(), operateLog.subType(), e);
            }
        }
    }

    private static OperateLogRecord buildRecord(ProceedingJoinPoint joinPoint, OperateLog operateLog, Object result,
                                                Throwable error, long duration) {
        OperateLogRecord record = new OperateLogRecord();
        record.setTraceId(StrUtil.nullToEmpty(TracerUtils.getTraceId()));
        record.setType(operateLog.type());
        record.setSubType(operateLog.subType());
        record.setBizId(evaluateBizId(joinPoint, operateLog.bizId(), error == null ? result : null));
        record.setSuccess(error == null);
        record.setAction(StrUtil.sub(error == null ? operateLog.subType()
                : operateLog.subType() + "失败：" + StrUtil.blankToDefault(error.getMessage(), error.getClass().getSimpleName()),
                0, ACTION_MAX_LENGTH));

        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("operateType", operateLog.operateType().getType());
        if (operateLog.logArgs()) {
            extra.put("args", DesensitizeUtils.desensitize(collectArgs(joinPoint)));
        }
        if (operateLog.logResult() && error == null) {
            extra.put("result", DesensitizeUtils.desensitize(result));
        }
        extra.put("duration", duration);
        record.setExtra(StrUtil.sub(JsonUtils.toJsonString(extra), 0, EXTRA_MAX_LENGTH));

        HttpServletRequest request = WebFrameworkUtils.getRequest();
        if (request != null) {
            record.setUserId(WebFrameworkUtils.getLoginUserId(request));
            record.setUserType(WebFrameworkUtils.getLoginUserType(request));
            record.setRequestMethod(request.getMethod());
            record.setRequestUrl(request.getRequestURI());
            record.setUserIp(ServletUtils.getClientIP(request));
            record.setUserAgent(StrUtil.sub(ServletUtils.getUserAgent(request), 0, 200));
        }
        if (record.getUserId() == null) {
            record.setUserId(0L);
        }
        if (record.getUserType() == null) {
            record.setUserType(0);
        }
        record.setRequestMethod(StrUtil.nullToEmpty(record.getRequestMethod()));
        record.setRequestUrl(StrUtil.nullToEmpty(record.getRequestUrl()));
        return record;
    }

    /**
     * 参数名 → 值；请求、响应、文件、校验结果不可序列化或过大，跳过
     */
    private static Map<String, Object> collectArgs(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] names = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg instanceof ServletRequest || arg instanceof ServletResponse
                    || arg instanceof MultipartFile || arg instanceof BindingResult) {
                continue;
            }
            result.put(names != null ? names[i] : "arg" + i, arg);
        }
        return result;
    }

    private static Long evaluateBizId(ProceedingJoinPoint joinPoint, String expression, Object result) {
        if (StrUtil.isBlank(expression)) {
            return 0L;
        }
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(null, signature.getMethod(),
                    joinPoint.getArgs(), NAME_DISCOVERER);
            context.setVariable("_ret", result);
            Object value = PARSER.parseExpression(expression).getValue(context);
            return value == null ? 0L : Long.valueOf(String.valueOf(value));
        } catch (RuntimeException e) {
            log.warn("[evaluateBizId] 业务编号表达式 {} 求值失败", expression, e);
            return 0L;
        }
    }
}
