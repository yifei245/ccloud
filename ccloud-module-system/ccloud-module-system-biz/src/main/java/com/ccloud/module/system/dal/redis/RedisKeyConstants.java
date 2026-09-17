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
     * 某权限标识对应的菜单编号：key 为权限标识，value 为 List&lt;Long&gt;
     */
    String PERMISSION_MENU_ID_LIST = "permission_menu_ids#30m";
}
