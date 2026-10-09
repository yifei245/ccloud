package com.ccloud.module.infra.service.logger;

import com.ccloud.framework.apilog.core.service.ApiErrorLog;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.infra.controller.admin.logger.vo.ApiErrorLogPageReqVO;
import com.ccloud.module.infra.dal.dataobject.logger.ApiErrorLogDO;
import com.ccloud.module.infra.dal.mysql.logger.ApiErrorLogMapper;
import com.ccloud.module.infra.test.BaseInfraDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;

import static com.ccloud.module.infra.enums.ErrorCodeConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

@Import(ApiErrorLogServiceImpl.class)
class ApiErrorLogServiceImplTest extends BaseInfraDbUnitTest {

    @Resource
    private ApiErrorLogServiceImpl apiErrorLogService;
    @Resource
    private ApiErrorLogMapper apiErrorLogMapper;
    @Resource
    private JdbcTemplate jdbcTemplate;

    @Test
    void createApiErrorLog_按日志中的租户写入_空字段取默认值_初始为未处理() {
        ApiErrorLog log = new ApiErrorLog();
        log.setApplicationName("ut-err");
        log.setTenantId(2L);
        apiErrorLogService.createApiErrorLog(log);

        assertThat(TenantUtils.execute(1L, () -> apiErrorLogMapper.selectCount(ApiErrorLogDO::getApplicationName, "ut-err"))).isZero();
        ApiErrorLogDO db = TenantUtils.execute(2L, () -> apiErrorLogMapper.selectList(ApiErrorLogDO::getApplicationName, "ut-err")).get(0);
        assertThat(db.getProcessStatus()).isZero();
        assertThat(db.getRequestUrl()).isEmpty();
        assertThat(db.getExceptionStackTrace()).isEmpty();
        assertThat(db.getExceptionLineNumber()).isZero();
        assertThat(db.getExceptionTime()).isNotNull();
        assertThat(db.getUserId()).isZero();
    }

    @Test
    void updateApiErrorLogProcess_标记已处理_记录处理人与时间_不能重复处理() {
        Long id = insert("ut-err");
        apiErrorLogService.updateApiErrorLogProcess(id, 1, 5L);

        ApiErrorLogDO db = apiErrorLogMapper.selectById(id);
        assertThat(db.getProcessStatus()).isEqualTo(1);
        assertThat(db.getProcessUserId()).isEqualTo(5);
        assertThat(db.getProcessTime()).isNotNull();
        assertServiceException(() -> apiErrorLogService.updateApiErrorLogProcess(id, 2, 5L), API_ERROR_LOG_PROCESSED);
    }

    @Test
    void updateApiErrorLogProcess_状态只能是已处理或已忽略_日志须存在() {
        Long id = insert("ut-err");
        assertServiceException(() -> apiErrorLogService.updateApiErrorLogProcess(id, 0, 5L), API_ERROR_LOG_PROCESS_STATUS_INVALID);
        assertServiceException(() -> apiErrorLogService.updateApiErrorLogProcess(id, 3, 5L), API_ERROR_LOG_PROCESS_STATUS_INVALID);
        assertServiceException(() -> apiErrorLogService.updateApiErrorLogProcess(-1L, 1, 5L), API_ERROR_LOG_NOT_FOUND);
        assertThat(apiErrorLogMapper.selectById(id).getProcessStatus()).isZero();
    }

    @Test
    void cleanApiErrorLog_只删除保留期之前的_跨租户_分批删完() {
        Long old1 = insert("ut-clean", 1L);
        Long old2 = insert("ut-clean", 2L);
        Long old3 = insert("ut-clean", 1L);
        Long today = insert("ut-clean", 2L);
        for (Long id : new Long[]{old1, old2, old3}) {
            jdbcTemplate.update("UPDATE infra_api_error_log SET create_time = ? WHERE id = ?",
                    LocalDateTime.now().minusDays(40), id);
        }

        assertThat(apiErrorLogService.cleanApiErrorLog(30, 2)).isEqualTo(3);

        assertThat(jdbcTemplate.queryForList("SELECT id FROM infra_api_error_log WHERE application_name = 'ut-clean'", Long.class))
                .containsExactly(today);
    }

    @Test
    void getApiErrorLogPage_按应用名与处理状态筛选() {
        insert("ut-page-a");
        Long handled = insert("ut-page-b");
        apiErrorLogService.updateApiErrorLogProcess(handled, 2, 5L);

        ApiErrorLogPageReqVO req = new ApiErrorLogPageReqVO();
        req.setApplicationName("ut-page-b");
        PageResult<ApiErrorLogDO> page = apiErrorLogService.getApiErrorLogPage(req);
        assertThat(page.getTotal()).isEqualTo(1);

        req.setApplicationName(null);
        req.setProcessStatus(2);
        assertThat(apiErrorLogService.getApiErrorLogPage(req).getList())
                .extracting(ApiErrorLogDO::getApplicationName).contains("ut-page-b").doesNotContain("ut-page-a");
    }

    private Long insert(String applicationName) {
        return insert(applicationName, 1L);
    }

    private Long insert(String applicationName, Long tenantId) {
        ApiErrorLog log = new ApiErrorLog();
        log.setApplicationName(applicationName);
        log.setTenantId(tenantId);
        log.setExceptionName("java.lang.IllegalStateException");
        apiErrorLogService.createApiErrorLog(log);
        return jdbcTemplate.queryForList("SELECT id FROM infra_api_error_log WHERE application_name = ? AND tenant_id = ? ORDER BY id DESC",
                Long.class, applicationName, tenantId).get(0);
    }
}
