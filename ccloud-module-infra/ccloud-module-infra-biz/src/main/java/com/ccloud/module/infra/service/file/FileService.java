package com.ccloud.module.infra.service.file;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.infra.controller.admin.file.vo.FilePageReqVO;
import com.ccloud.module.infra.dal.dataobject.file.FileDO;

/**
 * 文件 Service
 */
public interface FileService {

    /**
     * 保存文件到主配置的存储。类型按内容识别，只收白名单类型
     *
     * @param name    原始文件名，可为空
     * @param content 文件内容
     * @return 访问地址
     */
    String createFile(String name, byte[] content);

    /**
     * 删除文件记录；同一内容再无记录引用时，一并删除存储中的内容
     */
    void deleteFile(Long id);

    PageResult<FileDO> getFilePage(FilePageReqVO reqVO);

    /**
     * 供匿名读取接口使用，只服务数据库与本地磁盘存储
     *
     * @return 文件内容；不存在或存储器为 S3 时返回 null
     */
    byte[] getFileContent(Long configId, String path);
}
