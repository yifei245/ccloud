package com.ccloud.module.system.service.i18n;

import com.ccloud.framework.i18n.core.I18nMessageFrameworkService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * system 服务自身直接查库加载文案；其他服务经 I18nMessageApi 获取
 */
@Service
public class I18nMessageFrameworkServiceImpl implements I18nMessageFrameworkService {

    @Resource
    private I18nMessageService i18nMessageService;

    @Override
    public Map<String, String> getMessages(String locale) {
        return i18nMessageService.getMessages(locale);
    }
}
