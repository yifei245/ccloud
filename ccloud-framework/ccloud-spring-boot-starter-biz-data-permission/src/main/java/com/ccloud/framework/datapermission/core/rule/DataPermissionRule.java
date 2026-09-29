package com.ccloud.framework.datapermission.core.rule;

import net.sf.jsqlparser.expression.Expression;

import java.util.Set;
import java.util.function.Supplier;

/**
 * 数据权限规则。一条规则负责一个维度（部门、项目、业务线等）。
 */
public interface DataPermissionRule {

    /**
     * @return 该规则关心的表名。不在其中的表不会被改写
     */
    Set<String> getTableNames();

    /**
     * 生成该表的过滤条件
     *
     * @param tableName  表名
     * @param tableAlias 表别名提供者。SQL 里可能写了别名，条件必须用别名限定列，
     *                   否则多表关联时会产生歧义列错误
     * @return 过滤条件；返回 null 表示不过滤
     */
    Expression getExpression(String tableName, Supplier<String> tableAlias);

    /**
     * 清理请求级缓存。规则若在请求内缓存了权限查询结果，必须实现它，
     * 否则线程被复用时会一直沿用上一个用户的权限。
     */
    default void clearRequestCache() {
    }
}
