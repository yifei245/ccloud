package com.ccloud.module.system.service.sms;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.system.controller.admin.sms.vo.channel.SmsChannelPageReqVO;
import com.ccloud.module.system.controller.admin.sms.vo.channel.SmsChannelSaveReqVO;
import com.ccloud.module.system.dal.dataobject.sms.SmsChannelDO;
import com.ccloud.module.system.dal.mysql.sms.SmsChannelMapper;
import com.ccloud.module.system.dal.mysql.sms.SmsTemplateMapper;
import com.ccloud.module.system.enums.sms.SmsChannelEnum;
import com.ccloud.module.system.framework.sms.core.client.SmsClient;
import com.ccloud.module.system.framework.sms.core.client.SmsClientFactory;
import com.ccloud.module.system.framework.sms.core.property.SmsChannelProperties;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static com.ccloud.module.system.enums.ErrorCodeConstants.SMS_CHANNEL_HAS_CHILDREN;
import static com.ccloud.module.system.enums.ErrorCodeConstants.SMS_CHANNEL_NOT_EXISTS;

@Service
@Validated
public class SmsChannelServiceImpl implements SmsChannelService {

    @Resource
    private SmsChannelMapper smsChannelMapper;
    @Resource
    private SmsTemplateMapper smsTemplateMapper;
    @Resource
    private SmsClientFactory smsClientFactory;

    @Override
    public Long createSmsChannel(SmsChannelSaveReqVO createReqVO) {
        validateChannelCode(createReqVO.getCode());
        SmsChannelDO channel = BeanUtils.toBean(createReqVO, SmsChannelDO.class);
        smsChannelMapper.insert(channel);
        return channel.getId();
    }

    @Override
    public void updateSmsChannel(SmsChannelSaveReqVO updateReqVO) {
        validateSmsChannelExists(updateReqVO.getId());
        validateChannelCode(updateReqVO.getCode());
        smsChannelMapper.updateById(BeanUtils.toBean(updateReqVO, SmsChannelDO.class));
    }

    @Override
    public void deleteSmsChannel(Long id) {
        validateSmsChannelExists(id);
        if (smsTemplateMapper.selectCountByChannelId(id) > 0) {
            throw exception(SMS_CHANNEL_HAS_CHILDREN);
        }
        smsChannelMapper.deleteById(id);
    }

    private void validateSmsChannelExists(Long id) {
        if (smsChannelMapper.selectById(id) == null) {
            throw exception(SMS_CHANNEL_NOT_EXISTS);
        }
    }

    private static void validateChannelCode(String code) {
        if (SmsChannelEnum.getByCode(code) == null) {
            throw invalidParamException("未知的短信渠道编码({})", code);
        }
    }

    @Override
    public SmsChannelDO getSmsChannel(Long id) {
        return smsChannelMapper.selectById(id);
    }

    @Override
    public List<SmsChannelDO> getSmsChannelList() {
        return smsChannelMapper.selectList();
    }

    @Override
    public PageResult<SmsChannelDO> getSmsChannelPage(SmsChannelPageReqVO pageReqVO) {
        return smsChannelMapper.selectPage(pageReqVO);
    }

    @Override
    public SmsClient getSmsClient(Long id) {
        SmsChannelDO channel = smsChannelMapper.selectById(id);
        if (channel == null) {
            return null;
        }
        return smsClientFactory.createOrUpdateSmsClient(BeanUtils.toBean(channel, SmsChannelProperties.class));
    }
}
