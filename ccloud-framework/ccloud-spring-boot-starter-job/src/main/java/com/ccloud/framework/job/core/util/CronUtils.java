package com.ccloud.framework.job.core.util;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.support.CronExpression;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * cron 表达式工具。
 *
 * <p>用 Spring 的 {@link CronExpression} 而非 Quartz 的解析器：
 * 无需额外依赖，且与 Spring 调度的语义一致。注意它支持 6 位表达式
 * （秒 分 时 日 月 周），第 6 位可用 {@code ?} 或 {@code *}。
 */
@Slf4j
public class CronUtils {

    /**
     * 校验表达式。界面上保存任务时就该拦下非法表达式，
     * 而不是等到调度时才在日志里报错——那时任务已经在库里了。
     */
    public static boolean isValid(String cron) {
        if (StrUtil.isBlank(cron)) {
            return false;
        }
        return CronExpression.isValidExpression(cron);
    }

    /**
     * @return 下次执行时间；表达式非法或不会再执行时返回 null
     */
    public static LocalDateTime getNextTime(String cron, LocalDateTime from) {
        if (!isValid(cron)) {
            return null;
        }
        try {
            return CronExpression.parse(cron).next(from);
        } catch (Exception e) {
            log.warn("[getNextTime] 解析 cron 失败: {}", cron, e);
            return null;
        }
    }

    public static LocalDateTime getNextTime(String cron) {
        return getNextTime(cron, LocalDateTime.now());
    }

    /**
     * 连续推算多个执行时间，供界面预览调度计划
     */
    public static List<LocalDateTime> getNextTimes(String cron, LocalDateTime from, int count) {
        List<LocalDateTime> result = new ArrayList<>(count);
        if (!isValid(cron)) {
            return result;
        }
        CronExpression expression = CronExpression.parse(cron);
        LocalDateTime current = from;
        for (int i = 0; i < count; i++) {
            current = expression.next(current);
            if (current == null) {
                break;
            }
            result.add(current);
        }
        return result;
    }
}
