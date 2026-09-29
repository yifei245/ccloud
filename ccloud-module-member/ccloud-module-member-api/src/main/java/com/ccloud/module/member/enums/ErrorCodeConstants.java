package com.ccloud.module.member.enums;

import com.ccloud.framework.common.exception.ErrorCode;

/**
 * member 错误码，段位 1-004-000-000。每个错误码须在 sql/mysql/dml/06-i18n.sql 中登记中英文文案
 */
public interface ErrorCodeConstants {

    // ========== 用户 1-004-001-000 ==========
    ErrorCode USER_NOT_EXISTS = new ErrorCode(1_004_001_000, "用户不存在");
    ErrorCode USER_MOBILE_NOT_EXISTS = new ErrorCode(1_004_001_001, "手机号未注册用户");
    ErrorCode USER_MOBILE_USED = new ErrorCode(1_004_001_002, "修改手机失败，该手机号({})已经被使用");
    ErrorCode USER_POINT_NOT_ENOUGH = new ErrorCode(1_004_001_003, "用户积分余额不足");

    // ========== 认证 1-004-003-000 ==========
    ErrorCode AUTH_LOGIN_BAD_CREDENTIALS = new ErrorCode(1_004_003_000, "登录失败，账号密码不正确");
    ErrorCode AUTH_LOGIN_USER_DISABLED = new ErrorCode(1_004_003_001, "登录失败，账号被禁用");
    ErrorCode AUTH_SOCIAL_USER_NOT_FOUND = new ErrorCode(1_004_003_005, "登录失败，解析不到三方登录信息");
    ErrorCode AUTH_MOBILE_USED = new ErrorCode(1_004_003_007, "手机号已经被使用");
    ErrorCode AUTH_SOCIAL_USER_NOT_BIND = new ErrorCode(1_004_003_008, "该社交账号未绑定用户，请先用手机号登录后绑定");
    ErrorCode AUTH_LOGIN_FAIL_LOCKED = new ErrorCode(1_004_003_009, "登录失败次数过多，账号已锁定，请 {} 分钟后再试");

    // ========== 收件地址 1-004-004-000 ==========
    ErrorCode ADDRESS_NOT_EXISTS = new ErrorCode(1_004_004_000, "用户收件地址不存在");
    ErrorCode ADDRESS_COUNT_EXCEEDED = new ErrorCode(1_004_004_001, "收件地址最多 {} 个");

    // ========== 标签 1-004-006-000 ==========
    ErrorCode TAG_NOT_EXISTS = new ErrorCode(1_004_006_000, "用户标签不存在");
    ErrorCode TAG_NAME_EXISTS = new ErrorCode(1_004_006_001, "用户标签已经存在");
    ErrorCode TAG_HAS_USER = new ErrorCode(1_004_006_002, "用户标签下存在用户，无法删除");

    // ========== 积分 1-004-008-000 ==========
    ErrorCode POINT_BIZ_TYPE_INVALID = new ErrorCode(1_004_008_000, "积分业务类型与操作不符");

    // ========== 签到 1-004-009-000 ==========
    ErrorCode SIGN_IN_CONFIG_NOT_EXISTS = new ErrorCode(1_004_009_000, "签到规则不存在");
    ErrorCode SIGN_IN_CONFIG_DAY_EXISTS = new ErrorCode(1_004_009_001, "签到天数规则已存在");
    ErrorCode SIGN_IN_RECORD_TODAY_EXISTS = new ErrorCode(1_004_010_000, "今日已签到，请勿重复签到");

    // ========== 等级 1-004-011-000 ==========
    ErrorCode LEVEL_NOT_EXISTS = new ErrorCode(1_004_011_000, "用户等级不存在");
    ErrorCode LEVEL_VALUE_EXISTS = new ErrorCode(1_004_011_002, "用户等级值已被使用");
    ErrorCode LEVEL_EXPERIENCE_ORDER_INVALID = new ErrorCode(1_004_011_003, "等级越高，升级所需经验必须越多");
    ErrorCode LEVEL_HAS_USER = new ErrorCode(1_004_011_004, "用户等级下存在用户，无法删除");
    ErrorCode LEVEL_DISABLED = new ErrorCode(1_004_011_005, "用户等级已停用");

    // ========== 分组 1-004-012-000 ==========
    ErrorCode GROUP_NOT_EXISTS = new ErrorCode(1_004_012_000, "用户分组不存在");
    ErrorCode GROUP_NAME_EXISTS = new ErrorCode(1_004_012_001, "用户分组已经存在");
    ErrorCode GROUP_HAS_USER = new ErrorCode(1_004_012_002, "用户分组下存在用户，无法删除");
    ErrorCode GROUP_DISABLED = new ErrorCode(1_004_012_003, "用户分组已停用");

    // ========== 经验 1-004-013-000 ==========
    ErrorCode EXPERIENCE_BIZ_TYPE_INVALID = new ErrorCode(1_004_013_000, "经验业务类型与操作不符");
}
