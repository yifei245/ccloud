package com.ccloud.module.pay.controller.app.notify;

import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.exception.ServiceException;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.pay.dal.dataobject.channel.PayChannelDO;
import com.ccloud.module.pay.framework.pay.core.client.PayClient;
import com.ccloud.module.pay.framework.pay.core.client.PayNotifyVerifyException;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayNotifyRequest;
import com.ccloud.module.pay.service.channel.PayChannelService;
import com.ccloud.module.pay.service.order.PayOrderService;
import com.ccloud.module.pay.service.refund.PayRefundService;
import com.ccloud.module.pay.service.transfer.PayTransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "渠道回调")
@RestController
@RequestMapping("/app-api/pay/notify")
@Slf4j
public class PayNotifyController {

    @Resource
    private PayChannelService channelService;
    @Resource
    private PayOrderService orderService;
    @Resource
    private PayRefundService refundService;
    @Resource
    private PayTransferService transferService;

    /**
     * 免登录、免租户头：按渠道确定租户，按渠道实现验签。成功应答渠道要求的报文；
     * 验签失败或业务校验不通过返回 400（渠道会重试，无害；应答体不带细节，细节只记日志）；
     * 渠道配置不可用等服务端问题与其他未预期异常返回 500，等渠道重试（全局异常处理会把异常包成 HTTP 200，渠道会当作已送达）
     */
    @PostMapping("/{type}/{channelId}")
    @Operation(summary = "接收渠道回调")
    public ResponseEntity<String> notify(@PathVariable("type") String type, @PathVariable("channelId") Long channelId,
                                         @RequestParam Map<String, String> params,
                                         @RequestBody(required = false) String body, HttpServletRequest request) {
        PayChannelDO channel;
        try {
            channel = channelService.getChannelIgnoreTenant(channelId);
        } catch (Exception e) {
            // 数据库异常、配置解密失败等：服务端问题，返回 500 让渠道重试
            log.error("[notify] 渠道 {} 的 {} 回调查询渠道失败", channelId, type, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("error");
        }
        if (channel == null || !List.of("order", "refund", "transfer").contains(type)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("not found");
        }
        Map<String, String> headers = new HashMap<>();
        Collections.list(request.getHeaderNames()).forEach(name -> headers.put(name.toLowerCase(), request.getHeader(name)));
        PayNotifyRequest notifyRequest = new PayNotifyRequest(params, headers, StrUtil.nullToEmpty(body));
        PayClient client;
        try {
            client = TenantUtils.execute(channel.getTenantId(), () -> channelService.getPayClient(channel));
        } catch (Exception e) {
            // 渠道类型未实现或配置不正确：服务端问题，不是回调方的错
            log.error("[notify] 渠道 {} 无法创建客户端，{} 回调无法处理", channelId, type, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("error");
        }
        try {
            String ack = TenantUtils.execute(channel.getTenantId(), () -> handle(type, channel, client, notifyRequest));
            return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(ack);
        } catch (PayNotifyVerifyException | ServiceException e) {
            log.warn("[notify] 渠道 {} 的 {} 回调被拒绝：{}", channelId, type, e.getMessage());
            return ResponseEntity.badRequest().body("rejected");
        } catch (Exception e) {
            log.error("[notify] 渠道 {} 的 {} 回调处理异常", channelId, type, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("error");
        }
    }

    /**
     * type 已在入口限定为 order、refund、transfer；default 是防御：以后入口放行了新类型而这里漏加分支时，
     * 抛 IllegalStateException，由 notify 的 catch (Exception) 应答 500，而不是静默应答成功
     */
    private String handle(String type, PayChannelDO channel, PayClient client, PayNotifyRequest request) {
        switch (type) {
            case "order" -> orderService.notifyOrder(channel, client.parseOrderNotify(request));
            case "refund" -> refundService.notifyRefund(channel, client.parseRefundNotify(request));
            case "transfer" -> transferService.notifyTransfer(channel, client.parseTransferNotify(request));
            default -> throw new IllegalStateException("未支持的回调类型: " + type);
        }
        return client.notifyAckBody();
    }
}
