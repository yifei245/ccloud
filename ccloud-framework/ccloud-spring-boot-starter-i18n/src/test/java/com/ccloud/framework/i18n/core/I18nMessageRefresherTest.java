package com.ccloud.framework.i18n.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class I18nMessageRefresherTest {

    private final I18nMessageFrameworkService messageService = mock(I18nMessageFrameworkService.class);
    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private DatabaseMessageSource messageSource;
    private I18nMessageRefresher refresher;

    @BeforeEach
    void setUp() {
        messageSource = new DatabaseMessageSource(new StaticMessageSource());
        refresher = new I18nMessageRefresher(messageSource, messageService, List.of("zh_CN", "en_US"), redisTemplate);
    }

    @Test
    void refresh_按支持的语言逐个加载数据库文案() {
        when(messageService.getMessages("zh_CN")).thenReturn(Map.of("hello", "你好"));
        when(messageService.getMessages("en_US")).thenReturn(Map.of("hello", "Hello"));

        refresher.refresh();

        assertThat(messageSource.getMessage("hello", null, Locale.SIMPLIFIED_CHINESE)).isEqualTo("你好");
        assertThat(messageSource.getMessage("hello", null, Locale.US)).isEqualTo("Hello");
    }

    @Test
    void refresh_某个语言加载失败时保留已有文案_不影响其他语言() {
        when(messageService.getMessages("zh_CN")).thenReturn(Map.of("hello", "你好"));
        when(messageService.getMessages("en_US")).thenReturn(Map.of("hello", "Hello"));
        refresher.refresh();

        when(messageService.getMessages("zh_CN")).thenThrow(new IllegalStateException("system 服务不可用"));
        when(messageService.getMessages("en_US")).thenReturn(Map.of("hello", "Hi"));
        refresher.refresh();

        assertThat(messageSource.getMessage("hello", null, Locale.SIMPLIFIED_CHINESE)).isEqualTo("你好");
        assertThat(messageSource.getMessage("hello", null, Locale.US)).isEqualTo("Hi");
    }

    @Test
    void publishRefresh_向刷新频道发消息_收到消息时重新加载() {
        refresher.publishRefresh();
        verify(redisTemplate).convertAndSend(eq(I18nMessageRefresher.CHANNEL), anyString());

        when(messageService.getMessages(anyString())).thenReturn(Map.of("hello", "新文案"));
        refresher.onMessage(new DefaultMessage(I18nMessageRefresher.CHANNEL.getBytes(), "refresh".getBytes()), null);

        assertThat(messageSource.getMessage("hello", null, Locale.SIMPLIFIED_CHINESE)).isEqualTo("新文案");
    }
}
