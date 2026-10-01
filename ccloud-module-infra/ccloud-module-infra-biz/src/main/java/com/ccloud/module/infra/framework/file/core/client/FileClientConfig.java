package com.ccloud.module.infra.framework.file.core.client;

/**
 * 文件存储配置，各存储器的配置类实现它，并按存储器自带的校验注解校验
 */
public interface FileClientConfig {

    /**
     * @return 访问域名，如 http://127.0.0.1:28080，不以 / 结尾
     */
    String getDomain();
}
