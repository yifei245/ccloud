package com.ccloud.framework.common.exception.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * 业务错误码分段登记表。仅用于日志与排查时把错误码映射回所属模块，
 * 不参与任何运行期逻辑判断。
 */
public class ServiceErrorCodeRange {

    private static final Map<Integer, String> RANGES = new HashMap<>();

    static {
        put(1_001, "system 系统服务");
        put(1_002, "infra 基础设施服务");
        put(1_004, "member 会员服务");
        put(1_005, "open 开放平台服务");
        put(1_007, "pay 支付服务");
        put(1_008, "product 商品服务");
        put(1_009, "bpm 工作流服务");
        put(1_011, "trade 交易服务");
        put(1_013, "promotion 营销服务");
        put(1_020, "ai 智能服务");
    }

    private static void put(Integer prefix, String module) {
        if (RANGES.containsKey(prefix)) {
            throw new IllegalArgumentException(
                    String.format("错误码段 %d 已被 [%s] 占用，不能重复登记", prefix, RANGES.get(prefix)));
        }
        RANGES.put(prefix, module);
    }

    /**
     * 按错误码反查所属模块
     *
     * @param code 错误码，例如 1_001_000_001
     * @return 模块描述；未登记时返回 null
     */
    public static String getModule(Integer code) {
        if (code == null) {
            return null;
        }
        return RANGES.get(code / 1_000_000);
    }
}
