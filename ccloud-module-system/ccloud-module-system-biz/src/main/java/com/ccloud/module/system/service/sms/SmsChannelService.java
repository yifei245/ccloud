package com.ccloud.module.system.service.sms;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.system.controller.admin.sms.vo.channel.SmsChannelPageReqVO;
import com.ccloud.module.system.controller.admin.sms.vo.channel.SmsChannelSaveReqVO;
import com.ccloud.module.system.dal.dataobject.sms.SmsChannelDO;
import com.ccloud.module.system.framework.sms.core.client.SmsClient;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 短信渠道 Service
 */
public interface SmsChannelService {

    Long createSmsChannel(@Valid SmsChannelSaveReqVO createReqVO);

    void updateSmsChannel(@Valid SmsChannelSaveReqVO updateReqVO);

    /**
     * 渠道下仍有模板时不允许删除
     */
    void deleteSmsChannel(Long id);

    SmsChannelDO getSmsChannel(Long id);

    List<SmsChannelDO> getSmsChannelList();

    PageResult<SmsChannelDO> getSmsChannelPage(SmsChannelPageReqVO pageReqVO);

    /**
     * 按数据库中的最新配置获得发送客户端。每次读库而不缓存配置：
     * 多实例部署时，任一实例修改渠道后其他实例下次发送即生效
     *
     * @return 渠道不存在返回 null
     */
    SmsClient getSmsClient(Long id);
}
