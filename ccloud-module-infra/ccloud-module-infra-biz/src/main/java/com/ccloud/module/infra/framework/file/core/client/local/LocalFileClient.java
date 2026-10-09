package com.ccloud.module.infra.framework.file.core.client.local;

import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.exception.ServiceException;
import com.ccloud.module.infra.framework.file.core.client.AbstractFileClient;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.infra.enums.ErrorCodeConstants.FILE_CONFIG_BASE_PATH_INVALID;
import static com.ccloud.module.infra.enums.ErrorCodeConstants.FILE_PATH_INVALID;

/**
 * 本地磁盘存储
 */
@Slf4j
public class LocalFileClient extends AbstractFileClient<LocalFileClientConfig> {

    private final Path basePath;

    /**
     * @param localRoot 受控根目录，basePath 须等于它或位于它之下。保存配置时已校验，这里再校验一次，
     *                  防止库里的旧配置或直接改库绕过
     */
    public LocalFileClient(Long id, LocalFileClientConfig config, Path localRoot) {
        super(id, config);
        try {
            this.basePath = validateBasePath(config.getBasePath(), localRoot);
        } catch (ServiceException e) {
            log.error("[LocalFileClient][配置 {} 的根目录 {} 不在受控根目录 {} 之内，拒绝使用]",
                    id, config.getBasePath(), localRoot);
            throw e;
        }
    }

    /**
     * 校验 basePath 等于受控根目录或位于其下，两者都按真实路径比较（解析 ..、符号链接）
     *
     * @return 规范化后的 basePath
     * @throws ServiceException FILE_CONFIG_BASE_PATH_INVALID
     */
    public static Path validateBasePath(String basePath, Path localRoot) {
        Path root = realPath(localRoot);
        Path base;
        try {
            base = realPath(Paths.get(basePath));
        } catch (InvalidPathException e) {
            throw exception(FILE_CONFIG_BASE_PATH_INVALID, root);
        }
        if (!base.startsWith(root)) {
            throw exception(FILE_CONFIG_BASE_PATH_INVALID, root);
        }
        return base;
    }

    /**
     * 目录可能尚未创建：取最深的已存在祖先的真实路径（解析符号链接），再拼上其余部分
     */
    private static Path realPath(Path path) {
        Path absolute = path.toAbsolutePath().normalize();
        Path existing = absolute;
        while (existing != null && !Files.exists(existing)) {
            existing = existing.getParent();
        }
        if (existing == null) {
            return absolute;
        }
        try {
            return existing.toRealPath().resolve(existing.relativize(absolute)).normalize();
        } catch (IOException e) {
            return absolute;
        }
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
        try {
            Path file = basePath.resolve(path).normalize();
            if (StrUtil.isBlank(path) || Paths.get(path).isAbsolute() || !file.startsWith(basePath) || file.equals(basePath)) {
                throw exception(FILE_PATH_INVALID);
            }
            return file;
        } catch (InvalidPathException e) {
            // 路径含 NUL 等文件系统不接受的字符
            throw exception(FILE_PATH_INVALID);
        }
    }
}
