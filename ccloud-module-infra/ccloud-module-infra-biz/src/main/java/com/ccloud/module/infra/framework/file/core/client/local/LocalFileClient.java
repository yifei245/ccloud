package com.ccloud.module.infra.framework.file.core.client.local;

import cn.hutool.core.util.StrUtil;
import com.ccloud.module.infra.framework.file.core.client.AbstractFileClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.infra.enums.ErrorCodeConstants.FILE_PATH_INVALID;

/**
 * 本地磁盘存储
 */
public class LocalFileClient extends AbstractFileClient<LocalFileClientConfig> {

    private final Path basePath;

    public LocalFileClient(Long id, LocalFileClientConfig config) {
        super(id, config);
        this.basePath = Paths.get(config.getBasePath()).toAbsolutePath().normalize();
    }

    @Override
    public String upload(byte[] content, String path, String type) throws IOException {
        Path file = resolve(path);
        if (!Files.exists(file)) {
            Files.createDirectories(file.getParent());
            Files.write(file, content);
        }
        return formatFileUrl(path);
    }

    @Override
    public void delete(String path) throws IOException {
        Files.deleteIfExists(resolve(path));
    }

    @Override
    public byte[] getContent(String path) throws IOException {
        Path file = resolve(path);
        return Files.isRegularFile(file) ? Files.readAllBytes(file) : null;
    }

    /**
     * 规范化后必须仍在根目录内：路径来自 URL，不校验就能用 ../ 读写服务器上的任意文件
     */
    private Path resolve(String path) {
        Path file = basePath.resolve(path).normalize();
        if (StrUtil.isBlank(path) || Paths.get(path).isAbsolute() || !file.startsWith(basePath) || file.equals(basePath)) {
            throw exception(FILE_PATH_INVALID);
        }
        return file;
    }
}
