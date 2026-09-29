package com.ccloud.module.system.framework.datapermission.config;

import com.ccloud.framework.datapermission.core.rule.dept.DeptDataPermissionRuleCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * system 模块参与部门数据权限的表。
 *
 * <p>用表名而非实体类登记：实体类在后续任务中逐个落地，表名在 DDL 中已经确定，
 * 登记不必等实体类就位。表名写错时数据权限对该表静默不生效，由 Task 26 的
 * 用户分页集成测试兜住。
 */
@Configuration(proxyBeanMethods = false)
public class DataPermissionConfiguration {

    @Bean
    public DeptDataPermissionRuleCustomizer systemDeptDataPermissionRuleCustomizer() {
        return rule -> {
            // 用户：按所属部门过滤，"仅本人"时按用户编号过滤
            rule.addDeptColumn("system_users", "dept_id");
            rule.addUserColumn("system_users", "id");
            // 部门：按部门编号自身过滤
            rule.addDeptColumn("system_dept", "id");
        };
    }
}
