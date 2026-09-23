package com.ccloud.module.system.service.sms;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.system.controller.admin.sms.vo.channel.SmsChannelPageReqVO;
import com.ccloud.module.system.controller.admin.sms.vo.channel.SmsChannelSaveReqVO;
import com.ccloud.module.system.dal.dataobject.sms.SmsChannelDO;
import com.ccloud.module.system.dal.mysql.sms.SmsChannelMapper;
import com.ccloud.module.system.framework.sms.core.client.SmsClient;
import com.ccloud.module.system.framework.sms.core.client.impl.MockSmsClient;
import com.ccloud.module.system.framework.sms.core.client.impl.SmsClientFactoryImpl;
import com.ccloud.module.system.test.BaseSystemDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static com.ccloud.module.system.enums.ErrorCodeConstants.*;
import static com.ccloud.module.system.service.sms.SmsTestData.channelReqVO;
import static com.ccloud.module.system.service.sms.SmsTestData.templateReqVO;
import static org.assertj.core.api.Assertions.assertThat;

@Import({SmsChannelServiceImpl.class, SmsTemplateServiceImpl.class, SmsClientFactoryImpl.class})
class SmsChannelServiceImplTest extends BaseSystemDbUnitTest {

    @Resource
    private SmsChannelService channelService;
    @Resource
    private SmsTemplateService templateService;
    @Resource
    private SmsChannelMapper channelMapper;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void createSmsChannel_成功() {
        Long id = channelService.createSmsChannel(channelReqVO("ccloud", CommonStatusEnum.ENABLE));
        SmsChannelDO channel = channelMapper.selectById(id);
        assertThat(channel.getSignature()).isEqualTo("ccloud");
        assertThat(channel.getCode()).isEqualTo("MOCK");
    }

    @Test
    void updateSmsChannel_成功后发送客户端使用新配置() {
        Long id = channelService.createSmsChannel(channelReqVO("旧签名", CommonStatusEnum.ENABLE));
        MockSmsClient before = (MockSmsClient) channelService.getSmsClient(id);
        assertThat(before.getProperties().getSignature()).isEqualTo("旧签名");

        SmsChannelSaveReqVO reqVO = channelReqVO("新签名", CommonStatusEnum.ENABLE);
        reqVO.setId(id);
        channelService.updateSmsChannel(reqVO);

        MockSmsClient after = (MockSmsClient) channelService.getSmsClient(id);
        assertThat(after.getProperties().getSignature()).isEqualTo("新签名");
    }

    @Test
    void updateSmsChannel_不存在() {
        SmsChannelSaveReqVO reqVO = channelReqVO("x", CommonStatusEnum.ENABLE);
        reqVO.setId(-1L);
        assertServiceException(() -> channelService.updateSmsChannel(reqVO), SMS_CHANNEL_NOT_EXISTS);
    }

    @Test
    void deleteSmsChannel_成功与不存在() {
        Long id = channelService.createSmsChannel(channelReqVO("待删", CommonStatusEnum.ENABLE));
        channelService.deleteSmsChannel(id);
        assertThat(channelMapper.selectById(id)).isNull();
        assertServiceException(() -> channelService.deleteSmsChannel(-1L), SMS_CHANNEL_NOT_EXISTS);
    }

    @Test
    void deleteSmsChannel_仍有模板时不允许删除() {
        Long id = channelService.createSmsChannel(channelReqVO("有模板", CommonStatusEnum.ENABLE));
        templateService.createSmsTemplate(templateReqVO(id, "t-child", "验证码 {code}"));
        assertServiceException(() -> channelService.deleteSmsChannel(id), SMS_CHANNEL_HAS_CHILDREN);
    }

    @Test
    void getSmsClient_渠道不存在返回null() {
        SmsClient client = channelService.getSmsClient(-1L);
        assertThat(client).isNull();
    }

    @Test
    void getSmsChannelPage_按签名与状态过滤() {
        channelService.createSmsChannel(channelReqVO("page-甲", CommonStatusEnum.ENABLE));
        channelService.createSmsChannel(channelReqVO("page-乙", CommonStatusEnum.DISABLE));
        SmsChannelPageReqVO reqVO = new SmsChannelPageReqVO();
        reqVO.setSignature("page-");
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());

        PageResult<SmsChannelDO> page = channelService.getSmsChannelPage(reqVO);

        assertThat(page.getList()).extracting(SmsChannelDO::getSignature).containsExactly("page-甲");
    }
}
