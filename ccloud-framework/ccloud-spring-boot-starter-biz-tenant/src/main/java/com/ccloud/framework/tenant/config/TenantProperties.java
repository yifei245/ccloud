package com.ccloud.framework.tenant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 多租户配置
 */
@ConfigurationProperties(prefix = "ccloud.tenant")
@Data
public class TenantProperties {

    private Boolean enable = true;

    /**
     * 不做租户过滤的表。
     *
     * <p>两类表需要在此登记：一是租户自身的元数据表（租户表、套餐表），
     * 查它们时还没有租户身份；二是全局共享的字典类表（菜单、字典），
     * 所有租户看到的是同一份。
     */
    private Set<String> ignoreTables = Collections.emptySet();

    /**
     * 不做租户校验的请求路径。登录、获取租户编号等接口在拿到身份之前就要访问。
     */
    private List<String> ignoreUrls = Collections.emptyList();
}
