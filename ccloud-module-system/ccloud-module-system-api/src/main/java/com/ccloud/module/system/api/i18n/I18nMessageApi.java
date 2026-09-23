package com.ccloud.module.system.api.i18n;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.module.system.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 国际化文案 RPC。各服务的 DatabaseMessageSource 经由它加载数据库中的文案覆盖
 */
@FeignClient(name = ApiConstants.NAME, contextId = "systemI18nMessageApi")
@Tag(name = "RPC 服务 - 国际化文案")
public interface I18nMessageApi {

    String PREFIX = ApiConstants.PREFIX + "/i18n-message";

    @GetMapping(PREFIX + "/list")
    @Operation(summary = "获得指定语言的全部文案，key 为文案编码")
    CommonResult<Map<String, String>> getMessages(@RequestParam("locale") String locale);
}
