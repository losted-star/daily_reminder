package com.dailylist.app.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromPriority(value: Priority): String = value.name

    @TypeConverter
    fun toPriority(value: String): Priority = Priority.valueOf(value)

    @TypeConverter
    fun fromRepeatRule(value: RepeatRule): String = value.name

    @TypeConverter
    fun toRepeatRule(value: String): RepeatRule = RepeatRule.valueOf(value)

    @TypeConverter
    fun fromIntSet(value: Set<Int>): String = value.joinToString(",")

    @TypeConverter
    fun toIntSet(value: String): Set<Int> =
        if (value.isBlank()) emptySet() else value.split(",").mapNotNull { it.toIntOrNull() }.toSet()
}
