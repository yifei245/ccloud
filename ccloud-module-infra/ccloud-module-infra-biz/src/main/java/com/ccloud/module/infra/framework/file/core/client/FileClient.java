package com.ccloud.module.infra.framework.file.core.client;

/**
 * 文件存储客户端
 */
public interface FileClient {

    /**
     * @return 文件配置编号
     */
    Long getId();

    /**
     * 上传文件；同一路径已存在时不重复写入
     *
     * @return 访问地址
     */
    String upload(byte[] content, String path, String type) throws Exception;

    void delete(String path) throws Exception;

    /**
     * @return 文件内容，不存在返回 null
     */
    byte[] getContent(String path) throws Exception;
}
