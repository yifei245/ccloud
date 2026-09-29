package com.ccloud.framework.web.core.handler;

import com.ccloud.framework.common.exception.enums.GlobalErrorCodeConstants;
import com.ccloud.framework.common.pojo.CommonResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Spring Security 相关异常的处理器。
 *
 * <p>单独成类并由 {@code @ConditionalOnClass} 控制注册：未引入 security 的模块
 * 不会因为找不到 {@link AccessDeniedException} 而启动失败。
 *
 * <p>{@code @Order} 优先级高于 {@link GlobalExceptionHandler}，
 * 否则会被后者的 {@code Exception.class} 兜底吞掉。
 */
@Slf4j
@Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class SecurityExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public CommonResult<?> accessDeniedExceptionHandler(AccessDeniedException ex) {
        log.warn("[accessDeniedExceptionHandler] 权限不足", ex);
        return CommonResult.error(GlobalErrorCodeConstants.FORBIDDEN);
    }
}
