package com.dailylist.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class BackupManagerTest {

    @Test
    fun `序列化后解析得到完全一致的数据`() {
        val cat = Category(id = 1, name = "工作", colorArgb = 0xFF6B9DF2.toInt(), createdAt = 1000L)
        val task = Task(
            id = 2,
            title = "写报告",
            description = "备注",
            dueDate = "2026-10-21",
            reminderTimeMinutes = 570,
            reminderAdvanceMinutes = 10,
            repeatRule = RepeatRule.WEEKLY,
            repeatDays = setOf(1, 3, 5),
            priority = Priority.HIGH,
            categoryId = 1,
            isCompleted = false,
            completedAt = null,
            createdAt = 2000L,
            updatedAt = 3000L,
        )
        val occ = TaskOccurrence(id = 3, taskId = 2, occurDate = "2026-10-21", completedAt = 4000L)

        val json = BackupManager.serialize(listOf(task), listOf(cat), listOf(occ))
        val data = BackupManager.parse(json)

        assertEquals(listOf(cat), data.categories)
        assertEquals(listOf(task), data.tasks)
        assertEquals(listOf(occ), data.occurrences)
    }

    @Test
    fun `无提醒与无分类字段的往返`() {
        val task = Task(
            id = 5,
            title = "无提醒",
            dueDate = "2026-10-21",
            reminderTimeMinutes = null,
            categoryId = null,
            repeatRule = RepeatRule.NONE,
        )
        val data = BackupManager.parse(BackupManager.serialize(listOf(task), emptyList(), emptyList()))
        assertEquals(listOf(task), data.tasks)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `非 DailyList 备份被拒绝`() {
        BackupManager.parse("{}")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `空标题任务被拒绝`() {
        BackupManager.parse("""{"app":"DailyList","tasks":[{"title":" ","dueDate":"2026-10-21"}]}""")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `非法日期被拒绝`() {
        BackupManager.parse("""{"app":"DailyList","tasks":[{"title":"x","dueDate":"not-a-date"}]}""")
    }
}
