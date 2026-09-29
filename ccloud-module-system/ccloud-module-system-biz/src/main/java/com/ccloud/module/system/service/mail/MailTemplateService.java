package com.ccloud.module.system.service.mail;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.system.controller.admin.mail.vo.template.MailTemplatePageReqVO;
import com.ccloud.module.system.controller.admin.mail.vo.template.MailTemplateSaveReqVO;
import com.ccloud.module.system.dal.dataobject.mail.MailTemplateDO;
import jakarta.validation.Valid;

import java.util.Map;

/**
 * 邮件模板 Service
 */
public interface MailTemplateService {

    /**
     * 参数名由标题与内容解析得出，邮箱账号须存在
     */
    Long createMailTemplate(@Valid MailTemplateSaveReqVO createReqVO);

    void updateMailTemplate(@Valid MailTemplateSaveReqVO updateReqVO);

    void deleteMailTemplate(Long id);

    MailTemplateDO getMailTemplate(Long id);

    /**
     * @return 未命中返回 null
     */
    MailTemplateDO getMailTemplateByCode(String code);

    PageResult<MailTemplateDO> getMailTemplatePage(MailTemplatePageReqVO pageReqVO);

    /**
     * 渲染标题：纯文本，参数原样替换
     */
    String formatMailTemplateTitle(String title, Map<String, Object> params);

    /**
     * 渲染内容：内容是 HTML，参数值先做 HTML 转义。参数常来自用户输入（昵称、订单备注等），
     * 不转义就能借系统邮件向收件人投放任意链接
     */
    String formatMailTemplateContent(String content, Map<String, Object> params);
}
