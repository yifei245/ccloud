package com.ccloud.framework.rpc.core.decoder;

import com.ccloud.framework.common.exception.ServiceException;
import com.ccloud.framework.common.exception.enums.GlobalErrorCodeConstants;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.util.json.JsonUtils;
import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * 把下游返回的业务错误码还原成 {@link ServiceException}。
 *
 * <p>没有它，调用方拿到的是 Feign 的 {@code FeignException}，
 * 里面只有 HTTP 状态码，业务错误码（"手机号已注册"这类）全丢了。
 */
@Slf4j
public class CcloudErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        String body = readBody(response);
        if (body != null && !body.isBlank()) {
            try {
                CommonResult<?> result = JsonUtils.parseObject(body, CommonResult.class);
                if (result != null && result.getCode() != null) {
                    // 下游是成功结果却走到错误解码，说明链路有问题，仍按错误处理而非静默放过
                    if (result.isSuccess()) {
                        log.warn("[decode] {} 返回成功结果却进入错误解码，按服务异常处理", methodKey);
                        return new ServiceException(GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR.getCode(),
                                GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR.getMsg());
                    }
                    return new ServiceException(result.getCode(), result.getMsg());
                }
            } catch (Exception e) {
                log.debug("[decode] {} 的响应体不是 CommonResult 格式", methodKey);
            }
        }
        // 不把下游的原始报文透给上游：里面可能有路径、堆栈、内网地址
        log.error("[decode] {} 调用失败，HTTP {}，响应体 {}", methodKey, response.status(),
                body == null ? "<空>" : body.substring(0, Math.min(body.length(), 500)));
        return new ServiceException(GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR.getCode(),
                GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR.getMsg());
    }

    private String readBody(Response response) {
        if (response.body() == null) {
            return null;
        }
        try {
            return Util.toString(response.body().asReader(Util.UTF_8));
        } catch (IOException e) {
            return null;
        }
    }
}
