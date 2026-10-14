package com.ccloud.module.pay.controller.app.mock;

import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HtmlUtil;
import com.ccloud.framework.common.util.number.MoneyUtils;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.pay.dal.dataobject.channel.PayChannelDO;
import com.ccloud.module.pay.dal.dataobject.order.PayOrderDO;
import com.ccloud.module.pay.dal.dataobject.order.PayOrderExtensionDO;
import com.ccloud.module.pay.framework.pay.core.client.PayClient;
import com.ccloud.module.pay.framework.pay.core.client.impl.mock.MockPayClient;
import com.ccloud.module.pay.service.channel.PayChannelService;
import com.ccloud.module.pay.service.order.PayOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Mock 支付确认页")
@RestController
@RequestMapping("/app-api/pay/mock/confirm")
@ConditionalOnProperty(prefix = "ccloud.pay", name = "mock-enabled", havingValue = "true")
public class PayMockConfirmController {

    @Resource
    private PayOrderService orderService;
    @Resource
    private PayChannelService channelService;

    @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "模拟支付确认页", description = "仅开启模拟支付的环境可用。展示商品与金额，点击按钮后以 POST 确认")
    public ResponseEntity<String> page(@RequestParam("no") String no) {
        PayOrderExtensionDO ext = orderService.getOrderExtensionByNoIgnoreTenant(no);
        if (ext == null) {
            return notFound();
        }
        PayOrderDO order = TenantUtils.execute(ext.getTenantId(), () -> orderService.getOrder(ext.getOrderId()));
        String html = "<!doctype html><html><head><meta charset=\"utf-8\"><title>模拟支付</title></head><body>"
                + "<h3>模拟支付（仅用于本地调试）</h3>"
                + "<p>商品：" + HtmlUtil.escape(order.getSubject()) + "</p>"
                + "<p>金额：" + MoneyUtils.fenToYuanStr(order.getPrice()) + " 元</p>"
                + "<form method=\"post\" action=\"/app-api/pay/mock/confirm?no=" + URLUtil.encodeQuery(no) + "\">"
                + "<button type=\"submit\">确认支付</button></form></body></html>";
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }

    @PostMapping(produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "确认模拟支付", description = "按 Mock 渠道配置的行为落定结果并回调")
    public ResponseEntity<String> confirm(@RequestParam("no") String no) {
        PayOrderExtensionDO ext = orderService.getOrderExtensionByNoIgnoreTenant(no);
        if (ext == null) {
            return notFound();
        }
        boolean confirmed = TenantUtils.execute(ext.getTenantId(), () -> {
            PayChannelDO channel = channelService.getChannel(ext.getChannelId());
            PayClient client = channel == null ? null : channelService.getPayClient(channel);
            return client instanceof MockPayClient mock && mock.confirm(no);
        });
        return confirmed ? ResponseEntity.ok().contentType(MediaType.TEXT_HTML)
                .body("<!doctype html><meta charset=\"utf-8\"><p>已确认支付，可以关闭本页。</p>") : notFound();
    }

    private static ResponseEntity<String> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.TEXT_HTML)
                .body("<!doctype html><meta charset=\"utf-8\"><p>支付不存在或已过期</p>");
    }
}
