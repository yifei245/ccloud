package com.ccloud.module.pay.controller.admin.order;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.pojo.PageParam;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.pay.controller.admin.order.vo.PayOrderDetailRespVO;
import com.ccloud.module.pay.controller.admin.order.vo.PayOrderExtensionRespVO;
import com.ccloud.module.pay.controller.admin.order.vo.PayOrderPageReqVO;
import com.ccloud.module.pay.controller.admin.order.vo.PayOrderRespVO;
import com.ccloud.module.pay.dal.dataobject.order.PayOrderDO;
import com.ccloud.module.pay.service.order.PayOrderService;
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
import static com.ccloud.module.pay.enums.ErrorCodeConstants.PAY_ORDER_NOT_FOUND;

@Tag(name = "管理后台 - 支付单")
@RestController
@RequestMapping("/admin-api/pay/order")
@Validated
public class PayOrderController {

    @Resource
    private PayOrderService orderService;

    @GetMapping("/page")
    @Operation(summary = "获得支付单分页")
    @PreAuthorize("@ss.hasPermission('pay:order:query')")
    public CommonResult<PageResult<PayOrderRespVO>> getOrderPage(@Valid PayOrderPageReqVO pageReqVO) {
        return success(BeanUtils.toBean(orderService.getOrderPage(pageReqVO), PayOrderRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获得支付单详情", description = "含全部支付尝试及渠道回调原始报文，供排查")
    @Parameter(name = "id", description = "支付单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pay:order:query')")
    public CommonResult<PayOrderDetailRespVO> getOrder(@RequestParam("id") Long id) {
        PayOrderDO order = orderService.getOrder(id);
        if (order == null) {
            throw exception(PAY_ORDER_NOT_FOUND);
        }
        PayOrderDetailRespVO detail = BeanUtils.toBean(order, PayOrderDetailRespVO.class);
        detail.setExtensions(BeanUtils.toBean(orderService.getOrderExtensionListByOrderId(id), PayOrderExtensionRespVO.class));
        return success(detail);
    }

    @GetMapping("/abnormal-page")
    @Operation(summary = "获得异常支付分页", description = "支付尝试已成功，但不是订单记录的成功尝试（重复支付，或订单关闭后才付款成功），需人工处理")
    @PreAuthorize("@ss.hasPermission('pay:order:query')")
    public CommonResult<PageResult<PayOrderExtensionRespVO>> getAbnormalPage(@Valid PageParam pageParam) {
        return success(BeanUtils.toBean(orderService.getAbnormalExtensionPage(pageParam), PayOrderExtensionRespVO.class));
    }
}
