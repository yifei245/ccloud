package com.ccloud.framework.i18n.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Map;

/**
 * 把数据库文案加载进 {@link DatabaseMessageSource}：服务启动时加载，文案变更时经 Redis 频道通知全部服务重新加载，
 * 另有定时兜底（见自动配置），防止通知丢失或启动时 system 尚不可用。
 *
 * <p>某个语言加载失败时保留原有文案，不清空——宁可旧一点，也不能让界面退回到 code
 */
@Slf4j
public class I18nMessageRefresher implements MessageListener {

    public static final String CHANNEL = "ccloud:i18n:refresh";

    private final DatabaseMessageSource messageSource;
    private final I18nMessageFrameworkService messageService;
    private final List<String> locales;
    private final StringRedisTemplate redisTemplate;

    public I18nMessageRefresher(DatabaseMessageSource messageSource, I18nMessageFrameworkService messageService,
                                List<String> locales, StringRedisTemplate redisTemplate) {
        this.messageSource = messageSource;
        this.messageService = messageService;
        this.locales = locales;
        this.redisTemplate = redisTemplate;
    }

    public void refresh() {
        for (String locale : locales) {
            try {
                Map<String, String> messages = messageService.getMessages(locale);
                if (messages != null) {
                    messageSource.refresh(locale, messages);
                }
            } catch (RuntimeException e) {
                log.warn("[refresh] 加载语言 {} 的文案失败，保留原有文案", locale, e);
            }
        }
    }

    /**
     * 通知全部服务（含本服务）重新加载文案
     */
    public void publishRefresh() {
        redisTemplate.convertAndSend(CHANNEL, String.valueOf(System.currentTimeMillis()));
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        refresh();
    }
}
