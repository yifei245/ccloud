package com.ccloud.framework.excel.core.service;

/**
 * 字典翻译契约。实现由 system 模块经 RPC 提供。
 */
public interface DictFrameworkService {

    /**
     * @return 字典值对应的标签；未命中返回 null
     */
    String getDictDataLabel(String dictType, String value);

    /**
     * @return 字典标签对应的值；未命中返回 null
     */
    String getDictDataValue(String dictType, String label);
}
