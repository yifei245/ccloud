package com.ccloud.framework.i18n.core;

import java.util.Map;

/**
 * 文案查询契约。实现由 system 模块经 RPC 提供。
 */
public interface I18nMessageFrameworkService {

    /**
     * @return 指定语言的全部文案，key 为文案编码
     */
    Map<String, String> getMessages(String locale);
}
