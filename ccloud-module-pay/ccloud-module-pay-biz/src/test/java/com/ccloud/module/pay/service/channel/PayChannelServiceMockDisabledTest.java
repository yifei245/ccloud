package com.ccloud.module.pay.service.channel;

import com.ccloud.module.pay.controller.admin.channel.vo.PayChannelSaveReqVO;
import com.ccloud.module.pay.dal.dataobject.app.PayAppDO;
import com.ccloud.module.pay.dal.dataobject.channel.PayChannelDO;
import com.ccloud.module.pay.dal.mysql.channel.PayChannelMapper;
import com.ccloud.module.pay.framework.config.PayProperties;
import com.ccloud.module.pay.framework.pay.core.client.PayClientFactory;
import com.ccloud.module.pay.service.app.PayAppService;
import com.ccloud.module.pay.test.BasePayDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static com.ccloud.module.pay.enums.ErrorCodeConstants.CHANNEL_MOCK_DISABLED;
import static org.mockito.Mockito.when;

/**
 * 工厂里没有注册任何渠道实现，等同于生产环境未开启模拟支付
 */
@Import({PayChannelServiceImpl.class, PayClientFactory.class, PayProperties.class})
class PayChannelServiceMockDisabledTest extends BasePayDbUnitTest {

    @Resource
    private PayChannelServiceImpl payChannelService;
    @Resource
    private PayChannelMapper payChannelMapper;
    @MockitoBean
    private PayAppService payAppService;

    @BeforeEach
    void stubApp() {
        when(payAppService.getApp(1L)).thenReturn(new PayAppDO().setId(1L).setStatus(0));
    }

    private static PayChannelSaveReqVO req(String code, String config) {
        return new PayChannelSaveReqVO().setAppId(1L).setCode(code).setStatus(0).setFeeRate(0.6).setConfig(config);
    }

    @Test
    void 未开启模拟支付时不能新建Mock渠道() {
        assertServiceException(() -> payChannelService.createChannel(req("mock", "{}")), CHANNEL_MOCK_DISABLED);
    }

    @Test
    void 未开启模拟支付时不能启用已有的Mock渠道() {
        // 直接插入一条禁用的 mock 渠道（经 mapper 插入即自动加密），再更新为启用
        PayChannelDO channel = new PayChannelDO().setAppId(1L).setCode("mock").setStatus(1).setFeeRate(0D).setConfig("{}");
        payChannelMapper.insert(channel);
        assertServiceException(() -> payChannelService.updateChannel(req("mock", "{}").setId(channel.getId()).setStatus(0)),
                CHANNEL_MOCK_DISABLED);
    }
}
