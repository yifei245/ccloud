package com.ccloud.module.open.framework.contract;

import com.ccloud.framework.i18n.core.I18nMessageFrameworkService;
import com.ccloud.module.system.api.i18n.I18nMessageApi;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 数据库文案经 RPC 由 system 提供，启动时、收到刷新通知时及定时拉取
 */
@Component
public class I18nMessageFrameworkServiceImpl implements I18nMessageFrameworkService {

    @Lazy
    @Resource
    private I18nMessageApi i18nMessageApi;

    @Override
    public Map<String, String> getMessages(String locale) {
        return i18nMessageApi.getMessages(locale).getCheckedData();
    }
}
