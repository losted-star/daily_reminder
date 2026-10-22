package com.dailylist.app.domain

import com.dailylist.app.data.RepeatRule
import com.dailylist.app.data.Task
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RepeatLogicTest {

    private fun task(dueDate: String, rule: RepeatRule, days: Set<Int> = emptySet()) =
        Task(title = "t", dueDate = dueDate, repeatRule = rule, repeatDays = days)

    @Test
    fun `不重复任务仅在其到期日适用`() {
        val t = task("2026-10-21", RepeatRule.NONE)
        assertTrue(RepeatLogic.isApplicable(t, LocalDate.of(2026, 10, 21)))
        assertFalse(RepeatLogic.isApplicable(t, LocalDate.of(2026, 10, 22)))
        assertFalse(RepeatLogic.isApplicable(t, LocalDate.of(2026, 10, 20)))
    }

    @Test
    fun `每天重复自到期日起每天适用`() {
        val t = task("2026-10-21", RepeatRule.DAILY)
        assertTrue(RepeatLogic.isApplicable(t, LocalDate.of(2026, 10, 21)))
        assertTrue(RepeatLogic.isApplicable(t, LocalDate.of(2026, 10, 22)))
        assertTrue(RepeatLogic.isApplicable(t, LocalDate.of(2027, 1, 1)))
        assertFalse(RepeatLogic.isApplicable(t, LocalDate.of(2026, 10, 20)))
    }

    @Test
    fun `每周重复仅在选中星期且不早于到期日`() {
        // 2026-10-21 为星期三（ISO 3）
        val t = task("2026-10-21", RepeatRule.WEEKLY, days = setOf(1, 3, 5))
        assertTrue(RepeatLogic.isApplicable(t, LocalDate.of(2026, 10, 21))) // 周三
        assertTrue(RepeatLogic.isApplicable(t, LocalDate.of(2026, 10, 26))) // 下周一
        assertFalse(RepeatLogic.isApplicable(t, LocalDate.of(2026, 10, 22))) // 周四
        assertFalse(RepeatLogic.isApplicable(t, LocalDate.of(2026, 10, 19))) // 周一但早于到期日
    }

    @Test
    fun `每月重复按到期日的日号匹配`() {
        val t = task("2026-01-21", RepeatRule.MONTHLY)
        assertTrue(RepeatLogic.isApplicable(t, LocalDate.of(2026, 1, 21)))
        assertTrue(RepeatLogic.isApplicable(t, LocalDate.of(2026, 2, 21)))
        assertFalse(RepeatLogic.isApplicable(t, LocalDate.of(2026, 2, 22)))
        assertFalse(RepeatLogic.isApplicable(t, LocalDate.of(2025, 12, 21))) // 早于到期日
    }

    @Test
    fun `每月重复对不存在的日号当月跳过`() {
        val t = task("2026-01-31", RepeatRule.MONTHLY)
        assertFalse(RepeatLogic.isApplicable(t, LocalDate.of(2026, 2, 28))) // 2 月无 31 号
        assertTrue(RepeatLogic.isApplicable(t, LocalDate.of(2026, 3, 31)))
    }
}
