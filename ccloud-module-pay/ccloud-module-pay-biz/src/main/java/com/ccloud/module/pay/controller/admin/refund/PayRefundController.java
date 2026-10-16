package com.ccloud.module.pay.controller.admin.refund;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.pay.controller.admin.refund.vo.PayRefundPageReqVO;
import com.ccloud.module.pay.controller.admin.refund.vo.PayRefundRespVO;
import com.ccloud.module.pay.dal.dataobject.refund.PayRefundDO;
import com.ccloud.module.pay.service.refund.PayRefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.framework.common.pojo.CommonResult.success;
import static com.ccloud.module.pay.enums.ErrorCodeConstants.PAY_REFUND_NOT_FOUND;

@Tag(name = "管理后台 - 退款单")
@RestController
@RequestMapping("/admin-api/pay/refund")
@Validated
public class PayRefundController {

    @Resource
    private PayRefundService refundService;

    @GetMapping("/page")
    @Operation(summary = "获得退款单分页")
    @PreAuthorize("@ss.hasPermission('pay:refund:query')")
    public CommonResult<PageResult<PayRefundRespVO>> getRefundPage(@Valid PayRefundPageReqVO pageReqVO) {
        return success(BeanUtils.toBean(refundService.getRefundPage(pageReqVO), PayRefundRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获得退款单详情")
    @Parameter(name = "id", description = "退款单编号", required = true, example = "2048")
    @PreAuthorize("@ss.hasPermission('pay:refund:query')")
    public CommonResult<PayRefundRespVO> getRefund(@RequestParam("id") Long id) {
        PayRefundDO refund = refundService.getRefund(id);
        if (refund == null) {
            throw exception(PAY_REFUND_NOT_FOUND);
        }
        return success(BeanUtils.toBean(refund, PayRefundRespVO.class));
    }
}
