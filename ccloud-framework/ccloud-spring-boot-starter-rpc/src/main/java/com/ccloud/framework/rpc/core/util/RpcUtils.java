package com.ccloud.framework.rpc.core.util;

import com.ccloud.framework.common.pojo.CommonResult;

import java.util.List;
import java.util.function.Supplier;

/**
 * RPC 调用工具
 */
public class RpcUtils {

    /**
     * 取出数据，失败时抛出业务异常。
     *
     * <p>用它替代 {@code result.getData()}：后者在下游失败时会静默返回 null，
     * 错误被吞掉，问题往往要到很远的地方才以空指针的形式暴露。
     */
    public static <T> T getData(CommonResult<T> result) {
        return result.getCheckedData();
    }

    /**
     * 调用失败时返回兜底值而非抛异常。用于非关键链路的降级。
     */
    public static <T> T getDataOrDefault(Supplier<CommonResult<T>> supplier, T defaultValue) {
        try {
            CommonResult<T> result = supplier.get();
            return result != null && result.isSuccess() ? result.getData() : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static <T> List<T> getListOrEmpty(Supplier<CommonResult<List<T>>> supplier) {
        return getDataOrDefault(supplier, List.of());
    }
}
