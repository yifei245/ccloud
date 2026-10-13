package com.ccloud.module.pay.framework.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "ccloud.pay")
@Component
@Validated
@Data
public class PayProperties {

    /**
     * 模拟支付开关。只在 local、dev 的 profile 中打开；关闭时不能新建或启用 Mock 渠道，确认页返回 404
     */
    private boolean mockEnabled = false;

    /**
     * 渠道回调地址前缀（网关地址），回调地址为 {notifyBaseUrl}/app-api/pay/notify/{order|refund|transfer}/{channelId}
     */
    @NotBlank
    private String notifyBaseUrl = "http://127.0.0.1:28080";

    @Valid
    private Wallet wallet = new Wallet();
    @Valid
    private Job job = new Job();

    @Data
    public static class Wallet {
        /**
         * 钱包充值所用支付应用的标识，各租户自建；默认与演示数据一致
         */
        @NotBlank
        private String rechargeAppKey = "wallet";
        /**
         * 充值支付单的有效期
         */
        @NotNull
        private Duration rechargeExpire = Duration.ofHours(2);
    }

    @Data
    public static class Job {
        /**
         * 定时任务每次处理的条数上限
         */
        @Min(1)
        private int batchSize = 100;
        /**
         * 单据创建多久后仍在等待才主动查询
         */
        @NotNull
        private Duration syncDelay = Duration.ofMinutes(1);
        /**
         * 只补查这段时间内创建的支付尝试，更早的交给超时关单
         */
        @NotNull
        private Duration syncWindow = Duration.ofDays(1);
    }
}
