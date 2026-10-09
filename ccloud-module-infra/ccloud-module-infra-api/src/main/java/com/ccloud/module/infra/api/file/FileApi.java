package com.ccloud.module.infra.api.file;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.module.infra.api.file.dto.FileCreateReqDTO;
import com.ccloud.module.infra.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = ApiConstants.NAME, contextId = "infraFileApi")
@Tag(name = "RPC 服务 - 文件")
public interface FileApi {

    String PREFIX = ApiConstants.PREFIX + "/file";

    @PostMapping(PREFIX + "/create")
    @Operation(summary = "保存文件", description = "按主配置存储，返回访问地址；类型与大小的限制与上传接口相同")
    CommonResult<String> createFile(@Valid @RequestBody FileCreateReqDTO reqDTO);
}
