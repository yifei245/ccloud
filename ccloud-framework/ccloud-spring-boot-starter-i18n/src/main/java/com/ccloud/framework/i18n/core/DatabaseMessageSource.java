package com.ccloud.framework.i18n.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.support.AbstractMessageSource;
import org.springframework.lang.Nullable;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 支持数据库覆盖的文案源。
 *
 * <p>查找顺序：数据库 → properties 基线 → code 本身。
 *
 * <p>之所以要数据库这一层：运营改一句提示语不该走一次发版。properties 里放的是
 * 兜底文案，保证数据库不可用或条目缺失时界面不会出现空白。
 *
 * <p>全都找不到时返回 code 而非抛异常——文案缺失是体验问题，不该升级成故障。
 */
@Slf4j
public class DatabaseMessageSource extends AbstractMessageSource {

    /**
     * locale -> (code -> 文案)
     */
    private final Map<String, Map<String, String>> messages = new ConcurrentHashMap<>();

    /**
     * properties 基线
     */
    private final MessageSource baseline;

    public DatabaseMessageSource(MessageSource baseline) {
        this.baseline = baseline;
        setUseCodeAsDefaultMessage(true);
    }

    /**
     * 整体替换某个语言的文案。用替换而非合并，否则后台删掉的条目会一直残留。
     */
    public void refresh(String locale, Map<String, String> localeMessages) {
        messages.put(locale, Map.copyOf(localeMessages));
        log.info("[refresh] 刷新语言 {} 的文案 {} 条", locale, localeMessages.size());
    }

    public void clear() {
        messages.clear();
    }

    @Nullable
    @Override
    protected MessageFormat resolveCode(String code, Locale locale) {
        String message = resolveFromDatabase(code, locale);
        return message != null ? new MessageFormat(message, locale) : null;
    }

    @Nullable
    @Override
    protected String resolveCodeWithoutArguments(String code, Locale locale) {
        return resolveFromDatabase(code, locale);
    }

    @Nullable
    private String resolveFromDatabase(String code, Locale locale) {
        Map<String, String> localeMessages = messages.get(toKey(locale));
        return localeMessages != null ? localeMessages.get(code) : null;
    }

    private static String toKey(Locale locale) {
        return locale.getLanguage() + "_" + locale.getCountry();
    }

    /**
     * 三级查找链：数据库 → properties 基线 → code
     */
    @Nullable
    @Override
    protected String getMessageInternal(@Nullable String code, @Nullable Object[] args, @Nullable Locale locale) {
        if (code == null) {
            return null;
        }
        Locale target = locale != null ? locale : Locale.getDefault();
        String dbMessage = resolveFromDatabase(code, target);
        if (dbMessage != null) {
            return args == null || args.length == 0 ? dbMessage
                    : new MessageFormat(dbMessage, target).format(args);
        }
        try {
            String baselineMessage = baseline.getMessage(code, args, target);
            return baselineMessage != null ? baselineMessage : code;
        } catch (Exception e) {
            return code;
        }
    }
}
