package com.ccloud.module.infra.service.logger;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.apilog.core.service.ApiErrorLog;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.infra.controller.admin.logger.vo.ApiErrorLogPageReqVO;
import com.ccloud.module.infra.dal.dataobject.logger.ApiErrorLogDO;
import com.ccloud.module.infra.dal.mysql.logger.ApiErrorLogMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.infra.enums.ErrorCodeConstants.*;

@Service
public class ApiErrorLogServiceImpl implements ApiErrorLogService {

    /**
     * text 列最多 64KB，留足中文占位的余量，避免一条超长日志写库失败后被反复重投
     */
    private static final int TEXT_MAX_LENGTH = 10_000;

    @Resource
    private ApiErrorLogMapper apiErrorLogMapper;

    /**
     * 按日志自带的租户写入，原因同访问日志。NOT NULL 的列缺值时写空串，宁可日志不全也不能丢
     */
    @Override
    public void createApiErrorLog(ApiErrorLog errorLog) {
        ApiErrorLogDO log = BeanUtils.toBean(errorLog, ApiErrorLogDO.class);
        log.setTenantId(ObjectUtil.defaultIfNull(errorLog.getTenantId(), 0L));
        log.setUserId(ObjectUtil.defaultIfNull(log.getUserId(), 0L));
        log.setUserType(ObjectUtil.defaultIfNull(log.getUserType(), 0));
        log.setTraceId(StrUtil.sub(StrUtil.nullToEmpty(log.getTraceId()), 0, 64));
        log.setApplicationName(StrUtil.sub(StrUtil.nullToEmpty(log.getApplicationName()), 0, 50));
        log.setRequestMethod(StrUtil.sub(StrUtil.nullToEmpty(log.getRequestMethod()), 0, 16));
        log.setRequestUrl(StrUtil.sub(StrUtil.nullToEmpty(log.getRequestUrl()), 0, 255));
        log.setRequestParams(StrUtil.sub(StrUtil.nullToEmpty(log.getRequestParams()), 0, 8000));
        log.setUserIp(StrUtil.sub(StrUtil.nullToEmpty(log.getUserIp()), 0, 50));
        log.setUserAgent(StrUtil.sub(StrUtil.nullToEmpty(log.getUserAgent()), 0, 512));
        log.setExceptionTime(ObjectUtil.defaultIfNull(log.getExceptionTime(), LocalDateTime.now()));
        log.setExceptionName(StrUtil.sub(StrUtil.nullToEmpty(log.getExceptionName()), 0, 128));
        log.setExceptionMessage(StrUtil.sub(StrUtil.nullToEmpty(log.getExceptionMessage()), 0, TEXT_MAX_LENGTH));
        log.setExceptionRootCauseMessage(StrUtil.sub(StrUtil.nullToEmpty(log.getExceptionRootCauseMessage()), 0, TEXT_MAX_LENGTH));
        log.setExceptionStackTrace(StrUtil.sub(StrUtil.nullToEmpty(log.getExceptionStackTrace()), 0, TEXT_MAX_LENGTH));
        log.setExceptionClassName(StrUtil.sub(StrUtil.nullToEmpty(log.getExceptionClassName()), 0, 512));
        log.setExceptionFileName(StrUtil.sub(StrUtil.nullToEmpty(log.getExceptionFileName()), 0, 512));
        log.setExceptionMethodName(StrUtil.sub(StrUtil.nullToEmpty(log.getExceptionMethodName()), 0, 512));
        log.setExceptionLineNumber(ObjectUtil.defaultIfNull(log.getExceptionLineNumber(), 0));
        log.setProcessStatus(0);
        TenantUtils.executeIgnore(() -> apiErrorLogMapper.insert(log));
    }

    @Override
    public PageResult<ApiErrorLogDO> getApiErrorLogPage(ApiErrorLogPageReqVO reqVO) {
        return apiErrorLogMapper.selectPage(reqVO);
    }

    @Override
    public void updateApiErrorLogProcess(Long id, Integer processStatus, Long processUserId) {
        if (!Objects.equals(processStatus, 1) && !Objects.equals(processStatus, 2)) {
            throw exception(API_ERROR_LOG_PROCESS_STATUS_INVALID);
        }
        ApiErrorLogDO log = apiErrorLogMapper.selectById(id);
        if (log == null) {
            throw exception(API_ERROR_LOG_NOT_FOUND);
        }
        if (!Objects.equals(log.getProcessStatus(), 0)) {
            throw exception(API_ERROR_LOG_PROCESSED);
        }
        apiErrorLogMapper.updateById(new ApiErrorLogDO().setId(id).setProcessStatus(processStatus)
                .setProcessUserId(processUserId.intValue()).setProcessTime(LocalDateTime.now()));
    }

    @Override
    public int cleanApiErrorLog(int retainDays, int batchSize) {
        LocalDateTime before = LocalDateTime.now().minusDays(retainDays);
        int total = 0;
        int deleted;
        do {
            deleted = TenantUtils.executeIgnore(() -> apiErrorLogMapper.deleteByCreateTimeLt(before, batchSize));
            total += deleted;
        } while (deleted >= batchSize);
        return total;
    }
}
