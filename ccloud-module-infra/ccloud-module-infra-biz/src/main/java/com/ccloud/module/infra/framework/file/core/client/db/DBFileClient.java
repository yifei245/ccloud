package com.ccloud.module.infra.framework.file.core.client.db;

import com.ccloud.module.infra.dal.dataobject.file.FileContentDO;
import com.ccloud.module.infra.dal.mysql.file.FileContentMapper;
import com.ccloud.module.infra.framework.file.core.client.AbstractFileClient;

/**
 * 数据库存储：文件内容存入 infra_file_content
 */
public class DBFileClient extends AbstractFileClient<DBFileClientConfig> {

    private final FileContentMapper fileContentMapper;

    public DBFileClient(Long id, DBFileClientConfig config, FileContentMapper fileContentMapper) {
        super(id, config);
        this.fileContentMapper = fileContentMapper;
    }

    @Override
    public String upload(byte[] content, String path, String type) {
        if (fileContentMapper.selectByConfigIdAndPath(getId(), path) == null) {
            fileContentMapper.insert(new FileContentDO().setConfigId(getId()).setPath(path).setContent(content));
        }
        return formatFileUrl(path);
    }

    @Override
    public void delete(String path) {
        fileContentMapper.deleteByConfigIdAndPath(getId(), path);
    }

    @Override
    public byte[] getContent(String path) {
        FileContentDO record = fileContentMapper.selectByConfigIdAndPath(getId(), path);
        return record != null ? record.getContent() : null;
    }
}
