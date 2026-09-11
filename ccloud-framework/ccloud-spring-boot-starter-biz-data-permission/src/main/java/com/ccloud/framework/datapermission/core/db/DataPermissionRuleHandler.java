package com.ccloud.framework.datapermission.core.db;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import com.ccloud.framework.datapermission.core.rule.DataPermissionRule;
import com.ccloud.framework.datapermission.core.rule.DataPermissionRuleFactory;
import lombok.RequiredArgsConstructor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.schema.Table;

import java.util.List;

/**
 * 把数据权限规则接到 MyBatis Plus 的 SQL 改写上。
 */
@RequiredArgsConstructor
public class DataPermissionRuleHandler implements MultiDataPermissionHandler {

    private final DataPermissionRuleFactory ruleFactory;

    @Override
    public Expression getSqlSegment(Table table, Expression where, String mappedStatementId) {
        List<DataPermissionRule> rules = ruleFactory.getDataPermissionRule(mappedStatementId);
        if (CollUtil.isEmpty(rules)) {
            return null;
        }
        // MP 未提供取表名的静态方法，且表名可能带库名前缀（db.table），只取最后一段
        String tableName = table.getName();
        int dotIndex = tableName.lastIndexOf('.');
        if (dotIndex >= 0) {
            tableName = tableName.substring(dotIndex + 1);
        }
        Expression result = null;
        for (DataPermissionRule rule : rules) {
            if (!rule.getTableNames().contains(tableName)) {
                continue;
            }
            Expression expression = rule.getExpression(tableName,
                    () -> table.getAlias() != null ? table.getAlias().getName() : null);
            if (expression == null) {
                continue;
            }
            // 多条规则之间取交集：每条规则都是一道独立的限制，都必须满足
            result = result == null ? expression : new AndExpression(result, expression);
        }
        return result;
    }
}
