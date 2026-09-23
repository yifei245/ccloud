package com.ccloud.module.system.service.sms;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.module.system.controller.admin.sms.vo.template.SmsTemplateSaveReqVO;
import com.ccloud.module.system.dal.dataobject.sms.SmsTemplateDO;
import com.ccloud.module.system.dal.mysql.sms.SmsTemplateMapper;
import com.ccloud.module.system.framework.sms.core.client.impl.SmsClientFactoryImpl;
import com.ccloud.module.system.test.BaseSystemDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;

import static com.ccloud.module.system.enums.ErrorCodeConstants.*;
import static com.ccloud.module.system.service.sms.SmsTestData.channelReqVO;
import static com.ccloud.module.system.service.sms.SmsTestData.templateReqVO;
import static org.assertj.core.api.Assertions.assertThat;

@Import({SmsTemplateServiceImpl.class, SmsChannelServiceImpl.class, SmsClientFactoryImpl.class})
class SmsTemplateServiceImplTest extends BaseSystemDbUnitTest {

    @Resource
    private SmsTemplateService templateService;
    @Resource
    private SmsChannelService channelService;
    @Resource
    private SmsTemplateMapper templateMapper;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    private Long channelId;

    @BeforeEach
    void seed() {
        channelId = channelService.createSmsChannel(channelReqVO("ccloud", CommonStatusEnum.ENABLE));
    }

    @Test
    void createSmsTemplate_成功时从内容解析参数并冗余渠道编码() {
        Long id = templateService.createSmsTemplate(templateReqVO(channelId, "t-login", "您好 {name}，验证码为 {code}"));

        SmsTemplateDO template = templateMapper.selectById(id);
        assertThat(template.getParams()).containsExactly("name", "code");
        assertThat(template.getChannelCode()).isEqualTo("MOCK");
    }

    @Test
    void createSmsTemplate_渠道不存在() {
        assertServiceException(() -> templateService.createSmsTemplate(templateReqVO(-1L, "t1", "x")),
                SMS_CHANNEL_NOT_EXISTS);
    }

    @Test
    void createSmsTemplate_渠道已关闭() {
        Long disabled = channelService.createSmsChannel(channelReqVO("关闭", CommonStatusEnum.DISABLE));
        assertServiceException(() -> templateService.createSmsTemplate(templateReqVO(disabled, "t1", "x")),
                SMS_CHANNEL_DISABLE);
    }

    @Test
    void createSmsTemplate_编码重复() {
        templateService.createSmsTemplate(templateReqVO(channelId, "t-dup", "x"));
        assertServiceException(() -> templateService.createSmsTemplate(templateReqVO(channelId, "t-dup", "y")),
                SMS_TEMPLATE_CODE_DUPLICATE);
    }

    @Test
    void updateSmsTemplate_成功时重新解析参数() {
        Long id = templateService.createSmsTemplate(templateReqVO(channelId, "t-upd", "旧 {a}"));
        SmsTemplateSaveReqVO reqVO = templateReqVO(channelId, "t-upd", "新 {b} {c}");
        reqVO.setId(id);

        templateService.updateSmsTemplate(reqVO);

        SmsTemplateDO template = templateMapper.selectById(id);
        assertThat(template.getContent()).isEqualTo("新 {b} {c}");
        assertThat(template.getParams()).containsExactly("b", "c");
    }

    @Test
    void updateSmsTemplate_不存在() {
        SmsTemplateSaveReqVO reqVO = templateReqVO(channelId, "x", "x");
        reqVO.setId(-1L);
        assertServiceException(() -> templateService.updateSmsTemplate(reqVO), SMS_TEMPLATE_NOT_EXISTS);
    }

    @Test
    void updateSmsTemplate_编码与其他模板重复() {
        templateService.createSmsTemplate(templateReqVO(channelId, "taken", "x"));
        Long id = templateService.createSmsTemplate(templateReqVO(channelId, "self", "x"));
        SmsTemplateSaveReqVO reqVO = templateReqVO(channelId, "taken", "x");
        reqVO.setId(id);
        assertServiceException(() -> templateService.updateSmsTemplate(reqVO), SMS_TEMPLATE_CODE_DUPLICATE);
    }

    @Test
    void deleteSmsTemplate_成功与不存在() {
        Long id = templateService.createSmsTemplate(templateReqVO(channelId, "t-del", "x"));
        templateService.deleteSmsTemplate(id);
        assertThat(templateMapper.selectById(id)).isNull();
        assertServiceException(() -> templateService.deleteSmsTemplate(-1L), SMS_TEMPLATE_NOT_EXISTS);
    }

    @Test
    void getSmsTemplateByCode_命中与未命中() {
        templateService.createSmsTemplate(templateReqVO(channelId, "t-code", "x"));
        assertThat(templateService.getSmsTemplateByCode("t-code").getName()).isEqualTo("模板t-code");
        assertThat(templateService.getSmsTemplateByCode("none")).isNull();
    }

    @Test
    void formatSmsTemplateContent_按参数名替换() {
        assertThat(templateService.formatSmsTemplateContent("您好 {name}，验证码为 {code}",
                Map.of("name", "张三", "code", 1234))).isEqualTo("您好 张三，验证码为 1234");
    }
}
