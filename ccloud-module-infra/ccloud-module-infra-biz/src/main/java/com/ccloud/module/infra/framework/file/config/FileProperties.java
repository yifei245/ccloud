package com.ccloud.module.infra.framework.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * 文件上传配置。由 {@link FileConfiguration} 注册
 */
@Data
@ConfigurationProperties("ccloud.infra.file")
public class FileProperties {

    /**
     * 单个文件的大小上限。Spring 的 multipart 上限须大于它
     */
    private DataSize maxSize = DataSize.ofMegabytes(10);
}
