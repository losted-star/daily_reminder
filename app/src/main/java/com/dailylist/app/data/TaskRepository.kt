package com.dailylist.app.data

import com.dailylist.app.domain.DateUtils
import com.dailylist.app.domain.ReminderCalculator
import com.dailylist.app.domain.RepeatLogic
import com.dailylist.app.reminder.AlarmScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/** 任务 + 指定日期的完成状态 */
data class TaskItem(val task: Task, val completed: Boolean)

/** 日历中某一天的标记 */
data class DayStatus(val hasTask: Boolean, val hasUncompleted: Boolean)

data class TodayStats(val total: Int, val completed: Int) {
    val rate: Float get() = if (total == 0) 0f else completed.toFloat() / total
}

data class ImportResult(val importedTasks: Int, val importedCategories: Int, val importedOccurrences: Int)

class TaskRepository(
    private val taskDao: TaskDao,
    private val categoryDao: CategoryDao,
    private val occurrenceDao: TaskOccurrenceDao,
    private val scheduler: AlarmScheduler,
) {

    // ---- 查询 ----

    fun observeCategories(): Flow<List<Category>> = categoryDao.observeAll()

    fun observeTasksForDate(date: LocalDate): Flow<List<TaskItem>> {
        val dateStr = DateUtils.formatDate(date)
        return combine(taskDao.observeAll(), occurrenceDao.observeAll()) { tasks, occurrences ->
            val completedIds = occurrences.filter { it.occurDate == dateStr }.map { it.taskId }.toSet()
            tasks.filter { RepeatLogic.isApplicable(it, date) }.map { task ->
                val completed = if (task.repeatRule == RepeatRule.NONE) {
                    task.isCompleted
                } else {
                    task.id in completedIds
                }
                TaskItem(task, completed)
            }
        }
    }

    fun observeBacklog(date: LocalDate): Flow<List<Task>> = taskDao.observeAll().map { tasks ->
        tasks.filter {
            it.repeatRule == RepeatRule.NONE && !it.isCompleted &&
                DateUtils.parseDate(it.dueDate).isBefore(date)
        }
    }

    suspend fun getTask(id: Long): Task? = taskDao.getById(id)

    suspend fun getCategories(): List<Category> = categoryDao.getAll()

    // ---- 写操作 ----

    suspend fun addTask(task: Task): Long {
        val id = taskDao.insert(task)
        resyncReminder(id)
        return id
    }

    suspend fun updateTask(task: Task) {
        taskDao.update(task.copy(updatedAt = System.currentTimeMillis()))
        resyncReminder(task.id)
    }

    suspend fun deleteTask(id: Long) {
        scheduler.cancel(id)
        occurrenceDao.deleteByTask(id)
        taskDao.deleteById(id)
    }

    suspend fun setCompleted(task: Task, onDate: LocalDate, completed: Boolean) {
        if (task.repeatRule == RepeatRule.NONE) {
            taskDao.update(
                task.copy(
                    isCompleted = completed,
                    completedAt = if (completed) System.currentTimeMillis() else null,
                    updatedAt = System.currentTimeMillis(),
                )
            )
        } else {
            val dateStr = DateUtils.formatDate(onDate)
            if (completed) {
                occurrenceDao.insert(TaskOccurrence(taskId = task.id, occurDate = dateStr))
            } else {
                occurrenceDao.delete(task.id, dateStr)
            }
        }
        resyncReminder(task.id)
    }

    suspend fun isCompletedOn(task: Task, date: LocalDate): Boolean {
        return if (task.repeatRule == RepeatRule.NONE) {
            task.isCompleted
        } else {
            occurrenceDao.get(task.id, DateUtils.formatDate(date)) != null
        }
    }

    // ---- 分类 ----

    suspend fun addCategory(category: Category): Long = categoryDao.insert(category)

    suspend fun updateCategory(category: Category) = categoryDao.update(category)

    suspend fun deleteCategory(id: Long) {
        val tasks = taskDao.getAll()
        // 将被删分类的任务迁移到「其他」分类；若没有「其他」则置空
        val fallback = categoryDao.getAll().firstOrNull { it.name == "其他" }?.id
        tasks.filter { it.categoryId == id }.forEach { task ->
            taskDao.update(task.copy(categoryId = fallback, updatedAt = System.currentTimeMillis()))
        }
        categoryDao.deleteById(id)
    }

    suspend fun seedDefaults() {
        if (categoryDao.getAll().isEmpty()) {
            listOf(
                "工作" to 0xFF6B9DF2.toInt(),
                "生活" to 0xFFE39B6A.toInt(),
                "其他" to 0xFFB08AD9.toInt(),
            ).forEach { (name, color) ->
                categoryDao.insert(Category(name = name, colorArgb = color))
            }
        }
    }

    // ---- 日历与统计 ----

    suspend fun monthSummary(year: Int, month: Int): Map<LocalDate, DayStatus> {
        val tasks = taskDao.getAll()
        val completedDatesByTask = occurrenceDao.getAll()
            .groupBy { it.taskId }
            .mapValues { (_, v) -> v.map { it.occurDate }.toSet() }

        val first = LocalDate.of(year, month, 1)
        val result = mutableMapOf<LocalDate, DayStatus>()
        for (day in 1..first.lengthOfMonth()) {
            val date = first.withDayOfMonth(day)
            var hasTask = false
            var hasUncompleted = false
            for (task in tasks) {
                if (RepeatLogic.isApplicable(task, date)) {
                    hasTask = true
                    val completed = if (task.repeatRule == RepeatRule.NONE) {
                        task.isCompleted
                    } else {
                        completedDatesByTask[task.id]?.contains(DateUtils.formatDate(date)) == true
                    }
                    if (!completed) hasUncompleted = true
                }
            }
            if (hasTask) result[date] = DayStatus(true, hasUncompleted)
        }
        return result
    }

    suspend fun todayStats(date: LocalDate): TodayStats {
        val tasks = taskDao.getAll()
        val dateStr = DateUtils.formatDate(date)
        val completedIds = occurrenceDao.getAll()
            .filter { it.occurDate == dateStr }
            .map { it.taskId }
            .toSet()
        var total = 0
        var completed = 0
        for (task in tasks) {
            if (RepeatLogic.isApplicable(task, date)) {
                total++
                val c = if (task.repeatRule == RepeatRule.NONE) task.isCompleted else task.id in completedIds
                if (c) completed++
            }
        }
        return TodayStats(total, completed)
    }

    /** 最近 [days] 天（含 [start]）每天的完成数量 */
    suspend fun completedPerDay(start: LocalDate, days: Int): List<Pair<LocalDate, Int>> {
        val tasks = taskDao.getAll()
        val occurrences = occurrenceDao.getAll()
        val zone = ZoneId.systemDefault()
        return (0 until days).map { i ->
            val day = start.plusDays(i.toLong())
            val dayStart = day.atStartOfDay(zone).toInstant().toEpochMilli()
            val dayEnd = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            var count = 0
            for (t in tasks) {
                if (t.repeatRule == RepeatRule.NONE && t.completedAt != null && t.completedAt in dayStart until dayEnd) count++
            }
            for (o in occurrences) {
                if (o.completedAt in dayStart until dayEnd) count++
            }
            day to count
        }
    }

    // ---- 提醒调度 ----

    suspend fun resyncAllReminders() {
        for (task in taskDao.getAll()) {
            resyncReminder(task.id)
        }
    }

    suspend fun resyncReminder(taskId: Long) {
        val task = taskDao.getById(taskId) ?: return
        if (task.reminderTimeMinutes == null) {
            scheduler.cancel(taskId)
            return
        }
        val skipDates = if (task.repeatRule == RepeatRule.NONE) {
            emptySet()
        } else {
            occurrenceDao.getByTask(taskId).map { it.occurDate }.toSet()
        }
        val fire = ReminderCalculator.nextFireEpochMillis(task, ZonedDateTime.now(), skipDates)
        if (fire != null) scheduler.schedule(taskId, fire) else scheduler.cancel(taskId)
    }

    // ---- 备份 / 导入 ----

    suspend fun exportData(): String =
        BackupManager.serialize(taskDao.getAll(), categoryDao.getAll(), occurrenceDao.getAll())

    suspend fun importData(json: String): ImportResult {
        val data = BackupManager.parse(json)

        // 分类：按名称去重，仅新增不存在的分类
        val existingNames = categoryDao.getAll().map { it.name }.toSet()
        val categoryIdMap = mutableMapOf<Long, Long>()
        var importedCategories = 0
        for (c in data.categories) {
            if (c.name !in existingNames) {
                val newId = categoryDao.insert(Category(name = c.name, colorArgb = c.colorArgb, createdAt = c.createdAt))
                categoryIdMap[c.id] = newId
                importedCategories++
            }
        }

        // 任务：全部作为新任务追加（id 重映射），不覆盖已有数据
        val taskIdMap = mutableMapOf<Long, Long>()
        var importedTasks = 0
        for (t in data.tasks) {
            val newCatId = t.categoryId?.let { categoryIdMap[it] }
            val newId = taskDao.insert(
                t.copy(id = 0L, categoryId = newCatId)
            )
            taskIdMap[t.id] = newId
            importedTasks++
        }

        // 完成记录：跟随重映射后的 taskId
        var importedOccurrences = 0
        for (o in data.occurrences) {
            val newTaskId = taskIdMap[o.taskId] ?: continue
            occurrenceDao.insert(o.copy(id = 0L, taskId = newTaskId))
            importedOccurrences++
        }

        resyncAllReminders()
        return ImportResult(importedTasks, importedCategories, importedOccurrences)
    }
}
