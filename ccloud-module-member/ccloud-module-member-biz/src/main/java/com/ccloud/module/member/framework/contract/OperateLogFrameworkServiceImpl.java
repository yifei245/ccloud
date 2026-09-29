package com.ccloud.module.member.framework.contract;

import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.framework.operatelog.core.service.OperateLogFrameworkService;
import com.ccloud.framework.operatelog.core.service.OperateLogRecord;
import com.ccloud.module.system.api.logger.OperateLogApi;
import com.ccloud.module.system.api.logger.dto.OperateLogCreateReqDTO;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * {@code @OperateLog} 的记录经 RPC 写入 system，统一在管理后台查询
 */
@Component
public class OperateLogFrameworkServiceImpl implements OperateLogFrameworkService {

    @Lazy
    @Resource
    private OperateLogApi operateLogApi;

    @Override
    public void createOperateLog(OperateLogRecord record) {
        operateLogApi.createOperateLog(BeanUtils.toBean(record, OperateLogCreateReqDTO.class)).checkError();
    }
}
