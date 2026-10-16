package com.ccloud.module.pay.support;

import com.ccloud.framework.common.util.json.JsonUtils;
import com.ccloud.module.pay.framework.pay.core.client.PayClient;
import com.ccloud.module.pay.framework.pay.core.client.PayNotifyVerifyException;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayDisplayMode;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayNotifyRequest;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayOrderResult;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayOrderUnifiedReq;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayRefundResult;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayRefundUnifiedReq;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayTransferResult;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayTransferUnifiedReq;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * 测试用渠道客户端：各方法的返回值由测试预先设置，未设置时返回等待。字段均为 public，便于测试直接改
 */
public class FakePayClient implements PayClient {

    public volatile Long channelId;
    public volatile Function<PayOrderUnifiedReq, PayOrderResult> onUnifiedOrder;
    public volatile Function<String, PayOrderResult> onGetOrder;
    public volatile Function<PayRefundUnifiedReq, PayRefundResult> onUnifiedRefund;
    public volatile Function<String, PayRefundResult> onGetRefund;
    public volatile Function<PayTransferUnifiedReq, PayTransferResult> onUnifiedTransfer;
    public volatile Function<String, PayTransferResult> onGetTransfer;
    /** 非空时每次调用都抛出它，模拟渠道超时等异常 */
    public volatile RuntimeException throwOnCall;
    public volatile PayOrderUnifiedReq lastOrderReq;
    public volatile PayRefundUnifiedReq lastRefundReq;
    public volatile PayTransferUnifiedReq lastTransferReq;
    public final AtomicInteger unifiedOrderCalls = new AtomicInteger();
    public final AtomicInteger unifiedTransferCalls = new AtomicInteger();

    public void reset() {
        onUnifiedOrder = null; onGetOrder = null; onUnifiedRefund = null; onGetRefund = null;
        onUnifiedTransfer = null; onGetTransfer = null; throwOnCall = null;
        lastOrderReq = null; lastRefundReq = null; lastTransferReq = null;
        unifiedOrderCalls.set(0); unifiedTransferCalls.set(0);
    }

    private void maybeThrow() {
        if (throwOnCall != null) throw throwOnCall;
    }

    @Override public Long getChannelId() { return channelId; }

    @Override
    public PayOrderResult unifiedOrder(PayOrderUnifiedReq req) {
        lastOrderReq = req; unifiedOrderCalls.incrementAndGet(); maybeThrow();
        return onUnifiedOrder != null ? onUnifiedOrder.apply(req)
                : PayOrderResult.waiting(req.getOutTradeNo(), PayDisplayMode.URL, "https://fake.pay/" + req.getOutTradeNo());
    }

    @Override
    public PayOrderResult getOrder(String outTradeNo) {
        maybeThrow();
        return onGetOrder != null ? onGetOrder.apply(outTradeNo) : PayOrderResult.waiting(outTradeNo, PayDisplayMode.NONE, null);
    }

    /** 回调报文即 PayOrderResult 的 JSON；请求头 x-fake-sign 为 bad 时验签失败 */
    @Override
    public PayOrderResult parseOrderNotify(PayNotifyRequest request) {
        if ("bad".equals(request.getHeaders().get("x-fake-sign"))) throw new PayNotifyVerifyException("验签失败");
        return JsonUtils.parseObject(request.getBody(), PayOrderResult.class);
    }

    @Override
    public PayRefundResult unifiedRefund(PayRefundUnifiedReq req) {
        lastRefundReq = req; maybeThrow();
        return onUnifiedRefund != null ? onUnifiedRefund.apply(req) : PayRefundResult.waiting(req.getOutRefundNo());
    }

    @Override
    public PayRefundResult getRefund(String outTradeNo, String outRefundNo) {
        maybeThrow();
        return onGetRefund != null ? onGetRefund.apply(outRefundNo) : PayRefundResult.waiting(outRefundNo);
    }

    @Override
    public PayRefundResult parseRefundNotify(PayNotifyRequest request) {
        if ("bad".equals(request.getHeaders().get("x-fake-sign"))) throw new PayNotifyVerifyException("验签失败");
        return JsonUtils.parseObject(request.getBody(), PayRefundResult.class);
    }

    @Override
    public PayTransferResult unifiedTransfer(PayTransferUnifiedReq req) {
        lastTransferReq = req; unifiedTransferCalls.incrementAndGet(); maybeThrow();
        return onUnifiedTransfer != null ? onUnifiedTransfer.apply(req) : PayTransferResult.waiting(req.getOutTransferNo());
    }

    @Override
    public PayTransferResult getTransfer(String outTransferNo) {
        maybeThrow();
        return onGetTransfer != null ? onGetTransfer.apply(outTransferNo) : PayTransferResult.waiting(outTransferNo);
    }

    @Override
    public PayTransferResult parseTransferNotify(PayNotifyRequest request) {
        if ("bad".equals(request.getHeaders().get("x-fake-sign"))) throw new PayNotifyVerifyException("验签失败");
        return JsonUtils.parseObject(request.getBody(), PayTransferResult.class);
    }

}
