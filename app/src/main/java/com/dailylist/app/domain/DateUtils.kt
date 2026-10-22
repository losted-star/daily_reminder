package com.dailylist.app.domain

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object DateUtils {
    private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun today(): LocalDate = LocalDate.now()

    fun parseDate(value: String): LocalDate = LocalDate.parse(value, DATE_FORMATTER)

    fun formatDate(date: LocalDate): String = date.format(DATE_FORMATTER)

    /** 分钟数(0..1439) 转 HH:mm 字符串 */
    fun minutesToTime(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return "%02d:%02d".format(h, m)
    }

    /** HH:mm 字符串 转分钟数，非法输入返回 null */
    fun timeToMinutes(time: String): Int? {
        val parts = time.trim().split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h * 60 + m
    }

    fun minutesOfNow(): Int {
        val t = LocalTime.now()
        return t.hour * 60 + t.minute
    }
}
