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

    // ========== 等级 1-004-011-000 ==========
    ErrorCode LEVEL_NOT_EXISTS = new ErrorCode(1_004_011_000, "用户等级不存在");
}
