package com.ccloud.module.pay.api.refund;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.pay.api.refund.dto.PayRefundCreateReqDTO;
import com.ccloud.module.pay.api.refund.dto.PayRefundRespDTO;
import com.ccloud.module.pay.service.refund.PayRefundService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import static com.ccloud.framework.common.pojo.CommonResult.success;

@RestController
@Validated
public class PayRefundApiImpl implements PayRefundApi {

    @Resource
    private PayRefundService refundService;

    @Override
    public CommonResult<Long> createRefund(PayRefundCreateReqDTO reqDTO) {
        return success(refundService.createRefund(reqDTO));
    }

    @Override
    public CommonResult<PayRefundRespDTO> getRefund(Long id) {
        return success(BeanUtils.toBean(refundService.getRefund(id), PayRefundRespDTO.class));
    }
}
