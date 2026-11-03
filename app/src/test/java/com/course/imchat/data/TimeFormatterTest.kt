package com.course.imchat.data

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.TimeZone
import kotlin.random.Random

/**
 * [TimeFormatter] 用纯整数运算替代了 java.time + String.format，
 * 这里拿 java.time 当参照实现做等价性验证 —— 包括夏令时切换那一天。
 */
class TimeFormatterTest {

    private val originalZone: TimeZone = TimeZone.getDefault()

    @After
    fun restoreZone() {
        TimeZone.setDefault(originalZone)
    }

    private fun oracle(timestampSeconds: Long): Pair<String, String> {
        val dt = LocalDateTime.ofInstant(
            Instant.ofEpochSecond(timestampSeconds),
            ZoneId.systemDefault(),
        )
        val date = "%04d-%02d-%02d".format(dt.year, dt.monthValue, dt.dayOfMonth)
        val time = "%02d:%02d".format(dt.hour, dt.minute)
        return date to time
    }

    @Test
    fun `与 java time 输出一致`() {
        // 1970-01-01 ~ 2100-01-01 随机采样
        val random = Random(20261103)
        repeat(2_000) {
            val ts = random.nextLong(0, 4_102_444_800L)
            val (date, time) = oracle(ts)
            assertEquals("date@$ts", date, TimeFormatter.ymd(ts))
            assertEquals("time@$ts", time, TimeFormatter.hhmm(ts))
        }
    }

    @Test
    fun `固定样本`() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"))

        assertEquals("1970-01-01", TimeFormatter.ymd(0))
        assertEquals("08:00", TimeFormatter.hhmm(0))

        for (ts in longArrayOf(1_793_000_000L, 1_700_000_000L, 946_684_800L)) {
            val (date, time) = oracle(ts)
            assertEquals("date@$ts", date, TimeFormatter.ymd(ts))
            assertEquals("time@$ts", time, TimeFormatter.hhmm(ts))
        }
    }

    @Test
    fun `夏令时切换当天不会错一小时`() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"))

        // 切换发生在 2026-03-29 02:00 CET -> 03:00 CEST
        val beforeSwitch = LocalDateTime.of(2026, 3, 29, 1, 30)
            .atZone(ZoneId.of("Europe/Berlin")).toEpochSecond()
        val afterSwitch = LocalDateTime.of(2026, 3, 29, 3, 30)
            .atZone(ZoneId.of("Europe/Berlin")).toEpochSecond()

        assertEquals("01:30", TimeFormatter.hhmm(beforeSwitch))
        assertEquals("03:30", TimeFormatter.hhmm(afterSwitch))

        // 同一天，只是偏移从 +1 变成 +2
        assertEquals(TimeFormatter.epochDay(beforeSwitch), TimeFormatter.epochDay(afterSwitch))
        assertEquals("2026-03-29", TimeFormatter.ymd(beforeSwitch))
        assertEquals("2026-03-29", TimeFormatter.ymd(afterSwitch))
    }

    @Test
    fun `切换时区后缓存会失效`() {
        val ts = 1_793_000_000L

        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"))
        val shanghai = TimeFormatter.hhmm(ts)
        val shanghaiExpected = oracle(ts).second

        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        val utc = TimeFormatter.hhmm(ts)
        val utcExpected = oracle(ts).second

        assertEquals(shanghaiExpected, shanghai)
        assertEquals(utcExpected, utc)
        assertNotEquals(shanghai, utc)
    }

    @Test
    fun `跨天边界`() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"))

        // 2026-11-03 23:59:59 与 2026-11-04 00:00:00（东八区）
        val beforeMidnight = LocalDateTime.of(2026, 11, 3, 23, 59, 59)
            .atZone(ZoneId.of("Asia/Shanghai")).toEpochSecond()
        val afterMidnight = beforeMidnight + 1

        assertEquals("2026-11-03", TimeFormatter.ymd(beforeMidnight))
        assertEquals("2026-11-04", TimeFormatter.ymd(afterMidnight))
        assertEquals("23:59", TimeFormatter.hhmm(beforeMidnight))
        assertEquals("00:00", TimeFormatter.hhmm(afterMidnight))
        assertNotEquals(
            TimeFormatter.epochDay(beforeMidnight),
            TimeFormatter.epochDay(afterMidnight),
        )
    }
}
