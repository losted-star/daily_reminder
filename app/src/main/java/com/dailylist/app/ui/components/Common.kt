package com.dailylist.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dailylist.app.data.Priority
import com.dailylist.app.data.RepeatRule
import com.dailylist.app.data.Task
import com.dailylist.app.domain.DateUtils

fun priorityColor(priority: Priority): Color = when (priority) {
    Priority.HIGH -> Color(0xFFE06B6B)
    Priority.LOW -> Color(0xFF7E9CD6)
    Priority.NORMAL -> Color(0xFF9AA7A0)
}

fun repeatLabel(rule: RepeatRule, days: Set<Int>): String = when (rule) {
    RepeatRule.NONE -> ""
    RepeatRule.DAILY -> "每天"
    RepeatRule.WEEKLY -> if (days.isEmpty()) "每周" else "每周(${days.sorted().joinToString("") { weekdayShort(it) }})"
    RepeatRule.MONTHLY -> "每月"
}

fun reminderLabel(task: Task): String? {
    val m = task.reminderTimeMinutes ?: return null
    val t = DateUtils.minutesToTime(m)
    return if (task.reminderAdvanceMinutes > 0) "$t · 提前${task.reminderAdvanceMinutes}分钟" else t
}

@Composable
fun CategoryDot(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun EmptyState(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
fun MetaTag(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
