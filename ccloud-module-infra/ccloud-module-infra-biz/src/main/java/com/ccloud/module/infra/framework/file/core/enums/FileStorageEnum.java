package com.ccloud.module.infra.framework.file.core.enums;

import com.ccloud.module.infra.framework.file.core.client.FileClientConfig;
import com.ccloud.module.infra.framework.file.core.client.db.DBFileClientConfig;
import com.ccloud.module.infra.framework.file.core.client.local.LocalFileClientConfig;
import com.ccloud.module.infra.framework.file.core.client.s3.S3FileClientConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Set;

/**
 * 文件存储器，取值与字典 infra_file_storage 一致
 */
@Getter
@AllArgsConstructor
public enum FileStorageEnum {

    DB(1, DBFileClientConfig.class, Set.of()),
    LOCAL(10, LocalFileClientConfig.class, Set.of()),
    S3(20, S3FileClientConfig.class, Set.of("accessSecret"));

    private final Integer storage;
    private final Class<? extends FileClientConfig> configClass;
    /**
     * 返回给前端时需要掩码的配置项
     */
    private final Set<String> secretKeys;

    /**
     * @return 不支持的存储器返回 null
     */
    public static FileStorageEnum valueOf(Integer storage) {
        return Arrays.stream(values()).filter(e -> e.getStorage().equals(storage)).findFirst().orElse(null);
    }
}
