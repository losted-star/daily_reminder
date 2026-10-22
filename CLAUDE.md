# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## 项目：每日清单 DailyList（AI 接手速查）

> 用途：让 AI 快速理解本项目并直接上手，无需从头通读代码。含架构、数据模型、关键文件、构建、坑与约定。面向用户的说明见 `README.md`。

## 1. 项目概述

- 纯**离线** Android 待办/提醒应用，包名 `com.dailylist.app`，无网络、无登录、无服务器，数据全存本地 Room。
- 目标设备：华为 Mate 40（HarmonyOS 4.2，直接运行 Android APK，无需 HAP）。
- 三个底部 Tab：**今日 / 日历 / 我的**；绿色系 Material 3，支持浅色/深色主题。
- 当前版本 `1.0`（versionCode 1），已交付 debug APK（`DailyList-v1.0-debug.apk`）。

## 2. 技术栈（精确版本）

| 项 | 值 |
| --- | --- |
| 语言 | Kotlin 2.0.21 |
| 构建 | AGP 8.5.2 + Gradle 8.9 + JDK 17 |
| SDK | compileSdk/targetSdk **34**，minSdk **26** |
| UI | Jetpack Compose（BOM `2024.09.03`）+ Material 3 + material-icons-extended |
| 存储 | Room **2.6.1**（用 **kapt**，非 KSP）+ DataStore Preferences 1.1.1 |
| 异步 | kotlinx-coroutines 1.8.1（Flow / StateFlow） |
| 导航 | Navigation Compose 2.8.1 |
| DI | **手动**：`ServiceLocator`（单例）+ `ViewModelProvider.Factory`（无 Hilt/Koin） |
| 日期 | `java.time`（minSdk 26 原生支持，无 desugaring） |

**无**：网络库、WorkManager、Gson/Moshi/kotlinx-serialization（备份用 `org.json` 手写）。`org.json:json:20240303` 仅 `testImplementation`。

## 3. 架构与数据流

```
Compose UI → ViewModel(StateFlow) → TaskRepository → Room DAO
                                      ├─ AlarmScheduler（提醒调度）
                                      └─ 纯函数：RepeatLogic / ReminderCalculator / DateUtils
```

- MVVM；ViewModel 通过 `SharingStarted.WhileSubscribed(5_000)` 把 Flow 转 StateFlow。
- `TaskRepository` 是唯一业务入口，组合 DAO + 领域纯函数 + `AlarmScheduler`。
- 纯函数（`domain/`）无 Android 依赖，可直接单测。

## 4. 目录结构与关键文件

```
app/src/main/java/com/dailylist/app/
├── DailyListApp.kt          # Application：ensureChannel + seedDefaults + resyncAllReminders
├── MainActivity.kt          # 入口 Activity（singleTop）
├── data/
│   ├── Task.kt / TaskOccurrence.kt / Category.kt   # Room 实体
│   ├── RepeatRule.kt / Priority.kt                  # 枚举（含 label/sortOrder）
│   ├── Converters.kt                                # Room TypeConverter
│   ├── TaskDao.kt / TaskOccurrenceDao.kt / CategoryDao.kt
│   ├── DailyListDatabase.kt  # @Database version=1，无 Migration，无破坏性降级
│   ├── TaskRepository.kt     # 唯一业务入口（见 §6）
│   ├── BackupManager.kt      # JSON 导入导出（手写 serialize/parse，非反射）
│   ├── ServiceLocator.kt     # 手动 DI 单例
│   └── SettingsRepository.kt # DataStore：主题/默认提醒设置
├── domain/
│   ├── DateUtils.kt          # 分钟↔时间字符串、日期 parse/format
│   ├── RepeatLogic.kt        # isApplicable(task, date)
│   └── ReminderCalculator.kt # nextFireEpochMillis(task, now, skipDates)
├── reminder/
│   ├── AlarmScheduler.kt     # setExactAndAllowWhileIdle + setAndAllowWhileIdle 兜底
│   ├── NotificationHelper.kt # 通知渠道 + showTaskReminder
│   ├── ReminderReceiver.kt   # 收闹钟广播 → goAsync → 通知 + resyncReminder
│   └── BootReceiver.kt       # 开机/时区/时间变更 → resyncAllReminders
└── ui/
    ├── MainViewModel.kt      # 编辑器状态 + 保存/删除/完成 + 主题
    ├── AppViewModelFactory.kt# ViewModel 工厂分发
    ├── DailyListRoot.kt      # Scaffold + NavHost + 底部 3 Tab + TaskEditorSheet 挂载
    ├── today/ calendar/ mine/  # 三个 Tab 的 Screen + ViewModel
    ├── edit/TaskEditorSheet.kt # ModalBottomSheet 任务编辑（buildTask()）
    ├── components/           # TaskCard、Common（MetaTag/EmptyState/…）、Format
    └── theme/                # Color/Theme/Type

app/src/test/java/com/dailylist/app/
├── data/BackupManagerTest.kt        # 5 用例
└── domain/ DateUtilsTest.kt(4) RepeatLogicTest.kt(5) ReminderCalculatorTest.kt(7)
```

## 5. 数据模型

### Task（`tasks` 表）
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | Long | PK autoGenerate |
| title | String | 必填 |
| description | String | 默认 `""` |
| dueDate | String | `yyyy-MM-dd` |
| reminderTimeMinutes | Int? | 当日分钟 0..1439，null=无提醒 |
| reminderAdvanceMinutes | Int | 提前分钟（5/10/15/30） |
| repeatRule | RepeatRule | NONE/DAILY/WEEKLY/MONTHLY |
| repeatDays | Set\<Int\> | 每周重复，1=周一..7=周日 |
| priority | Priority | LOW/NORMAL/HIGH（sortOrder 0/1/2） |
| categoryId | Long? | 无外键，Repository 手动关联 |
| isCompleted / completedAt | Boolean / Long? | 不重复任务的完成态 |
| createdAt / updatedAt | Long | epoch millis |

### TaskOccurrence（`task_occurrences` 表，重复任务按天完成态）
- `taskId`（FK→Task，CASCADE）+ `occurDate`（yyyy-MM-dd）唯一索引；`completedAt`。
- 重复任务某天是否完成 = 该表是否有对应记录，**各天互不覆盖**。

### Category（`categories` 表）
- `id` / `name` / `colorArgb: Int` / `createdAt`。

### 枚举
- `RepeatRule(label)`：NONE「不重复」/ DAILY「每天」/ WEEKLY「每周」/ MONTHLY「每月」。
- `Priority(label, sortOrder)`：LOW「低」0 / NORMAL「普通」1 / HIGH「高」2。

### Settings（DataStore，非 Room）
- `ThemeMode`：SYSTEM/LIGHT/DARK。
- `AppSettings(themeMode, defaultReminderMinutes, defaultReminderAdvanceMinutes)`。
- 键：`theme_mode` / `default_reminder_minutes` / `default_advance_minutes`。

## 6. TaskRepository 公开 API（业务入口）

- 查询：`observeCategories()` / `observeTasksForDate(date)` / `observeBacklog(date)` / `getTask(id)` / `getCategories()` / `isCompletedOn(task, date)`
- 写：`addTask` / `updateTask` / `deleteTask` / `setCompleted(task, onDate, completed)`
- 分类：`addCategory` / `updateCategory` / `deleteCategory` / `seedDefaults()`（无分类时插入「工作/生活/其他」）
- 统计：`monthSummary(year, month)` / `todayStats(date)` / `completedPerDay(start, days)`
- 提醒：`resyncAllReminders()` / `resyncReminder(taskId)`
- 备份：`exportData(): String` / `importData(json): ImportResult`

顶层数据类：`TaskItem(task, completed)`、`DayStatus(hasTask, hasUncompleted)`、`TodayStats(total, completed, rate)`、`ImportResult(...)`。

**关键**：`addTask`/`updateTask` 已内建「落地后 `resyncReminder`」——任何新增/改动的落地入口都应复用它们，无需重写调度逻辑。

## 7. 领域逻辑要点

- `RepeatLogic.isApplicable(task, date)`：NONE=日期相等；DAILY=不早于 dueDate；WEEKLY=不早于且 `repeatDays` 含该日；MONTHLY=不早于且 `dayOfMonth == due.dayOfMonth`。
- `ReminderCalculator.nextFireEpochMillis(task, now, skipDates=emptySet())`：先算 `baseTime = reminderTimeMinutes - advance`，再从 `now` 起逐日搜索（最多 370 天）返回首个未跳过的触发 epoch；无提醒时间或（不重复且已完成/已过期）返回 null。
- `DateUtils`：`timeToMinutes` / `minutesToTime` / `parseDate` / `formatDate`（`yyyy-MM-dd`）。

## 8. 提醒链路

```
AlarmScheduler.schedule(taskId, epoch)  [requestCode = taskId.toInt()]
  → 到点 → ReminderReceiver.onReceive（goAsync + 协程）
      → repo.getTask → 未完成则 NotificationHelper.showTaskReminder
      → 无条件 repo.resyncReminder(taskId)（调度下一次）
BootReceiver（BOOT_COMPLETED/TIMEZONE_CHANGED/TIME_SET）→ resyncAllReminders()
```

- `AlarmScheduler`：S+ 且无精确闹钟权限时用 `setAndAllowWhileIdle`（不精确兜底），否则 `setExactAndAllowWhileIdle`。
- 通知渠道 `CHANNEL_ID = "task_reminders"`（IMPORTANCE_HIGH）；通知点击回 `MainActivity`（带 taskId extra）。

## 9. 构建与运行

### 环境（本机绝对路径）
- JDK 17：`D:\Android\jdk\jdk-17.0.20.1+1`
- SDK：`D:\Android\Sdk`（`local.properties` 的 `sdk.dir`）
- Gradle：`D:\Android\gradle\gradle-8.9`

```powershell
$env:JAVA_HOME='D:\Android\jdk\jdk-17.0.20.1+1'
$env:ANDROID_HOME='D:\Android\Sdk'
$g = 'D:\Android\gradle\gradle-8.9\bin\gradle.bat'
& $g assembleDebug                              # 产物 app/build/outputs/apk/debug/app-debug.apk
& $g testDebugUnitTest                          # 全部单元测试
& $g testDebugUnitTest --tests "com.dailylist.app.domain.RepeatLogicTest"   # 单个测试类
& $g lintDebug                                  # Android Lint（报告 app/build/reports/lint-results-debug.html）
```

> 单个测试类包名：`com.dailylist.app.domain.{DateUtils,RepeatLogic,ReminderCalculator}Test`、`com.dailylist.app.data.BackupManagerTest`。`gradle.properties` 已设 `org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8` + `caching`/`parallel`。

### ⚠️ 非 ASCII 路径（重要）
项目位于 `D:\工作\项目\daily_reminder`（路径含中文）：
- 主构建已用 `gradle.properties` 的 `android.overridePathCheck=true` 规避路径检查，**正常**。
- 但 **Gradle 单元测试 worker** 在 Windows 非 ASCII 路径下会因 classpath 编码错乱报 `ClassNotFoundException`。跑单测需：把项目置于纯 ASCII 路径，或临时在 `settings.gradle.kts` 加 `gradle.beforeProject { layout.buildDirectory.set(ASCII路径) }` 重定位 build 目录。

## 10. 测试

- 共 **21 个纯 JVM 单测**（JUnit4，无 Android 依赖），位于 `app/src/test/`。
- 覆盖：DateUtils(4)、RepeatLogic(5)、ReminderCalculator(7)、BackupManager(5)。
- 依赖：`org.json:json`、`kotlinx-coroutines-test`、`core-testing` 均为 testImplementation。

## 11. 关键坑与约定（非显而易见）

1. **Material3 无 `TimePickerDialog`** → 用 `AlertDialog(onDismiss, title, text={TimePicker(state)}, confirm/dismiss)` 包裹；`MineScreen.kt`、`TaskEditorSheet.kt` 已有实现。
2. 大量 Compose API 是 `@ExperimentalMaterial3Api`：`ModalBottomSheet`/`DatePicker`/`TimePicker`/`remember*PickerState`/`DatePickerDialog` 等 → 加 `@OptIn(ExperimentalMaterial3Api::class)`。
3. `flatMapLatest` 是 `@ExperimentalCoroutinesApi`（CalendarViewModel/MineViewModel 已 OptIn）。
4. **`BackupManager` 手写 JSON 序列化（非反射）**：给 Task/Category/Occurrence **加字段必须同步改 `serialize` 与 `parse` 两处**，否则备份/恢复会丢字段；`VERSION=1`。
5. **`DailyListDatabase` version=1 无 Migration**：新增实体/字段必须升 version + 写 `Migration`，**不得** `fallbackToDestructiveMigration`（会清用户数据）。
6. **已知 bug**：提醒时间落在当日 0:00–0:30 且 `advance > reminderTimeMinutes` 时，`ReminderCalculator` 对「不重复」任务的下次触发时刻会落在到期日当晚而非前一日（跨天边界）。
7. `AlarmScheduler` 的 PendingIntent 唯一键 = `taskId.toInt()`（requestCode），同 receiver + 同 action + 同 requestCode 决定身份。
8. `deleteCategory`：被删分类的任务迁到「其他」，无「其他」则置 null。
9. `importData` 为**追加**策略，分类按 name 去重，任务 id 重映射，occurrence 跟随重映射。
10. 通知在 TIRAMISU+ 且未授权 `POST_NOTIFICATIONS` 时静默跳过（不崩溃）。

## 12. Git 与网络

- 远程：`git@github.com:losted-star/daily_reminder.git`（**SSH**，`main` 分支）。
- SSH 密钥：`~/.ssh/id_ed25519_github`（专用于 GitHub），`~/.ssh/config` 已配 `github.com` 走代理 + 该密钥。
- 本机代理：`http://127.0.0.1:6696`（HTTP）；git 全局 `http.proxy`/`https.proxy` 已设，SSH 走 config 的 `ProxyCommand connect.exe -H`。
- ⚠️ **push/pull 依赖代理在线**；关代理则 GitHub 不可达（本地提交不受影响）。

## 13. 常用命令速查

```powershell
$git = "C:\Program Files\Git\bin\git.exe"
& $git status; & $git add -A; & $git commit -m "..." ; & $git push
& "C:\Program Files\Git\usr\bin\ssh.exe" -T git@github.com   # 测 SSH 认证
```
