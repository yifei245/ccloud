package com.ccloud.module.pay.framework.pay.core.client.impl.mock;

import cn.hutool.core.util.IdUtil;
import com.ccloud.module.pay.framework.pay.core.client.PayNotifyVerifyException;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayClientStatus;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayDisplayMode;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayNotifyRequest;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayOrderResult;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayOrderUnifiedReq;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayRefundUnifiedReq;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayTransferUnifiedReq;
import com.ccloud.module.pay.test.BasePayDbAndRedisUnitTest;
import com.sun.net.httpserver.HttpServer;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MockPayClientTest extends BasePayDbAndRedisUnitTest {

    private static final String SECRET = "0123456789abcdef";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private HttpServer server;
    private final BlockingQueue<Map<String, String>> received = new LinkedBlockingQueue<>();
    private MockPayNotifySender sender;
    private String notifyUrl;

    @BeforeEach
    void startReceiver() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/notify", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            received.add(Map.of("body", body,
                    "ts", exchange.getRequestHeaders().getFirst("X-Mock-Timestamp"),
                    "sign", exchange.getRequestHeaders().getFirst("X-Mock-Sign")));
            exchange.sendResponseHeaders(200, 0);
            exchange.close();
        });
        server.start();
        notifyUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/notify";
        sender = new MockPayNotifySender();
    }

    @AfterEach
    void stop() {
        server.stop(0);
        sender.destroy();
    }

    private MockPayClient client(MockPayBehavior behavior) {
        MockPayClientConfig config = new MockPayClientConfig().setNotifySecret(SECRET).setBehavior(behavior).setDelaySeconds(1);
        return new MockPayClient(1L, config, stringRedisTemplate, sender);
    }

    private PayOrderUnifiedReq order(String no) {
        return new PayOrderUnifiedReq().setOutTradeNo(no).setSubject("测试").setPrice(100).setNotifyUrl(notifyUrl);
    }

    private static String uniqueNo(String prefix) {
        return prefix + IdUtil.fastSimpleUUID().substring(0, 12);
    }

    private static PayNotifyRequest asRequest(Map<String, String> got) {
        return new PayNotifyRequest(Map.of(), Map.of("x-mock-timestamp", got.get("ts"), "x-mock-sign", got.get("sign")), got.get("body"));
    }

    @Test
    void 下单返回确认页地址_确认前查单为等待() {
        String no = uniqueNo("P");
        PayOrderResult result = client(MockPayBehavior.SUCCESS).unifiedOrder(order(no));
        assertThat(result.getStatus()).isEqualTo(PayClientStatus.WAITING);
        assertThat(result.getDisplayMode()).isEqualTo(PayDisplayMode.URL);
        assertThat(result.getDisplayContent()).isEqualTo("/app-api/pay/mock/confirm?no=" + no);
        assertThat(client(MockPayBehavior.SUCCESS).getOrder(no).getStatus()).isEqualTo(PayClientStatus.WAITING);
    }

    @Test
    void 确认后回调成功结果且签名可验证_再次确认不重复回调() throws Exception {
        String no = uniqueNo("P");
        MockPayClient client = client(MockPayBehavior.SUCCESS);
        client.unifiedOrder(order(no));
        client.confirm(no);
        client.confirm(no);

        Map<String, String> got = received.poll(5, TimeUnit.SECONDS);
        assertThat(got).isNotNull();
        PayOrderResult parsed = client.parseOrderNotify(asRequest(got));
        assertThat(parsed.getStatus()).isEqualTo(PayClientStatus.SUCCESS);
        assertThat(parsed.getOutTradeNo()).isEqualTo(no);
        assertThat(parsed.getPrice()).isEqualTo(100);
        assertThat(client.getOrder(no).getStatus()).isEqualTo(PayClientStatus.SUCCESS);
        assertThat(received.poll(1500, TimeUnit.MILLISECONDS)).as("重复确认不应再次回调").isNull();
    }

    @Test
    void 丢弃回调时不回调_但查单为成功() throws Exception {
        String no = uniqueNo("P");
        MockPayClient client = client(MockPayBehavior.DROP_NOTIFY);
        client.unifiedOrder(order(no));
        client.confirm(no);
        assertThat(received.poll(1500, TimeUnit.MILLISECONDS)).isNull();
        assertThat(client.getOrder(no).getStatus()).isEqualTo(PayClientStatus.SUCCESS);
    }

    @Test
    void 支付失败时回调失败结果() throws Exception {
        String no = uniqueNo("P");
        MockPayClient client = client(MockPayBehavior.FAIL);
        client.unifiedOrder(order(no));
        client.confirm(no);
        Map<String, String> got = received.poll(5, TimeUnit.SECONDS);
        assertThat(got).isNotNull();
        assertThat(got.get("body")).contains("\"status\":\"FAILURE\"");
        assertThat(client.getOrder(no).getStatus()).isEqualTo(PayClientStatus.FAILURE);
    }

    @Test
    void 延迟回调在配置的秒数之后到达() throws Exception {
        String no = uniqueNo("P");
        MockPayClient client = client(MockPayBehavior.DELAY);
        client.unifiedOrder(order(no));
        long start = System.currentTimeMillis();
        client.confirm(no);
        assertThat(received.poll(5, TimeUnit.SECONDS)).isNotNull();
        assertThat(System.currentTimeMillis() - start).isGreaterThanOrEqualTo(900);
    }

    @Test
    void 退款与转账发起后异步回调_查询结果一致_重复发起不重复回调() throws Exception {
        MockPayClient client = client(MockPayBehavior.SUCCESS);
        String refundNo = uniqueNo("R");
        PayRefundUnifiedReq refundReq = new PayRefundUnifiedReq().setOutTradeNo("P1").setOutRefundNo(refundNo)
                .setRefundPrice(30).setPayPrice(100).setNotifyUrl(notifyUrl);
        assertThat(client.unifiedRefund(refundReq).getStatus()).isEqualTo(PayClientStatus.WAITING);
        Map<String, String> got = received.poll(5, TimeUnit.SECONDS);
        assertThat(got.get("body")).contains("\"type\":\"refund\"").contains(refundNo);
        assertThat(client.parseRefundNotify(asRequest(got)).getRefundPrice()).isEqualTo(30);
        assertThat(client.getRefund("P1", refundNo).getStatus()).isEqualTo(PayClientStatus.SUCCESS);
        client.unifiedRefund(refundReq);
        assertThat(received.poll(1500, TimeUnit.MILLISECONDS)).as("重复发起同一退款不再回调").isNull();

        String transferNo = uniqueNo("T");
        client.unifiedTransfer(new PayTransferUnifiedReq().setOutTransferNo(transferNo).setPrice(20).setUserAccount("a").setNotifyUrl(notifyUrl));
        assertThat(received.poll(5, TimeUnit.SECONDS).get("body")).contains("\"type\":\"transfer\"");
        assertThat(client.getTransfer(transferNo).getStatus()).isEqualTo(PayClientStatus.SUCCESS);
    }

    @Test
    void 回调类型不符时拒绝() throws Exception {
        MockPayClient client = client(MockPayBehavior.SUCCESS);
        String refundNo = uniqueNo("R");
        client.unifiedRefund(new PayRefundUnifiedReq().setOutTradeNo("P1").setOutRefundNo(refundNo).setRefundPrice(1).setNotifyUrl(notifyUrl));
        Map<String, String> got = received.poll(5, TimeUnit.SECONDS);
        assertThatThrownBy(() -> client.parseOrderNotify(asRequest(got))).isInstanceOf(PayNotifyVerifyException.class);
    }

    @Test
    void 并发确认同一支付只回调一次() throws Exception {
        String no = uniqueNo("P");
        MockPayClient client = client(MockPayBehavior.SUCCESS);
        client.unifiedOrder(order(no));
        runConcurrently(8, () -> client.confirm(no));
        assertThat(received.poll(5, TimeUnit.SECONDS)).isNotNull();
        assertThat(received.poll(1500, TimeUnit.MILLISECONDS)).as("并发确认只应回调一次").isNull();
    }

    @Test
    void 并发发起同一退款与转账各只回调一次() throws Exception {
        MockPayClient client = client(MockPayBehavior.SUCCESS);
        PayRefundUnifiedReq refundReq = new PayRefundUnifiedReq().setOutTradeNo("P1").setOutRefundNo(uniqueNo("R"))
                .setRefundPrice(30).setPayPrice(100).setNotifyUrl(notifyUrl);
        runConcurrently(8, () -> client.unifiedRefund(refundReq));
        assertThat(received.poll(5, TimeUnit.SECONDS).get("body")).contains("\"type\":\"refund\"");
        assertThat(received.poll(1500, TimeUnit.MILLISECONDS)).as("并发退款只应回调一次").isNull();

        PayTransferUnifiedReq transferReq = new PayTransferUnifiedReq().setOutTransferNo(uniqueNo("T"))
                .setPrice(20).setUserAccount("a").setNotifyUrl(notifyUrl);
        runConcurrently(8, () -> client.unifiedTransfer(transferReq));
        assertThat(received.poll(5, TimeUnit.SECONDS).get("body")).contains("\"type\":\"transfer\"");
        assertThat(received.poll(1500, TimeUnit.MILLISECONDS)).as("并发转账只应回调一次").isNull();
    }

    private static void runConcurrently(int threads, Runnable task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            CyclicBarrier barrier = new CyclicBarrier(threads);
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    barrier.await();
                    task.run();
                    return null;
                }));
            }
            for (Future<?> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void 确认不存在的支付返回false() {
        assertThat(client(MockPayBehavior.SUCCESS).confirm("P-not-exists")).isFalse();
    }
}
