package com.ccloud.module.system.service.sms;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.system.controller.admin.sms.vo.template.SmsTemplatePageReqVO;
import com.ccloud.module.system.controller.admin.sms.vo.template.SmsTemplateSaveReqVO;
import com.ccloud.module.system.dal.dataobject.sms.SmsTemplateDO;
import jakarta.validation.Valid;

import java.util.Map;

/**
 * 短信模板 Service
 */
public interface SmsTemplateService {

    /**
     * 参数名由模板内容解析得出，渠道须存在且开启
     */
    Long createSmsTemplate(@Valid SmsTemplateSaveReqVO createReqVO);

    void updateSmsTemplate(@Valid SmsTemplateSaveReqVO updateReqVO);

    void deleteSmsTemplate(Long id);

    SmsTemplateDO getSmsTemplate(Long id);

    /**
     * @return 未命中返回 null
     */
    SmsTemplateDO getSmsTemplateByCode(String code);

    PageResult<SmsTemplateDO> getSmsTemplatePage(SmsTemplatePageReqVO pageReqVO);

    /**
     * 用参数渲染模板内容：{参数名} 替换为参数值
     */
    String formatSmsTemplateContent(String content, Map<String, Object> params);
}
