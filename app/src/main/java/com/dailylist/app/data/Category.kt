package com.dailylist.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** ARGB 颜色值（Color.toArgb()） */
    val colorArgb: Int,
    val createdAt: Long = System.currentTimeMillis(),
)
