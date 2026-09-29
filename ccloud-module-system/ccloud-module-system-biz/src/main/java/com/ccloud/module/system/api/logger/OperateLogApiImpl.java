package com.ccloud.module.system.api.logger;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.system.api.logger.dto.OperateLogCreateReqDTO;
import com.ccloud.module.system.dal.dataobject.logger.OperateLogDO;
import com.ccloud.module.system.service.logger.OperateLogService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import static com.ccloud.framework.common.pojo.CommonResult.success;

/**
 * 操作日志 RPC 实现：其他服务的 {@code @OperateLog} 记录统一写入 system
 */
@RestController
@Validated
public class OperateLogApiImpl implements OperateLogApi {

    @Resource
    private OperateLogService operateLogService;

    @Override
    public CommonResult<Boolean> createOperateLog(OperateLogCreateReqDTO reqDTO) {
        operateLogService.createOperateLog(BeanUtils.toBean(reqDTO, OperateLogDO.class));
        return success(true);
    }
}
