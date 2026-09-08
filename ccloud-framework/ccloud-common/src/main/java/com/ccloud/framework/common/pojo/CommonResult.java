package com.ccloud.framework.common.pojo;

import cn.hutool.core.lang.Assert;
import com.ccloud.framework.common.exception.ErrorCode;
import com.ccloud.framework.common.exception.ServiceException;
import com.ccloud.framework.common.exception.enums.GlobalErrorCodeConstants;
import com.ccloud.framework.common.exception.util.ServiceExceptionUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 通用返回体。所有 HTTP 接口与 RPC 接口的统一外壳。
 *
 * @param <T> 业务数据类型
 */
@Data
@Schema(description = "通用返回体")
public class CommonResult<T> implements Serializable {

    @Schema(description = "错误码，0 表示成功", example = "0")
    private Integer code;

    @Schema(description = "业务数据")
    private T data;

    @Schema(description = "错误提示，成功时为空字符串", example = "")
    private String msg;

    public static <T> CommonResult<T> success(T data) {
        CommonResult<T> result = new CommonResult<>();
        result.code = GlobalErrorCodeConstants.SUCCESS.getCode();
        result.data = data;
        result.msg = "";
        return result;
    }

    /**
     * 从另一个失败结果复制错误信息。用于 RPC 调用方把下游错误原样上抛。
     */
    public static <T> CommonResult<T> error(CommonResult<?> result) {
        return error(result.getCode(), result.getMsg());
    }

    public static <T> CommonResult<T> error(Integer code, String message) {
        Assert.isTrue(!GlobalErrorCodeConstants.SUCCESS.getCode().equals(code), "code 不能为 0，那是成功");
        CommonResult<T> result = new CommonResult<>();
        result.code = code;
        result.msg = message;
        return result;
    }

    public static <T> CommonResult<T> error(ErrorCode errorCode, Object... params) {
        ServiceException e = ServiceExceptionUtil.exception(errorCode, params);
        return error(e.getCode(), e.getMessage());
    }

    @JsonIgnore
    public boolean isSuccess() {
        return GlobalErrorCodeConstants.SUCCESS.getCode().equals(code);
    }

    @JsonIgnore
    public boolean isError() {
        return !isSuccess();
    }

    /**
     * 失败时抛出 {@link ServiceException}，供 RPC 调用方在不关心失败分支时直接使用
     */
    public void checkError() throws ServiceException {
        if (isSuccess()) {
            return;
        }
        throw new ServiceException(code, msg);
    }

    /**
     * 校验成功后返回数据，失败则抛异常
     */
    @JsonIgnore
    public T getCheckedData() {
        checkError();
        return data;
    }
}
