package com.ccloud.module.system.dal.redis;

/**
 * system 模块的缓存名。
 *
 * <p>"#30m" 后缀由 TimeoutRedisCacheManager 解析为过期时间；读写与清除必须使用同一个常量，
 * 否则会落到不同的缓存里导致清除无效。多租户下缓存键会自动带上租户编号。
 */
public interface RedisKeyConstants {

    /**
     * 角色：key 为角色编号，value 为 RoleDO
     */
    String ROLE = "role#30m";

    /**
     * 用户拥有的角色编号：key 为用户编号，value 为 Set&lt;Long&gt;
     */
    String USER_ROLE_ID_LIST = "user_role_ids#30m";

    /**
     * 拥有某菜单的角色编号：key 为菜单编号，value 为 Set&lt;Long&gt;
     */
    String MENU_ROLE_ID_LIST = "menu_role_ids#30m";

    /**
     * 某权限标识对应的菜单编号：key 为权限标识，value 为 List&lt;Long&gt;。
     * 菜单为全局数据，缓存需登记在 ccloud.tenant.ignore-caches 中
     */
    String PERMISSION_MENU_ID_LIST = "permission_menu_ids#30m";

    /**
     * 部门的全部子孙部门编号：key 为部门编号，value 为 Set&lt;Long&gt;。数据权限"本部门及以下"依赖它
     */
    String DEPT_CHILDREN_ID_LIST = "dept_children_ids#30m";

    /**
     * 字典类型下的全部字典数据（含停用）：key 为字典类型，value 为 List&lt;DictDataDO&gt;。
     * 字典为全局数据，缓存需登记在 ccloud.tenant.ignore-caches 中
     */
    String DICT_DATA_LIST = "dict_data_list#1h";
}
