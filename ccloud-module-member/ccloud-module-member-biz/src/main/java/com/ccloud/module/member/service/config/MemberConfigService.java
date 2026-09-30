package com.ccloud.module.member.service.config;

import com.ccloud.module.member.controller.admin.config.vo.MemberConfigSaveReqVO;
import com.ccloud.module.member.dal.dataobject.config.MemberConfigDO;

/**
 * 会员配置，每个租户一条
 */
public interface MemberConfigService {

    /**
     * 当前租户没有配置时新增，已有时修改
     */
    void saveConfig(MemberConfigSaveReqVO reqVO);

    /**
     * @return 当前租户未配置时返回 null，由前端按默认值展示
     */
    MemberConfigDO getConfig();
}
