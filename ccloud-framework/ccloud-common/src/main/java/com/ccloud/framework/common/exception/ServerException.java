package com.ccloud.framework.common.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 服务端异常。表示"系统自身出了问题"，与 {@link ServiceException} 的区别在于：
 * 业务异常是预期内的规则拒绝，服务端异常是需要开发介入排查的故障。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public final class ServerException extends RuntimeException {

    private Integer code;
    private String message;

    public ServerException(ErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getMsg());
    }

    public ServerException(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
