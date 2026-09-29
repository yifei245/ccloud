package com.ccloud.module.system.service.notify;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.websocket.core.sender.WebSocketMessageSender;
import com.ccloud.module.system.controller.admin.notify.vo.message.NotifyMessageMyPageReqVO;
import com.ccloud.module.system.controller.admin.notify.vo.template.NotifyTemplateSaveReqVO;
import com.ccloud.module.system.dal.dataobject.notify.NotifyMessageDO;
import com.ccloud.module.system.dal.mysql.notify.NotifyMessageMapper;
import com.ccloud.module.system.test.BaseSystemDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;

import static com.ccloud.module.system.enums.ErrorCodeConstants.NOTIFY_SEND_TEMPLATE_PARAM_MISS;
import static com.ccloud.module.system.enums.ErrorCodeConstants.NOTIFY_TEMPLATE_NOT_EXISTS;
import static com.ccloud.module.system.service.notify.NotifyTemplateServiceImplTest.reqVO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Import({NotifySendServiceImpl.class, NotifyMessageServiceImpl.class, NotifyTemplateServiceImpl.class})
class NotifyMessageServiceImplTest extends BaseSystemDbUnitTest {

    private static final Integer ADMIN = UserTypeEnum.ADMIN.getValue();
    private static final Long ME = 1L;
    private static final Long OTHER = 2L;

    @Resource
    private NotifySendService sendService;
    @Resource
    private NotifyMessageService messageService;
    @Resource
    private NotifyTemplateService templateService;
    @Resource
    private NotifyMessageMapper messageMapper;

    @MockitoBean
    private WebSocketMessageSender webSocketMessageSender;

    @BeforeEach
    void seed() {
        templateService.createNotifyTemplate(reqVO("t-audit", "申请 {no} 已审批"));
    }

    // ========== 发送 ==========

    @Test
    void sendSingleNotify_落库渲染内容并在事务提交后推送() {
        Long id = sendService.sendSingleNotifyToAdmin(ME, "t-audit", Map.of("no", "A001"));

        NotifyMessageDO message = messageMapper.selectById(id);
        assertThat(message.getTemplateContent()).isEqualTo("申请 A001 已审批");
        assertThat(message.getTemplateNickname()).isEqualTo("系统");
        assertThat(message.getReadStatus()).isFalse();
        assertThat(message.getTenantId()).isEqualTo(TENANT_ID);
        // 提交前不推送：事务回滚时用户不应看到一条不存在的消息
        verifyNoInteractions(webSocketMessageSender);

        commitCurrentTransaction();
        verify(webSocketMessageSender).sendObject(eq(ADMIN), eq(ME), eq("notify-message"),
                argThat(content -> content.toString().contains("申请 A001 已审批")));
    }

    @Test
    void sendSingleNotify_模板不存在() {
        assertServiceException(() -> sendService.sendSingleNotifyToAdmin(ME, "none", Map.of()), NOTIFY_TEMPLATE_NOT_EXISTS);
    }

    @Test
    void sendSingleNotify_模板参数缺失() {
        assertServiceException(() -> sendService.sendSingleNotifyToAdmin(ME, "t-audit", Map.of()),
                NOTIFY_SEND_TEMPLATE_PARAM_MISS);
    }

    @Test
    void sendSingleNotify_模板关闭时不发送() {
        NotifyTemplateSaveReqVO off = reqVO("t-off", "x");
        off.setStatus(CommonStatusEnum.DISABLE.getStatus());
        templateService.createNotifyTemplate(off);

        assertThat(sendService.sendSingleNotifyToAdmin(ME, "t-off", Map.of())).isNull();
        assertThat(messageMapper.selectList()).isEmpty();
    }

    // ========== 我的站内信 ==========

    @Test
    void 发送后接收人未读数加1_其他人不受影响() {
        sendService.sendSingleNotifyToAdmin(ME, "t-audit", Map.of("no", "1"));

        assertThat(messageService.getUnreadNotifyMessageCount(ME, ADMIN)).isEqualTo(1);
        assertThat(messageService.getUnreadNotifyMessageCount(OTHER, ADMIN)).isZero();
        // 同编号的会员不是同一个人
        assertThat(messageService.getUnreadNotifyMessageCount(ME, UserTypeEnum.MEMBER.getValue())).isZero();
    }

    @Test
    void 标记已读后未读数减1() {
        Long first = sendService.sendSingleNotifyToAdmin(ME, "t-audit", Map.of("no", "1"));
        sendService.sendSingleNotifyToAdmin(ME, "t-audit", Map.of("no", "2"));

        int updated = messageService.updateNotifyMessageRead(List.of(first), ME, ADMIN);

        assertThat(updated).isEqualTo(1);
        assertThat(messageService.getUnreadNotifyMessageCount(ME, ADMIN)).isEqualTo(1);
        assertThat(messageMapper.selectById(first).getReadTime()).isNotNull();
    }

    @Test
    void 标记已读_编号为空时不做任何更新_不能变成全部已读() {
        sendService.sendSingleNotifyToAdmin(ME, "t-audit", Map.of("no", "1"));
        assertThat(messageService.updateNotifyMessageRead(List.of(), ME, ADMIN)).isZero();
        assertThat(messageService.getUnreadNotifyMessageCount(ME, ADMIN)).isEqualTo(1);
    }

    @Test
    void 全部已读后未读数为0() {
        sendService.sendSingleNotifyToAdmin(ME, "t-audit", Map.of("no", "1"));
        sendService.sendSingleNotifyToAdmin(ME, "t-audit", Map.of("no", "2"));
        Long others = sendService.sendSingleNotifyToAdmin(OTHER, "t-audit", Map.of("no", "3"));

        messageService.updateAllNotifyMessageRead(ME, ADMIN);

        assertThat(messageService.getUnreadNotifyMessageCount(ME, ADMIN)).isZero();
        assertThat(messageMapper.selectById(others).getReadStatus()).as("不影响其他人").isFalse();
    }

    @Test
    void 用户只能读到发给自己的消息_也不能把别人的消息标为已读() {
        Long mine = sendService.sendSingleNotifyToAdmin(ME, "t-audit", Map.of("no", "mine"));
        Long others = sendService.sendSingleNotifyToAdmin(OTHER, "t-audit", Map.of("no", "others"));

        PageResult<NotifyMessageDO> page = messageService.getMyNotifyMessagePage(new NotifyMessageMyPageReqVO(), ME, ADMIN);
        assertThat(page.getList()).extracting(NotifyMessageDO::getId).containsExactly(mine);
        assertThat(messageService.getUnreadNotifyMessageList(ME, ADMIN, 10)).extracting(NotifyMessageDO::getId)
                .containsExactly(mine);

        assertThat(messageService.updateNotifyMessageRead(List.of(others), ME, ADMIN)).isZero();
        assertThat(messageMapper.selectById(others).getReadStatus()).isFalse();
    }

    @Test
    void getMyNotifyMessagePage_可按已读状态过滤_最新的在前() {
        Long first = sendService.sendSingleNotifyToAdmin(ME, "t-audit", Map.of("no", "1"));
        Long second = sendService.sendSingleNotifyToAdmin(ME, "t-audit", Map.of("no", "2"));
        messageService.updateNotifyMessageRead(List.of(first), ME, ADMIN);

        NotifyMessageMyPageReqVO reqVO = new NotifyMessageMyPageReqVO();
        assertThat(messageService.getMyNotifyMessagePage(reqVO, ME, ADMIN).getList())
                .extracting(NotifyMessageDO::getId).containsExactly(second, first);
        reqVO.setReadStatus(false);
        assertThat(messageService.getMyNotifyMessagePage(reqVO, ME, ADMIN).getList())
                .extracting(NotifyMessageDO::getId).containsExactly(second);
    }

    /**
     * 测试事务最终回滚，提交回调不会自然触发，这里手动调用以验证提交后的行为
     */
    private static void commitCurrentTransaction() {
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
    }
}
