package com.ccloud.module.infra.service.config;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.framework.tenant.core.service.SystemTenantValidator;
import com.ccloud.module.infra.controller.admin.config.vo.ConfigPageReqVO;
import com.ccloud.module.infra.controller.admin.config.vo.ConfigSaveReqVO;
import com.ccloud.module.infra.dal.dataobject.config.ConfigDO;
import com.ccloud.module.infra.dal.mysql.config.ConfigMapper;
import com.ccloud.module.infra.dal.redis.RedisKeyConstants;
import com.ccloud.module.infra.enums.config.ConfigTypeEnum;
import jakarta.annotation.Resource;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.infra.enums.ErrorCodeConstants.*;

@Service
@Validated
public class ConfigServiceImpl implements ConfigService {

    @Resource
    private ConfigMapper configMapper;
    @Resource
    private SystemTenantValidator systemTenantValidator;

    @Override
    @CacheEvict(cacheNames = RedisKeyConstants.CONFIG, allEntries = true)
    public Long createConfig(ConfigSaveReqVO reqVO) {
        systemTenantValidator.validate(ONLY_SYSTEM_TENANT);
        validateConfigKeyUnique(null, reqVO.getKey());
        ConfigDO config = toDO(reqVO).setType(ConfigTypeEnum.CUSTOM.getType());
        configMapper.insert(config);
        return config.getId();
    }

    @Override
    @CacheEvict(cacheNames = RedisKeyConstants.CONFIG, allEntries = true)
    public void updateConfig(ConfigSaveReqVO reqVO) {
        systemTenantValidator.validate(ONLY_SYSTEM_TENANT);
        ConfigDO old = validateConfigExists(reqVO.getId());
        // 系统内置参数的键名被代码引用，改了就等于删除
        if (ConfigTypeEnum.SYSTEM.getType().equals(old.getType()) && !old.getConfigKey().equals(reqVO.getKey())) {
            throw exception(CONFIG_KEY_CAN_NOT_UPDATE_SYSTEM_TYPE);
        }
        validateConfigKeyUnique(reqVO.getId(), reqVO.getKey());
        configMapper.updateById(toDO(reqVO).setType(null));
    }

    @Override
    @CacheEvict(cacheNames = RedisKeyConstants.CONFIG, allEntries = true)
    public void deleteConfig(Long id) {
        systemTenantValidator.validate(ONLY_SYSTEM_TENANT);
        ConfigDO config = validateConfigExists(id);
        if (ConfigTypeEnum.SYSTEM.getType().equals(config.getType())) {
            throw exception(CONFIG_CAN_NOT_DELETE_SYSTEM_TYPE);
        }
        configMapper.deleteById(id);
    }

    @Override
    public ConfigDO getConfig(Long id) {
        return configMapper.selectById(id);
    }

    @Override
    @Cacheable(cacheNames = RedisKeyConstants.CONFIG, key = "#key", unless = "#result == null")
    public ConfigDO getConfigByKey(String key) {
        return configMapper.selectByKey(key);
    }

    @Override
    public PageResult<ConfigDO> getConfigPage(ConfigPageReqVO reqVO) {
        return configMapper.selectPage(reqVO);
    }

    private ConfigDO validateConfigExists(Long id) {
        ConfigDO config = id == null ? null : configMapper.selectById(id);
        if (config == null) {
            throw exception(CONFIG_NOT_EXISTS);
        }
        return config;
    }

    private void validateConfigKeyUnique(Long id, String key) {
        ConfigDO config = configMapper.selectByKey(key);
        if (config != null && !config.getId().equals(id)) {
            throw exception(CONFIG_KEY_DUPLICATE);
        }
    }

    private static ConfigDO toDO(ConfigSaveReqVO reqVO) {
        return BeanUtils.toBean(reqVO, ConfigDO.class).setConfigKey(reqVO.getKey());
    }
}
