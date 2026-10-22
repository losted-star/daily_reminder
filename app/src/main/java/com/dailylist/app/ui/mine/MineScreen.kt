package com.dailylist.app.ui.mine

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dailylist.app.data.Category
import com.dailylist.app.data.ThemeMode
import com.dailylist.app.ui.components.CategoryDot
import com.dailylist.app.ui.components.weekdayShort
import com.dailylist.app.ui.theme.CategoryColors
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun MineScreen(
    factory: ViewModelProvider.Factory,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
) {
    val viewModel: MineViewModel = viewModel(factory = factory)
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val weekCompletions by viewModel.weekCompletions.collectAsStateWithLifecycle()
    val todayStats by viewModel.todayStats.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { viewModel.refresh() }

    // 通知权限
    var notificationGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        )
    }
    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationGranted = granted
    }

    // 精确闹钟
    val alarmManager = remember { context.getSystemService(AlarmManager::class.java) }
    var canExactAlarm by remember {
        mutableStateOf(Build.VERSION.SDK_INT < 31 || alarmManager.canScheduleExactAlarms())
    }

    // 备份导出
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val json = viewModel.exportJson()
                    context.contentResolver.openOutputStream(it)?.use { out -> out.write(json.toByteArray()) }
                    snackbarHostState.showSnackbar("已导出")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("导出失败：${e.message}")
                }
            }
        }
    }

    // 备份导入
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val json = context.contentResolver.openInputStream(it)?.use { ins ->
                        ins.readBytes().toString(Charsets.UTF_8)
                    } ?: throw IllegalStateException("无法读取文件")
                    val result = viewModel.importJson(json)
                    snackbarHostState.showSnackbar(
                        "导入成功：任务 ${result.importedTasks}、分类 ${result.importedCategories}、完成记录 ${result.importedOccurrences}"
                    )
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("导入失败：${e.message}")
                }
            }
        }
    }

    // 分类编辑对话框
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var showCategoryDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { StatsCard(todayStats, weekCompletions) }

            item {
                SectionTitle("分类管理")
                Card {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        categories.forEach { c ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CategoryDot(color = Color(c.colorArgb))
                                Spacer(Modifier.width(10.dp))
                                Text(c.name, modifier = Modifier.weight(1f))
                                IconButton(onClick = {
                                    editingCategory = c
                                    showCategoryDialog = true
                                }) {
                                    Icon(Icons.Outlined.Edit, contentDescription = "编辑", modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { viewModel.deleteCategory(c.id) }) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = "删除",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                editingCategory = null
                                showCategoryDialog = true
                            },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Text("新增分类")
                        }
                    }
                }
            }

            item {
                SectionTitle("外观")
                Card {
                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = { onThemeModeChange(mode) },
                                label = { Text(modeLabel(mode)) },
                            )
                        }
                    }
                }
            }

            item {
                SectionTitle("默认提醒时间")
                Card {
                    DefaultReminderRow(
                        currentMinutes = settings.defaultReminderMinutes,
                        onSet = { minutes, advance -> viewModel.setDefaultReminder(minutes, advance) },
                    )
                }
            }

            item {
                SectionTitle("通知与提醒")
                Card {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        PermissionRow(
                            label = "通知权限",
                            granted = notificationGranted,
                            actionLabel = if (notificationGranted) null else "去开启",
                            onAction = {
                                if (Build.VERSION.SDK_INT >= 33) {
                                    requestPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                        )
                        PermissionRow(
                            label = "精确闹钟",
                            granted = canExactAlarm,
                            actionLabel = if (canExactAlarm) null else "去开启",
                            onAction = {
                                if (Build.VERSION.SDK_INT >= 31) {
                                    context.startActivity(
                                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
                                    )
                                }
                            },
                        )
                        Text(
                            text = "提醒可靠性：为保证息屏时正常提醒，建议在系统设置中允许「每日清单」自启动，并在电池管理中设为「允许后台活动」。受系统省电策略影响，极端情况下提醒可能延迟。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                SectionTitle("数据备份")
                Card {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = {
                                exportLauncher.launch("DailyList-backup-${LocalDate.now()}.json")
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("导出数据 (JSON)")
                        }
                        OutlinedButton(
                            onClick = { importLauncher.launch(arrayOf("application/json")) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("导入数据 (JSON)")
                        }
                        Text(
                            text = "导入采用「追加」策略，不会覆盖已有数据；分类按名称去重。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                SectionTitle("关于")
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("每日清单 DailyList", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "版本 1.0 · 离线优先 · 数据仅存储于本机",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (showCategoryDialog) {
        CategoryDialog(
            category = editingCategory,
            onDismiss = { showCategoryDialog = false },
            onConfirm = { name, color ->
                if (editingCategory == null) {
                    viewModel.addCategory(name, color)
                } else {
                    viewModel.updateCategory(editingCategory!!.copy(name = name, colorArgb = color))
                }
                showCategoryDialog = false
            },
        )
    }
}

private fun modeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> "跟随系统"
    ThemeMode.LIGHT -> "浅色"
    ThemeMode.DARK -> "深色"
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

@Composable
private fun StatsCard(stats: com.dailylist.app.data.TodayStats, week: List<Pair<LocalDate, Int>>) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("今日统计", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "完成率 ${(stats.rate * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = "${stats.completed} / ${stats.total}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "最近 7 天完成情况",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(8.dp))
            WeekBarChart(week)
        }
    }
}

@Composable
private fun WeekBarChart(week: List<Pair<LocalDate, Int>>) {
    val maxCount = (week.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        week.forEach { (date, count) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height((40f * count / maxCount).coerceAtLeast(4f).dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = weekdayShort(date.dayOfWeek.value),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, actionLabel: String?, onAction: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = if (granted) "已开启" else "未开启",
                style = MaterialTheme.typography.bodySmall,
                color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
        if (actionLabel != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
private fun DefaultReminderRow(
    currentMinutes: Int?,
    onSet: (Int?, Int) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showPicker = true }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("默认提醒时间", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = currentMinutes?.let { com.dailylist.app.domain.DateUtils.minutesToTime(it) } ?: "未设置",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (showPicker) {
        DefaultReminderDialog(
            currentMinutes = currentMinutes,
            onDismiss = { showPicker = false },
            onConfirm = { minutes ->
                onSet(minutes, 0)
                showPicker = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DefaultReminderDialog(
    currentMinutes: Int?,
    onDismiss: () -> Unit,
    onConfirm: (Int?) -> Unit,
) {
    val timeState = rememberTimePickerState(
        initialHour = (currentMinutes ?: (9 * 60)) / 60,
        initialMinute = (currentMinutes ?: 0) % 60,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("默认提醒时间") },
        text = { TimePicker(state = timeState) },
        confirmButton = {
            TextButton(onClick = { onConfirm(timeState.hour * 60 + timeState.minute) }) { Text("确定") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onConfirm(null) }) { Text("不提醒") }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        },
    )
}

@Composable
private fun CategoryDialog(
    category: Category?,
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit,
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var selectedColor by remember { mutableStateOf(category?.let { Color(it.colorArgb) } ?: CategoryColors.first()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "新增分类" else "编辑分类") },
        text = {
            Column {
                androidx.compose.material3.OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("分类名称") },
                    singleLine = true,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryColors.forEach { c ->
                        val selected = c == selectedColor
                        Box(
                            modifier = Modifier
                                .size(if (selected) 34.dp else 28.dp)
                                .clip(CircleShape)
                                .background(c)
                                .clickable { selectedColor = c }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isNotEmpty()) onConfirm(trimmed, colorToArgb(selectedColor))
                },
            ) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

private fun colorToArgb(c: Color): Int =
    ((c.alpha * 255f + 0.5f).toInt() shl 24) or
        ((c.red * 255f + 0.5f).toInt() shl 16) or
        ((c.green * 255f + 0.5f).toInt() shl 8) or
        (c.blue * 255f + 0.5f).toInt()
