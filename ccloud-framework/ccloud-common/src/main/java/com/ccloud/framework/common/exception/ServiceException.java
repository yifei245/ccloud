package com.ccloud.framework.common.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 业务逻辑异常。表示"请求本身没问题，但业务规则不允许"，对应可预期的失败。
 *
 * <p>不继承受检异常，也不填充堆栈——业务异常在链路中通常会被频繁抛出，
 * 省去堆栈能显著降低开销。需要排查时通过错误码与 TraceId 定位。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public final class ServiceException extends RuntimeException {

    /**
     * 错误码，见 {@link ErrorCode}
     */
    private Integer code;

    /**
     * 错误提示
     */
    private String message;

    public ServiceException(ErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getMsg());
    }

    public ServiceException(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }

    /**
     * 业务异常不需要堆栈，覆盖以避免填充开销
     */
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
