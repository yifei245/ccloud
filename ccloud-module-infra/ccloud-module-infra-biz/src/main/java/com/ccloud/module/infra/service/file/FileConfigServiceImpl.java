package com.ccloud.module.infra.service.file;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ccloud.framework.common.exception.ServiceException;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.json.JsonUtils;
import com.ccloud.framework.operatelog.core.util.DesensitizeUtils;
import com.ccloud.module.infra.controller.admin.file.vo.FileConfigPageReqVO;
import com.ccloud.module.infra.controller.admin.file.vo.FileConfigSaveReqVO;
import com.ccloud.module.infra.dal.dataobject.file.FileConfigDO;
import com.ccloud.module.infra.dal.mysql.file.FileConfigMapper;
import com.ccloud.module.infra.framework.file.core.FileClientFactory;
import com.ccloud.module.infra.framework.file.core.FileConfigRefresher;
import com.ccloud.module.infra.framework.file.core.client.FileClientConfig;
import com.ccloud.module.infra.framework.file.core.enums.FileStorageEnum;
import com.ccloud.module.infra.framework.tenant.core.SystemTenantValidator;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.infra.enums.ErrorCodeConstants.*;

@Service
@Validated
public class FileConfigServiceImpl implements FileConfigService {

    @Resource
    private FileConfigMapper fileConfigMapper;
    @Resource
    private FileClientFactory fileClientFactory;
    @Resource
    private FileConfigRefresher fileConfigRefresher;
    @Resource
    private Validator validator;
    @Resource
    private SystemTenantValidator systemTenantValidator;

    @Override
    public Long createFileConfig(FileConfigSaveReqVO reqVO) {
        systemTenantValidator.validate();
        FileClientConfig config = parseConfig(reqVO.getStorage(), reqVO.getConfig(), null);
        FileConfigDO fileConfig = new FileConfigDO().setName(reqVO.getName()).setStorage(reqVO.getStorage())
                .setRemark(reqVO.getRemark()).setConfig(JsonUtils.toJsonString(config))
                // 没有主配置时第一条自动成为主配置，否则上传会失败
                .setMaster(fileConfigMapper.selectMaster() == null);
        fileConfigMapper.insert(fileConfig);
        fileConfigRefresher.publish();
        return fileConfig.getId();
    }

    @Override
    public void updateFileConfig(FileConfigSaveReqVO reqVO) {
        systemTenantValidator.validate();
        FileConfigDO old = validateFileConfigExists(reqVO.getId());
        FileClientConfig config = parseConfig(reqVO.getStorage(), reqVO.getConfig(), old);
        fileConfigMapper.updateById(new FileConfigDO().setId(reqVO.getId()).setName(reqVO.getName())
                .setStorage(reqVO.getStorage()).setRemark(reqVO.getRemark()).setConfig(JsonUtils.toJsonString(config)));
        fileConfigRefresher.publish();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFileConfigMaster(Long id) {
        systemTenantValidator.validate();
        validateFileConfigExists(id);
        fileConfigMapper.update(new FileConfigDO().setMaster(false),
                new LambdaUpdateWrapper<FileConfigDO>().eq(FileConfigDO::getMaster, true));
        fileConfigMapper.updateById(new FileConfigDO().setId(id).setMaster(true));
        fileConfigRefresher.publish();
    }

    @Override
    public void deleteFileConfig(Long id) {
        systemTenantValidator.validate();
        FileConfigDO config = validateFileConfigExists(id);
        if (Boolean.TRUE.equals(config.getMaster())) {
            throw exception(FILE_CONFIG_DELETE_FAIL_MASTER);
        }
        fileConfigMapper.deleteById(id);
        fileConfigRefresher.publish();
    }

    @Override
    public FileConfigDO getFileConfig(Long id) {
        return fileConfigMapper.selectById(id);
    }

    @Override
    public PageResult<FileConfigDO> getFileConfigPage(FileConfigPageReqVO reqVO) {
        return fileConfigMapper.selectPage(reqVO);
    }

    @Override
    public String testFileConfig(Long id) {
        systemTenantValidator.validate();
        validateFileConfigExists(id);
        byte[] content = ("ccloud 文件配置测试 " + LocalDateTime.now()).getBytes(StandardCharsets.UTF_8);
        String path = "test/" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + "-" + IdUtil.fastSimpleUUID() + ".txt";
        try {
            return fileClientFactory.getFileClient(id).upload(content, path, "text/plain");
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw exception(FILE_UPLOAD_FAILED, e.getMessage());
        }
    }

    /**
     * 按存储器解析并校验配置。提交的密钥为掩码时沿用原值：前端拿到的就是掩码，原样提交不应清空密钥
     */
    FileClientConfig parseConfig(Integer storage, Map<String, Object> config, FileConfigDO old) {
        FileStorageEnum storageEnum = FileStorageEnum.valueOf(storage);
        if (storageEnum == null) {
            throw exception(FILE_CONFIG_INVALID, "不支持的存储器 " + storage);
        }
        Map<String, Object> merged = new HashMap<>(config);
        if (old != null && storage.equals(old.getStorage())) {
            Map<String, Object> oldConfig = JsonUtils.parseObject(old.getConfig(), new TypeReference<>() {});
            for (String key : storageEnum.getSecretKeys()) {
                if (DesensitizeUtils.MASK.equals(merged.get(key))) {
                    merged.put(key, oldConfig.get(key));
                }
            }
        }
        FileClientConfig parsed = JsonUtils.parseObject(JsonUtils.toJsonString(merged), storageEnum.getConfigClass());
        Set<ConstraintViolation<FileClientConfig>> violations = validator.validate(parsed);
        if (!violations.isEmpty()) {
            throw exception(FILE_CONFIG_INVALID, violations.iterator().next().getMessage());
        }
        return parsed;
    }

    private FileConfigDO validateFileConfigExists(Long id) {
        FileConfigDO config = id == null ? null : fileConfigMapper.selectById(id);
        if (config == null) {
            throw exception(FILE_CONFIG_NOT_EXISTS);
        }
        return config;
    }
}
