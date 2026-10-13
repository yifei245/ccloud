package com.ccloud.module.pay.service.channel;

import com.ccloud.module.pay.controller.admin.channel.vo.PayChannelSaveReqVO;
import com.ccloud.module.pay.dal.dataobject.channel.PayChannelDO;
import com.ccloud.module.pay.framework.pay.core.client.PayClient;

import java.util.List;

/**
 * 支付渠道 Service。渠道配置含商户密钥：本接口返回的 {@link PayChannelDO#getConfig()} 为原文，只在服务内使用
 */
public interface PayChannelService {

    Long createChannel(PayChannelSaveReqVO reqVO);

    void updateChannel(PayChannelSaveReqVO reqVO);

    void deleteChannel(Long id);

    PayChannelDO getChannel(Long id);

    List<PayChannelDO> getChannelListByAppId(Long appId);

    List<PayChannelDO> getEnableChannelList(Long appId);

    /**
     * 校验渠道可用：不存在 CHANNEL_NOT_FOUND，禁用 CHANNEL_IS_DISABLE
     */
    PayChannelDO validPayChannel(Long appId, String code);

    /**
     * 忽略租户查询渠道，回调入口使用
     */
    PayChannelDO getChannelIgnoreTenant(Long id);

    PayClient getPayClient(PayChannelDO channel);

    /**
     * 渠道回调地址
     *
     * @param type      order、refund 或 transfer
     * @param channelId 渠道编号
     */
    String buildNotifyUrl(String type, Long channelId);
}
