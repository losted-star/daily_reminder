# 每日清单 DailyList

一款完全离线的 Android 个人待办 / 提醒应用。目标设备为华为 Mate 40（HarmonyOS 4.2，向下兼容运行 Android APK），因此采用原生 Android 开发，无需 HAP。

- 包名：`com.dailylist.app`
- 版本：`1.0`（versionCode 1）
- 最低系统：Android 8.0（API 26，`minSdk 26`）
- 目标系统：Android 14（`targetSdk 34`）
- 网络：**无**（不申请 INTERNET 权限，数据全部保存在本机）

---

## 一、功能特性

### 今日（首页）
- 问候语 + 当前日期
- 今日进度卡片（完成数 / 总数 + 进度条）
- 今日待办列表，支持按优先级排序切换
- 「待处理」区：逾期未完成的任务自动归入
- 任务卡片：优先级色条、分类圆点、提醒 / 重复图标、完成勾选、删除线
- 长按任务弹出菜单（标记完成 / 编辑 / 删除），删除可撤销
- 右下角 FAB 新建任务

### 任务编辑
- 标题（必填）、日期、分类、优先级（低 / 普通 / 高）
- 提醒：可选时间（24 小时制），支持提前 5 / 10 / 15 / 30 分钟
- 重复：不重复 / 每天 / 每周（自选星期）/ 每月（按日号）
- 备注（可选）

### 日历
- 月视图（周一为起始），可切换月份
- 日期圆点标记：有未完成任务显示主色圆点，全部完成显示描边圆点
- 选中日期下方展示当天任务，可直接勾选 / 编辑 / 删除，支持为指定日期新建任务

### 提醒与重复
- 基于 `AlarmManager`（精确闹钟 + 不精确兜底），熄屏可触发
- 重复任务的完成状态按「天」记录（`TaskOccurrence` 表），各天互不覆盖
- 精确闹钟权限引导；通知权限引导（Android 13+）
- 开机 / 时区变更 / 时间变更后自动对账并恢复提醒（`BootReceiver`）

### 我的
- 统计：今日完成率 + 最近 7 天完成情况柱状图
- 分类管理：内置「工作 / 生活 / 其他」，支持新增 / 编辑 / 删除自定义分类（名称 + 颜色）
- 外观：跟随系统 / 浅色 / 深色
- 默认提醒时间
- 通知权限 / 精确闹钟状态与开启入口，以及华为后台管理（自启动、电池优化）可靠性指引
- 数据备份：导出 / 导入 JSON（导入为「追加」策略，不覆盖已有数据，分类按名称去重）

---

## 二、技术栈

| 分类 | 选型 |
| --- | --- |
| 语言 | Kotlin 2.0.21 |
| UI | Jetpack Compose（BOM 2024.09.03）+ Material 3 + material-icons-extended |
| 架构 | MVVM（ViewModel + StateFlow），手动依赖注入（`ServiceLocator` + `ViewModelProvider.Factory`） |
| 存储 | Room 2.6.1（kapt）+ DataStore Preferences |
| 异步 | Kotlin Coroutines / Flow |
| 导航 | Navigation Compose |
| 提醒 | AlarmManager + BroadcastReceiver（goAsync + 协程） |
| 日期 | java.time（`minSdk 26` 原生支持） |

---

## 三、构建说明

### 环境要求
- JDK 17
- Android SDK：`platforms;android-34`、`build-tools;34.0.0`、`platform-tools`
- Gradle 8.9（AGP 8.5.2）

本机实际环境路径（示例）：
- JDK：`D:\Android\jdk\jdk-17.0.20.1+1`
- SDK：`D:\Android\Sdk`（见 `local.properties` 的 `sdk.dir`）
- Gradle：`D:\Android\gradle\gradle-8.9`

### 构建 Debug APK

```powershell
$env:JAVA_HOME='D:\Android\jdk\jdk-17.0.20.1+1'
$env:ANDROID_HOME='D:\Android\Sdk'
& 'D:\Android\gradle\gradle-8.9\bin\gradle.bat' assembleDebug
```

产物：`app/build/outputs/apk/debug/app-debug.apk`（debug 签名，可直接安装）。项目根目录已附带一份 `DailyList-v1.0-debug.apk`。

### 运行单元测试

```powershell
& 'D:\Android\gradle\gradle-8.9\bin\gradle.bat' testDebugUnitTest
```

> ⚠️ **重要**：本项目位于非 ASCII 路径 `D:\工作\项目\daily_reminder`。主构建已通过 `gradle.properties` 中的 `android.overridePathCheck=true` 规避路径检查；但 **Gradle 单元测试的 worker 进程在 Windows 非 ASCII 路径下会因 classpath 编码问题导致 `ClassNotFoundException`**。运行单元测试时，需将项目置于纯 ASCII 路径，或临时把构建产物重定位到 ASCII 路径（例如在 `settings.gradle.kts` 中 `gradle.beforeProject { layout.buildDirectory.set(...) }`）。此问题不影响编译与 APK 打包。

### 构建 Release APK（可选）

`release` 构建类型默认未配置签名（`isMinifyEnabled = false`）。如需可安装的 release 包，请在 `app/build.gradle.kts` 中配置 `signingConfig` 后执行 `assembleRelease`。

---

## 四、测试结果

共 **21 个单元测试，全部通过**（JUnit 4，纯 JVM，无模拟器依赖）：

| 测试类 | 用例数 | 覆盖内容 |
| --- | --- | --- |
| `DateUtilsTest` | 4 | 分钟 ↔ 时间字符串互转、非法输入、日期序列化往返 |
| `RepeatLogicTest` | 5 | 不重复 / 每天 / 每周 / 每月重复的适用日期判断，短月日号跳过 |
| `ReminderCalculatorTest` | 7 | 无提醒、已完成跳过、当天未来触发、提前分钟、过期顺延、skipDates 跳过 |
| `BackupManagerTest` | 5 | JSON 序列化 / 解析往返、空字段往返、非法备份 / 空标题 / 非法日期拒绝 |

---

## 五、已知限制

1. **非 ASCII 路径下的单元测试**：Windows + 非 ASCII 项目路径会令 Gradle 测试 worker 的 classpath 编码错乱，需置于 ASCII 路径或重定位 build 目录（见「运行单元测试」）。
2. **系统省电策略**：HarmonyOS / 华为 EMUI 的省电策略可能在极端情况下延迟或拦截后台提醒，需用户在系统设置中允许自启动并关闭电池优化（应用内「我的」页提供了指引）。
3. **精确闹钟权限**：`SCHEDULE_EXACT_ALARM` 在部分系统需用户手动授权；未授权时自动降级为 `setAndAllowWhileIdle`（可能不精确）。
4. **凌晨提醒跨天边界**：当提醒时间落在当日 0:00–0:30 且提前分钟数大于提醒时刻（如 00:10 提醒、提前 30 分钟）时，`ReminderCalculator` 对「不重复」任务的下一次触发时刻会落在到期日当晚而非前一日，属于极窄边界场景。
5. **每月重复**：按到期日的「日号」匹配，对不存在的日号（如 31 号）当月自动跳过。
6. **删除分类**：被删分类下的任务会迁移到「其他」分类；若无「其他」则置为无分类。
7. **主题与系统栏**：应用内手动切换浅色 / 深色时，系统状态栏颜色仍跟随系统主题（`enableEdgeToEdge` 默认行为），存在轻微视觉不一致。

---

## 六、项目结构

```
app/src/main/java/com/dailylist/app/
├── data/             # Room 实体、DAO、仓库、DataStore、备份序列化、ServiceLocator
├── domain/           # DateUtils、RepeatLogic、ReminderCalculator（纯逻辑，可单测）
├── reminder/         # AlarmScheduler、NotificationHelper、ReminderReceiver、BootReceiver
├── ui/               # MainViewModel、DailyListRoot、AppViewModelFactory
│   ├── today/        # 今日页
│   ├── calendar/     # 日历页
│   ├── mine/         # 我的页
│   ├── edit/         # 任务编辑底部弹窗
│   └── components/   # TaskCard、格式化工具、通用组件
├── MainActivity.kt
└── DailyListApp.kt   # Application：初始化通知渠道、默认分类、提醒对账
app/src/test/         # 单元测试
```

---

## 七、安装与使用

1. 将 `DailyList-v1.0-debug.apk` 传到设备后直接安装（允许「安装未知来源应用」）。
2. 首次启动自动创建「工作 / 生活 / 其他」三个默认分类。
3. 建议在「我的」页开启通知权限、精确闹钟，并按指引允许自启动 / 后台活动，以保证熄屏提醒。
4. 数据可通过「我的 → 数据备份 → 导出」导出为 JSON，换机后「导入」即可恢复。
