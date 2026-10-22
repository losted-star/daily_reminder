package com.dailylist.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskOccurrenceDao {
    @Query("SELECT * FROM task_occurrences")
    fun observeAll(): Flow<List<TaskOccurrence>>

    @Query("SELECT * FROM task_occurrences")
    suspend fun getAll(): List<TaskOccurrence>

    @Query("SELECT * FROM task_occurrences WHERE taskId = :taskId AND occurDate = :date")
    suspend fun get(taskId: Long, date: String): TaskOccurrence?

    @Query("SELECT * FROM task_occurrences WHERE taskId = :taskId")
    suspend fun getByTask(taskId: Long): List<TaskOccurrence>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(occurrence: TaskOccurrence): Long

    @Query("DELETE FROM task_occurrences WHERE taskId = :taskId AND occurDate = :date")
    suspend fun delete(taskId: Long, date: String)

    @Query("DELETE FROM task_occurrences WHERE taskId = :taskId")
    suspend fun deleteByTask(taskId: Long)
}
