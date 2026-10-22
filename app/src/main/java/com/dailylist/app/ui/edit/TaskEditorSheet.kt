package com.dailylist.app.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dailylist.app.data.Category
import com.dailylist.app.data.Priority
import com.dailylist.app.data.RepeatRule
import com.dailylist.app.data.Task
import com.dailylist.app.domain.DateUtils
import com.dailylist.app.ui.components.formatFullDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val WEEKDAYS = listOf(1 to "一", 2 to "二", 3 to "三", 4 to "四", 5 to "五", 6 to "六", 7 to "日")
private val ADVANCE_OPTIONS = listOf(0 to "准时", 5 to "提前5分", 10 to "提前10分", 15 to "提前15分", 30 to "提前30分")

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorSheet(
    editingTask: Task?,
    defaultDate: LocalDate,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (Task) -> Unit,
    defaultReminderMinutes: Int? = null,
    defaultReminderAdvanceMinutes: Int = 0,
) {
    var title by remember { mutableStateOf(editingTask?.title ?: "") }
    var description by remember { mutableStateOf(editingTask?.description ?: "") }
    var date by remember { mutableStateOf(defaultDate) }
    var categoryId by remember { mutableStateOf(editingTask?.categoryId) }
    var priority by remember { mutableStateOf(editingTask?.priority ?: Priority.NORMAL) }
    var reminderTimeMinutes by remember { mutableStateOf(editingTask?.reminderTimeMinutes ?: defaultReminderMinutes) }
    var reminderAdvanceMinutes by remember {
        mutableStateOf(editingTask?.reminderAdvanceMinutes ?: defaultReminderAdvanceMinutes)
    }
    var repeatRule by remember { mutableStateOf(editingTask?.repeatRule ?: RepeatRule.NONE) }
    var repeatDays by remember { mutableStateOf(editingTask?.repeatDays ?: emptySet()) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    fun buildTask(): Task = Task(
        id = editingTask?.id ?: 0L,
        title = title.trim(),
        description = description.trim(),
        dueDate = DateUtils.formatDate(date),
        reminderTimeMinutes = reminderTimeMinutes,
        reminderAdvanceMinutes = if (reminderTimeMinutes != null) reminderAdvanceMinutes else 0,
        repeatRule = repeatRule,
        repeatDays = if (repeatRule == RepeatRule.WEEKLY) repeatDays else emptySet(),
        priority = priority,
        categoryId = categoryId,
        isCompleted = editingTask?.isCompleted ?: false,
        completedAt = editingTask?.completedAt,
        createdAt = editingTask?.createdAt ?: System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
    )

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .imePadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (editingTask == null) "新建任务" else "编辑任务",
                style = MaterialTheme.typography.titleLarge,
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("标题") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // 日期
            SectionLabel("日期")
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatFullDate(date),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { showDatePicker = true }) { Text("选择日期") }
            }

            // 分类
            SectionLabel("分类")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = categoryId == null,
                    onClick = { categoryId = null },
                    label = { Text("无分类") },
                )
                categories.forEach { c ->
                    FilterChip(
                        selected = categoryId == c.id,
                        onClick = { categoryId = c.id },
                        label = { Text(c.name) },
                    )
                }
            }

            // 优先级
            SectionLabel("优先级")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Priority.entries.forEach { p ->
                    FilterChip(
                        selected = priority == p,
                        onClick = { priority = p },
                        label = { Text(p.label) },
                    )
                }
            }

            // 提醒
            SectionLabel("提醒")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (reminderTimeMinutes == null) {
                    FilterChip(
                        selected = false,
                        onClick = {
                            reminderTimeMinutes = 9 * 60
                            showTimePicker = true
                        },
                        label = { Text("开启提醒") },
                    )
                } else {
                    FilterChip(
                        selected = true,
                        onClick = { showTimePicker = true },
                        label = { Text(DateUtils.minutesToTime(reminderTimeMinutes!!)) },
                    )
                    TextButton(onClick = { reminderTimeMinutes = null }) { Text("移除") }
                }
            }
            if (reminderTimeMinutes != null) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ADVANCE_OPTIONS.forEach { (advance, label) ->
                        FilterChip(
                            selected = reminderAdvanceMinutes == advance,
                            onClick = { reminderAdvanceMinutes = advance },
                            label = { Text(label) },
                        )
                    }
                }
            }

            // 重复
            SectionLabel("重复")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RepeatRule.entries.forEach { rule ->
                    FilterChip(
                        selected = repeatRule == rule,
                        onClick = { repeatRule = rule },
                        label = { Text(rule.label) },
                    )
                }
            }
            if (repeatRule == RepeatRule.WEEKLY) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WEEKDAYS.forEach { (day, label) ->
                        FilterChip(
                            selected = day in repeatDays,
                            onClick = {
                                repeatDays = if (day in repeatDays) repeatDays - day else repeatDays + day
                            },
                            label = { Text(label) },
                        )
                    }
                }
            }

            // 备注
            SectionLabel("备注")
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("备注（可选）") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { onSave(buildTask()) },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("保存")
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        }
                        showDatePicker = false
                    },
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val current = reminderTimeMinutes ?: (9 * 60)
        val timeState = rememberTimePickerState(
            initialHour = current / 60,
            initialMinute = current % 60,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("设置提醒时间") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        reminderTimeMinutes = timeState.hour * 60 + timeState.minute
                        showTimePicker = false
                    },
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
