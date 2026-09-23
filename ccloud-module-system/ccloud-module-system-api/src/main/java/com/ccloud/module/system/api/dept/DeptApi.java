package com.ccloud.module.system.api.dept;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.module.system.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import com.ccloud.module.system.api.dept.dto.DeptRespDTO;

import java.util.Collection;
import java.util.List;

@FeignClient(name = ApiConstants.NAME, contextId = "systemDeptApi")
@Tag(name = "RPC 服务 - 部门")
public interface DeptApi {

    String PREFIX = ApiConstants.PREFIX + "/dept";

    @GetMapping(PREFIX + "/get")
    @Operation(summary = "获得部门信息")
    CommonResult<DeptRespDTO> getDept(@RequestParam("id") Long id);

    @GetMapping(PREFIX + "/list")
    @Operation(summary = "获得部门信息数组")
    CommonResult<List<DeptRespDTO>> getDeptList(@RequestParam("ids") Collection<Long> ids);

    @GetMapping(PREFIX + "/list-child")
    @Operation(summary = "获得指定部门的全部后代部门，不含自身")
    CommonResult<List<DeptRespDTO>> getChildDeptList(@RequestParam("id") Long id);

    @GetMapping(PREFIX + "/valid")
    @Operation(summary = "校验部门是否均存在且处于开启状态")
    CommonResult<Boolean> validateDeptList(@RequestParam("ids") Collection<Long> ids);
}
