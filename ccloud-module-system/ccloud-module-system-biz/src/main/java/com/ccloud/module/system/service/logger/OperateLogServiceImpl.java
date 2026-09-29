package com.ccloud.module.system.service.logger;

import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.system.controller.admin.logger.vo.OperateLogPageReqVO;
import com.ccloud.module.system.dal.dataobject.logger.OperateLogDO;
import com.ccloud.module.system.dal.mysql.logger.OperateLogMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class OperateLogServiceImpl implements OperateLogService {

    @Resource
    private OperateLogMapper operateLogMapper;

    @Override
    public void createOperateLog(OperateLogDO operateLog) {
        // 与 system_operate_log 列长度一致：日志写不进去比日志被截断更糟
        operateLog.setTraceId(truncate(StrUtil.nullToEmpty(operateLog.getTraceId()), 60))
                .setType(truncate(operateLog.getType(), 50))
                .setSubType(truncate(operateLog.getSubType(), 50))
                .setAction(truncate(operateLog.getAction(), 2000))
                .setExtra(truncate(StrUtil.nullToEmpty(operateLog.getExtra()), 2000))
                .setRequestMethod(truncate(operateLog.getRequestMethod(), 16))
                .setRequestUrl(truncate(operateLog.getRequestUrl(), 255))
                .setUserIp(truncate(operateLog.getUserIp(), 50))
                .setUserAgent(truncate(operateLog.getUserAgent(), 200));
        operateLogMapper.insert(operateLog);
    }

    /**
     * 按列长度截断。不用 StrUtil.maxLength：它会在末尾追加 "..."，结果反而超出列长度
     */
    private static String truncate(String value, int maxLength) {
        return StrUtil.sub(value, 0, maxLength);
    }

    @Override
    public PageResult<OperateLogDO> getOperateLogPage(OperateLogPageReqVO pageReqVO) {
        return operateLogMapper.selectPage(pageReqVO);
    }
}
