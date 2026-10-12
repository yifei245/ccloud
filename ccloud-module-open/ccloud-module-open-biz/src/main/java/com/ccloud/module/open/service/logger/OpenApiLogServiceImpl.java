package com.ccloud.module.open.service.logger;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.apilog.core.service.ApiAccessLog;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.open.controller.admin.logger.vo.OpenApiLogPageReqVO;
import com.ccloud.module.open.dal.dataobject.api.OpenApiDO;
import com.ccloud.module.open.dal.dataobject.app.OpenAppDO;
import com.ccloud.module.open.dal.dataobject.logger.OpenApiLogDO;
import com.ccloud.module.open.dal.mysql.logger.OpenApiLogMapper;
import com.ccloud.module.open.service.api.OpenApiService;
import com.ccloud.module.open.service.app.OpenAppService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class OpenApiLogServiceImpl implements OpenApiLogService {

    @Resource
    private OpenApiLogMapper openApiLogMapper;
    @Resource
    private OpenAppService openAppService;
    @Resource
    private OpenApiService openApiService;

    /**
     * 按日志自带的租户写入：消费线程没有租户上下文
     */
    @Override
    public void createApiLog(ApiAccessLog accessLog) {
        Long appId = ObjectUtil.defaultIfNull(accessLog.getUserId(), 0L);
        OpenAppDO app = openAppService.getAppIgnoreTenant(appId);
        String method = StrUtil.nullToEmpty(accessLog.getRequestMethod()).toUpperCase(Locale.ROOT);
        String url = StrUtil.nullToEmpty(accessLog.getRequestUrl());
        String path = StrUtil.subBefore(url, '?', false);
        OpenApiDO api = openApiService.getApiByMethodAndPath(method, path);
        OpenApiLogDO log = new OpenApiLogDO()
                .setAppId(appId)
                .setAppKey(app == null ? "" : app.getAppKey())
                .setApiCode(api == null ? null : api.getCode())
                .setRequestMethod(StrUtil.sub(method, 0, 16))
                .setRequestUrl(StrUtil.sub(url, 0, 255))
                .setRequestParams(accessLog.getRequestParams())
                .setResultCode(ObjectUtil.defaultIfNull(accessLog.getResultCode(), 0))
                .setResultMsg(StrUtil.sub(StrUtil.nullToEmpty(accessLog.getResultMsg()), 0, 512))
                .setUserIp(StrUtil.sub(StrUtil.nullToEmpty(accessLog.getUserIp()), 0, 50))
                .setDuration(ObjectUtil.defaultIfNull(accessLog.getDuration(), 0))
                .setTraceId(StrUtil.sub(StrUtil.nullToEmpty(accessLog.getTraceId()), 0, 64));
        log.setBeginTime(ObjectUtil.defaultIfNull(accessLog.getBeginTime(), LocalDateTime.now()));
        log.setEndTime(ObjectUtil.defaultIfNull(accessLog.getEndTime(), log.getBeginTime()));
        log.setTenantId(ObjectUtil.defaultIfNull(accessLog.getTenantId(), 0L));
        TenantUtils.executeIgnore(() -> openApiLogMapper.insert(log));
    }

    @Override
    public PageResult<OpenApiLogDO> getApiLogPage(OpenApiLogPageReqVO reqVO) {
        return openApiLogMapper.selectPage(reqVO);
    }

    @Override
    public int cleanApiLog(int retainDays, int batchSize) {
        LocalDateTime before = LocalDateTime.now().minusDays(retainDays);
        int total = 0;
        int deleted;
        do {
            deleted = TenantUtils.executeIgnore(() -> openApiLogMapper.deleteByCreateTimeLt(before, batchSize));
            total += deleted;
        } while (deleted >= batchSize);
        return total;
    }
}
