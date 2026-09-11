package com.ccloud.framework.datapermission.core.rule;

import java.util.List;

/**
 * 规则工厂。按当前 MyBatis 语句挑出该生效的规则。
 */
public interface DataPermissionRuleFactory {

    List<DataPermissionRule> getDataPermissionRules();

    /**
     * @param mappedStatementId MyBatis 语句标识
     * @return 该语句应生效的规则；为空表示不做数据权限改写
     */
    List<DataPermissionRule> getDataPermissionRule(String mappedStatementId);

    /**
     * 清理所有规则的请求级缓存
     */
    void clearRequestCache();
}
