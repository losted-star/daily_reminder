package com.dailylist.app.domain

import com.dailylist.app.data.RepeatRule
import com.dailylist.app.data.Task
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

object ReminderCalculator {
    /** 最多向前搜索 370 天，避免死循环 */
    private const val MAX_SEARCH_DAYS = 370L

    /**
     * 计算下一次提醒的绝对触发时刻（epoch millis）。
     *
     * @param skipDates 已完成的日期集合（仅对重复任务有意义），这些日期不再触发。
     * @return 无未来触发时刻时返回 null。
     */
    fun nextFireEpochMillis(task: Task, now: ZonedDateTime, skipDates: Set<String> = emptySet()): Long? {
        val minutes = task.reminderTimeMinutes ?: return null
        if (task.repeatRule == RepeatRule.NONE && task.isCompleted) return null

        val zone = now.zone
        val baseTime = LocalTime.ofSecondOfDay(minutes * 60L).minusMinutes(task.reminderAdvanceMinutes.toLong())
        var date = now.toLocalDate()

        for (i in 0 until MAX_SEARCH_DAYS) {
            if (RepeatLogic.isApplicable(task, date)) {
                val dateStr = DateUtils.formatDate(date)
                if (dateStr !in skipDates) {
                    val fireZdt = ZonedDateTime.of(date, baseTime, zone)
                    if (fireZdt.isAfter(now)) {
                        return fireZdt.toInstant().toEpochMilli()
                    }
                }
            }
            date = date.plusDays(1)
        }
        return null
    }
}
