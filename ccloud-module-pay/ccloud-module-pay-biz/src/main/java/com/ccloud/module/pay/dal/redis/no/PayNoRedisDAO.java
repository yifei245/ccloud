package com.ccloud.module.pay.dal.redis.no;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 外部单号生成：前缀 + 秒级时间 + 当秒自增序号。单号须全局唯一（各表 uk_no 不分租户），
 * 因此直接用 StringRedisTemplate，不经按租户加前缀的缓存
 */
@Repository
public class PayNoRedisDAO {

    public static final String ORDER_PREFIX = "P";
    public static final String REFUND_PREFIX = "R";
    public static final String TRANSFER_PREFIX = "T";
    public static final String WALLET_TRANSACTION_PREFIX = "W";
    public static final String WALLET_ADJUST_PREFIX = "A";

    private static final String KEY_FORMAT = "pay:no:%s";
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public String generate(String prefix) {
        String time = LocalDateTime.now().format(TIME);
        String key = String.format(KEY_FORMAT, prefix + time);
        Long seq = stringRedisTemplate.opsForValue().increment(key);
        if (seq != null && seq == 1L) {
            stringRedisTemplate.expire(key, Duration.ofMinutes(1));
        }
        return prefix + time + String.format("%06d", seq);
    }

}
