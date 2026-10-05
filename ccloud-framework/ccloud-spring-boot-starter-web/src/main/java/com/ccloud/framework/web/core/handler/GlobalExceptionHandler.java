package com.ccloud.framework.web.core.handler;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.apilog.core.service.ApiErrorLog;
import com.ccloud.framework.apilog.core.service.ApiErrorLogFrameworkService;
import com.ccloud.framework.apilog.core.util.ApiLogUtils;
import com.ccloud.framework.common.exception.ServerException;
import com.ccloud.framework.common.exception.ServiceException;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.util.monitor.TracerUtils;
import com.ccloud.framework.common.util.servlet.ServletUtils;
import com.ccloud.framework.web.core.util.WebFrameworkUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static com.ccloud.framework.common.exception.enums.GlobalErrorCodeConstants.*;

/**
 * 全局异常处理器。
 *
 * <p>核心原则：<b>永远不把原始异常信息暴露给调用方</b>。未知异常一律返回固定文案，
 * 真实堆栈只进日志与异常日志表。数据库连接串、内网地址这类信息一旦随错误响应
 * 泄漏出去，就是现成的攻击入口。
 */
@Slf4j
@AllArgsConstructor
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final String applicationName;

    /**
     * 异常日志落库服务。允许为 null——单测与轻量服务不需要它。
     */
    private final ApiErrorLogFrameworkService apiErrorLogFrameworkService;

    /**
     * 兜底入口。供过滤器等无法走 @ExceptionHandler 的场景手工调用，
     * 内部按类型分派，保证与注解路径行为一致。
     */
    public CommonResult<?> allExceptionHandler(HttpServletRequest request, Throwable ex) {
        if (ex instanceof MissingServletRequestParameterException e) {
            return missingServletRequestParameterExceptionHandler(e);
        }
        if (ex instanceof MethodArgumentTypeMismatchException e) {
            return methodArgumentTypeMismatchExceptionHandler(e);
        }
        if (ex instanceof MethodArgumentNotValidException e) {
            return methodArgumentNotValidExceptionHandler(e);
        }
        if (ex instanceof BindException e) {
            return bindExceptionHandler(e);
        }
        if (ex instanceof ConstraintViolationException e) {
            return constraintViolationExceptionHandler(e);
        }
        if (ex instanceof ValidationException e) {
            return validationException(e);
        }
        if (ex instanceof NoHandlerFoundException e) {
            return noHandlerFoundExceptionHandler(e);
        }
        if (ex instanceof NoResourceFoundException e) {
            return noResourceFoundExceptionHandler(request, e);
        }
        if (ex instanceof HttpRequestMethodNotSupportedException e) {
            return httpRequestMethodNotSupportedExceptionHandler(e);
        }
        if (ex instanceof ServiceException e) {
            return serviceExceptionHandler(e);
        }
        if (ex instanceof ServerException e) {
            return serverExceptionHandler(request, e);
        }
        return defaultExceptionHandler(request, ex);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public CommonResult<?> missingServletRequestParameterExceptionHandler(MissingServletRequestParameterException ex) {
        log.warn("[missingServletRequestParameterExceptionHandler]", ex);
        return CommonResult.error(BAD_REQUEST.getCode(),
                String.format("请求参数缺失: %s", ex.getParameterName()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public CommonResult<?> methodArgumentTypeMismatchExceptionHandler(MethodArgumentTypeMismatchException ex) {
        log.warn("[methodArgumentTypeMismatchExceptionHandler]", ex);
        return CommonResult.error(BAD_REQUEST.getCode(),
                String.format("请求参数类型错误: %s", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public CommonResult<?> methodArgumentNotValidExceptionHandler(MethodArgumentNotValidException ex) {
        log.warn("[methodArgumentNotValidExceptionHandler]", ex);
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage()
                : ex.getBindingResult().getAllErrors().stream().findFirst()
                        .map(org.springframework.context.support.DefaultMessageSourceResolvable::getDefaultMessage)
                        .orElse("参数校验未通过");
        return CommonResult.error(BAD_REQUEST.getCode(), String.format("请求参数不正确: %s", message));
    }

    @ExceptionHandler(BindException.class)
    public CommonResult<?> bindExceptionHandler(BindException ex) {
        log.warn("[bindExceptionHandler]", ex);
        FieldError fieldError = ex.getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage()
                : ex.getAllErrors().stream().findFirst()
                        .map(org.springframework.context.support.DefaultMessageSourceResolvable::getDefaultMessage)
                        .orElse("参数校验未通过");
        return CommonResult.error(BAD_REQUEST.getCode(), String.format("请求参数不正确: %s", message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public CommonResult<?> constraintViolationExceptionHandler(ConstraintViolationException ex) {
        log.warn("[constraintViolationExceptionHandler]", ex);
        Set<ConstraintViolation<?>> violations = ex.getConstraintViolations();
        String message = violations == null || violations.isEmpty() ? "参数校验未通过"
                : violations.iterator().next().getMessage();
        return CommonResult.error(BAD_REQUEST.getCode(), String.format("请求参数不正确: %s", message));
    }

    @ExceptionHandler(ValidationException.class)
    public CommonResult<?> validationException(ValidationException ex) {
        log.warn("[validationException]", ex);
        return CommonResult.error(BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public CommonResult<?> httpMessageNotReadableExceptionHandler(HttpMessageNotReadableException ex) {
        log.warn("[httpMessageNotReadableExceptionHandler]", ex);
        // 不回传 ex.getMessage()，它会包含反序列化的类名与字段结构
        return CommonResult.error(BAD_REQUEST.getCode(), "请求体格式错误，请检查 JSON 是否合法");
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public CommonResult<?> noHandlerFoundExceptionHandler(NoHandlerFoundException ex) {
        log.warn("[noHandlerFoundExceptionHandler]", ex);
        return CommonResult.error(NOT_FOUND.getCode(),
                String.format("请求地址不存在: %s", ex.getRequestURL()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public CommonResult<?> noResourceFoundExceptionHandler(HttpServletRequest req, NoResourceFoundException ex) {
        log.warn("[noResourceFoundExceptionHandler]", ex);
        return CommonResult.error(NOT_FOUND.getCode(),
                String.format("请求地址不存在: %s", ex.getResourcePath()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public CommonResult<?> httpRequestMethodNotSupportedExceptionHandler(HttpRequestMethodNotSupportedException ex) {
        log.warn("[httpRequestMethodNotSupportedExceptionHandler]", ex);
        return CommonResult.error(METHOD_NOT_ALLOWED.getCode(),
                String.format("请求方法不正确: %s", ex.getMessage()));
    }

    /**
     * 业务异常：属于预期内的规则拒绝，只记 info，不记异常日志表
     */
    @ExceptionHandler(ServiceException.class)
    public CommonResult<?> serviceExceptionHandler(ServiceException ex) {
        log.info("[serviceExceptionHandler] code={} msg={}", ex.getCode(), ex.getMessage());
        return CommonResult.error(ex.getCode(), ex.getMessage());
    }

    /**
     * 服务端异常：需要开发介入，记 error 并落异常日志表
     */
    @ExceptionHandler(ServerException.class)
    public CommonResult<?> serverExceptionHandler(HttpServletRequest req, ServerException ex) {
        log.error("[serverExceptionHandler] code={} msg={}", ex.getCode(), ex.getMessage(), ex);
        createExceptionLog(req, ex);
        return CommonResult.error(ex.getCode(), ex.getMessage());
    }

    /**
     * 未知异常：统一返回固定文案，原始信息只进日志
     */
    @ExceptionHandler(Exception.class)
    public CommonResult<?> defaultExceptionHandler(HttpServletRequest req, Throwable ex) {
        log.error("[defaultExceptionHandler] url={}", req == null ? "-" : req.getRequestURI(), ex);
        createExceptionLog(req, ex);
        return CommonResult.error(INTERNAL_SERVER_ERROR);
    }

    private void createExceptionLog(HttpServletRequest req, Throwable ex) {
        if (apiErrorLogFrameworkService == null) {
            return;
        }
        try {
            ApiErrorLog errorLog = new ApiErrorLog();
            errorLog.setTraceId(TracerUtils.getTraceId());
            errorLog.setApplicationName(applicationName);
            errorLog.setExceptionTime(LocalDateTime.now());
            errorLog.setExceptionName(ex.getClass().getName());
            errorLog.setExceptionMessage(ExceptionUtil.getMessage(ex));
            errorLog.setExceptionRootCauseMessage(ExceptionUtil.getRootCauseMessage(ex));
            errorLog.setExceptionStackTrace(StrUtil.maxLength(ExceptionUtil.stacktraceToString(ex), 10000));
            StackTraceElement[] trace = ex.getStackTrace();
            if (trace.length > 0) {
                errorLog.setExceptionClassName(trace[0].getClassName());
                errorLog.setExceptionFileName(trace[0].getFileName());
                errorLog.setExceptionMethodName(trace[0].getMethodName());
                errorLog.setExceptionLineNumber(trace[0].getLineNumber());
            }
            if (req != null) {
                errorLog.setUserId(WebFrameworkUtils.getLoginUserId(req));
                errorLog.setUserType(WebFrameworkUtils.getLoginUserType(req));
                errorLog.setRequestMethod(req.getMethod());
                errorLog.setRequestUrl(req.getRequestURI());
                errorLog.setTenantId(WebFrameworkUtils.getTenantId(req));
                errorLog.setRequestParams(ApiLogUtils.buildRequestParams(
                        ServletUtils.getParamMap(req), ServletUtils.getBody(req), List.of()));
                errorLog.setUserIp(ServletUtils.getClientIP(req));
                errorLog.setUserAgent(ServletUtils.getUserAgent(req));
            }
            apiErrorLogFrameworkService.createApiErrorLogAsync(errorLog);
        } catch (Throwable th) {
            // 记日志本身失败绝不能影响主流程
            log.error("[createExceptionLog] 异常日志记录失败", th);
        }
    }
}
