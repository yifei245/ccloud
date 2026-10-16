package com.ccloud.module.pay.api.transfer;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.pay.api.transfer.dto.PayTransferCreateReqDTO;
import com.ccloud.module.pay.api.transfer.dto.PayTransferRespDTO;
import com.ccloud.module.pay.service.transfer.PayTransferService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import static com.ccloud.framework.common.pojo.CommonResult.success;

@RestController
@Validated
public class PayTransferApiImpl implements PayTransferApi {

    @Resource
    private PayTransferService transferService;

    @Override
    public CommonResult<Long> createTransfer(PayTransferCreateReqDTO reqDTO) {
        return success(transferService.createTransfer(reqDTO));
    }

    @Override
    public CommonResult<PayTransferRespDTO> getTransfer(Long id) {
        return success(BeanUtils.toBean(transferService.getTransfer(id), PayTransferRespDTO.class));
    }
}
