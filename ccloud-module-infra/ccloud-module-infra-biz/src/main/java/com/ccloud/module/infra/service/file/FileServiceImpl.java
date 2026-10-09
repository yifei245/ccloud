package com.ccloud.module.infra.service.file;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.ccloud.framework.common.exception.ServiceException;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.infra.controller.admin.file.vo.FilePageReqVO;
import com.ccloud.module.infra.dal.dataobject.file.FileDO;
import com.ccloud.module.infra.dal.mysql.file.FileMapper;
import com.ccloud.module.infra.framework.file.config.FileProperties;
import com.ccloud.module.infra.framework.file.core.FileClientFactory;
import com.ccloud.module.infra.framework.file.core.client.FileClient;
import com.ccloud.module.infra.framework.file.core.client.s3.S3FileClient;
import com.ccloud.module.infra.framework.file.core.utils.FileTypeUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.infra.enums.ErrorCodeConstants.*;

@Service
@Slf4j
public class FileServiceImpl implements FileService {

    @Resource
    private FileMapper fileMapper;
    @Resource
    private FileClientFactory fileClientFactory;
    @Resource
    private FileProperties fileProperties;

    @Override
    public String createFile(String name, byte[] content) {
        if (content == null || content.length == 0) {
            throw exception(FILE_EMPTY);
        }
        if (content.length > fileProperties.getMaxSize().toBytes()) {
            throw exception(FILE_SIZE_EXCEEDED, FileUtil.readableFileSize(fileProperties.getMaxSize().toBytes()));
        }
        FileTypeUtils.FileType type = FileTypeUtils.detect(content, name);
        if (type == null) {
            throw exception(FILE_TYPE_NOT_ALLOWED, FileTypeUtils.detectMimeType(content, name));
        }
        // 按内容哈希命名：同一存储里相同内容只存一份，路径也无法按顺序猜出
        String path = DateUtil.format(new Date(), "yyyyMMdd") + "/" + DigestUtil.sha256Hex(content) + "." + type.extension();
        FileClient client = fileClientFactory.getMasterFileClient();
        String url;
        try {
            url = client.upload(content, path, type.mimeType());
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("[createFile][配置 {} 存储 {} 失败]", client.getId(), path, e);
            throw exception(FILE_UPLOAD_FAILED, e.getMessage());
        }
        fileMapper.insert(new FileDO().setConfigId(client.getId()).setName(StrUtil.maxLength(name, 250))
                .setPath(path).setUrl(url).setType(type.mimeType()).setSize(content.length));
        return url;
    }

    @Override
    public void deleteFile(Long id) {
        FileDO file = fileMapper.selectById(id);
        if (file == null) {
            throw exception(FILE_NOT_EXISTS);
        }
        fileMapper.deleteById(id);
        // 相同内容可能被其他租户的记录引用，计数时忽略租户
        Long refs = TenantUtils.executeIgnore(() -> fileMapper.selectCountByConfigIdAndPath(file.getConfigId(), file.getPath()));
        if (refs > 0) {
            return;
        }
        try {
            fileClientFactory.getFileClient(file.getConfigId()).delete(file.getPath());
        } catch (Exception e) {
            // 记录已删除，存储中的内容残留不影响使用，打日志后人工清理
            log.warn("[deleteFile][配置 {} 删除 {} 失败]", file.getConfigId(), file.getPath(), e);
        }
    }

    @Override
    public PageResult<FileDO> getFilePage(FilePageReqVO reqVO) {
        return fileMapper.selectPage(reqVO);
    }

    @Override
    public byte[] getFileContent(Long configId, String path) {
        try {
            FileClient client = fileClientFactory.getFileClient(configId);
            // 匿名读取只服务数据库与本地磁盘存储；S3 文件由存储桶自身公开读，代读会让匿名用户借服务端密钥读出桶内任意对象
            if (client instanceof S3FileClient) {
                return null;
            }
            return client.getContent(path);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("[getFileContent][配置 {} 读取 {} 失败]", configId, path, e);
            return null;
        }
    }
}
