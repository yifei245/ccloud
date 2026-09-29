package com.ccloud.module.system.api.i18n;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.module.system.service.i18n.I18nMessageService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static com.ccloud.framework.common.pojo.CommonResult.success;

/**
 * 国际化文案 RPC 实现：各服务启动与收到刷新通知时拉取文案
 */
@RestController
@Validated
public class I18nMessageApiImpl implements I18nMessageApi {

    @Resource
    private I18nMessageService i18nMessageService;

    @Override
    public CommonResult<Map<String, String>> getMessages(String locale) {
        return success(i18nMessageService.getMessages(locale));
    }
}
