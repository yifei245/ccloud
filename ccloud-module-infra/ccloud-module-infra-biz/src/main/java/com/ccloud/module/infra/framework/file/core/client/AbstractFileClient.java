package com.ccloud.module.infra.framework.file.core.client;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

@Getter
public abstract class AbstractFileClient<C extends FileClientConfig> implements FileClient {

    private final Long id;
    protected final C config;

    protected AbstractFileClient(Long id, C config) {
        this.id = id;
        this.config = config;
    }

    /**
     * 数据库与本地磁盘存储没有自己的访问地址，由 infra 的读取接口代为输出
     */
    protected String formatFileUrl(String path) {
        return StrUtil.removeSuffix(config.getDomain(), "/") + "/admin-api/infra/file/" + id + "/get/" + path;
    }
}
