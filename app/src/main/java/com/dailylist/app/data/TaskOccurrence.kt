package com.dailylist.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 重复任务在「某一天」的完成记录。
 * 仅在用户完成某天任务时创建一条记录，未完成 = 无记录，从而保证每天完成状态互不覆盖。
 */
@Entity(
    tableName = "task_occurrences",
    indices = [Index(value = ["taskId", "occurDate"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
)
data class TaskOccurrence(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    /** 完成对应的日历日 yyyy-MM-dd */
    val occurDate: String,
    val completedAt: Long = System.currentTimeMillis(),
)
