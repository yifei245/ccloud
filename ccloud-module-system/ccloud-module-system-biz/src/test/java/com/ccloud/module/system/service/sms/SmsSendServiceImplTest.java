package com.ccloud.module.system.service.sms;

import com.ccloud.framework.common.core.KeyValue;
import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.mq.outbox.core.OutboxMessageService;
import com.ccloud.module.system.controller.admin.sms.vo.template.SmsTemplateSaveReqVO;
import com.ccloud.module.system.dal.dataobject.sms.SmsLogDO;
import com.ccloud.module.system.dal.dataobject.user.AdminUserDO;
import com.ccloud.module.system.dal.mysql.sms.SmsLogMapper;
import com.ccloud.module.system.enums.sms.SmsSendStatusEnum;
import com.ccloud.module.system.framework.sms.core.client.SmsClient;
import com.ccloud.module.system.framework.sms.core.client.SmsClientFactory;
import com.ccloud.module.system.framework.sms.core.client.dto.SmsSendRespDTO;
import com.ccloud.module.system.mq.message.sms.SmsSendMessage;
import com.ccloud.module.system.service.user.AdminUserService;
import com.ccloud.module.system.test.BaseSystemDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.ccloud.module.system.enums.ErrorCodeConstants.*;
import static com.ccloud.module.system.service.sms.SmsTestData.channelReqVO;
import static com.ccloud.module.system.service.sms.SmsTestData.templateReqVO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Import({SmsSendServiceImpl.class, SmsTemplateServiceImpl.class, SmsChannelServiceImpl.class, SmsLogServiceImpl.class})
class SmsSendServiceImplTest extends BaseSystemDbUnitTest {

    private static final String MOBILE = "13800000000";

    @Resource
    private SmsSendService smsSendService;
    @Resource
    private SmsTemplateService templateService;
    @Resource
    private SmsChannelService channelService;
    @Resource
    private SmsLogMapper smsLogMapper;

    @MockitoBean
    private OutboxMessageService outboxMessageService;
    @MockitoBean
    private SmsClientFactory smsClientFactory;
    @MockitoBean
    private AdminUserService adminUserService;

    private final SmsClient smsClient = mock(SmsClient.class);

    private Long channelId;

    @BeforeEach
    void seed() {
        channelId = channelService.createSmsChannel(channelReqVO("ccloud", CommonStatusEnum.ENABLE));
        templateService.createSmsTemplate(templateReqVO(channelId, "t-notice", "您好 {name}，订单 {orderNo} 已发货"));
        when(smsClientFactory.createOrUpdateSmsClient(any())).thenReturn(smsClient);
    }

    // ========== 发送：落日志并发消息 ==========

    @Test
    void sendSingleSms_模板与渠道开启时日志为初始化并在事务提交后投递消息() {
        Long logId = smsSendService.sendSingleSms(MOBILE, 7L, UserTypeEnum.MEMBER.getValue(), "t-notice",
                params("orderNo", "A001", "name", "张三"));

        SmsLogDO log = smsLogMapper.selectById(logId);
        assertThat(log.getSendStatus()).isEqualTo(SmsSendStatusEnum.INIT.getStatus());
        assertThat(log.getTemplateContent()).isEqualTo("您好 张三，订单 A001 已发货");
        assertThat(log.getUserId()).isEqualTo(7L);
        assertThat(log.getTenantId()).isEqualTo(TENANT_ID);

        ArgumentCaptor<SmsSendMessage> captor = ArgumentCaptor.forClass(SmsSendMessage.class);
        verify(outboxMessageService).saveAndPublish(eq(SmsSendMessage.TOPIC), captor.capture());
        SmsSendMessage message = captor.getValue();
        assertThat(message.getLogId()).isEqualTo(logId);
        assertThat(message.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(message.getChannelId()).isEqualTo(channelId);
        assertThat(message.getApiTemplateId()).isEqualTo("api-t-notice");
        // 按模板声明顺序排列，而不是调用方传入的顺序：腾讯云按位置传参
        assertThat(message.getTemplateParams()).extracting(KeyValue::getKey).containsExactly("name", "orderNo");
    }

    @Test
    void sendSingleSms_模板关闭时记为不发送且不投递() {
        SmsTemplateSaveReqVO reqVO = templateReqVO(channelId, "t-off", "x {code}");
        reqVO.setStatus(CommonStatusEnum.DISABLE.getStatus());
        templateService.createSmsTemplate(reqVO);

        Long logId = smsSendService.sendSingleSms(MOBILE, null, null, "t-off", params("code", "1"));

        assertThat(smsLogMapper.selectById(logId).getSendStatus()).isEqualTo(SmsSendStatusEnum.IGNORE.getStatus());
        verifyNoInteractions(outboxMessageService);
    }

    @Test
    void sendSingleSms_渠道关闭时记为不发送且不投递() {
        var channelReqVO = channelReqVO("ccloud", CommonStatusEnum.DISABLE);
        channelReqVO.setId(channelId);
        channelService.updateSmsChannel(channelReqVO);

        Long logId = smsSendService.sendSingleSms(MOBILE, null, null, "t-notice", params("name", "a", "orderNo", "b"));

        assertThat(smsLogMapper.selectById(logId).getSendStatus()).isEqualTo(SmsSendStatusEnum.IGNORE.getStatus());
        verifyNoInteractions(outboxMessageService);
    }

    @Test
    void sendSingleSms_模板不存在() {
        assertServiceException(() -> smsSendService.sendSingleSms(MOBILE, null, null, "none", Map.of()),
                SMS_SEND_TEMPLATE_NOT_EXISTS);
    }

    @Test
    void sendSingleSms_手机号为空() {
        assertServiceException(() -> smsSendService.sendSingleSms(null, null, null, "t-notice", Map.of()),
                SMS_SEND_MOBILE_NOT_EXISTS);
    }

    @Test
    void sendSingleSms_模板参数缺失时不落日志() {
        assertServiceException(() -> smsSendService.sendSingleSms(MOBILE, null, null, "t-notice", params("name", "a")),
                SMS_SEND_MOBILE_TEMPLATE_PARAM_MISS);
        assertThat(smsLogMapper.selectList()).isEmpty();
    }

    @Test
    void sendSingleSmsToAdmin_未传手机号时取管理员手机号() {
        when(adminUserService.getUser(9L)).thenReturn(new AdminUserDO().setId(9L).setMobile("13900000000"));

        Long logId = smsSendService.sendSingleSmsToAdmin(null, 9L, "t-notice", params("name", "a", "orderNo", "b"));

        SmsLogDO log = smsLogMapper.selectById(logId);
        assertThat(log.getMobile()).isEqualTo("13900000000");
        assertThat(log.getUserType()).isEqualTo(UserTypeEnum.ADMIN.getValue());
    }

    // ========== 消费：调用渠道并回写结果 ==========

    @Test
    void doSendSms_渠道受理成功时回写成功与流水号() throws Throwable {
        Long logId = smsSendService.sendSingleSms(MOBILE, null, null, "t-notice", params("name", "a", "orderNo", "b"));
        when(smsClient.sendSms(eq(logId), eq(MOBILE), eq("api-t-notice"), anyList()))
                .thenReturn(new SmsSendRespDTO().setSuccess(true).setApiCode("OK").setSerialNo("S1").setApiRequestId("R1"));

        smsSendService.doSendSms(message(logId));

        SmsLogDO log = smsLogMapper.selectById(logId);
        assertThat(log.getSendStatus()).isEqualTo(SmsSendStatusEnum.SUCCESS.getStatus());
        assertThat(log.getApiSerialNo()).isEqualTo("S1");
        assertThat(log.getApiRequestId()).isEqualTo("R1");
        assertThat(log.getSendTime()).isNotNull();
    }

    @Test
    void doSendSms_渠道拒绝时回写失败原因() throws Throwable {
        Long logId = smsSendService.sendSingleSms(MOBILE, null, null, "t-notice", params("name", "a", "orderNo", "b"));
        when(smsClient.sendSms(any(), any(), any(), anyList()))
                .thenReturn(new SmsSendRespDTO().setSuccess(false).setApiCode("isv.BUSINESS_LIMIT_CONTROL").setApiMsg("触发流控"));

        smsSendService.doSendSms(message(logId));

        SmsLogDO log = smsLogMapper.selectById(logId);
        assertThat(log.getSendStatus()).isEqualTo(SmsSendStatusEnum.FAILURE.getStatus());
        assertThat(log.getApiSendMsg()).isEqualTo("触发流控");
    }

    @Test
    void doSendSms_渠道调用异常时回写失败且不抛出_验证码过时重发无意义() throws Throwable {
        Long logId = smsSendService.sendSingleSms(MOBILE, null, null, "t-notice", params("name", "a", "orderNo", "b"));
        when(smsClient.sendSms(any(), any(), any(), anyList())).thenThrow(new java.net.SocketTimeoutException("超时"));

        smsSendService.doSendSms(message(logId));

        SmsLogDO log = smsLogMapper.selectById(logId);
        assertThat(log.getSendStatus()).isEqualTo(SmsSendStatusEnum.FAILURE.getStatus());
        assertThat(log.getApiSendMsg()).contains("超时");
    }

    @Test
    void doSendSms_日志已处理过时跳过_重复投递不重复发送() throws Throwable {
        Long logId = smsSendService.sendSingleSms(MOBILE, null, null, "t-notice", params("name", "a", "orderNo", "b"));
        when(smsClient.sendSms(any(), any(), any(), anyList())).thenReturn(new SmsSendRespDTO().setSuccess(true));

        smsSendService.doSendSms(message(logId));
        smsSendService.doSendSms(message(logId));

        verify(smsClient, times(1)).sendSms(any(), any(), any(), anyList());
    }

    private SmsSendMessage message(Long logId) {
        return new SmsSendMessage().setLogId(logId).setTenantId(TENANT_ID).setMobile(MOBILE).setChannelId(channelId)
                .setApiTemplateId("api-t-notice")
                .setTemplateParams(List.of(new KeyValue<>("name", "a"), new KeyValue<>("orderNo", "b")));
    }

    private static Map<String, Object> params(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }
}
