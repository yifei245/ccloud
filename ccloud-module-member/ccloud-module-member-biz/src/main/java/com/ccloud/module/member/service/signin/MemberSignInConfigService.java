package com.ccloud.module.member.service.signin;

import com.ccloud.module.member.controller.admin.signin.vo.MemberSignInConfigSaveReqVO;
import com.ccloud.module.member.dal.dataobject.signin.MemberSignInConfigDO;

import java.util.List;

public interface MemberSignInConfigService {

    Long createSignInConfig(MemberSignInConfigSaveReqVO reqVO);

    void updateSignInConfig(MemberSignInConfigSaveReqVO reqVO);

    void deleteSignInConfig(Long id);

    MemberSignInConfigDO getSignInConfig(Long id);

    /**
     * @param status 为 null 时返回全部
     * @return 按天数升序
     */
    List<MemberSignInConfigDO> getSignInConfigList(Integer status);
}
