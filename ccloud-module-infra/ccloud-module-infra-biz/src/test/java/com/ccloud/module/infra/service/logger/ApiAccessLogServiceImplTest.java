package com.ccloud.module.infra.service.logger;

import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.apilog.core.service.ApiAccessLog;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.infra.controller.admin.logger.vo.ApiAccessLogPageReqVO;
import com.ccloud.module.infra.dal.dataobject.logger.ApiAccessLogDO;
import com.ccloud.module.infra.dal.mysql.logger.ApiAccessLogMapper;
import com.ccloud.module.infra.test.BaseInfraDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@Import(ApiAccessLogServiceImpl.class)
class ApiAccessLogServiceImplTest extends BaseInfraDbUnitTest {

    @Resource
    private ApiAccessLogServiceImpl apiAccessLogService;
    @Resource
    private ApiAccessLogMapper apiAccessLogMapper;
    @Resource
    private JdbcTemplate jdbcTemplate;

    @Test
    void createApiAccessLog_按日志中的租户写入_与当前上下文无关() {
        // 当前上下文租户为 1
        ApiAccessLog log = log("ut-svc", "/a");
        log.setTenantId(2L);
        apiAccessLogService.createApiAccessLog(log);

        assertThat(TenantUtils.execute(2L, () -> apiAccessLogMapper.selectCount(ApiAccessLogDO::getApplicationName, "ut-svc")))
                .isEqualTo(1L);
        assertThat(TenantUtils.execute(1L, () -> apiAccessLogMapper.selectCount(ApiAccessLogDO::getApplicationName, "ut-svc")))
                .isZero();
    }

    @Test
    void createApiAccessLog_超长字段截断_空字段取默认值() {
        ApiAccessLog log = log("ut-svc", StrUtil.repeat('u', 300));
        log.setUserAgent(null);
        log.setUserIp(null);
        log.setUserId(null);
        log.setTenantId(null);
        apiAccessLogService.createApiAccessLog(log);

        ApiAccessLogDO db = TenantUtils.executeIgnore(() -> apiAccessLogMapper.selectList(ApiAccessLogDO::getApplicationName, "ut-svc")).get(0);
        assertThat(db.getRequestUrl()).hasSize(255);
        assertThat(db.getUserAgent()).isEmpty();
        assertThat(db.getUserIp()).isEmpty();
        assertThat(db.getUserId()).isZero();
        assertThat(db.getTenantId()).isZero();
    }

    @Test
    void cleanApiAccessLog_只删除保留期之前的_跨租户_分批删完() {
        Long old1 = insert(1L, "ut-clean");
        Long old2 = insert(2L, "ut-clean");
        Long old3 = insert(1L, "ut-clean");
        Long today = insert(2L, "ut-clean");
        for (Long id : new Long[]{old1, old2, old3}) {
            jdbcTemplate.update("UPDATE infra_api_access_log SET create_time = ? WHERE id = ?",
                    LocalDateTime.now().minusDays(40), id);
        }

        assertThat(apiAccessLogService.cleanApiAccessLog(30, 2)).isEqualTo(3);

        assertThat(jdbcTemplate.queryForList("SELECT id FROM infra_api_access_log WHERE application_name = 'ut-clean'", Long.class))
                .containsExactly(today);
    }

    @Test
    void getApiAccessLogPage_按应用名与结果码筛选() {
        insert(1L, "ut-page-a");
        ApiAccessLog failed = log("ut-page-b", "/b");
        failed.setResultCode(1_002_000_001);
        apiAccessLogService.createApiAccessLog(failed);

        ApiAccessLogPageReqVO req = new ApiAccessLogPageReqVO();
        req.setApplicationName("ut-page-b");
        PageResult<ApiAccessLogDO> byApp = apiAccessLogService.getApiAccessLogPage(req);
        assertThat(byApp.getTotal()).isEqualTo(1);
        assertThat(byApp.getList().get(0).getResultCode()).isEqualTo(1_002_000_001);

        req.setApplicationName(null);
        req.setResultCode(1_002_000_001);
        assertThat(apiAccessLogService.getApiAccessLogPage(req).getList())
                .extracting(ApiAccessLogDO::getApplicationName).contains("ut-page-b").doesNotContain("ut-page-a");
    }

    private Long insert(Long tenantId, String applicationName) {
        ApiAccessLog log = log(applicationName, "/x");
        log.setTenantId(tenantId);
        apiAccessLogService.createApiAccessLog(log);
        return jdbcTemplate.queryForList("SELECT id FROM infra_api_access_log WHERE application_name = ? AND tenant_id = ? ORDER BY id DESC",
                Long.class, applicationName, tenantId).get(0);
    }

    private static ApiAccessLog log(String applicationName, String url) {
        ApiAccessLog log = new ApiAccessLog();
        log.setApplicationName(applicationName);
        log.setRequestUrl(url);
        log.setTenantId(1L);
        log.setBeginTime(LocalDateTime.now());
        log.setEndTime(LocalDateTime.now());
        log.setDuration(5);
        log.setResultCode(0);
        return log;
    }
}
