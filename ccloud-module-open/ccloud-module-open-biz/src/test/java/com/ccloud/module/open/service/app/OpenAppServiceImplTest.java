package com.ccloud.module.open.service.app;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.mybatis.core.type.EncryptTypeHandler;
import com.ccloud.framework.tenant.core.context.TenantContextHolder;
import com.ccloud.framework.tenant.core.service.SystemTenantValidator;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.open.api.app.dto.OpenAppSnapshotRespDTO;
import com.ccloud.module.open.controller.admin.app.vo.OpenAppCreateRespVO;
import com.ccloud.module.open.controller.admin.app.vo.OpenAppSaveReqVO;
import com.ccloud.module.open.dal.dataobject.api.OpenApiDO;
import com.ccloud.module.open.dal.dataobject.app.OpenAppDO;
import com.ccloud.module.open.dal.mysql.api.OpenApiMapper;
import com.ccloud.module.open.dal.mysql.app.OpenAppMapper;
import com.ccloud.module.open.service.api.OpenApiServiceImpl;
import com.ccloud.module.open.test.BaseOpenDbAndRedisUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Set;

import static com.ccloud.module.open.enums.ErrorCodeConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@Import({OpenAppServiceImpl.class, OpenApiServiceImpl.class})
class OpenAppServiceImplTest extends BaseOpenDbAndRedisUnitTest {

    @Resource
    private OpenAppService openAppService;
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

    private Long onApiId;
    private Long offApiId;

    @BeforeEach
    void insertApis() {
        OpenApiDO on = new OpenApiDO().setName("Web测试").setCode("webtest.on").setMethod("GET")
                .setPath("/open-api/webtest/on").setStatus(CommonStatusEnum.ENABLE.getStatus());
        OpenApiDO off = new OpenApiDO().setName("Web测试").setCode("webtest.off").setMethod("GET")
                .setPath("/open-api/webtest/off").setStatus(CommonStatusEnum.DISABLE.getStatus());
        openApiMapper.insert(on);
        openApiMapper.insert(off);
        // 开启但不授权给任何应用的接口：快照不得包含它
        openApiMapper.insert(new OpenApiDO().setName("Web测试").setCode("webtest.other").setMethod("GET")
                .setPath("/open-api/webtest/other").setStatus(CommonStatusEnum.ENABLE.getStatus()));
        onApiId = on.getId();
        offApiId = off.getId();
    }

    @Test
    void 新建_返回一次性密钥_库中为密文_查询为原文() {
        OpenAppCreateRespVO resp = openAppService.createApp(saveReq());
        assertThat(resp.getAppKey()).matches("[a-z0-9]{16}");
        assertThat(resp.getAppSecret()).matches("[A-Za-z0-9]{32}");
        String stored = jdbcTemplate.queryForObject("SELECT app_secret FROM open_app WHERE id = ?", String.class, resp.getId());
        assertThat(stored).isNotEqualTo(resp.getAppSecret()).isEqualTo(EncryptTypeHandler.encrypt(resp.getAppSecret()));
        assertThat(openAppService.getApp(resp.getId()).getAppSecret()).isEqualTo(resp.getAppSecret());
    }

    @Test
    void 修改_不能改appKey与密钥_发布该应用的失效() {
        OpenAppCreateRespVO resp = openAppService.createApp(saveReq());
        clearInvocations(invalidator);
        openAppService.updateApp(saveReq().setId(resp.getId()).setName("Web测试改").setRateLimit(5));
        OpenAppDO app = openAppService.getApp(resp.getId());
        assertThat(app.getAppKey()).isEqualTo(resp.getAppKey());
        assertThat(app.getAppSecret()).isEqualTo(resp.getAppSecret());
        assertThat(app.getRateLimit()).isEqualTo(5);
        verify(invalidator).publish(resp.getAppKey());
    }

    @Test
    void 修改_允许清空白名单_过期时间_回调地址() {
        OpenAppCreateRespVO resp = openAppService.createApp(saveReq().setIpWhitelist("10.0.0.0/8")
                .setNotifyUrl("https://example.com/notify").setExpireTime(java.time.LocalDateTime.now().plusDays(1)));
        assertThat(openAppService.getApp(resp.getId()).getIpWhitelist()).isEqualTo("10.0.0.0/8");
        openAppService.updateApp(saveReq().setId(resp.getId()).setIpWhitelist("").setNotifyUrl(null).setExpireTime(null));
        OpenAppDO app = openAppService.getApp(resp.getId());
        assertThat(app.getIpWhitelist()).isNull();
        assertThat(app.getNotifyUrl()).isNull();
        assertThat(app.getExpireTime()).isNull();
    }

    @Test
    void 修改不存在的应用报错() {
        assertServiceException(() -> openAppService.updateApp(saveReq().setId(-1L)), OPEN_APP_NOT_EXISTS);
        assertServiceException(() -> openAppService.resetAppSecret(-1L), OPEN_APP_NOT_EXISTS);
        assertServiceException(() -> openAppService.deleteApp(-1L), OPEN_APP_NOT_EXISTS);
        assertServiceException(() -> openAppService.assignApis(-1L, Set.of()), OPEN_APP_NOT_EXISTS);
    }

    @Test
    void 重置密钥_旧密钥失效_发布失效() {
        OpenAppCreateRespVO resp = openAppService.createApp(saveReq());
        String secret = openAppService.resetAppSecret(resp.getId());
        assertThat(secret).isNotEqualTo(resp.getAppSecret());
        assertThat(openAppService.getApp(resp.getId()).getAppSecret()).isEqualTo(secret);
        verify(invalidator, atLeastOnce()).publish(resp.getAppKey());
    }

    @Test
    void IP白名单与回调地址校验() {
        assertServiceException(() -> openAppService.createApp(saveReq().setIpWhitelist("example.com")), OPEN_APP_IP_WHITELIST_INVALID);
        assertServiceException(() -> openAppService.createApp(saveReq().setNotifyUrl("ftp://a.com/x")), OPEN_APP_NOTIFY_URL_INVALID);
        assertServiceException(() -> openAppService.createApp(saveReq().setNotifyUrl("http:///x")), OPEN_APP_NOTIFY_URL_INVALID);
        openAppService.createApp(saveReq().setIpWhitelist("10.0.0.0/8, ::1").setNotifyUrl("https://example.com/notify"));
    }

    @Test
    void 授权_整体替换_只能选开启的接口() {
        Long appId = openAppService.createApp(saveReq()).getId();
        openAppService.assignApis(appId, Set.of(onApiId));
        assertThat(openAppService.getAppApiIds(appId)).containsExactly(onApiId);
        assertServiceException(() -> openAppService.assignApis(appId, Set.of(offApiId)), OPEN_APP_API_NOT_ENABLED);
        openAppService.assignApis(appId, Set.of());
        assertThat(openAppService.getAppApiIds(appId)).isEmpty();
    }

    @Test
    void 删除_同时删除授权() {
        OpenAppCreateRespVO resp = openAppService.createApp(saveReq());
        openAppService.assignApis(resp.getId(), Set.of(onApiId));
        clearInvocations(invalidator);

        openAppService.deleteApp(resp.getId());

        assertThat(openAppService.getApp(resp.getId())).isNull();
        Integer rows = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM open_app_api WHERE app_id = ? AND deleted = 0", Integer.class, resp.getId());
        assertThat(rows).isZero();
        verify(invalidator).publish(resp.getAppKey());
    }

    @Test
    void 快照_跨租户_含原文密钥与开启的授权接口() {
        Long appId = TenantUtils.execute(2L, () -> openAppService.createApp(saveReq()).getId());
        TenantUtils.execute(2L, () -> openAppService.assignApis(appId, Set.of(onApiId)));
        jdbcTemplate.update("INSERT INTO open_app_api (app_id, api_id, tenant_id) VALUES (?, ?, 2)", appId, offApiId);  // 授权后被关闭的接口
        String appKey = TenantUtils.executeIgnore(() -> openAppMapper.selectById(appId)).getAppKey();

        TenantContextHolder.clear();
        OpenAppSnapshotRespDTO snapshot = openAppService.getAppSnapshot(appKey);
        assertThat(snapshot.getTenantId()).isEqualTo(2L);
        assertThat(snapshot.getAppSecret()).matches("[A-Za-z0-9]{32}");
        assertThat(snapshot.getApis()).extracting(OpenAppSnapshotRespDTO.Api::getPath).containsExactly("/open-api/webtest/on");
        // 快照携带密钥原文：toString 不得泄露，防止被日志、异常信息带出
        assertThat(snapshot.toString()).doesNotContain(snapshot.getAppSecret());
        assertThat(openAppService.getAppSnapshot("not-exists")).isNull();
    }

    @Test
    void 跨租户查询应用_日志与回调用() {
        Long appId = TenantUtils.execute(2L, () -> openAppService.createApp(saveReq()).getId());
        assertThat(openAppService.getApp(appId)).isNull();
        assertThat(openAppService.getAppIgnoreTenant(appId)).isNotNull();
    }

    private static OpenAppSaveReqVO saveReq() {
        return new OpenAppSaveReqVO().setName("Web测试").setStatus(CommonStatusEnum.ENABLE.getStatus()).setRateLimit(100);
    }
}
