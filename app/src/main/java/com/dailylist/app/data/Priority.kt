package com.dailylist.app.data

enum class Priority(val label: String, val sortOrder: Int) {
    LOW("低", 0),
    NORMAL("普通", 1),
    HIGH("高", 2);
}
