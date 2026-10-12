package com.ccloud.module.open.service.logger;

import com.ccloud.framework.apilog.core.service.ApiAccessLog;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.tenant.core.context.TenantContextHolder;
import com.ccloud.framework.tenant.core.service.SystemTenantValidator;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.open.controller.admin.logger.vo.OpenApiLogPageReqVO;
import com.ccloud.module.open.dal.dataobject.api.OpenApiDO;
import com.ccloud.module.open.dal.dataobject.app.OpenAppDO;
import com.ccloud.module.open.dal.dataobject.logger.OpenApiLogDO;
import com.ccloud.module.open.dal.mysql.api.OpenApiMapper;
import com.ccloud.module.open.dal.mysql.app.OpenAppMapper;
import com.ccloud.module.open.dal.mysql.logger.OpenApiLogMapper;
import com.ccloud.module.open.service.api.OpenApiServiceImpl;
import com.ccloud.module.open.service.app.OpenAppCacheInvalidator;
import com.ccloud.module.open.service.app.OpenAppServiceImpl;
import com.ccloud.module.open.test.BaseOpenDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@Import({OpenApiLogServiceImpl.class, OpenAppServiceImpl.class, OpenApiServiceImpl.class})
class OpenApiLogServiceImplTest extends BaseOpenDbUnitTest {

    @Resource
    private OpenApiLogService openApiLogService;
    @Resource
    private OpenApiLogMapper openApiLogMapper;
    @Resource
    private OpenAppMapper openAppMapper;
    @Resource
    private OpenApiMapper openApiMapper;
    @Resource
    private JdbcTemplate jdbcTemplate;
    @MockitoBean
    private OpenAppCacheInvalidator invalidator;
    @MockitoBean
    private SystemTenantValidator systemTenantValidator;

    @Test
    void 按日志中的租户写入_应用标识与接口编码反查_超长字段截断() {
        OpenAppDO app = 插入租户2的应用("webtestkey000001");
        openApiMapper.insert(new OpenApiDO().setName("Web测试").setCode("webtest.log").setMethod("GET")
                .setPath("/open-api/webtest/log").setStatus(0));
        ApiAccessLog log = new ApiAccessLog();
        log.setUserId(app.getId());
        log.setUserType(3);
        log.setTenantId(2L);
        log.setRequestMethod("GET");
        log.setRequestUrl("/open-api/webtest/log?x=1");
        log.setRequestParams("{\"mobile\":\"13800000000\"}");
        log.setResultCode(0);
        log.setResultMsg("x".repeat(600));
        log.setUserIp("10.0.0.1");
        log.setTraceId("t1");
        log.setBeginTime(LocalDateTime.now());
        log.setEndTime(LocalDateTime.now());
        log.setDuration(12);

        TenantContextHolder.clear();
        openApiLogService.createApiLog(log);

        OpenApiLogDO saved = TenantUtils.executeIgnore(() -> openApiLogMapper.selectOne(OpenApiLogDO::getTraceId, "t1"));
        assertThat(saved.getTenantId()).isEqualTo(2L);
        assertThat(saved.getAppId()).isEqualTo(app.getId());
        assertThat(saved.getAppKey()).isEqualTo("webtestkey000001");
        assertThat(saved.getApiCode()).isEqualTo("webtest.log");
        assertThat(saved.getResultMsg()).hasSize(512);
        assertThat(saved.getRequestMethod()).isEqualTo("GET");
        assertThat(saved.getRequestUrl()).isEqualTo("/open-api/webtest/log?x=1");
        assertThat(saved.getRequestParams()).isEqualTo("{\"mobile\":\"13800000000\"}");
        assertThat(saved.getUserIp()).isEqualTo("10.0.0.1");
        assertThat(saved.getDuration()).isEqualTo(12);
    }

    @Test
    void 应用或接口查不到时仍写入_字段留空() {
        ApiAccessLog log = new ApiAccessLog();
        log.setUserId(Long.MAX_VALUE - 1);
        log.setUserType(3);
        log.setTenantId(1L);
        log.setRequestMethod("get");
        log.setRequestUrl("/open-api/webtest/none");
        log.setTraceId("t-none");

        openApiLogService.createApiLog(log);

        OpenApiLogDO saved = openApiLogMapper.selectOne(OpenApiLogDO::getTraceId, "t-none");
        assertThat(saved.getAppId()).isEqualTo(Long.MAX_VALUE - 1);
        assertThat(saved.getAppKey()).isEqualTo("");
        assertThat(saved.getApiCode()).isNull();
        assertThat(saved.getRequestMethod()).isEqualTo("GET");
        assertThat(saved.getResultCode()).isZero();
        assertThat(saved.getResultMsg()).isEqualTo("");
        assertThat(saved.getUserIp()).isEqualTo("");
        assertThat(saved.getDuration()).isZero();
        assertThat(saved.getBeginTime()).isNotNull();
        assertThat(saved.getEndTime()).isEqualTo(saved.getBeginTime());
    }

    @Test
    void 分页按当前租户隔离() {
        openApiMapper.insert(new OpenApiDO().setName("Web测试").setCode("webtest.page").setMethod("GET")
                .setPath("/open-api/webtest/page").setStatus(0));
        openApiLogService.createApiLog(log("t-page-1", 1L));
        openApiLogService.createApiLog(log("t-page-2", 2L));

        OpenApiLogPageReqVO req = new OpenApiLogPageReqVO();
        req.setApiCode("webtest.page");

        TenantContextHolder.setTenantId(1L);
        PageResult<OpenApiLogDO> tenant1 = openApiLogService.getApiLogPage(req);
        assertThat(tenant1.getTotal()).isEqualTo(1);
        assertThat(tenant1.getList()).extracting(OpenApiLogDO::getTraceId).containsExactly("t-page-1");

        TenantContextHolder.setTenantId(2L);
        PageResult<OpenApiLogDO> tenant2 = openApiLogService.getApiLogPage(req);
        assertThat(tenant2.getTotal()).isEqualTo(1);
        assertThat(tenant2.getList()).extracting(OpenApiLogDO::getTraceId).containsExactly("t-page-2");
    }

    @Test
    void 清理N天前的日志_跨租户_分批() {
        openApiLogService.createApiLog(log("t-clean-1", 1L));
        openApiLogService.createApiLog(log("t-clean-2", 2L));
        openApiLogService.createApiLog(log("t-clean-3", 1L));
        openApiLogService.createApiLog(log("t-clean-new", 2L));
        jdbcTemplate.update("UPDATE open_api_log SET create_time = ? WHERE trace_id LIKE 't-clean-%' AND trace_id <> 't-clean-new'",
                LocalDateTime.now().minusDays(40));

        // 批大小 1：需要循环四次才删完三条
        assertThat(openApiLogService.cleanApiLog(30, 1)).isEqualTo(3);

        assertThat(jdbcTemplate.queryForList("SELECT trace_id FROM open_api_log WHERE trace_id LIKE 't-clean-%'", String.class))
                .containsExactly("t-clean-new");
    }

    private OpenAppDO 插入租户2的应用(String appKey) {
        OpenAppDO app = new OpenAppDO().setName("Web测试").setAppKey(appKey).setAppSecret("secret0123456789")
                .setStatus(0).setRateLimit(100);
        TenantUtils.execute(2L, () -> openAppMapper.insert(app));
        return app;
    }

    private static ApiAccessLog log(String traceId, Long tenantId) {
        ApiAccessLog log = new ApiAccessLog();
        log.setUserId(1L);
        log.setUserType(3);
        log.setTenantId(tenantId);
        log.setRequestMethod("GET");
        log.setRequestUrl("/open-api/webtest/page");
        log.setTraceId(traceId);
        log.setUserIp("10.0.0.1");
        log.setBeginTime(LocalDateTime.now());
        log.setEndTime(LocalDateTime.now());
        log.setDuration(1);
        return log;
    }
}
