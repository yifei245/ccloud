package com.ccloud.module.pay.controller.open.refund;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.util.servlet.ServletUtils;
import com.ccloud.module.pay.api.refund.dto.PayRefundCreateReqDTO;
import com.ccloud.module.pay.controller.open.OpenPayAppResolver;
import com.ccloud.module.pay.controller.open.refund.vo.OpenPayRefundCreateReqVO;
import com.ccloud.module.pay.controller.open.refund.vo.OpenPayRefundRespVO;
import com.ccloud.module.pay.dal.dataobject.app.PayAppDO;
import com.ccloud.module.pay.dal.dataobject.refund.PayRefundDO;
import com.ccloud.module.pay.service.refund.PayRefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.framework.common.pojo.CommonResult.success;
import static com.ccloud.module.pay.enums.ErrorCodeConstants.PAY_REFUND_NOT_FOUND;

@Tag(name = "开放接口 - 退款单")
@RestController
@RequestMapping("/open-api/pay/refund")
@Validated
public class OpenPayRefundController {

    @Resource
    private PayRefundService refundService;
    @Resource
    private OpenPayAppResolver appResolver;

    @PostMapping("/create")
    @Operation(summary = "发起退款", description = "按商户退款单号幂等：重复发起返回原退款单，订单或金额不同报错；只能退本应用的订单")
    public CommonResult<OpenPayRefundRespVO> createRefund(@Valid @RequestBody OpenPayRefundCreateReqVO reqVO,
                                                          HttpServletRequest request) {
        PayAppDO app = appResolver.validApp();
        Long id = refundService.createRefund(app, new PayRefundCreateReqDTO().setAppKey(app.getAppKey())
                .setMerchantOrderId(reqVO.getMerchantOrderId()).setMerchantRefundId(reqVO.getMerchantRefundId())
                .setPrice(reqVO.getPrice()).setReason(reqVO.getReason()).setUserIp(ServletUtils.getClientIP(request)));
        return success(toResp(refundService.getRefund(id)));
    }

    @GetMapping("/get")
    @Operation(summary = "按商户退款单号查询退款单", description = "只能查询本应用的退款单")
    @Parameter(name = "merchantRefundId", description = "商户退款单号", required = true, example = "REFUND-20261012-0001")
    public CommonResult<OpenPayRefundRespVO> getRefund(@RequestParam("merchantRefundId") String merchantRefundId) {
        PayAppDO app = appResolver.validApp();
        PayRefundDO refund = refundService.getRefund(app.getId(), merchantRefundId);
        if (refund == null) {
            throw exception(PAY_REFUND_NOT_FOUND);
        }
        return success(toResp(refund));
    }

    private static OpenPayRefundRespVO toResp(PayRefundDO refund) {
        return new OpenPayRefundRespVO().setMerchantOrderId(refund.getMerchantOrderId())
                .setMerchantRefundId(refund.getMerchantRefundId()).setRefundNo(refund.getNo())
                .setRefundPrice(refund.getRefundPrice()).setStatus(refund.getStatus())
                .setSuccessTime(refund.getSuccessTime()).setChannelErrorMsg(refund.getChannelErrorMsg());
    }
}
