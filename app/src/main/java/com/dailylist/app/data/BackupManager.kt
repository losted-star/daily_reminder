package com.dailylist.app.data

import com.dailylist.app.domain.DateUtils
import org.json.JSONArray
import org.json.JSONObject

data class BackupData(
    val tasks: List<Task>,
    val categories: List<Category>,
    val occurrences: List<TaskOccurrence>,
)

object BackupManager {
    private const val VERSION = 1

    fun serialize(
        tasks: List<Task>,
        categories: List<Category>,
        occurrences: List<TaskOccurrence>,
    ): String {
        val root = JSONObject()
        root.put("app", "DailyList")
        root.put("version", VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        root.put("categories", JSONArray().apply {
            categories.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("colorArgb", c.colorArgb)
                    put("createdAt", c.createdAt)
                })
            }
        })

        root.put("tasks", JSONArray().apply {
            tasks.forEach { t ->
                put(JSONObject().apply {
                    put("id", t.id)
                    put("title", t.title)
                    put("description", t.description)
                    put("dueDate", t.dueDate)
                    t.reminderTimeMinutes?.let { put("reminderTimeMinutes", it) }
                    put("reminderAdvanceMinutes", t.reminderAdvanceMinutes)
                    put("repeatRule", t.repeatRule.name)
                    put("repeatDays", JSONArray().apply { t.repeatDays.forEach { put(it) } })
                    put("priority", t.priority.name)
                    t.categoryId?.let { put("categoryId", it) }
                    put("isCompleted", t.isCompleted)
                    t.completedAt?.let { put("completedAt", it) }
                    put("createdAt", t.createdAt)
                    put("updatedAt", t.updatedAt)
                })
            }
        })

        root.put("occurrences", JSONArray().apply {
            occurrences.forEach { o ->
                put(JSONObject().apply {
                    put("id", o.id)
                    put("taskId", o.taskId)
                    put("occurDate", o.occurDate)
                    put("completedAt", o.completedAt)
                })
            }
        })

        return root.toString(2)
    }

    /** 解析并校验备份 JSON，非法输入抛出 [IllegalArgumentException] */
    fun parse(json: String): BackupData {
        val root = JSONObject(json)
        if (root.optString("app", "") != "DailyList") {
            throw IllegalArgumentException("不是有效的每日清单备份文件")
        }

        val categories = mutableListOf<Category>()
        root.optJSONArray("categories")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                categories.add(
                    Category(
                        id = o.optLong("id", 0),
                        name = o.getString("name"),
                        colorArgb = o.optInt("colorArgb", 0xFF8CA96B.toInt()),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    )
                )
            }
        }

        val tasks = mutableListOf<Task>()
        root.optJSONArray("tasks")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val repeatDays = o.optJSONArray("repeatDays")?.let { da ->
                    (0 until da.length()).mapNotNull { da.optInt(it) }.toSet()
                } ?: emptySet()
                tasks.add(
                    Task(
                        id = o.optLong("id", 0),
                        title = o.getString("title"),
                        description = o.optString("description", ""),
                        dueDate = o.getString("dueDate"),
                        reminderTimeMinutes = if (o.has("reminderTimeMinutes")) o.optInt("reminderTimeMinutes") else null,
                        reminderAdvanceMinutes = o.optInt("reminderAdvanceMinutes", 0),
                        repeatRule = runCatching { RepeatRule.valueOf(o.optString("repeatRule", "NONE")) }
                            .getOrDefault(RepeatRule.NONE),
                        repeatDays = repeatDays,
                        priority = runCatching { Priority.valueOf(o.optString("priority", "NORMAL")) }
                            .getOrDefault(Priority.NORMAL),
                        categoryId = if (o.has("categoryId")) o.optLong("categoryId") else null,
                        isCompleted = o.optBoolean("isCompleted", false),
                        completedAt = if (o.has("completedAt")) o.optLong("completedAt") else null,
                        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
                    )
                )
            }
        }

        val occurrences = mutableListOf<TaskOccurrence>()
        root.optJSONArray("occurrences")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                occurrences.add(
                    TaskOccurrence(
                        id = o.optLong("id", 0),
                        taskId = o.getLong("taskId"),
                        occurDate = o.getString("occurDate"),
                        completedAt = o.optLong("completedAt", System.currentTimeMillis()),
                    )
                )
            }
        }

        // 校验
        tasks.forEach { t ->
            if (t.title.isBlank()) throw IllegalArgumentException("备份中存在空标题任务")
            runCatching { DateUtils.parseDate(t.dueDate) }
                .getOrElse { throw IllegalArgumentException("日期格式错误：${t.dueDate}") }
        }

        return BackupData(tasks, categories, occurrences)
    }
}
