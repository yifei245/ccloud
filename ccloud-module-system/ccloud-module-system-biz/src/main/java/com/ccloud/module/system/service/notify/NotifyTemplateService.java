package com.ccloud.module.system.service.notify;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.system.controller.admin.notify.vo.template.NotifyTemplatePageReqVO;
import com.ccloud.module.system.controller.admin.notify.vo.template.NotifyTemplateSaveReqVO;
import com.ccloud.module.system.dal.dataobject.notify.NotifyTemplateDO;
import jakarta.validation.Valid;

import java.util.Map;

/**
 * 站内信模板 Service
 */
public interface NotifyTemplateService {

    /**
     * 参数名由内容解析得出
     */
    Long createNotifyTemplate(@Valid NotifyTemplateSaveReqVO createReqVO);

    void updateNotifyTemplate(@Valid NotifyTemplateSaveReqVO updateReqVO);

    void deleteNotifyTemplate(Long id);

    NotifyTemplateDO getNotifyTemplate(Long id);

    /**
     * @return 未命中返回 null
     */
    NotifyTemplateDO getNotifyTemplateByCode(String code);

    PageResult<NotifyTemplateDO> getNotifyTemplatePage(NotifyTemplatePageReqVO pageReqVO);

    /**
     * 渲染内容：{参数名} 替换为参数值。内容按纯文本展示，前端不得以 HTML 渲染
     */
    String formatNotifyTemplateContent(String content, Map<String, Object> params);
}
