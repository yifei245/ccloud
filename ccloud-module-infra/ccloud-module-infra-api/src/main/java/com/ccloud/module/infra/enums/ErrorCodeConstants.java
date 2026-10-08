package com.ccloud.module.infra.enums;

import com.ccloud.framework.common.exception.ErrorCode;

/**
 * infra 错误码，段位 1-002-000-000。每个错误码须在 sql/mysql/dml/06-i18n.sql 中登记中英文文案
 */
public interface ErrorCodeConstants {

    // ========== 参数配置 1-002-000-000 ==========
    ErrorCode CONFIG_NOT_EXISTS = new ErrorCode(1_002_000_001, "参数配置不存在");
    ErrorCode CONFIG_KEY_DUPLICATE = new ErrorCode(1_002_000_002, "参数配置 key 重复");
    ErrorCode CONFIG_CAN_NOT_DELETE_SYSTEM_TYPE = new ErrorCode(1_002_000_003, "不能删除类型为系统内置的参数配置");
    ErrorCode CONFIG_GET_VALUE_ERROR_IF_VISIBLE = new ErrorCode(1_002_000_004, "获取参数配置失败，原因：不允许获取不可见配置");
    ErrorCode CONFIG_KEY_CAN_NOT_UPDATE_SYSTEM_TYPE = new ErrorCode(1_002_000_005, "不能修改系统内置参数的键名");

    // ========== API 错误日志 1-002-002-000 ==========
    ErrorCode API_ERROR_LOG_NOT_FOUND = new ErrorCode(1_002_002_000, "API 错误日志不存在");
    ErrorCode API_ERROR_LOG_PROCESSED = new ErrorCode(1_002_002_001, "API 错误日志已处理");
    ErrorCode API_ERROR_LOG_PROCESS_STATUS_INVALID = new ErrorCode(1_002_002_002, "处理状态只能是已处理或已忽略");

    // ========== 数据源 1-002-003-000 ==========
    ErrorCode DATA_SOURCE_CONFIG_NOT_EXISTS = new ErrorCode(1_002_003_000, "数据源配置不存在");
    ErrorCode DATA_SOURCE_CONFIG_NOT_OK = new ErrorCode(1_002_003_001, "数据源配置不正确，无法进行连接");
    ErrorCode DATA_SOURCE_CONFIG_MASTER_READONLY = new ErrorCode(1_002_003_002, "主数据源取自应用配置，不能修改或删除");
    ErrorCode DATA_SOURCE_CONFIG_IN_USE = new ErrorCode(1_002_003_003, "数据源下存在已导入的代码生成表，无法删除");

    // ========== 代码生成 1-002-004-000 ==========
    ErrorCode CODEGEN_TABLE_EXISTS = new ErrorCode(1_002_004_000, "表定义已经存在");
    ErrorCode CODEGEN_IMPORT_TABLE_NULL = new ErrorCode(1_002_004_001, "导入的表不存在");
    ErrorCode CODEGEN_TABLE_NOT_EXISTS = new ErrorCode(1_002_004_004, "表定义不存在");
    ErrorCode CODEGEN_COLUMN_NOT_EXISTS = new ErrorCode(1_002_004_005, "字段定义不存在");
    ErrorCode CODEGEN_SYNC_NONE_CHANGE = new ErrorCode(1_002_004_007, "同步失败，表结构没有变化");
    ErrorCode CODEGEN_TABLE_INFO_TABLE_COMMENT_IS_NULL = new ErrorCode(1_002_004_008, "数据库的表注释未填写");
    ErrorCode CODEGEN_TABLE_INFO_COLUMN_COMMENT_IS_NULL = new ErrorCode(1_002_004_009, "数据库的表字段({})注释未填写");
    ErrorCode CODEGEN_COLUMN_TYPE_UNSUPPORTED = new ErrorCode(1_002_004_010, "字段({})的类型 {} 不支持代码生成");
    ErrorCode CODEGEN_TREE_COLUMN_REQUIRED = new ErrorCode(1_002_004_011, "树表须指定本表中类型为 Long 的父编号字段，以及名称字段");
    ErrorCode CODEGEN_PRIMARY_KEY_REQUIRED = new ErrorCode(1_002_004_012, "表须有且只有一个 bigint 类型的自增主键");
    ErrorCode CODEGEN_TABLE_NAME_INVALID = new ErrorCode(1_002_004_013, "表名须为 模块_业务 格式，如 member_point_record");
    ErrorCode CODEGEN_INVALID_IDENTIFIER = new ErrorCode(1_002_004_014, "代码生成配置不合法：{}");

    // ========== 消息死信 1-002-005-000 ==========
    ErrorCode MESSAGE_DEAD_LETTER_NOT_EXISTS = new ErrorCode(1_002_005_000, "死信不存在");
    ErrorCode MESSAGE_DEAD_LETTER_HANDLED = new ErrorCode(1_002_005_001, "死信已处理");

    // ========== 文件配置 1-002-006-000 ==========
    ErrorCode FILE_CONFIG_NOT_EXISTS = new ErrorCode(1_002_006_000, "文件配置不存在");
    ErrorCode FILE_CONFIG_DELETE_FAIL_MASTER = new ErrorCode(1_002_006_001, "该文件配置不允许删除，原因：它是主配置，删除会导致无法上传文件");
    ErrorCode FILE_CONFIG_INVALID = new ErrorCode(1_002_006_002, "文件配置不正确：{}");

    // ========== 文件 1-002-007-000 ==========
    ErrorCode FILE_NOT_EXISTS = new ErrorCode(1_002_007_000, "文件不存在");
    ErrorCode FILE_TYPE_NOT_ALLOWED = new ErrorCode(1_002_007_001, "不支持上传该类型的文件：{}");
    ErrorCode FILE_SIZE_EXCEEDED = new ErrorCode(1_002_007_002, "文件大小超过上限 {}");
    ErrorCode FILE_PATH_INVALID = new ErrorCode(1_002_007_003, "文件路径不合法");
    ErrorCode FILE_EMPTY = new ErrorCode(1_002_007_004, "文件内容为空");
    ErrorCode FILE_UPLOAD_FAILED = new ErrorCode(1_002_007_005, "文件存储失败：{}");
}
