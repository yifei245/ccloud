package com.ccloud.module.system.service.logger;

import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.framework.operatelog.core.service.OperateLogFrameworkService;
import com.ccloud.framework.operatelog.core.service.OperateLogRecord;
import com.ccloud.module.system.dal.dataobject.logger.OperateLogDO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * system 服务内的 {@code @OperateLog} 直接写库；其他服务经 OperateLogApi 交给 system
 */
@Service
public class OperateLogFrameworkServiceImpl implements OperateLogFrameworkService {

    @Resource
    private OperateLogService operateLogService;

    @Override
    public void createOperateLog(OperateLogRecord record) {
        operateLogService.createOperateLog(BeanUtils.toBean(record, OperateLogDO.class));
    }
}
