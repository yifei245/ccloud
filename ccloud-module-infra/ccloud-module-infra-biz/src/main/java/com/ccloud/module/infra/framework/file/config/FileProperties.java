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

    /**
     * 本地磁盘存储的受控根目录：文件配置的 basePath 必须等于它或位于它之下。
     * 匿名读取接口会输出 basePath 内的任意文件，不加限制时 basePath=/proc/self 即可读到进程环境变量中的密钥
     */
    private String localRoot = "/app/files";
}
