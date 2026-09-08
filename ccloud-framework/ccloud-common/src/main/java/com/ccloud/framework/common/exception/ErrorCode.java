package com.ccloud.framework.common.exception;

import java.io.Serializable;
import java.util.Objects;

/**
 * 错误码。不可变值对象，可安全用作 Map 键。
 *
 * <p>错误码分段约定见项目文档：0 为成功；4xx/5xx 为全局通用；其余按
 * {@code 1-模块编号-子模块-序号} 编排，例如 system 模块为 {@code 1001xxxxxx}。
 */
public class ErrorCode implements Serializable {

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误提示，支持 Hutool 风格的 {} 占位符
     */
    private final String msg;

    public ErrorCode(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ErrorCode that)) {
            return false;
        }
        return Objects.equals(code, that.code) && Objects.equals(msg, that.msg);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, msg);
    }

    @Override
    public String toString() {
        return "ErrorCode(code=" + code + ", msg=" + msg + ")";
    }
}
