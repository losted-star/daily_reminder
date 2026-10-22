package com.dailylist.app.ui.components

import java.time.LocalDate
import java.time.LocalTime

private val WEEKDAYS = listOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")

fun formatFullDate(date: LocalDate): String =
    "${date.year}年${date.monthValue}月${date.dayOfMonth}日 ${WEEKDAYS[date.dayOfWeek.value - 1]}"

fun weekdayShort(isoValue: Int): String = when (isoValue) {
    1 -> "一"
    2 -> "二"
    3 -> "三"
    4 -> "四"
    5 -> "五"
    6 -> "六"
    7 -> "日"
    else -> ""
}

fun greeting(now: LocalTime = LocalTime.now()): String = when (now.hour) {
    in 5..10 -> "早上好"
    in 11..12 -> "中午好"
    in 13..17 -> "下午好"
    else -> "晚上好"
}
