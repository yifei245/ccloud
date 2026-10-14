package com.ccloud.module.pay.framework.pay.core.client.impl.mock;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.StrUtil;
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
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 模拟渠道：单据状态存 Redis（多实例查单一致），按配置的行为模拟成功、失败、丢回调、延迟回调。
 * 支付须用户访问确认页（confirm）；退款与转账发起即按行为处理
 */
public class MockPayClient implements PayClient {

    static final String CONFIRM_PATH = "/app-api/pay/mock/confirm?no=";
    private static final String KEY_FORMAT = "pay:mock:%s:%s";
    private static final Duration TTL = Duration.ofDays(1);

    private static final String WAITING = "WAITING";
    private static final String SUCCESS = "SUCCESS";
    private static final String FAILURE = "FAILURE";

    private final Long channelId;
    private final MockPayClientConfig config;
    private final StringRedisTemplate redisTemplate;
    private final MockPayNotifySender sender;

    public MockPayClient(Long channelId, MockPayClientConfig config, StringRedisTemplate redisTemplate,
                         MockPayNotifySender sender) {
        this.channelId = channelId;
        this.config = config;
        this.redisTemplate = redisTemplate;
        this.sender = sender;
    }

    @Override
    public Long getChannelId() {
        return channelId;
    }

    @Override
    public PayOrderResult unifiedOrder(PayOrderUnifiedReq req) {
        save(new MockPayState().setType("order").setNo(req.getOutTradeNo()).setPrice(req.getPrice())
                .setNotifyUrl(req.getNotifyUrl()).setStatus(WAITING));
        return PayOrderResult.waiting(req.getOutTradeNo(), PayDisplayMode.URL, CONFIRM_PATH + req.getOutTradeNo());
    }

    /**
     * 用户在确认页点击支付
     *
     * @return 单据不存在（如已过期）返回 false
     */
    public boolean confirm(String outTradeNo) {
        MockPayState state = load("order", outTradeNo);
        if (state == null) {
            return false;
        }
        if (WAITING.equals(state.getStatus())) {
            finish(state);
        }
        return true;
    }

    @Override
    public PayOrderResult getOrder(String outTradeNo) {
        MockPayState state = load("order", outTradeNo);
        if (state == null || WAITING.equals(state.getStatus())) {
            return PayOrderResult.waiting(outTradeNo, PayDisplayMode.URL, CONFIRM_PATH + outTradeNo);
        }
        return toOrderResult(state, JsonUtils.toJsonString(state));
    }

    @Override
    public PayOrderResult parseOrderNotify(PayNotifyRequest request) {
        return toOrderResult(verifyAndParse(request, "order"), request.getBody());
    }

    /**
     * 按退款单号幂等：已发起过时返回当前结果，不重复回调（补查任务会对等待中的退款重发）
     */
    @Override
    public PayRefundResult unifiedRefund(PayRefundUnifiedReq req) {
        MockPayState existing = load("refund", req.getOutRefundNo());
        if (existing != null) {
            return WAITING.equals(existing.getStatus()) ? PayRefundResult.waiting(req.getOutRefundNo())
                    : toRefundResult(existing, JsonUtils.toJsonString(existing));
        }
        MockPayState state = new MockPayState().setType("refund").setNo(req.getOutRefundNo()).setOutTradeNo(req.getOutTradeNo())
                .setPrice(req.getRefundPrice()).setNotifyUrl(req.getNotifyUrl()).setStatus(WAITING);
        save(state);
        finish(state);
        return PayRefundResult.waiting(req.getOutRefundNo());
    }

    @Override
    public PayRefundResult getRefund(String outTradeNo, String outRefundNo) {
        MockPayState state = load("refund", outRefundNo);
        if (state == null || WAITING.equals(state.getStatus())) {
            return PayRefundResult.waiting(outRefundNo);
        }
        return toRefundResult(state, JsonUtils.toJsonString(state));
    }

    @Override
    public PayRefundResult parseRefundNotify(PayNotifyRequest request) {
        return toRefundResult(verifyAndParse(request, "refund"), request.getBody());
    }

    /**
     * 按转账单号幂等，同退款
     */
    @Override
    public PayTransferResult unifiedTransfer(PayTransferUnifiedReq req) {
        MockPayState existing = load("transfer", req.getOutTransferNo());
        if (existing != null) {
            return WAITING.equals(existing.getStatus()) ? PayTransferResult.waiting(req.getOutTransferNo())
                    : toTransferResult(existing, JsonUtils.toJsonString(existing));
        }
        MockPayState state = new MockPayState().setType("transfer").setNo(req.getOutTransferNo())
                .setPrice(req.getPrice()).setNotifyUrl(req.getNotifyUrl()).setStatus(WAITING);
        save(state);
        finish(state);
        return PayTransferResult.waiting(req.getOutTransferNo());
    }

    @Override
    public PayTransferResult getTransfer(String outTransferNo) {
        MockPayState state = load("transfer", outTransferNo);
        if (state == null || WAITING.equals(state.getStatus())) {
            return PayTransferResult.waiting(outTransferNo);
        }
        return toTransferResult(state, JsonUtils.toJsonString(state));
    }

    @Override
    public PayTransferResult parseTransferNotify(PayNotifyRequest request) {
        return toTransferResult(verifyAndParse(request, "transfer"), request.getBody());
    }

    /**
     * 按行为落定结果并决定是否、何时回调
     */
    private void finish(MockPayState state) {
        boolean fail = config.getBehavior() == MockPayBehavior.FAIL;
        state.setStatus(fail ? FAILURE : SUCCESS).setSuccessTime(fail ? null : System.currentTimeMillis());
        save(state);
        if (config.getBehavior() == MockPayBehavior.DROP_NOTIFY || StrUtil.isBlank(state.getNotifyUrl())) {
            return;
        }
        long delay = config.getBehavior() == MockPayBehavior.DELAY ? config.getDelaySeconds() : 0;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", state.getType());
        body.put("no", state.getNo());
        body.put("status", state.getStatus());
        body.put("channelNo", "MOCK-" + state.getNo());
        body.put("price", state.getPrice());
        body.put("successTime", state.getSuccessTime());
        sender.send(state.getNotifyUrl(), config.getNotifySecret(), JsonUtils.toJsonString(body), delay);
    }

    private MockPayState verifyAndParse(PayNotifyRequest request, String expectedType) {
        MockPayNotifySigner.verify(config.getNotifySecret(), request.getHeaders(), request.getBody(), System.currentTimeMillis());
        Map<String, Object> body;
        try {
            body = JsonUtils.parseObject(request.getBody(), new TypeReference<Map<String, Object>>() {});
        } catch (RuntimeException e) {
            throw new PayNotifyVerifyException("报文不是合法的 JSON");
        }
        if (body == null || !expectedType.equals(body.get("type"))) {
            throw new PayNotifyVerifyException("回调类型不符");
        }
        try {
            return new MockPayState().setType(expectedType).setNo(String.valueOf(body.get("no")))
                    .setStatus(String.valueOf(body.get("status")))
                    .setPrice(body.get("price") == null ? null : Integer.valueOf(String.valueOf(body.get("price"))))
                    .setSuccessTime(body.get("successTime") == null ? null : Long.valueOf(String.valueOf(body.get("successTime"))));
        } catch (NumberFormatException e) {
            throw new PayNotifyVerifyException("报文字段格式不正确");
        }
    }

    private PayOrderResult toOrderResult(MockPayState state, String raw) {
        if (SUCCESS.equals(state.getStatus())) {
            return PayOrderResult.success(state.getNo(), "MOCK-" + state.getNo(), state.getPrice(), toTime(state.getSuccessTime()), raw);
        }
        return PayOrderResult.failure(state.getNo(), "MOCK_FAIL", "模拟支付失败", raw);
    }

    private PayRefundResult toRefundResult(MockPayState state, String raw) {
        if (SUCCESS.equals(state.getStatus())) {
            return PayRefundResult.success(state.getNo(), "MOCK-" + state.getNo(), state.getPrice(), toTime(state.getSuccessTime()), raw);
        }
        return PayRefundResult.failure(state.getNo(), "MOCK_FAIL", "模拟退款失败", raw);
    }

    private PayTransferResult toTransferResult(MockPayState state, String raw) {
        if (SUCCESS.equals(state.getStatus())) {
            return PayTransferResult.success(state.getNo(), "MOCK-" + state.getNo(), state.getPrice(), toTime(state.getSuccessTime()), raw);
        }
        return PayTransferResult.failure(state.getNo(), "MOCK_FAIL", "模拟转账失败", raw);
    }

    private static LocalDateTime toTime(Long millis) {
        return millis == null ? null : LocalDateTimeUtil.of(millis);
    }

    private void save(MockPayState state) {
        redisTemplate.opsForValue().set(String.format(KEY_FORMAT, state.getType(), state.getNo()), JsonUtils.toJsonString(state), TTL);
    }

    private MockPayState load(String type, String no) {
        String json = redisTemplate.opsForValue().get(String.format(KEY_FORMAT, type, no));
        return json == null ? null : JsonUtils.parseObject(json, MockPayState.class);
    }
}
