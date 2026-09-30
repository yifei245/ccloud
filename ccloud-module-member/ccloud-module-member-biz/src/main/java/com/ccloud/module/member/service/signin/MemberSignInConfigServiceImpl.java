package com.ccloud.module.member.service.signin;

import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.member.controller.admin.signin.vo.MemberSignInConfigSaveReqVO;
import com.ccloud.module.member.dal.dataobject.signin.MemberSignInConfigDO;
import com.ccloud.module.member.dal.mysql.signin.MemberSignInConfigMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.member.enums.ErrorCodeConstants.SIGN_IN_CONFIG_DAY_EXISTS;
import static com.ccloud.module.member.enums.ErrorCodeConstants.SIGN_IN_CONFIG_NOT_EXISTS;

@Service
@Validated
public class MemberSignInConfigServiceImpl implements MemberSignInConfigService {

    @Resource
    private MemberSignInConfigMapper configMapper;

    @Override
    public Long createSignInConfig(MemberSignInConfigSaveReqVO reqVO) {
        validateDayUnique(null, reqVO.getDay());
        MemberSignInConfigDO config = BeanUtils.toBean(reqVO, MemberSignInConfigDO.class);
        configMapper.insert(config);
        return config.getId();
    }

    @Override
    public void updateSignInConfig(MemberSignInConfigSaveReqVO reqVO) {
        validateConfigExists(reqVO.getId());
        validateDayUnique(reqVO.getId(), reqVO.getDay());
        configMapper.updateById(BeanUtils.toBean(reqVO, MemberSignInConfigDO.class));
    }

    @Override
    public void deleteSignInConfig(Long id) {
        validateConfigExists(id);
        configMapper.deleteById(id);
    }

    private void validateConfigExists(Long id) {
        if (id == null || configMapper.selectById(id) == null) {
            throw exception(SIGN_IN_CONFIG_NOT_EXISTS);
        }
    }

    private void validateDayUnique(Long id, Integer day) {
        MemberSignInConfigDO config = configMapper.selectByDay(day);
        if (config != null && !config.getId().equals(id)) {
            throw exception(SIGN_IN_CONFIG_DAY_EXISTS);
        }
    }

    @Override
    public MemberSignInConfigDO getSignInConfig(Long id) {
        return configMapper.selectById(id);
    }

    @Override
    public List<MemberSignInConfigDO> getSignInConfigList(Integer status) {
        return configMapper.selectListByStatus(status);
    }
}
