package com.ccloud.module.open.framework.notify;

import cn.hutool.core.util.StrUtil;
import com.ccloud.module.open.framework.config.OpenProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.net.InetAddressUtils;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 回调的 HTTP 发送。不跟随重定向（302 到内网地址可绕过地址校验）、不自动重试（重试由回调日志的退避策略负责）
 */
@Slf4j
@Component
public class OpenNotifyClient implements DisposableBean {

    static final int MAX_RESPONSE_LENGTH = 2048;

    private final boolean allowPrivate;
    private final CloseableHttpClient httpClient;
    /**
     * 单次发送的整体期限（连接 + 读取超时）。读取超时只限制两次读之间的间隔，对方逐字节缓慢输出时请求可无限延长，
     * 超过回调记录的抢占租约后会被其他执行方重复发送；到期由下面的调度线程取消请求
     */
    private final Duration deadline;
    private final ScheduledExecutorService deadlineScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "open-notify-deadline");
        thread.setDaemon(true);
        return thread;
    });

    @Autowired
    public OpenNotifyClient(OpenProperties openProperties) {
        this(openProperties.getNotify());
    }

    OpenNotifyClient(OpenProperties.Notify notify) {
        this.allowPrivate = notify.isAllowPrivateAddress();
        this.deadline = notify.getConnectTimeout().plus(notify.getReadTimeout());
        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDnsResolver(new RestrictedDnsResolver(allowPrivate))
                .setDefaultConnectionConfig(ConnectionConfig.custom()
                        .setConnectTimeout(Timeout.of(notify.getConnectTimeout()))
                        .setSocketTimeout(Timeout.of(notify.getReadTimeout()))
                        .build())
                .setMaxConnTotal(50)
                .setMaxConnPerRoute(5)
                .build();
        this.httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .disableRedirectHandling()
                .disableAutomaticRetries()
                .disableCookieManagement()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setResponseTimeout(Timeout.of(notify.getReadTimeout()))
                        .build())
                .build();
    }

    public NotifyResult post(String url, Map<String, String> headers, String body) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            return new NotifyResult(false, 0, "回调地址不合法");
        }
        if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())
                || uri.getHost() == null) {
            return new NotifyResult(false, 0, "回调地址须为 http 或 https");
        }
        // 字面量 IP 不一定经过 DNS 解析器，这里先校验一次
        String host = uri.getHost().replaceAll("^\\[|]$", "");
        if (InetAddressUtils.isIPv4Address(host) || InetAddressUtils.isIPv6Address(host)) {
            try {
                RestrictedDnsResolver.check(InetAddress.getByName(host), allowPrivate);
            } catch (UnknownHostException e) {
                return new NotifyResult(false, 0, e.getMessage());
            }
        }
        HttpPost post = new HttpPost(uri);
        headers.forEach(post::setHeader);
        post.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));
        ScheduledFuture<?> abort = deadlineScheduler.schedule(post::cancel, deadline.toMillis(), TimeUnit.MILLISECONDS);
        try {
            return httpClient.execute(post, response -> {
                int status = response.getCode();
                String text = response.getEntity() == null ? "" : readLimited(response.getEntity().getContent());
                return new NotifyResult(status >= 200 && status < 300, status, text);
            });
        } catch (UnknownHostException e) {
            // 受限地址由 DNS 解析器拒绝，HttpClient 原样抛出，消息即拒绝原因
            return new NotifyResult(false, 0, StrUtil.sub(e.getMessage(), 0, MAX_RESPONSE_LENGTH));
        } catch (IOException e) {
            String reason = post.isCancelled() ? "发送超时：超过 " + deadline.toSeconds() + " 秒未完成" : "发送失败：" + e.getMessage();
            log.info("[post] 回调发送失败 url={}：{}", url, e.toString());
            return new NotifyResult(false, 0, StrUtil.sub(reason, 0, MAX_RESPONSE_LENGTH));
        } finally {
            abort.cancel(false);
        }
    }

    private static String readLimited(InputStream in) throws IOException {
        byte[] bytes = in.readNBytes(MAX_RESPONSE_LENGTH * 4);
        return StrUtil.sub(new String(bytes, StandardCharsets.UTF_8), 0, MAX_RESPONSE_LENGTH);
    }

    @Override
    public void destroy() throws IOException {
        deadlineScheduler.shutdownNow();
        httpClient.close();
    }
}
