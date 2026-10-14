package com.ccloud.module.pay.framework.pay.core.client.impl.mock;

/**
 * Mock 渠道的行为
 */
public enum MockPayBehavior {

    /** 成功并立即回调 */
    SUCCESS,
    /** 支付失败并回调 */
    FAIL,
    /** 成功但不回调，用于验证补查 */
    DROP_NOTIFY,
    /** 成功并在 delaySeconds 秒后回调 */
    DELAY

}
