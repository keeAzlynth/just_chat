package com.course.imchat.data

import java.time.Instant
import java.time.ZoneId

/**
 * 消息时间格式化。
 *
 * 原实现每次调用都构造 `Instant` + `LocalDateTime`，再走 `String.format`：
 * 单次约 1~3 µs，而且每次都分配一堆临时对象。这条路径在消息列表里是
 * 「每个气泡每帧一次」，属于绝对热点。
 *
 * 这里换成纯整数运算：
 *  - 时区偏移按「天」缓存，跨天或跨时区才会重新查一次；
 *  - 年月日用 Howard Hinnant 的 civil-from-days 算法，只做除法和减法；
 *  - 结果直接写进定长 CharArray，全程只有最后那一个 String 分配。
 *
 * 等价性由 `TimeFormatterTest` 用 java.time 作为参照实现做随机比对。
 */
object TimeFormatter {

    private const val SECONDS_PER_DAY = 86_400L
    private const val SECONDS_PER_HOUR = 3_600L
    private const val SECONDS_PER_MINUTE = 60L

    @Volatile
    private var cachedDay = Long.MIN_VALUE

    /** 这一天内部偏移是否恒定（没有夏令时切换）；恒定才能真正缓存 */
    @Volatile
    private var cachedDayOffset = 0

    @Volatile
    private var cachedZone: ZoneId? = null

    /**
     * 取某个时间戳对应的时区偏移。
     *
     * 思路：按「天」缓存，但先确认这一天内偏移没有变化（大多数日子成立）。
     * 如果这一天正好是夏令时切换日，缓存失效，退化成精确查询 —— 一年只有两天会走这条路。
     * 这样既有整数比较的速度，也不会在 DST 边界上算错一小时。
     */
    private fun offsetSecondsAt(timestampSeconds: Long): Int {
        val zone = ZoneId.systemDefault()
        val day = Math.floorDiv(timestampSeconds, SECONDS_PER_DAY)

        if (day == cachedDay && zone == cachedZone) return cachedDayOffset

        val rules = zone.rules
        val dayStart = rules.getOffset(Instant.ofEpochSecond(day * SECONDS_PER_DAY)).totalSeconds
        val nextDayStart = rules.getOffset(Instant.ofEpochSecond((day + 1) * SECONDS_PER_DAY)).totalSeconds

        return if (dayStart == nextDayStart) {
            cachedDay = day
            cachedDayOffset = dayStart
            cachedZone = zone
            dayStart
        } else {
            // 切换日：不走缓存，直接精确计算
            cachedDay = Long.MIN_VALUE
            cachedZone = null
            rules.getOffset(Instant.ofEpochSecond(timestampSeconds)).totalSeconds
        }
    }

    /** 本地时区下的「第几天」，用于判断是否跨天（代替解析日期字符串） */
    fun epochDay(timestampSeconds: Long): Long {
        val local = timestampSeconds + offsetSecondsAt(timestampSeconds)
        return Math.floorDiv(local, SECONDS_PER_DAY)
    }

    /** "HH:mm" */
    fun hhmm(timestampSeconds: Long): String {
        val local = timestampSeconds + offsetSecondsAt(timestampSeconds)
        val secondsOfDay = Math.floorMod(local, SECONDS_PER_DAY)
        val hour = (secondsOfDay / SECONDS_PER_HOUR).toInt()
        val minute = ((secondsOfDay % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE).toInt()

        val out = CharArray(5)
        out[0] = '0' + hour / 10
        out[1] = '0' + hour % 10
        out[2] = ':'
        out[3] = '0' + minute / 10
        out[4] = '0' + minute % 10
        return String(out)
    }

    /** "yyyy-MM-dd" */
    fun ymd(timestampSeconds: Long): String {
        val local = timestampSeconds + offsetSecondsAt(timestampSeconds)
        val localDay = Math.floorDiv(local, SECONDS_PER_DAY)

        // 打包成 yyyyMMdd，避免中间对象
        val packed = civilFromDays(localDay)
        val year = (packed / 10_000).toInt()
        val month = ((packed / 100) % 100).toInt()
        val day = (packed % 100).toInt()

        val out = CharArray(10)
        out[0] = '0' + (year / 1000) % 10
        out[1] = '0' + (year / 100) % 10
        out[2] = '0' + (year / 10) % 10
        out[3] = '0' + year % 10
        out[4] = '-'
        out[5] = '0' + month / 10
        out[6] = '0' + month % 10
        out[7] = '-'
        out[8] = '0' + day / 10
        out[9] = '0' + day % 10
        return String(out)
    }

    /**
     * 天数（自 1970-01-01）转公历日期，返回 yyyyMMdd。
     * 算法来自 Howard Hinnant 的 `civil_from_days`，全部是整数运算，无分支预测灾难。
     */
    private fun civilFromDays(days: Long): Long {
        val z = days + 719_468
        val era = (if (z >= 0) z else z - 146_096) / 146_097
        val doe = z - era * 146_097                                     // [0, 146096]
        val yoe = (doe - doe / 1_460 + doe / 36_524 - doe / 146_096) / 365
        val y = yoe + era * 400
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)               // [0, 365]
        val mp = (5 * doy + 2) / 153                                    // [0, 11]
        val d = doy - (153 * mp + 2) / 5 + 1                            // [1, 31]
        val m = if (mp < 10) mp + 3 else mp - 9                         // [1, 12]
        val year = if (m <= 2) y + 1 else y
        return year * 10_000 + m * 100 + d
    }
}
