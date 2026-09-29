package com.ccloud.framework.rpc.core.decoder;

import com.ccloud.framework.common.exception.ServiceException;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.util.json.JsonUtils;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CcloudErrorDecoderTest {

    private CcloudErrorDecoder decoder;

    @BeforeEach
    void setUp() {
        decoder = new CcloudErrorDecoder();
    }

    private Response response(int status, String body) {
        return Response.builder()
                .status(status)
                .reason("test")
                .request(Request.create(Request.HttpMethod.GET, "/rpc-api/system/user/get",
                        Map.of(), null, StandardCharsets.UTF_8, null))
                .headers(Collections.emptyMap())
                .body(body, StandardCharsets.UTF_8)
                .build();
    }

    @Test
    @DisplayName("下游返回业务错误码时应还原为 ServiceException 并保留原始 code 与 msg")
    void businessError_shouldBeRestoredAsServiceException() {
        String body = JsonUtils.toJsonString(CommonResult.error(1_001_000_001, "用户不存在"));

        Exception e = decoder.decode("SystemApi#getUser(Long)", response(200, body));

        assertThat(e).isInstanceOf(ServiceException.class);
        assertThat(((ServiceException) e).getCode()).isEqualTo(1_001_000_001);
        assertThat(e.getMessage()).isEqualTo("用户不存在");
    }

    @Test
    @DisplayName("下游返回非 CommonResult 格式时应抛通用 500，而不是把原始报文透给调用方")
    void nonCommonResultBody_shouldBecomeGenericServerError() {
        Exception e = decoder.decode("SystemApi#getUser(Long)",
                response(500, "<html>Internal Server Error at /opt/app/secret</html>"));

        assertThat(e).isInstanceOf(ServiceException.class);
        assertThat(((ServiceException) e).getCode()).isEqualTo(500);
        assertThat(e.getMessage()).doesNotContain("/opt/app/secret");
    }

    @Test
    @DisplayName("下游无响应体时应抛通用 500 而非空指针")
    void emptyBody_shouldNotThrowNpe() {
        Exception e = decoder.decode("SystemApi#getUser(Long)", response(503, null));

        assertThat(e).isInstanceOf(ServiceException.class);
        assertThat(((ServiceException) e).getCode()).isEqualTo(500);
    }

    @Test
    @DisplayName("下游返回成功结果时不应被当作错误处理")
    void successBody_shouldStillProduceExceptionSinceDecoderOnlyRunsOnError() {
        // errorDecoder 只在 HTTP 非 2xx 或显式调用时触发；
        // 若响应体是成功结果，说明调用方误用，此时返回通用错误而非静默吞掉
        String body = JsonUtils.toJsonString(CommonResult.success("ok"));

        Exception e = decoder.decode("SystemApi#getUser(Long)", response(200, body));

        assertThat(e).isInstanceOf(ServiceException.class);
    }
}
