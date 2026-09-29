package com.ccloud.module.system.api.logger;

import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.system.api.logger.dto.LoginLogCreateReqDTO;
import com.ccloud.module.system.dal.dataobject.logger.LoginLogDO;
import com.ccloud.module.system.service.logger.LoginLogService;
import com.ccloud.framework.common.pojo.CommonResult;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import static com.ccloud.framework.common.pojo.CommonResult.success;

/**
 * 登录日志 RPC 实现：会员服务的 C 端登录日志写入 system 统一存储
 */
@RestController
@Validated
public class LoginLogApiImpl implements LoginLogApi {

    @Resource
    private LoginLogService loginLogService;

    @Override
    public CommonResult<Boolean> createLoginLog(LoginLogCreateReqDTO reqDTO) {
        loginLogService.createLoginLog(BeanUtils.toBean(reqDTO, LoginLogDO.class));
        return success(true);
    }
}
