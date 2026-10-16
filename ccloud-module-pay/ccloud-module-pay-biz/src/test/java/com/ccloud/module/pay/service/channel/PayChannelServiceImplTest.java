package com.ccloud.module.pay.service.channel;

import cn.hutool.core.util.StrUtil;
import com.ccloud.module.pay.controller.admin.channel.vo.PayChannelSaveReqVO;
import com.ccloud.module.pay.dal.dataobject.app.PayAppDO;
import com.ccloud.module.pay.dal.mysql.channel.PayChannelMapper;
import com.ccloud.module.pay.framework.config.PayProperties;
import com.ccloud.module.pay.framework.pay.core.client.PayClientFactory;
import com.ccloud.module.pay.service.app.PayAppService;
import com.ccloud.module.pay.support.FakePayClientProvider;
import com.ccloud.module.pay.test.BasePayDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static com.ccloud.module.pay.enums.ErrorCodeConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@Import({PayChannelServiceImpl.class, PayClientFactory.class, FakePayClientProvider.class, PayProperties.class})
class PayChannelServiceImplTest extends BasePayDbUnitTest {

    @Resource
    private PayChannelServiceImpl payChannelService;
    @Resource
    private PayChannelMapper payChannelMapper;
    @Resource
    private JdbcTemplate jdbcTemplate;
    @Resource
    private PayProperties payProperties;
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
    void 配置加密存储_读取为原文() {
        Long id = payChannelService.createChannel(req("mock", "{\"secret\":\"s3cr3t-value\"}"));
        String raw = jdbcTemplate.queryForObject("SELECT config FROM pay_channel WHERE id = ?", String.class, id);
        assertThat(raw).doesNotContain("s3cr3t-value");
        assertThat(payChannelMapper.selectById(id).getConfig()).contains("s3cr3t-value");
    }

    @Test
    void 同一应用不能有两个相同编码的渠道() {
        payChannelService.createChannel(req("mock", "{}"));
        assertServiceException(() -> payChannelService.createChannel(req("mock", "{}")), CHANNEL_EXIST_SAME_CHANNEL_ERROR);
    }

    @Test
    void 未实现或未知的编码不能创建() {
        assertServiceException(() -> payChannelService.createChannel(req("wx_lite", "{}")), CHANNEL_NOT_SUPPORTED);
        assertServiceException(() -> payChannelService.createChannel(req("nope", "{}")), CHANNEL_NOT_SUPPORTED);
    }

    @Test
    void 更新时提交掩码保留原密钥() {
        Long id = payChannelService.createChannel(req("mock", "{\"secret\":\"s3cr3t-value\"}"));
        payChannelService.updateChannel(req("mock", "{\"secret\":\"***\"}").setId(id));
        assertThat(payChannelMapper.selectById(id).getConfig()).contains("s3cr3t-value");
    }

    @Test
    void 禁用的渠道不能使用() {
        payChannelService.createChannel(req("mock", "{}").setStatus(1));
        assertServiceException(() -> payChannelService.validPayChannel(1L, "mock"), CHANNEL_IS_DISABLE);
        assertServiceException(() -> payChannelService.validPayChannel(1L, "wallet"), CHANNEL_NOT_FOUND);
    }

    @Test
    void 回调地址由网关地址与渠道编号拼成() {
        assertThat(payChannelService.buildNotifyUrl("order", 7L))
                .isEqualTo(StrUtil.removeSuffix(payProperties.getNotifyBaseUrl(), "/") + "/app-api/pay/notify/order/7");
    }

    @Test
    void 应用不存在时不能创建渠道() {
        assertServiceException(() -> payChannelService.createChannel(req("mock", "{}").setAppId(404L)), PAY_APP_NOT_FOUND);
    }

    @Test
    void 配置不合法时不能创建() {
        assertServiceException(() -> payChannelService.createChannel(req("mock", "not-json")), CHANNEL_CONFIG_INVALID);
    }

    @Test
    void 更新不改渠道编码与所属应用() {
        Long id = payChannelService.createChannel(req("mock", "{}"));
        payChannelService.updateChannel(req("wx_lite", "{}").setAppId(2L).setId(id).setFeeRate(1.5));
        var channel = payChannelMapper.selectById(id);
        assertThat(channel.getCode()).isEqualTo("mock");
        assertThat(channel.getAppId()).isEqualTo(1L);
        assertThat(channel.getFeeRate()).isEqualTo(1.5);
    }

    @Test
    void 查询与删除渠道() {
        Long id = payChannelService.createChannel(req("mock", "{}"));
        assertThat(payChannelService.getChannel(id).getCode()).isEqualTo("mock");
        assertThat(payChannelService.getChannelListByAppId(1L)).hasSize(1);
        assertThat(payChannelService.getEnableChannelList(1L)).hasSize(1);
        assertThat(payChannelService.getChannelIgnoreTenant(id).getId()).isEqualTo(id);
        payChannelService.deleteChannel(id);
        assertThat(payChannelService.getChannel(id)).isNull();
        assertServiceException(() -> payChannelService.deleteChannel(id), CHANNEL_NOT_FOUND);
    }

    private void insertExtension(long channelId, int status) {
        jdbcTemplate.update("INSERT INTO pay_order_extension (no, order_id, channel_id, channel_code, user_ip, status, tenant_id) "
                + "VALUES (?, 1, ?, 'mock', '1.1.1.1', ?, ?)", "UT-DEL-" + System.nanoTime(), channelId, status, TENANT_ID);
    }

    private void insertRefund(long channelId, int status) {
        long n = System.nanoTime();
        jdbcTemplate.update("INSERT INTO pay_refund (no, app_id, channel_id, channel_code, order_id, order_no, merchant_order_id, "
                + "merchant_refund_id, status, pay_price, refund_price, reason, channel_order_no, tenant_id) "
                + "VALUES (?, 1, ?, 'mock', 1, 'o', 'mo', ?, ?, 100, 100, 'r', '', ?)", "UT-DEL-R" + n, channelId, "UT-DEL-R" + n, status, TENANT_ID);
    }

    private void insertTransfer(long channelId, int status) {
        long n = System.nanoTime();
        jdbcTemplate.update("INSERT INTO pay_transfer (no, app_id, channel_id, channel_code, merchant_transfer_id, subject, price, "
                + "user_account, status, user_ip, tenant_id) VALUES (?, 1, ?, 'mock', ?, 's', 100, 'acc', ?, '1.1.1.1', ?)",
                "UT-DEL-T" + n, channelId, "UT-DEL-T" + n, status, TENANT_ID);
    }

    @Test
    void 渠道下有等待中的支付尝试时不能删除_了结后可以删除() {
        Long id = payChannelService.createChannel(req("mock", "{}"));
        insertExtension(id, 0);
        assertServiceException(() -> payChannelService.deleteChannel(id), CHANNEL_HAS_UNFINISHED);
        assertThat(payChannelService.getChannel(id)).isNotNull();

        jdbcTemplate.update("UPDATE pay_order_extension SET status = 10 WHERE channel_id = ?", id);
        payChannelService.deleteChannel(id);
        assertThat(payChannelService.getChannel(id)).isNull();
    }

    @Test
    void 渠道下有未完成的退款时不能删除_已完成的不拦() {
        Long id = payChannelService.createChannel(req("mock", "{}"));
        insertRefund(id, 10);
        insertRefund(id, 20);
        insertRefund(id, 0);
        assertServiceException(() -> payChannelService.deleteChannel(id), CHANNEL_HAS_UNFINISHED);

        jdbcTemplate.update("UPDATE pay_refund SET status = 10 WHERE channel_id = ?", id);
        payChannelService.deleteChannel(id);
        assertThat(payChannelService.getChannel(id)).isNull();
    }

    @Test
    void 渠道下有未完成的转账时不能删除_等待与进行中都算_已完成的不拦() {
        Long waiting = payChannelService.createChannel(req("mock", "{}"));
        insertTransfer(waiting, 0);
        assertServiceException(() -> payChannelService.deleteChannel(waiting), CHANNEL_HAS_UNFINISHED);
        jdbcTemplate.update("UPDATE pay_transfer SET status = 5 WHERE channel_id = ?", waiting);
        assertServiceException(() -> payChannelService.deleteChannel(waiting), CHANNEL_HAS_UNFINISHED);

        jdbcTemplate.update("UPDATE pay_transfer SET status = 20 WHERE channel_id = ?", waiting);
        insertTransfer(waiting, 10);
        payChannelService.deleteChannel(waiting);
        assertThat(payChannelService.getChannel(waiting)).isNull();
    }

    @Test
    void 其他渠道的未完成单据不影响本渠道删除() {
        Long id = payChannelService.createChannel(req("mock", "{}"));
        insertExtension(id + 1000, 0);
        insertRefund(id + 1000, 0);
        insertTransfer(id + 1000, 0);
        payChannelService.deleteChannel(id);
        assertThat(payChannelService.getChannel(id)).isNull();
    }
}
