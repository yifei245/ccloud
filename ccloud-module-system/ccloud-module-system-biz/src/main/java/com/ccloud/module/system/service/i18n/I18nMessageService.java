package com.ccloud.module.system.service.i18n;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.system.controller.admin.i18n.vo.I18nMessagePageReqVO;
import com.ccloud.module.system.controller.admin.i18n.vo.I18nMessageSaveReqVO;
import com.ccloud.module.system.dal.dataobject.i18n.I18nMessageDO;
import jakarta.validation.Valid;

import java.util.Map;

/**
 * 国际化文案 Service。增删改后清除缓存并通知全部服务刷新
 */
public interface I18nMessageService {

    /**
     * 语言须在支持列表内，同一语言下编码唯一
     */
    Long createI18nMessage(@Valid I18nMessageSaveReqVO createReqVO);

    void updateI18nMessage(@Valid I18nMessageSaveReqVO updateReqVO);

    void deleteI18nMessage(Long id);

    I18nMessageDO getI18nMessage(Long id);

    PageResult<I18nMessageDO> getI18nMessagePage(I18nMessagePageReqVO pageReqVO);

    /**
     * @return 指定语言的全部文案，key 为编码；带缓存
     */
    Map<String, String> getMessages(String locale);
}
