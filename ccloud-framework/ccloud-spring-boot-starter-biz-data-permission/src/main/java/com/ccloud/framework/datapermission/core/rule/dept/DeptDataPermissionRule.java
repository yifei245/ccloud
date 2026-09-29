package com.ccloud.framework.datapermission.core.rule.dept;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ccloud.framework.datapermission.core.rule.DataPermissionRule;
import com.ccloud.framework.security.core.util.SecurityFrameworkUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;

import java.util.*;
import java.util.function.Supplier;

/**
 * 部门维度的数据权限规则。
 */
@Slf4j
@RequiredArgsConstructor
public class DeptDataPermissionRule implements DataPermissionRule {

    private final DeptDataPermissionFrameworkService deptDataPermissionFrameworkService;

    /**
     * 表名 -> 部门字段
     */
    private final Map<String, String> deptColumns = new HashMap<>();

    /**
     * 表名 -> 用户字段
     */
    private final Map<String, String> userColumns = new HashMap<>();

    private final Set<String> tableNames = new HashSet<>();

    /**
     * 以实体类登记、尚未解析出表名的条目。
     *
     * <p>本规则是 MyBatis 拦截器的依赖，而拦截器又是 MyBatis 自动配置的依赖——
     * 规则 Bean 创建时 MyBatis 还没解析过任何实体，此刻 {@code TableInfoHelper}
     * 一定取不到表信息。因此登记时只记住实体类，首次使用时才解析表名。
     */
    private final Map<Class<?>, String> pendingDeptColumns = new HashMap<>();
    private final Map<Class<?>, String> pendingUserColumns = new HashMap<>();

    private volatile boolean resolved = false;

    /**
     * 同一请求内缓存权限查询结果。一条 SQL 可能涉及多张表，
     * 每张表都回查一次 RPC 是不可接受的。
     */
    private final ThreadLocal<Map<Long, DeptDataPermissionRespDTO>> requestCache =
            ThreadLocal.withInitial(HashMap::new);

    public void addDeptColumn(Class<?> entityClass, String columnName) {
        pendingDeptColumns.put(entityClass, columnName);
        resolved = false;
    }

    public void addDeptColumn(String tableName, String columnName) {
        deptColumns.put(tableName, columnName);
        tableNames.add(tableName);
    }

    public void addUserColumn(Class<?> entityClass, String columnName) {
        pendingUserColumns.put(entityClass, columnName);
        resolved = false;
    }

    public void addUserColumn(String tableName, String columnName) {
        userColumns.put(tableName, columnName);
        tableNames.add(tableName);
    }

    private static String tableNameOf(Class<?> entityClass) {
        var tableInfo = TableInfoHelper.getTableInfo(entityClass);
        if (tableInfo == null) {
            throw new IllegalStateException("找不到实体 " + entityClass.getName()
                    + " 对应的表信息，请确认它被 MyBatis Plus 扫描到");
        }
        return tableInfo.getTableName();
    }

    /**
     * 首次使用时把以实体类登记的条目解析成表名
     */
    private void ensureResolved() {
        if (resolved) {
            return;
        }
        synchronized (this) {
            if (resolved) {
                return;
            }
            pendingDeptColumns.forEach((clazz, column) -> addDeptColumn(tableNameOf(clazz), column));
            pendingUserColumns.forEach((clazz, column) -> addUserColumn(tableNameOf(clazz), column));
            resolved = true;
        }
    }

    @Override
    public Set<String> getTableNames() {
        ensureResolved();
        return tableNames;
    }

    @Override
    public Expression getExpression(String tableName, Supplier<String> tableAlias) {
        ensureResolved();
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        // 没有登录身份时不过滤。定时任务、消息消费等场景本就不该受数据权限约束
        if (userId == null) {
            return null;
        }
        String deptColumn = deptColumns.get(tableName);
        String userColumn = userColumns.get(tableName);
        // 该表未登记任何维度，与本规则无关
        if (deptColumn == null && userColumn == null) {
            return null;
        }

        DeptDataPermissionRespDTO permission = requestCache.get()
                .computeIfAbsent(userId, deptDataPermissionFrameworkService::getDeptDataPermission);
        if (permission == null || permission.isAll()) {
            return null;
        }

        String prefix = columnPrefix(tableName, tableAlias);
        String deptCondition = buildDeptCondition(prefix, deptColumn, permission.getDeptIds());
        String userCondition = buildUserCondition(prefix, userColumn, userId, permission.isSelf());

        if (deptCondition == null && userCondition == null) {
            // 既无部门权限也无本人权限 —— 生成恒假条件，让查询返回空集。
            // 绝不能返回 null：那等同于"不过滤"，会把全部数据暴露给一个没有任何数据权限的用户
            String column = deptColumn != null ? deptColumn : userColumn;
            return parse(prefix + "." + column + " IS NULL");
        }
        if (deptCondition == null) {
            return parse(userCondition);
        }
        if (userCondition == null) {
            return parse(deptCondition);
        }
        // 两个维度是并集关系，加括号避免与外层 AND 结合时优先级出错
        return parse("(" + deptCondition + " OR " + userCondition + ")");
    }

    /**
     * 条件以 SQL 文本拼装后再解析成表达式。
     *
     * <p>不直接构造 JSqlParser 的 AST：其 {@code ExpressionList}、{@code Parenthesis}
     * 等类型的构造方式在小版本间反复变动（5.2 的 ExpressionList 不自带括号，
     * Parenthesis 也不能用构造器直接包裹表达式）。拼文本再解析只依赖 SQL 语法本身，
     * 不依赖这些内部类型，且生成结果一眼可读。
     *
     * <p>拼接的值全部来自数据库中的部门编号与当前登录用户编号，均为 Long，
     * 不存在注入面。
     */
    private static Expression parse(String condition) {
        try {
            return CCJSqlParserUtil.parseCondExpression(condition);
        } catch (Exception e) {
            throw new IllegalStateException("数据权限条件解析失败: " + condition, e);
        }
    }

    private String buildDeptCondition(String prefix, String deptColumn, Set<Long> deptIds) {
        if (deptColumn == null || CollUtil.isEmpty(deptIds)) {
            return null;
        }
        String values = deptIds.stream().sorted().map(String::valueOf)
                .collect(java.util.stream.Collectors.joining(", "));
        return prefix + "." + deptColumn + " IN (" + values + ")";
    }

    private String buildUserCondition(String prefix, String userColumn, Long userId, boolean self) {
        if (userColumn == null || !self) {
            return null;
        }
        return prefix + "." + userColumn + " = " + userId;
    }

    /**
     * 用别名限定列名。SQL 里写了别名时不加限定会产生歧义列错误。
     */
    private static String columnPrefix(String tableName, Supplier<String> tableAlias) {
        String alias = tableAlias != null ? tableAlias.get() : null;
        return alias != null ? alias : tableName;
    }

    /**
     * 清理请求级缓存。由拦截器在每条语句处理完后调用。
     */
    @Override
    public void clearRequestCache() {
        requestCache.remove();
    }
}
