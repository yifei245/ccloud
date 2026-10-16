package com.ccloud.module.pay.api.refund;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.module.pay.api.refund.dto.PayRefundCreateReqDTO;
import com.ccloud.module.pay.api.refund.dto.PayRefundRespDTO;
import com.ccloud.module.pay.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = ApiConstants.NAME, contextId = "payRefundApi")
@Tag(name = "RPC 服务 - 退款单")
public interface PayRefundApi {

    String PREFIX = ApiConstants.PREFIX + "/refund";

    @PostMapping(PREFIX + "/create")
    @Operation(summary = "创建退款单")
    CommonResult<Long> createRefund(@Valid @RequestBody PayRefundCreateReqDTO reqDTO);

    @GetMapping(PREFIX + "/get")
    @Operation(summary = "获得退款单")
    CommonResult<PayRefundRespDTO> getRefund(@RequestParam("id") Long id);
}
