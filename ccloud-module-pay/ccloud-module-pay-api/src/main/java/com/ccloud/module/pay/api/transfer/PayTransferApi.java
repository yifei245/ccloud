package com.ccloud.module.pay.api.transfer;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.module.pay.api.transfer.dto.PayTransferCreateReqDTO;
import com.ccloud.module.pay.api.transfer.dto.PayTransferRespDTO;
import com.ccloud.module.pay.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = ApiConstants.NAME, contextId = "payTransferApi")
@Tag(name = "RPC 服务 - 转账单")
public interface PayTransferApi {

    String PREFIX = ApiConstants.PREFIX + "/transfer";

    @PostMapping(PREFIX + "/create")
    @Operation(summary = "创建转账单")
    CommonResult<Long> createTransfer(@Valid @RequestBody PayTransferCreateReqDTO reqDTO);

    @GetMapping(PREFIX + "/get")
    @Operation(summary = "获得转账单")
    CommonResult<PayTransferRespDTO> getTransfer(@RequestParam("id") Long id);
}
