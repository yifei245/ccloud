package com.ccloud.module.pay.controller.open.order;

import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.framework.common.util.servlet.ServletUtils;
import com.ccloud.module.pay.api.order.dto.PayOrderCreateReqDTO;
import com.ccloud.module.pay.controller.admin.order.vo.PayOrderSubmitRespVO;
import com.ccloud.module.pay.controller.open.OpenPayAppResolver;
import com.ccloud.module.pay.controller.open.order.vo.OpenPayOrderCreateReqVO;
import com.ccloud.module.pay.controller.open.order.vo.OpenPayOrderRespVO;
import com.ccloud.module.pay.controller.open.order.vo.OpenPayOrderSubmitReqVO;
import com.ccloud.module.pay.dal.dataobject.app.PayAppDO;
import com.ccloud.module.pay.dal.dataobject.order.PayOrderDO;
import com.ccloud.module.pay.enums.PayChannelEnum;
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

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.framework.common.pojo.CommonResult.success;
import static com.ccloud.module.pay.enums.ErrorCodeConstants.PAY_ORDER_NOT_FOUND;
import static com.ccloud.module.pay.enums.ErrorCodeConstants.PAY_ORDER_WALLET_REQUIRE_MEMBER;

/**
 * 调用身份为网关验签后下发的开放应用，登录编号即开放应用编号，租户为应用所属租户；
 * 框架只在 /open-api 下接受开放应用身份，管理员、会员身份与匿名访问一律 401
 */
@Tag(name = "开放接口 - 支付单")
@RestController
@RequestMapping("/open-api/pay/order")
@Validated
public class OpenPayOrderController {

    @Resource
    private PayOrderService orderService;
    @Resource
    private OpenPayAppResolver appResolver;

    @PostMapping("/create")
    @Operation(summary = "创建支付单", description = "按商户订单号幂等：重复创建返回原单，金额不同报错。过期时间为毫秒时间戳")
    public CommonResult<OpenPayOrderRespVO> createOrder(@Valid @RequestBody OpenPayOrderCreateReqVO reqVO,
                                                        HttpServletRequest request) {
        PayAppDO app = appResolver.validApp();
        Long id = orderService.createOrder(app, new PayOrderCreateReqDTO().setAppKey(app.getAppKey())
                .setMerchantOrderId(reqVO.getMerchantOrderId()).setSubject(reqVO.getSubject()).setBody(reqVO.getBody())
                .setPrice(reqVO.getPrice()).setExpireTime(reqVO.getExpireTime()).setUserIp(ServletUtils.getClientIP(request)));
        return success(toResp(orderService.getOrder(id)));
    }

    @GetMapping("/get")
    @Operation(summary = "按商户订单号查询支付单", description = "只能查询本应用的订单")
    @Parameter(name = "merchantOrderId", description = "商户订单号", required = true, example = "ORDER-20261012-0001")
    public CommonResult<OpenPayOrderRespVO> getOrder(@RequestParam("merchantOrderId") String merchantOrderId) {
        return success(toResp(validateAppOrder(merchantOrderId)));
    }

    @PostMapping("/submit")
    @Operation(summary = "提交支付", description = "外部系统的订单没有付款会员，不能选择钱包")
    public CommonResult<PayOrderSubmitRespVO> submitOrder(@Valid @RequestBody OpenPayOrderSubmitReqVO reqVO,
                                                          HttpServletRequest request) {
        // 先于一切业务校验：开放接口永远不能走钱包，不依赖订单或渠道是否存在。
        // 渠道编码在库里按不区分大小写的排序规则匹配，所以这里同样忽略大小写与首尾空白
        if (PayChannelEnum.WALLET.getCode().equalsIgnoreCase(StrUtil.trim(reqVO.getChannelCode()))) {
            throw exception(PAY_ORDER_WALLET_REQUIRE_MEMBER);
        }
        PayOrderDO order = validateAppOrder(reqVO.getMerchantOrderId());
        return success(orderService.submitOrder(order.getId(), reqVO, ServletUtils.getClientIP(request)));
    }

    private PayOrderDO validateAppOrder(String merchantOrderId) {
        PayAppDO app = appResolver.validApp();
        PayOrderDO order = orderService.getOrder(app.getId(), merchantOrderId);
        if (order == null) {
            throw exception(PAY_ORDER_NOT_FOUND);
        }
        return order;
    }

    private static OpenPayOrderRespVO toResp(PayOrderDO order) {
        return BeanUtils.toBean(order, OpenPayOrderRespVO.class);
    }
}
