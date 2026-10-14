package com.ccloud.module.pay.api.order;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.pay.api.order.dto.PayOrderCreateReqDTO;
import com.ccloud.module.pay.api.order.dto.PayOrderRespDTO;
import com.ccloud.module.pay.service.order.PayOrderService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import static com.ccloud.framework.common.pojo.CommonResult.success;

@RestController
@Validated
public class PayOrderApiImpl implements PayOrderApi {

    @Resource
    private PayOrderService orderService;

    @Override
    public CommonResult<Long> createOrder(PayOrderCreateReqDTO reqDTO) {
        return success(orderService.createOrder(reqDTO));
    }

    @Override
    public CommonResult<PayOrderRespDTO> getOrder(Long id) {
        return success(BeanUtils.toBean(orderService.getOrder(id), PayOrderRespDTO.class));
    }
}
