package com.ccloud.module.infra.service.config;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.infra.controller.admin.config.vo.ConfigPageReqVO;
import com.ccloud.module.infra.controller.admin.config.vo.ConfigSaveReqVO;
import com.ccloud.module.infra.dal.dataobject.config.ConfigDO;

public interface ConfigService {

    /**
     * 创建参数，类型固定为自定义
     *
     * @throws com.ccloud.framework.common.exception.ServiceException 键名重复时抛 CONFIG_KEY_DUPLICATE
     */
    Long createConfig(ConfigSaveReqVO reqVO);

    /**
     * 修改参数。系统内置参数不能改键名，类型不可修改
     */
    void updateConfig(ConfigSaveReqVO reqVO);

    /**
     * @throws com.ccloud.framework.common.exception.ServiceException 系统内置参数抛 CONFIG_CAN_NOT_DELETE_SYSTEM_TYPE
     */
    void deleteConfig(Long id);

    ConfigDO getConfig(Long id);

    /**
     * 按键名查询，有 Redis 缓存
     *
     * @return 不存在返回 null
     */
    ConfigDO getConfigByKey(String key);

    PageResult<ConfigDO> getConfigPage(ConfigPageReqVO reqVO);
}
