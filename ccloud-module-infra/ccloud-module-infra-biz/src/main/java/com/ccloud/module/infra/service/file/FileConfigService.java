package com.ccloud.module.infra.service.file;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.infra.controller.admin.file.vo.FileConfigPageReqVO;
import com.ccloud.module.infra.controller.admin.file.vo.FileConfigSaveReqVO;
import com.ccloud.module.infra.dal.dataobject.file.FileConfigDO;

/**
 * 文件配置 Service
 */
public interface FileConfigService {

    /**
     * 创建文件配置。没有主配置时自动成为主配置，否则不是
     */
    Long createFileConfig(FileConfigSaveReqVO reqVO);

    /**
     * 修改文件配置，不改变是否为主配置
     */
    void updateFileConfig(FileConfigSaveReqVO reqVO);

    /**
     * 设为主配置，原主配置随之取消
     */
    void updateFileConfigMaster(Long id);

    /**
     * 删除文件配置，主配置不能删除
     */
    void deleteFileConfig(Long id);

    FileConfigDO getFileConfig(Long id);

    PageResult<FileConfigDO> getFileConfigPage(FileConfigPageReqVO reqVO);

    /**
     * 上传一个测试文件，验证配置可用
     *
     * @return 测试文件的访问地址
     */
    String testFileConfig(Long id);
}
