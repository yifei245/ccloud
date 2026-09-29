package com.ccloud.module.system.service.sms;

import com.ccloud.module.system.mq.message.sms.SmsSendMessage;

import java.util.Map;

/**
 * 短信发送 Service：同步落日志，异步调用渠道。
 *
 * <p>发送请求与日志在同一事务内写入本地消息表，事务提交后投递到 Redis Stream，
 * 由 {@link com.ccloud.module.system.mq.consumer.sms.SmsSendConsumer} 调用渠道并回写结果。
 * 渠道慢或不可用不会拖慢、拖垮业务请求
 */
public interface SmsSendService {

    /**
     * @param mobile 为空时按 userId 取管理员手机号
     * @return 短信日志编号
     */
    Long sendSingleSmsToAdmin(String mobile, Long userId, String templateCode, Map<String, Object> templateParams);

    /**
     * @param mobile 必填；按会员编号取手机号随会员模块（Task 40）提供
     * @return 短信日志编号
     */
    Long sendSingleSmsToMember(String mobile, Long userId, String templateCode, Map<String, Object> templateParams);

    /**
     * 模板或渠道关闭时记为「不发送」，不投递
     *
     * @param userId   可为空，如未登录用户的验证码
     * @param userType 可为空
     * @return 短信日志编号
     */
    Long sendSingleSms(String mobile, Long userId, Integer userType, String templateCode,
                       Map<String, Object> templateParams);

    /**
     * 消费发送消息：调用渠道并回写结果。日志已不是初始化状态时跳过，重复投递不会重复发送
     */
    void doSendSms(SmsSendMessage message);
}
