package com.ccloud.module.system.api.dept;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.module.system.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import com.ccloud.module.system.api.dept.dto.PostRespDTO;

import java.util.Collection;
import java.util.List;

@FeignClient(name = ApiConstants.NAME)
@Tag(name = "RPC 服务 - 岗位")
public interface PostApi {

    String PREFIX = ApiConstants.PREFIX + "/post";

    @GetMapping(PREFIX + "/list")
    @Operation(summary = "获得岗位列表")
    CommonResult<List<PostRespDTO>> getPostList(@RequestParam("ids") Collection<Long> ids);

    @GetMapping(PREFIX + "/valid")
    @Operation(summary = "校验岗位是否均存在且处于开启状态")
    CommonResult<Boolean> validPostList(@RequestParam("ids") Collection<Long> ids);
}
