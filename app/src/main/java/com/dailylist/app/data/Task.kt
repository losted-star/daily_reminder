package com.dailylist.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    /** 本地日历日，格式 yyyy-MM-dd（ISO 日期字符串，字典序即时间序） */
    val dueDate: String,
    /** 提醒时刻（当日分钟数 0..1439），null 表示无提醒 */
    val reminderTimeMinutes: Int? = null,
    /** 提前提醒分钟数：0 / 5 / 10 / 15 / 30 */
    val reminderAdvanceMinutes: Int = 0,
    val repeatRule: RepeatRule = RepeatRule.NONE,
    /** 每周重复时选中的星期（ISO：1=周一 .. 7=周日），其它规则忽略 */
    val repeatDays: Set<Int> = emptySet(),
    val priority: Priority = Priority.NORMAL,
    val categoryId: Long? = null,
    /** 仅对「不重复」任务有意义；重复任务的完成状态记录在 TaskOccurrence */
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
