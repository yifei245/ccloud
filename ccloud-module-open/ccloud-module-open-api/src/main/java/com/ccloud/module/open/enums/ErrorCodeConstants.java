package com.ccloud.module.open.enums;

import com.ccloud.framework.common.exception.ErrorCode;

/**
 * open 错误码，段位 1-005-000-000。每个错误码须在 sql/mysql/dml/06-i18n.sql 中登记中英文文案
 */
public interface ErrorCodeConstants {

    // ========== 通用 1-005-000-000 ==========
    ErrorCode ONLY_SYSTEM_TENANT = new ErrorCode(1_005_000_000, "该操作仅限平台管理员（系统租户）执行");

    // ========== 开放应用 1-005-001-000 ==========
    ErrorCode OPEN_APP_NOT_EXISTS = new ErrorCode(1_005_001_000, "开放应用不存在");
    ErrorCode OPEN_APP_IP_WHITELIST_INVALID = new ErrorCode(1_005_001_001, "IP 白名单格式不正确：{}");
    ErrorCode OPEN_APP_NOTIFY_URL_INVALID = new ErrorCode(1_005_001_002, "回调地址须为 http 或 https 开头的完整地址");
    ErrorCode OPEN_APP_NOTIFY_URL_NOT_CONFIGURED = new ErrorCode(1_005_001_003, "应用未配置回调地址");
    ErrorCode OPEN_APP_API_NOT_ENABLED = new ErrorCode(1_005_001_004, "接口({})不存在或已关闭，不能授权");

    // ========== 接口目录 1-005-002-000 ==========
    ErrorCode OPEN_API_NOT_EXISTS = new ErrorCode(1_005_002_000, "开放接口不存在");
    ErrorCode OPEN_API_CODE_EXISTS = new ErrorCode(1_005_002_001, "接口编码已存在");
    ErrorCode OPEN_API_METHOD_PATH_EXISTS = new ErrorCode(1_005_002_002, "相同请求方法与路径的接口已存在");
    ErrorCode OPEN_API_PATH_INVALID = new ErrorCode(1_005_002_003, "接口路径须以 /open-api/ 开头，且不能包含查询串、通配符或 ..");

    // ========== 回调通知 1-005-003-000 ==========
    ErrorCode OPEN_NOTIFY_LOG_NOT_EXISTS = new ErrorCode(1_005_003_000, "回调日志不存在");
    ErrorCode OPEN_NOTIFY_LOG_CANNOT_RETRY = new ErrorCode(1_005_003_001, "只有失败待重试或已放弃的回调可以重新通知");
}
