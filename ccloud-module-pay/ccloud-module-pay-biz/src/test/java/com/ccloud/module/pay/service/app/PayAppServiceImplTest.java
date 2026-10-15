package com.ccloud.module.pay.service.app;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.module.open.api.app.OpenAppApi;
import com.ccloud.module.open.api.app.dto.OpenAppSnapshotRespDTO;
import com.ccloud.module.pay.controller.admin.app.vo.PayAppSaveReqVO;
import com.ccloud.module.pay.dal.dataobject.app.PayAppDO;
import com.ccloud.module.pay.dal.dataobject.channel.PayChannelDO;
import com.ccloud.module.pay.dal.mysql.app.PayAppMapper;
import com.ccloud.module.pay.dal.mysql.channel.PayChannelMapper;
import com.ccloud.module.pay.framework.config.PayProperties;
import com.ccloud.module.pay.test.BasePayDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static com.ccloud.module.pay.enums.ErrorCodeConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@Import({PayAppServiceImpl.class, PayProperties.class})
class PayAppServiceImplTest extends BasePayDbUnitTest {

    @Resource
    private PayAppServiceImpl payAppService;
    @Resource
    private PayAppMapper payAppMapper;
    @Resource
    private PayChannelMapper payChannelMapper;
    @MockitoBean
    private OpenAppApi openAppApi;

    private static PayAppSaveReqVO req(String appKey, String openAppKey) {
        return new PayAppSaveReqVO().setAppKey(appKey).setName("测试应用").setStatus(0).setOpenAppKey(openAppKey);
    }

    private void stubOpenApp(String appKey, long id, long tenantId) {
        OpenAppSnapshotRespDTO snapshot = new OpenAppSnapshotRespDTO();
        snapshot.setId(id);
        snapshot.setAppKey(appKey);
        snapshot.setTenantId(tenantId);
        when(openAppApi.getAppSnapshot(appKey)).thenReturn(CommonResult.success(snapshot));
    }

    @Test
    void 钱包充值应用按配置的标识判断_空应用不是() {
        // 默认的充值应用标识是 wallet
        assertThat(payAppService.isRechargeApp(new PayAppDO().setAppKey("wallet"))).isTrue();
        assertThat(payAppService.isRechargeApp(new PayAppDO().setAppKey("mall"))).isFalse();
        assertThat(payAppService.isRechargeApp(new PayAppDO().setAppKey("Wallet"))).isFalse();
        assertThat(payAppService.isRechargeApp(null)).isFalse();
    }

    @Test
    void 创建应用_标识在租户内唯一() {
        payAppService.createApp(req("mall-test", null));
        assertServiceException(() -> payAppService.createApp(req("mall-test", null)), PAY_APP_KEY_EXISTS);
    }

    @Test
    void 绑定本租户的开放应用() {
        stubOpenApp("open-1", 91L, TENANT_ID);
        Long id = payAppService.createApp(req("mall-test", "open-1"));
        assertThat(payAppMapper.selectById(id).getOpenAppId()).isEqualTo(91L);
        assertThat(payAppService.validPayAppByOpenAppId(91L).getId()).isEqualTo(id);
    }

    @Test
    void 不能绑定其他租户或不存在的开放应用() {
        stubOpenApp("open-2", 92L, 2L);
        when(openAppApi.getAppSnapshot("missing")).thenReturn(CommonResult.success(null));
        assertServiceException(() -> payAppService.createApp(req("a1", "open-2")), PAY_APP_OPEN_APP_INVALID);
        assertServiceException(() -> payAppService.createApp(req("a2", "missing")), PAY_APP_OPEN_APP_INVALID);
    }

    @Test
    void 一个开放应用只能绑定一个支付应用() {
        stubOpenApp("open-1", 91L, TENANT_ID);
        payAppService.createApp(req("a1", "open-1"));
        assertServiceException(() -> payAppService.createApp(req("a2", "open-1")), PAY_APP_OPEN_APP_BOUND);
    }

    @Test
    void 更新时不修改应用标识_可解除绑定() {
        stubOpenApp("open-1", 91L, TENANT_ID);
        Long id = payAppService.createApp(req("a1", "open-1"));
        payAppService.updateApp(req("changed", null).setId(id));
        PayAppDO app = payAppMapper.selectById(id);
        assertThat(app.getAppKey()).isEqualTo("a1");
        assertThat(app.getOpenAppId()).isNull();
    }

    @Test
    void 未绑定或已禁用时开放应用不能使用支付() {
        assertServiceException(() -> payAppService.validPayAppByOpenAppId(999L), PAY_APP_NOT_OPENED_FOR_OPEN_APP);
        stubOpenApp("open-1", 91L, TENANT_ID);
        payAppService.createApp(req("a1", "open-1").setStatus(1));
        assertServiceException(() -> payAppService.validPayAppByOpenAppId(91L), PAY_APP_IS_DISABLE);
    }

    @Test
    void 更新时保持原绑定不算重复绑定() {
        stubOpenApp("open-1", 91L, TENANT_ID);
        Long id = payAppService.createApp(req("a1", "open-1"));
        payAppService.updateApp(req("a1", "open-1").setId(id).setName("改名"));
        PayAppDO app = payAppMapper.selectById(id);
        assertThat(app.getName()).isEqualTo("改名");
        assertThat(app.getOpenAppId()).isEqualTo(91L);
    }

    @Test
    void 按编号与标识校验应用() {
        Long id = payAppService.createApp(req("a1", null));
        assertThat(payAppService.validPayApp(id).getAppKey()).isEqualTo("a1");
        assertThat(payAppService.validPayApp("a1").getId()).isEqualTo(id);
        assertServiceException(() -> payAppService.validPayApp(-1L), PAY_APP_NOT_FOUND);
        assertServiceException(() -> payAppService.validPayApp("nope"), PAY_APP_NOT_FOUND);
        Long disabled = payAppService.createApp(req("a2", null).setStatus(1));
        assertServiceException(() -> payAppService.validPayApp(disabled), PAY_APP_IS_DISABLE);
        assertServiceException(() -> payAppService.validPayApp("a2"), PAY_APP_IS_DISABLE);
    }

    @Test
    void 删除应用同时删除它的渠道() {
        Long id = payAppService.createApp(req("a1", null));
        payChannelMapper.insert(new PayChannelDO().setAppId(id).setCode("mock").setStatus(0).setFeeRate(0D).setConfig("{}"));
        assertThat(payChannelMapper.selectListByAppId(id)).hasSize(1);
        payAppService.deleteApp(id);
        assertThat(payAppMapper.selectById(id)).isNull();
        assertThat(payChannelMapper.selectListByAppId(id)).isEmpty();
        assertServiceException(() -> payAppService.deleteApp(id), PAY_APP_NOT_FOUND);
    }
}
