package com.ccloud.framework.datapermission.core.rule.dept;

/**
 * 由各业务模块实现，登记本模块哪些表参与部门维度的数据权限。
 *
 * <p>登记制而非扫描制：哪张表的哪个字段代表"归属部门"是业务语义，
 * 猜不出来，也不该猜。
 */
@FunctionalInterface
public interface DeptDataPermissionRuleCustomizer {

    void customize(DeptDataPermissionRule rule);
}
