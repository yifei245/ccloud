package com.ccloud.module.system.framework.sms.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * 短信验证码配置
 */
@ConfigurationProperties(prefix = "ccloud.sms-code")
@Validated
@Data
public class SmsCodeProperties {

    /**
     * 有效期
     */
    @NotNull
    private Duration expireTimes = Duration.ofMinutes(10);

    /**
     * 同一手机号同一场景的最小发送间隔
     */
    @NotNull
    private Duration sendFrequency = Duration.ofMinutes(1);

    /**
     * 同一手机号同一场景每日最多发送条数
     */
    @NotNull
    @Min(1)
    private Integer sendMaximumQuantityPerDay = 10;

    /**
     * 验证码取值范围，含两端。本地联调可设为同一个值得到固定验证码
     */
    @NotNull
    @Min(0)
    private Integer beginCode = 100000;

    @NotNull
    @Min(0)
    private Integer endCode = 999999;

    /**
     * 同一条验证码允许输错的次数，达到后须重新获取，防止在有效期内穷举
     */
    @NotNull
    @Min(1)
    private Integer maxValidateFailTimes = 5;
}
