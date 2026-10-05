package com.ccloud.module.infra.service.logger;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.apilog.core.service.ApiAccessLog;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.infra.controller.admin.logger.vo.ApiAccessLogPageReqVO;
import com.ccloud.module.infra.dal.dataobject.logger.ApiAccessLogDO;
import com.ccloud.module.infra.dal.mysql.logger.ApiAccessLogMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ApiAccessLogServiceImpl implements ApiAccessLogService {

    @Resource
    private ApiAccessLogMapper apiAccessLogMapper;

    /**
     * 按日志自带的租户写入：消费线程没有租户上下文，也不能沿用发送方以外的租户
     */
    @Override
    public void createApiAccessLog(ApiAccessLog accessLog) {
        ApiAccessLogDO log = BeanUtils.toBean(accessLog, ApiAccessLogDO.class);
        log.setTenantId(ObjectUtil.defaultIfNull(accessLog.getTenantId(), 0L));
        log.setUserId(ObjectUtil.defaultIfNull(log.getUserId(), 0L));
        log.setUserType(ObjectUtil.defaultIfNull(log.getUserType(), 0));
        log.setTraceId(StrUtil.sub(StrUtil.nullToEmpty(log.getTraceId()), 0, 64));
        log.setRequestMethod(StrUtil.sub(StrUtil.nullToEmpty(log.getRequestMethod()), 0, 16));
        log.setRequestUrl(StrUtil.sub(StrUtil.nullToEmpty(log.getRequestUrl()), 0, 255));
        log.setUserIp(StrUtil.sub(StrUtil.nullToEmpty(log.getUserIp()), 0, 50));
        log.setUserAgent(StrUtil.sub(StrUtil.nullToEmpty(log.getUserAgent()), 0, 512));
        log.setApplicationName(StrUtil.sub(StrUtil.nullToEmpty(log.getApplicationName()), 0, 50));
        log.setOperateModule(StrUtil.sub(log.getOperateModule(), 0, 50));
        log.setOperateName(StrUtil.sub(log.getOperateName(), 0, 50));
        log.setResultCode(ObjectUtil.defaultIfNull(log.getResultCode(), 0));
        log.setResultMsg(StrUtil.sub(StrUtil.nullToEmpty(log.getResultMsg()), 0, 512));
        log.setBeginTime(ObjectUtil.defaultIfNull(log.getBeginTime(), LocalDateTime.now()));
        log.setEndTime(ObjectUtil.defaultIfNull(log.getEndTime(), log.getBeginTime()));
        log.setDuration(ObjectUtil.defaultIfNull(log.getDuration(), 0));
        TenantUtils.executeIgnore(() -> apiAccessLogMapper.insert(log));
    }

    @Override
    public PageResult<ApiAccessLogDO> getApiAccessLogPage(ApiAccessLogPageReqVO reqVO) {
        return apiAccessLogMapper.selectPage(reqVO);
    }

    @Override
    public int cleanApiAccessLog(int retainDays, int batchSize) {
        LocalDateTime before = LocalDateTime.now().minusDays(retainDays);
        int total = 0;
        int deleted;
        do {
            deleted = TenantUtils.executeIgnore(() -> apiAccessLogMapper.deleteByCreateTimeLt(before, batchSize));
            total += deleted;
        } while (deleted >= batchSize);
        return total;
    }
}
