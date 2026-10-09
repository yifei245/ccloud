package com.ccloud.module.infra.framework.file.core;

import com.ccloud.framework.common.util.json.JsonUtils;
import com.ccloud.module.infra.dal.dataobject.file.FileConfigDO;
import com.ccloud.module.infra.dal.mysql.file.FileConfigMapper;
import com.ccloud.module.infra.dal.mysql.file.FileContentMapper;
import com.ccloud.module.infra.framework.file.config.FileProperties;
import com.ccloud.module.infra.framework.file.core.client.FileClient;
import com.ccloud.module.infra.framework.file.core.client.FileClientConfig;
import com.ccloud.module.infra.framework.file.core.client.db.DBFileClient;
import com.ccloud.module.infra.framework.file.core.client.db.DBFileClientConfig;
import com.ccloud.module.infra.framework.file.core.client.local.LocalFileClient;
import com.ccloud.module.infra.framework.file.core.client.local.LocalFileClientConfig;
import com.ccloud.module.infra.framework.file.core.client.s3.S3FileClient;
import com.ccloud.module.infra.framework.file.core.client.s3.S3FileClientConfig;
import com.ccloud.module.infra.framework.file.core.enums.FileStorageEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.infra.enums.ErrorCodeConstants.FILE_CONFIG_NOT_EXISTS;

/**
 * 文件存储客户端工厂：按配置编号缓存客户端，配置变更时由 {@link FileConfigRefresher} 清空
 */
@Component
public class FileClientFactory {

    private final ConcurrentMap<Long, FileClient> clients = new ConcurrentHashMap<>();

    @Resource
    private FileConfigMapper fileConfigMapper;
    @Resource
    private FileContentMapper fileContentMapper;
    @Resource
    private FileProperties fileProperties;

    /**
     * @throws com.ccloud.framework.common.exception.ServiceException 配置不存在时抛 FILE_CONFIG_NOT_EXISTS
     */
    public FileClient getFileClient(Long configId) {
        return clients.computeIfAbsent(configId, id -> {
            FileConfigDO config = fileConfigMapper.selectById(id);
            if (config == null) {
                throw exception(FILE_CONFIG_NOT_EXISTS);
            }
            return createClient(config);
        });
    }

    /**
     * @throws com.ccloud.framework.common.exception.ServiceException 没有主配置时抛 FILE_CONFIG_NOT_EXISTS
     */
    public FileClient getMasterFileClient() {
        FileConfigDO master = fileConfigMapper.selectMaster();
        if (master == null) {
            throw exception(FILE_CONFIG_NOT_EXISTS);
        }
        return getFileClient(master.getId());
    }

    public void invalidateAll() {
        clients.clear();
    }

    FileClient createClient(FileConfigDO config) {
        FileStorageEnum storage = FileStorageEnum.valueOf(config.getStorage());
        FileClientConfig clientConfig = JsonUtils.parseObject(config.getConfig(), storage.getConfigClass());
        return switch (storage) {
            case DB -> new DBFileClient(config.getId(), (DBFileClientConfig) clientConfig, fileContentMapper);
            case LOCAL -> new LocalFileClient(config.getId(), (LocalFileClientConfig) clientConfig,
                    Path.of(fileProperties.getLocalRoot()));
            case S3 -> new S3FileClient(config.getId(), (S3FileClientConfig) clientConfig);
        };
    }
}
