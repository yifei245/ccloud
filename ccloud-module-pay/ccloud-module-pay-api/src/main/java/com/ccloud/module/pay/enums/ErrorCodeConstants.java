package com.ccloud.module.pay.enums;

import com.ccloud.framework.common.exception.ErrorCode;

/**
 * pay 错误码，段位 1-007-000-000。每个错误码须在 sql/mysql/dml/06-i18n.sql 中登记中英文文案
 */
public interface ErrorCodeConstants {

    // ========== 支付应用 1-007-000-000 ==========
    ErrorCode PAY_APP_NOT_FOUND = new ErrorCode(1_007_000_000, "支付应用不存在");
    ErrorCode PAY_APP_IS_DISABLE = new ErrorCode(1_007_000_001, "支付应用已禁用");
    ErrorCode PAY_APP_EXIST_ORDER = new ErrorCode(1_007_000_002, "支付应用下存在支付单，无法删除");
    ErrorCode PAY_APP_KEY_EXISTS = new ErrorCode(1_007_000_003, "支付应用标识已存在");
    ErrorCode PAY_APP_OPEN_APP_INVALID = new ErrorCode(1_007_000_004, "开放应用不存在或不属于当前租户");
    ErrorCode PAY_APP_OPEN_APP_BOUND = new ErrorCode(1_007_000_005, "该开放应用已绑定其他支付应用");
    ErrorCode PAY_APP_NOT_OPENED_FOR_OPEN_APP = new ErrorCode(1_007_000_006, "开放应用未开通支付");
    ErrorCode PAY_APP_RECHARGE_NOT_ALLOWED = new ErrorCode(1_007_000_007, "钱包充值应用不允许此操作：不能由外部下单、退款，也不能绑定开放应用");

    // ========== 支付渠道 1-007-001-000 ==========
    ErrorCode CHANNEL_NOT_FOUND = new ErrorCode(1_007_001_000, "支付渠道不存在");
    ErrorCode CHANNEL_IS_DISABLE = new ErrorCode(1_007_001_001, "支付渠道已禁用");
    ErrorCode CHANNEL_EXIST_SAME_CHANNEL_ERROR = new ErrorCode(1_007_001_002, "该应用已存在相同编码的渠道");
    ErrorCode CHANNEL_NOT_SUPPORTED = new ErrorCode(1_007_001_003, "渠道({})未实现");
    ErrorCode CHANNEL_CONFIG_INVALID = new ErrorCode(1_007_001_004, "渠道配置不正确：{}");
    ErrorCode CHANNEL_MOCK_DISABLED = new ErrorCode(1_007_001_005, "当前环境未开启模拟支付");
    ErrorCode CHANNEL_OPERATION_NOT_SUPPORTED = new ErrorCode(1_007_001_006, "渠道({})不支持该操作");
    ErrorCode CHANNEL_HAS_UNFINISHED = new ErrorCode(1_007_001_007, "渠道下存在未完成的支付、退款或转账，无法删除");

    // ========== 支付单 1-007-002-000 ==========
    ErrorCode PAY_ORDER_NOT_FOUND = new ErrorCode(1_007_002_000, "支付单不存在");
    ErrorCode PAY_ORDER_STATUS_IS_NOT_WAITING = new ErrorCode(1_007_002_001, "支付单不是待支付状态");
    ErrorCode PAY_ORDER_IS_EXPIRED = new ErrorCode(1_007_002_002, "支付单已过期");
    ErrorCode PAY_ORDER_EXISTS_PRICE_MISMATCH = new ErrorCode(1_007_002_003, "商户订单号已存在且金额不一致");
    ErrorCode PAY_ORDER_SUBMIT_CHANNEL_ERROR = new ErrorCode(1_007_002_004, "发起支付失败：{}");
    ErrorCode PAY_ORDER_WALLET_REQUIRE_MEMBER = new ErrorCode(1_007_002_005, "钱包支付仅限会员本人的订单");
    ErrorCode PAY_ORDER_EXTENSION_NOT_FOUND = new ErrorCode(1_007_002_006, "支付记录不存在");
    ErrorCode PAY_ORDER_NOTIFY_PRICE_MISMATCH = new ErrorCode(1_007_002_007, "渠道回调金额与订单金额不一致");
    ErrorCode PAY_ORDER_RECHARGE_NOT_ALLOW_WALLET = new ErrorCode(1_007_002_008, "钱包充值不能使用钱包余额支付");
    ErrorCode PAY_ORDER_SUBMIT_IN_PROGRESS = new ErrorCode(1_007_002_009, "支付单正在处理中，请稍后重试");

    // ========== 退款单 1-007-003-000 ==========
    ErrorCode PAY_REFUND_NOT_FOUND = new ErrorCode(1_007_003_000, "退款单不存在");
    ErrorCode PAY_REFUND_ORDER_STATUS_ERROR = new ErrorCode(1_007_003_001, "只有支付成功的订单可以退款");
    ErrorCode PAY_REFUND_PRICE_EXCEED = new ErrorCode(1_007_003_002, "退款金额超过订单可退金额");
    ErrorCode PAY_REFUND_HAS_REFUNDING = new ErrorCode(1_007_003_003, "订单已有退款在处理中");
    ErrorCode PAY_REFUND_EXISTS_MISMATCH = new ErrorCode(1_007_003_004, "商户退款单号已存在且订单或金额不一致");
    ErrorCode PAY_REFUND_NOTIFY_PRICE_MISMATCH = new ErrorCode(1_007_003_005, "渠道回调退款金额与退款单不一致");

    // ========== 转账单 1-007-004-000 ==========
    ErrorCode PAY_TRANSFER_NOT_FOUND = new ErrorCode(1_007_004_000, "转账单不存在");
    ErrorCode PAY_TRANSFER_EXISTS_MISMATCH = new ErrorCode(1_007_004_001, "商户转账单号已存在且金额或收款账号不一致");
    ErrorCode PAY_TRANSFER_WALLET_ACCOUNT_INVALID = new ErrorCode(1_007_004_002, "钱包转账的收款账号须为会员编号");
    ErrorCode PAY_TRANSFER_NOTIFY_PRICE_MISMATCH = new ErrorCode(1_007_004_003, "渠道回调转账金额与转账单不一致");

    // ========== 钱包 1-007-005-000 ==========
    ErrorCode WALLET_NOT_FOUND = new ErrorCode(1_007_005_000, "钱包不存在");
    ErrorCode WALLET_BALANCE_NOT_ENOUGH = new ErrorCode(1_007_005_001, "钱包余额不足");
    ErrorCode WALLET_ADJUST_PRICE_ZERO = new ErrorCode(1_007_005_002, "调整金额不能为 0");
    ErrorCode WALLET_RECHARGE_NOT_FOUND = new ErrorCode(1_007_005_003, "充值记录不存在");
    ErrorCode WALLET_RECHARGE_NOT_OPENED = new ErrorCode(1_007_005_004, "钱包充值未开通");
    ErrorCode WALLET_RECHARGE_PACKAGE_NOT_FOUND = new ErrorCode(1_007_005_005, "充值套餐不存在或已关闭");
    ErrorCode WALLET_RECHARGE_PACKAGE_NAME_EXISTS = new ErrorCode(1_007_005_006, "充值套餐名已存在");
    ErrorCode WALLET_RECHARGE_REFUND_STATUS_ERROR = new ErrorCode(1_007_005_007, "只有已支付且未退款的充值可以退款");
    ErrorCode WALLET_RECHARGE_PRICE_INVALID = new ErrorCode(1_007_005_008, "请选择充值套餐或填写充值金额");
}
