package com.ccloud.framework.web.core.handler;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.web.core.util.WebFrameworkUtils;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 把 Controller 返回的 {@link CommonResult} 暂存到 request，供访问日志过滤器读取
 * 真实业务错误码——HTTP 状态码始终是 200，光看它区分不出成功失败。
 *
 * <p>刻意<b>不做</b>自动包装：所有 Controller 都显式返回 CommonResult。
 * 隐式包装会让返回类型与实际响应不一致，Swagger 文档和 RPC 调用方都会被误导。
 */
@RestControllerAdvice
public class GlobalResponseBodyHandler implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        if (returnType.getMethod() == null) {
            return false;
        }
        return returnType.getMethod().getReturnType() == CommonResult.class;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType mediaType,
                                  Class selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof CommonResult<?> result && request instanceof ServletServerHttpRequest servletRequest) {
            WebFrameworkUtils.setCommonResult(servletRequest.getServletRequest(), result);
        }
        return body;
    }
}
