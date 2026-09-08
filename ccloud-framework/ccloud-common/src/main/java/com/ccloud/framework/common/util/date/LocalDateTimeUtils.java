package com.ccloud.framework.common.util.date;

import cn.hutool.core.date.LocalDateTimeUtil;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * LocalDateTime 工具。项目内部统一使用 LocalDateTime，
 * 仅在与第三方 SDK 交互时才转 Date。
 */
public class LocalDateTimeUtils {

    /**
     * 空时间。用于"永不过期"这类语义，避免用 null 引发空指针
     */
    public static final LocalDateTime EMPTY = buildTime(1970, 1, 1);

    public static final DateTimeFormatter FORMATTER_NORMAL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static LocalDateTime addTime(Duration duration) {
        return LocalDateTime.now().plus(duration);
    }

    public static LocalDateTime minusTime(Duration duration) {
        return LocalDateTime.now().minus(duration);
    }

    public static boolean beforeNow(LocalDateTime date) {
        return date != null && date.isBefore(LocalDateTime.now());
    }

    public static boolean afterNow(LocalDateTime date) {
        return date != null && date.isAfter(LocalDateTime.now());
    }

    public static LocalDateTime buildTime(int year, int month, int day) {
        return LocalDateTime.of(year, month, day, 0, 0, 0);
    }

    /**
     * 构造 [当天 00:00:00, 当天 23:59:59] 区间，用于按天查询
     */
    public static LocalDateTime[] buildBetweenTime(int year1, int month1, int day1,
                                                   int year2, int month2, int day2) {
        return new LocalDateTime[]{buildTime(year1, month1, day1),
                LocalDateTime.of(year2, month2, day2, 23, 59, 59)};
    }

    public static boolean isBetween(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(startTime) && !now.isAfter(endTime);
    }

    public static LocalDateTime getDayStart(LocalDateTime dateTime) {
        return dateTime.with(LocalTime.MIN);
    }

    public static LocalDateTime getDayEnd(LocalDateTime dateTime) {
        return dateTime.with(LocalTime.MAX);
    }

    public static LocalDateTime ofDate(Date date) {
        return date == null ? null : LocalDateTimeUtil.of(date);
    }

    public static Date toDate(LocalDateTime dateTime) {
        return dateTime == null ? null : Date.from(dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant());
    }

    public static String format(LocalDateTime dateTime) {
        return dateTime == null ? null : FORMATTER_NORMAL.format(dateTime);
    }

    public static LocalDateTime parse(String text) {
        return cn.hutool.core.util.StrUtil.isBlank(text) ? null : LocalDateTime.parse(text, FORMATTER_NORMAL);
    }

    /**
     * 计算两个日期相差的天数（按自然日）
     */
    public static long betweenDay(LocalDateTime start, LocalDateTime end) {
        return java.time.temporal.ChronoUnit.DAYS.between(start.toLocalDate(), end.toLocalDate());
    }

    public static LocalDate toLocalDate(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toLocalDate();
    }
}
