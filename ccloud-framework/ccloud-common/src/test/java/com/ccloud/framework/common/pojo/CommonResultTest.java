package com.ccloud.framework.common.pojo;

import com.ccloud.framework.common.exception.ErrorCode;
import com.ccloud.framework.common.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommonResultTest {

    @Test
    @DisplayName("success 应携带数据且 code 为 0")
    void success_shouldCarryDataWithZeroCode() {
        CommonResult<String> r = CommonResult.success("hi");
        assertThat(r.getCode()).isZero();
        assertThat(r.getData()).isEqualTo("hi");
        assertThat(r.getMsg()).isEmpty();
        assertThat(r.isSuccess()).isTrue();
        assertThat(r.isError()).isFalse();
    }

    @Test
    @DisplayName("error 传入 ErrorCode 与占位参数时应格式化 msg")
    void error_shouldFormatMessagePlaceholders() {
        ErrorCode ec = new ErrorCode(1_001_000_001, "用户【{}】不存在");
        CommonResult<Void> r = CommonResult.error(ec, "张三");
        assertThat(r.getCode()).isEqualTo(1_001_000_001);
        assertThat(r.getMsg()).isEqualTo("用户【张三】不存在");
        assertThat(r.getData()).isNull();
        assertThat(r.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("error 不允许用 0 作为错误码")
    void error_shouldRejectZeroCode() {
        assertThatThrownBy(() -> CommonResult.error(0, "boom"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("checkError 失败时抛 ServiceException 并保留 code 与 msg")
    void checkError_shouldThrowServiceExceptionPreservingCode() {
        CommonResult<Void> r = CommonResult.error(500, "boom");
        assertThatThrownBy(r::checkError)
                .isInstanceOf(ServiceException.class)
                .satisfies(e -> {
                    ServiceException se = (ServiceException) e;
                    assertThat(se.getCode()).isEqualTo(500);
                    assertThat(se.getMessage()).isEqualTo("boom");
                });
    }

    @Test
    @DisplayName("checkError 成功时不抛异常")
    void checkError_shouldNotThrowOnSuccess() {
        CommonResult.success("ok").checkError();
    }

    @Test
    @DisplayName("getCheckedData 成功时返回数据，失败时抛异常")
    void getCheckedData_shouldReturnDataOrThrow() {
        assertThat(CommonResult.success("ok").getCheckedData()).isEqualTo("ok");
        assertThatThrownBy(() -> CommonResult.error(500, "boom").getCheckedData())
                .isInstanceOf(ServiceException.class);
    }

    @Test
    @DisplayName("error(CommonResult) 应从另一个失败结果复制错误信息")
    void error_shouldCopyFromAnotherFailedResult() {
        CommonResult<String> src = CommonResult.error(1_001_000_002, "越权");
        CommonResult<Long> dst = CommonResult.error(src);
        assertThat(dst.getCode()).isEqualTo(1_001_000_002);
        assertThat(dst.getMsg()).isEqualTo("越权");
    }
}
