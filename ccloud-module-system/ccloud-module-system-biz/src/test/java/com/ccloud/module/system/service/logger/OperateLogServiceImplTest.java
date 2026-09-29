package com.ccloud.module.system.service.logger;

import com.ccloud.framework.operatelog.core.service.OperateLogFrameworkService;
import com.ccloud.framework.operatelog.core.service.OperateLogRecord;
import com.ccloud.module.system.controller.admin.logger.vo.OperateLogPageReqVO;
import com.ccloud.module.system.dal.dataobject.logger.OperateLogDO;
import com.ccloud.module.system.dal.mysql.logger.OperateLogMapper;
import com.ccloud.module.system.test.BaseSystemDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@Import({OperateLogServiceImpl.class, OperateLogFrameworkServiceImpl.class})
class OperateLogServiceImplTest extends BaseSystemDbUnitTest {

    @Resource
    private OperateLogService operateLogService;
    @Resource
    private OperateLogFrameworkService operateLogFrameworkService;
    @Resource
    private OperateLogMapper operateLogMapper;

    @Test
    void 框架切面的记录落库且归属当前租户() {
        operateLogFrameworkService.createOperateLog(record("用户管理", "修改用户", 42L, true));

        OperateLogDO log = operateLogMapper.selectList().get(0);
        assertThat(log.getType()).isEqualTo("用户管理");
        assertThat(log.getSubType()).isEqualTo("修改用户");
        assertThat(log.getBizId()).isEqualTo(42L);
        assertThat(log.getSuccess()).isTrue();
        assertThat(log.getUserId()).isEqualTo(7L);
        assertThat(log.getExtra()).isEqualTo("{\"duration\":3}");
        assertThat(log.getTenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    void 超长字段截断到列长度_不因日志过长导致写入失败() {
        OperateLogRecord record = record("用户管理", "导入用户", 0L, true);
        record.setAction("x".repeat(5000));
        record.setUserAgent("u".repeat(500));

        operateLogFrameworkService.createOperateLog(record);

        OperateLogDO log = operateLogMapper.selectList().get(0);
        assertThat(log.getAction()).hasSizeLessThanOrEqualTo(2000);
        assertThat(log.getUserAgent()).hasSizeLessThanOrEqualTo(200);
    }

    @Test
    void getOperateLogPage_按模块_操作人_结果与业务编号过滤() {
        operateLogFrameworkService.createOperateLog(record("用户管理", "修改用户", 1L, true));
        operateLogFrameworkService.createOperateLog(record("用户管理", "删除用户", 2L, false));
        operateLogFrameworkService.createOperateLog(record("角色管理", "修改角色", 3L, true));

        OperateLogPageReqVO reqVO = new OperateLogPageReqVO();
        reqVO.setType("用户");
        assertThat(operateLogService.getOperateLogPage(reqVO).getList()).extracting(OperateLogDO::getSubType)
                .containsExactly("删除用户", "修改用户");

        reqVO.setSuccess(false);
        assertThat(operateLogService.getOperateLogPage(reqVO).getList()).extracting(OperateLogDO::getSubType)
                .containsExactly("删除用户");

        OperateLogPageReqVO byBiz = new OperateLogPageReqVO();
        byBiz.setType("角色管理");
        byBiz.setBizId(3L);
        byBiz.setUserId(7L);
        assertThat(operateLogService.getOperateLogPage(byBiz).getList()).hasSize(1);
    }

    private static OperateLogRecord record(String type, String subType, Long bizId, boolean success) {
        OperateLogRecord record = new OperateLogRecord();
        record.setTraceId("t1");
        record.setUserId(7L);
        record.setUserType(2);
        record.setType(type);
        record.setSubType(subType);
        record.setBizId(bizId);
        record.setAction(subType);
        record.setSuccess(success);
        record.setExtra("{\"duration\":3}");
        record.setRequestMethod("PUT");
        record.setRequestUrl("/admin-api/system/user/update");
        record.setUserIp("10.0.0.1");
        record.setUserAgent("JUnit");
        return record;
    }
}
