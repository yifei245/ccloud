package com.ccloud.module.system.service.sms;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.system.controller.admin.sms.vo.template.SmsTemplatePageReqVO;
import com.ccloud.module.system.controller.admin.sms.vo.template.SmsTemplateSaveReqVO;
import com.ccloud.module.system.dal.dataobject.sms.SmsChannelDO;
import com.ccloud.module.system.dal.dataobject.sms.SmsTemplateDO;
import com.ccloud.module.system.dal.mysql.sms.SmsTemplateMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.system.enums.ErrorCodeConstants.*;

@Service
@Validated
public class SmsTemplateServiceImpl implements SmsTemplateService {

    /**
     * 模板参数占位符，形如 {code}
     */
    private static final Pattern PARAM_PATTERN = Pattern.compile("\\{(\\w+)}");

    @Resource
    private SmsTemplateMapper smsTemplateMapper;
    @Resource
    private SmsChannelService smsChannelService;

    @Override
    public Long createSmsTemplate(SmsTemplateSaveReqVO createReqVO) {
        SmsChannelDO channel = validateSmsChannel(createReqVO.getChannelId());
        validateSmsTemplateCodeUnique(null, createReqVO.getCode());

        SmsTemplateDO template = BeanUtils.toBean(createReqVO, SmsTemplateDO.class)
                .setParams(parseTemplateContentParams(createReqVO.getContent()))
                .setChannelCode(channel.getCode());
        smsTemplateMapper.insert(template);
        return template.getId();
    }

    @Override
    public void updateSmsTemplate(SmsTemplateSaveReqVO updateReqVO) {
        validateSmsTemplateExists(updateReqVO.getId());
        SmsChannelDO channel = validateSmsChannel(updateReqVO.getChannelId());
        validateSmsTemplateCodeUnique(updateReqVO.getId(), updateReqVO.getCode());

        smsTemplateMapper.updateById(BeanUtils.toBean(updateReqVO, SmsTemplateDO.class)
                .setParams(parseTemplateContentParams(updateReqVO.getContent()))
                .setChannelCode(channel.getCode()));
    }

    @Override
    public void deleteSmsTemplate(Long id) {
        validateSmsTemplateExists(id);
        smsTemplateMapper.deleteById(id);
    }

    private void validateSmsTemplateExists(Long id) {
        if (smsTemplateMapper.selectById(id) == null) {
            throw exception(SMS_TEMPLATE_NOT_EXISTS);
        }
    }

    private SmsChannelDO validateSmsChannel(Long channelId) {
        SmsChannelDO channel = smsChannelService.getSmsChannel(channelId);
        if (channel == null) {
            throw exception(SMS_CHANNEL_NOT_EXISTS);
        }
        if (CommonStatusEnum.isDisable(channel.getStatus())) {
            throw exception(SMS_CHANNEL_DISABLE);
        }
        return channel;
    }

    private void validateSmsTemplateCodeUnique(Long id, String code) {
        SmsTemplateDO template = smsTemplateMapper.selectByCode(code);
        if (template != null && !Objects.equals(template.getId(), id)) {
            throw exception(SMS_TEMPLATE_CODE_DUPLICATE, code);
        }
    }

    static List<String> parseTemplateContentParams(String content) {
        return ReUtil.findAllGroup1(PARAM_PATTERN, content);
    }

    @Override
    public SmsTemplateDO getSmsTemplate(Long id) {
        return smsTemplateMapper.selectById(id);
    }

    @Override
    public SmsTemplateDO getSmsTemplateByCode(String code) {
        return smsTemplateMapper.selectByCode(code);
    }

    @Override
    public PageResult<SmsTemplateDO> getSmsTemplatePage(SmsTemplatePageReqVO pageReqVO) {
        return smsTemplateMapper.selectPage(pageReqVO);
    }

    @Override
    public String formatSmsTemplateContent(String content, Map<String, Object> params) {
        return StrUtil.format(content, params);
    }
}
