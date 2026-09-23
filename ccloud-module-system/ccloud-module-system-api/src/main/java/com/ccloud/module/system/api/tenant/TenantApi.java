package com.ccloud.module.system.api.tenant;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.module.system.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 租户 RPC。多租户框架的 TenantFrameworkService 经由它回查
 */
@FeignClient(name = ApiConstants.NAME, contextId = "systemTenantApi")
@Tag(name = "RPC 服务 - 租户")
public interface TenantApi {

    String PREFIX = ApiConstants.PREFIX + "/tenant";

    @GetMapping(PREFIX + "/id-list")
    @Operation(summary = "获得全部开启且未过期的租户编号，定时任务按租户遍历时使用")
    CommonResult<List<Long>> getTenantIdList();

    /**
     * 租户不存在、已禁用、已过期时分别返回对应错误码，调用方据错误码区分提示
     */
    @GetMapping(PREFIX + "/valid")
    @Operation(summary = "校验租户是否可用")
    CommonResult<Boolean> validTenant(@RequestParam("id") Long id);
}
