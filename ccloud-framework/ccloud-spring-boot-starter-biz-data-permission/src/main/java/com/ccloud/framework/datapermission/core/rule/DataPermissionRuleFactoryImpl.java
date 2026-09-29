package com.ccloud.framework.datapermission.core.rule;

import cn.hutool.core.collection.CollUtil;
import com.ccloud.framework.datapermission.core.annotation.DataPermission;
import com.ccloud.framework.datapermission.core.aop.DataPermissionContextHolder;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 规则工厂实现。依据当前上下文中的 {@link DataPermission} 注解挑选规则。
 */
@RequiredArgsConstructor
public class DataPermissionRuleFactoryImpl implements DataPermissionRuleFactory {

    private final List<DataPermissionRule> rules;

    @Override
    public List<DataPermissionRule> getDataPermissionRules() {
        return rules;
    }

    @Override
    public List<DataPermissionRule> getDataPermissionRule(String mappedStatementId) {
        if (CollUtil.isEmpty(rules)) {
            return List.of();
        }
        DataPermission dataPermission = DataPermissionContextHolder.get();
        // 没有注解就不改写 SQL —— 数据权限是显式开启的
        if (dataPermission == null || !dataPermission.enable()) {
            return List.of();
        }
        if (dataPermission.includeRules().length == 0 && dataPermission.excludeRules().length == 0) {
            return rules;
        }
        List<DataPermissionRule> result = new ArrayList<>(rules.size());
        for (DataPermissionRule rule : rules) {
            if (dataPermission.includeRules().length > 0
                    && !containsRule(dataPermission.includeRules(), rule)) {
                continue;
            }
            if (containsRule(dataPermission.excludeRules(), rule)) {
                continue;
            }
            result.add(rule);
        }
        return result;
    }

    @Override
    public void clearRequestCache() {
        rules.forEach(DataPermissionRule::clearRequestCache);
    }

    private static boolean containsRule(Class<? extends DataPermissionRule>[] classes, DataPermissionRule rule) {
        return Arrays.stream(classes).anyMatch(clazz -> clazz.isInstance(rule));
    }
}
