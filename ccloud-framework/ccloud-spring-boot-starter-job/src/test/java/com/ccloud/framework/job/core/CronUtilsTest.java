package com.ccloud.framework.job.core;

import com.ccloud.framework.job.core.util.CronUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CronUtilsTest {

    @Test
    @DisplayName("合法的 cron 表达式应通过校验")
    void validCron_shouldPass() {
        assertThat(CronUtils.isValid("0 0 1 * * ?")).isTrue();
        assertThat(CronUtils.isValid("0 */5 * * * ?")).isTrue();
        assertThat(CronUtils.isValid("0 0 0 1 1 ?")).isTrue();
    }

    @Test
    @DisplayName("非法的 cron 表达式应被拒绝，而不是等到调度时才炸")
    void invalidCron_shouldFail() {
        assertThat(CronUtils.isValid("not a cron")).isFalse();
        assertThat(CronUtils.isValid("0 0 1 *")).isFalse();
        assertThat(CronUtils.isValid("")).isFalse();
        assertThat(CronUtils.isValid(null)).isFalse();
    }

    @Test
    @DisplayName("每天凌晨 1 点的表达式，下次执行时间应是最近的那个 1 点")
    void getNextTime_shouldReturnNextDailyRun() {
        LocalDateTime from = LocalDateTime.of(2026, 9, 12, 10, 30);
        LocalDateTime next = CronUtils.getNextTime("0 0 1 * * ?", from);

        assertThat(next).isEqualTo(LocalDateTime.of(2026, 9, 13, 1, 0));
    }

    @Test
    @DisplayName("当天尚未到达执行点时，下次执行应在当天")
    void getNextTime_shouldReturnSameDayWhenNotYetPassed() {
        LocalDateTime from = LocalDateTime.of(2026, 9, 12, 0, 30);
        assertThat(CronUtils.getNextTime("0 0 1 * * ?", from))
                .isEqualTo(LocalDateTime.of(2026, 9, 12, 1, 0));
    }

    @Test
    @DisplayName("应能连续推算出多个执行时间，供界面预览调度计划")
    void getNextTimes_shouldReturnConsecutiveRuns() {
        LocalDateTime from = LocalDateTime.of(2026, 9, 12, 10, 0);
        List<LocalDateTime> times = CronUtils.getNextTimes("0 0 */6 * * ?", from, 3);

        assertThat(times).containsExactly(
                LocalDateTime.of(2026, 9, 12, 12, 0),
                LocalDateTime.of(2026, 9, 12, 18, 0),
                LocalDateTime.of(2026, 9, 13, 0, 0));
    }

    @Test
    @DisplayName("非法表达式取下次执行时间应返回 null 而非抛异常")
    void getNextTime_shouldReturnNullForInvalidCron() {
        assertThat(CronUtils.getNextTime("garbage", LocalDateTime.now())).isNull();
    }
}
