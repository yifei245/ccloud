package com.ccloud.module.infra.dal.mysql.file;

import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import com.ccloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.ccloud.module.infra.dal.dataobject.file.FileContentDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileContentMapper extends BaseMapperX<FileContentDO> {

    default FileContentDO selectByConfigIdAndPath(Long configId, String path) {
        return selectOne(new LambdaQueryWrapperX<FileContentDO>()
                .eq(FileContentDO::getConfigId, configId).eq(FileContentDO::getPath, path)
                .orderByDesc(FileContentDO::getId).last("LIMIT 1"));
    }

    default int deleteByConfigIdAndPath(Long configId, String path) {
        return delete(new LambdaQueryWrapperX<FileContentDO>()
                .eq(FileContentDO::getConfigId, configId).eq(FileContentDO::getPath, path));
    }
}
