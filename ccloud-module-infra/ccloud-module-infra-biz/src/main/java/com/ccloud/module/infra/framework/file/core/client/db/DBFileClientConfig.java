package com.ccloud.module.infra.framework.file.core.client.db;

import com.ccloud.module.infra.framework.file.core.client.FileClientConfig;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

/**
 * 数据库存储配置
 */
@Data
public class DBFileClientConfig implements FileClientConfig {

    @NotBlank(message = "访问域名不能为空")
    @URL(message = "访问域名须为 http(s) 地址")
    private String domain;
}
