package com.ccloud.module.pay.api.order;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.module.pay.api.order.dto.PayOrderCreateReqDTO;
import com.ccloud.module.pay.api.order.dto.PayOrderRespDTO;
import com.ccloud.module.pay.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = ApiConstants.NAME, contextId = "payOrderApi")
@Tag(name = "RPC 服务 - 支付单")
public interface PayOrderApi {

    String PREFIX = ApiConstants.PREFIX + "/order";

    @PostMapping(PREFIX + "/create")
    @Operation(summary = "创建支付单")
    CommonResult<Long> createOrder(@Valid @RequestBody PayOrderCreateReqDTO reqDTO);

    @GetMapping(PREFIX + "/get")
    @Operation(summary = "获得支付单")
    CommonResult<PayOrderRespDTO> getOrder(@RequestParam("id") Long id);
}
