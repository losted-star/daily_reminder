package com.dailylist.app.domain

import com.dailylist.app.data.RepeatRule
import com.dailylist.app.data.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneOffset
import java.time.ZonedDateTime

class ReminderCalculatorTest {

    private fun millis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli()

    private fun task(
        dueDate: String,
        reminderMinutes: Int?,
        advance: Int = 0,
        rule: RepeatRule = RepeatRule.NONE,
        completed: Boolean = false,
    ) = Task(
        title = "t",
        dueDate = dueDate,
        reminderTimeMinutes = reminderMinutes,
        reminderAdvanceMinutes = advance,
        repeatRule = rule,
        isCompleted = completed,
    )

    @Test
    fun `无提醒时间返回 null`() {
        val t = task("2026-10-21", reminderMinutes = null)
        assertNull(ReminderCalculator.nextFireEpochMillis(t, ZonedDateTime.of(2026, 10, 21, 8, 0, 0, 0, ZoneOffset.UTC)))
    }

    @Test
    fun `已完成的不重复任务不再提醒`() {
        val t = task("2026-10-21", reminderMinutes = 600, completed = true)
        assertNull(ReminderCalculator.nextFireEpochMillis(t, ZonedDateTime.of(2026, 10, 21, 8, 0, 0, 0, ZoneOffset.UTC)))
    }

    @Test
    fun `当天未来时刻触发提醒`() {
        val t = task("2026-10-21", reminderMinutes = 600) // 10:00
        val now = ZonedDateTime.of(2026, 10, 21, 8, 0, 0, 0, ZoneOffset.UTC)
        assertEquals(millis(2026, 10, 21, 10, 0), ReminderCalculator.nextFireEpochMillis(t, now))
    }

    @Test
    fun `提前提醒分钟数生效`() {
        val t = task("2026-10-21", reminderMinutes = 600, advance = 10) // 09:50
        val now = ZonedDateTime.of(2026, 10, 21, 8, 0, 0, 0, ZoneOffset.UTC)
        assertEquals(millis(2026, 10, 21, 9, 50), ReminderCalculator.nextFireEpochMillis(t, now))
    }

    @Test
    fun `不重复任务当天时刻已过则返回 null`() {
        val t = task("2026-10-21", reminderMinutes = 600)
        val now = ZonedDateTime.of(2026, 10, 21, 12, 0, 0, 0, ZoneOffset.UTC)
        assertNull(ReminderCalculator.nextFireEpochMillis(t, now))
    }

    @Test
    fun `每天重复当天已过则顺延到次日`() {
        val t = task("2026-10-21", reminderMinutes = 600, rule = RepeatRule.DAILY)
        val now = ZonedDateTime.of(2026, 10, 21, 12, 0, 0, 0, ZoneOffset.UTC)
        assertEquals(millis(2026, 10, 22, 10, 0), ReminderCalculator.nextFireEpochMillis(t, now))
    }

    @Test
    fun `skipDates 中的日期被跳过`() {
        val t = task("2026-10-21", reminderMinutes = 600, rule = RepeatRule.DAILY)
        val now = ZonedDateTime.of(2026, 10, 21, 8, 0, 0, 0, ZoneOffset.UTC)
        assertEquals(
            millis(2026, 10, 22, 10, 0),
            ReminderCalculator.nextFireEpochMillis(t, now, skipDates = setOf("2026-10-21")),
        )
    }
}
