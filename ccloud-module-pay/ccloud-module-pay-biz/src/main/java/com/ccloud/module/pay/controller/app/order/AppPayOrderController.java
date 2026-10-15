package com.ccloud.module.pay.controller.app.order;

import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.framework.common.util.servlet.ServletUtils;
import com.ccloud.module.pay.controller.admin.order.vo.PayOrderSubmitRespVO;
import com.ccloud.module.pay.controller.app.order.vo.AppPayOrderRespVO;
import com.ccloud.module.pay.controller.app.order.vo.AppPayOrderSubmitReqVO;
import com.ccloud.module.pay.dal.dataobject.order.PayOrderDO;
import com.ccloud.module.pay.service.order.PayOrderService;
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

import java.util.Objects;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.framework.common.pojo.CommonResult.success;
import static com.ccloud.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static com.ccloud.module.pay.enums.ErrorCodeConstants.PAY_ORDER_NOT_FOUND;

@Tag(name = "用户 App - 支付单")
@RestController
@RequestMapping("/app-api/pay/order")
@Validated
public class AppPayOrderController {

    @Resource
    private PayOrderService orderService;

    @GetMapping("/get")
    @Operation(summary = "获得本人的支付单")
    @Parameter(name = "id", description = "支付单编号", required = true, example = "1024")
    public CommonResult<AppPayOrderRespVO> getOrder(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(validateOwnOrder(id), AppPayOrderRespVO.class));
    }

    @PostMapping("/submit")
    @Operation(summary = "提交支付", description = "选择渠道发起支付，返回二维码、跳转链接等展示信息；钱包支付直接返回结果")
    public CommonResult<PayOrderSubmitRespVO> submitOrder(@Valid @RequestBody AppPayOrderSubmitReqVO reqVO,
                                                          HttpServletRequest request) {
        validateOwnOrder(reqVO.getId());
        return success(orderService.submitOrder(reqVO.getId(), reqVO, ServletUtils.getClientIP(request)));
    }

    /**
     * 只能操作付款人是自己的订单；不存在、属于他人、没有付款会员（外部系统的订单）统一报"支付单不存在"，不泄露订单是否存在
     */
    private PayOrderDO validateOwnOrder(Long id) {
        PayOrderDO order = orderService.getOrder(id);
        if (order == null || !Objects.equals(order.getUserId(), getLoginUserId())
                || !UserTypeEnum.MEMBER.getValue().equals(order.getUserType())) {
            throw exception(PAY_ORDER_NOT_FOUND);
        }
        return order;
    }
}
